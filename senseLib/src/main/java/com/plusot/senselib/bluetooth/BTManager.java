/*******************************************************************************
 * Copyright (c) 2012 Plusot Biketech
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Peter Bruinink - initial API and implementation and/or initial documentation
 * 
 *******************************************************************************/
package com.plusot.senselib.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import com.plusot.common.Globals;
import com.plusot.common.share.LLog;
import com.plusot.common.util.Watchdog;
import com.plusot.senselib.values.DeviceListener;
import com.plusot.senselib.values.DeviceStub;
import com.plusot.senselib.values.Manager;
import com.plusot.senselib.values.ValueItem;
import com.plusot.senselib.values.ValueType;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

public class BTManager implements Manager, BTConnectThread.Listener, DeviceListener, Watchdog.Watchable {
	private static final String CLASSTAG = BTManager.class.getSimpleName();
	private static final boolean DEBUG = false;
	private BluetoothAdapter adapter = null;
	private EnumMap<BTType, BTDevice> btDevices = new EnumMap<BTType, BTDevice>(BTType.class);
	private BTConnectThread connectThread = null;
	private boolean wasEnabled = true;
	private Queue<String> addressQueue = new LinkedBlockingQueue<String>();
	private Map<String, BTContainer> containers = new HashMap<String, BTContainer>();
	private Set<String> connectAddresses = new HashSet<String>();
	private BTAcceptThread secureAcceptThread = null;
	//private boolean isServer = true;
	//private boolean mayInit = true;
	private Set<BluetoothDevice> bondedDevices = null;

	private class BTCreateException extends Exception {
		private static final long serialVersionUID = 1L;

		public BTCreateException(String msg) {
			super(msg);
		}

	}

	private class BTContainer {
		final BluetoothDevice device;
		final BTType type;
		final String name;
		final BTTypeMajor major;
		public BTContainer(final BluetoothDevice device) throws BTCreateException {
			type = BTType.fromDevice(device);
			if (type == null) throw new BTCreateException("BTType == null");
			name = BTHelper.getBTName(device);
			if (name == null) throw new BTCreateException("Name of BluetoothDevice == null");
			major = BTTypeMajor.fromDevice(device);
			if (major == null) throw new BTCreateException("Device Major of BluetoothDevice == null");
			this.device = device;
		}
	}

	public BTManager() { 
		if (adapter == null) adapter = BluetoothAdapter.getDefaultAdapter();
		Watchdog.getInstance().add(this, 20000);
	}

	@Override
	public synchronized boolean init() {
		if (adapter == null) adapter = BluetoothAdapter.getDefaultAdapter();
		if ((adapter == null || adapter.getState() == BluetoothAdapter.STATE_OFF)) {
			if (Globals.testing.isVerbose()) LLog.d(Globals.TAG, CLASSTAG + ".init: Bluetooh not enabled. Returning without action.");
			return false;
		}
		if (connectThread != null) {
			if (DEBUG) LLog.d(Globals.TAG, CLASSTAG + ".init: Connect still in process.");
			return false;
		}

		if (secureAcceptThread == null) {
			secureAcceptThread = new BTAcceptThread(new BTAcceptThread.Listener() {

				@Override
				public void onBluetoothServerConnect(int what,
						BluetoothDevice device, BluetoothSocket socket,
						BTSocketType socketType) {
					if (secureAcceptThread != null) {
						secureAcceptThread.cancel();
						secureAcceptThread = null;
					}
					LLog.d(Globals.TAG, CLASSTAG + ".init: Accept thread has accepted a connection to " + device.getAddress());
					addressQueue.remove(device.getAddress());
					if (connectAddresses.contains(device.getAddress())) 
						LLog.d(Globals.TAG, CLASSTAG + ".init: Accept neglect connections to " + device.getAddress() + " as it is already connecting to this address as client.");
					else
						onBluetoothConnect(device, socket, socketType);
				}

				@Override
				public void onTerminate() {
					secureAcceptThread = null;
				}
			}, adapter, true);
			secureAcceptThread.start();
		}

		if (addressQueue.size() <= 0) {
			//if (bondedDevices == null) 
			bondedDevices = adapter.getBondedDevices();
			if (bondedDevices.size() == 0) {
				if (DEBUG) LLog.d(Globals.TAG, CLASSTAG + ".init: No paired devices found");

			} else for (BluetoothDevice device : bondedDevices) try {
				BTContainer container = new BTContainer(device);
				containers.put(device.getAddress(), container);
				if (DEBUG) LLog.d(Globals.TAG, CLASSTAG + ".init: " + container.major + " at: " + device.getAddress() + " with name: " +  container.name + ".");
				if ((btDevices.get(container.type) == null || !btDevices.get(container.type).isActive()) && !addressQueue.contains(device.getAddress())) 
					addressQueue.add(device.getAddress());
			} catch (BTCreateException e) {
				LLog.d(Globals.TAG, CLASSTAG + " Cound not create container for bluetooth device:" + e.getMessage());
			}


		}
		if (addressQueue.size() > 0) {
			String address  = addressQueue.poll(); 
			BTContainer container = containers.get(address);
			if (container != null) {
				connectAddresses.add(address);
				if (DEBUG) LLog.d(Globals.TAG, CLASSTAG + ".init: Trying to connect to Bluetooth device: " + address + " " +  container.name);
				connectThread = new BTConnectThread(this, adapter, container.device);
				connectThread.start();
			}
		}  else {
			if (DEBUG) LLog.d(Globals.TAG, CLASSTAG + ".init: No (other) Bluetooth device found to connect to, starting search in 20 seconds again");
			//mayInit = true;
		} 
		return true;
	}

	private BTDevice getDevice(BluetoothDevice device) {
		BTType type = BTType.fromDevice(device);
		if (type == null) return null;
		return btDevices.get(type);	
	}

	private BTDevice getDevice(BluetoothDevice bluetoothDevice, boolean mayCreate) {
		BTType type = BTType.fromDevice(bluetoothDevice);
		if (type == null) return null;
		BTDevice device = btDevices.get(type);
		String devName = BTHelper.getBTName(bluetoothDevice);
		if (devName == null) return null;
		if (device == null && mayCreate) { 
			switch (type) {
			case KESTREL: btDevices.put(type, device = new KestrelDevice(devName)); break;
			case HXM: btDevices.put(type, device = new HxmDevice(devName)); break;
			case BTCHAT: btDevices.put(type, device = new BTChatDevice(devName)); break;
			default: return null;
			}
			device.addListener(this);
		}
		return device;
	}

	@Override
	public synchronized void onBluetoothConnect(BluetoothDevice device, BluetoothSocket socket, BTSocketType socketType) {
		connectThread = null; 
		connectAddresses.remove(device.getAddress());
		BTDevice btDevice = getDevice(device, true);
		if (btDevice != null && !btDevice.isActive()) {
			LLog.d(Globals.TAG, CLASSTAG + ".onBluetoothConnected: Opening: " + btDevice.getName());
			btDevice.open(device, socket);
		}
	}

	@Override
	public void onBluetoothConnectFailed(BluetoothDevice device) {
		synchronized(this) { connectThread = null; }
		connectAddresses.remove(device.getAddress());
		BTDevice btDevice = null;
		String devName = BTHelper.getBTName(device);
		if (devName == null) return;
		btDevice = getDevice(device);
		if (btDevice != null && !btDevice.isActive()) {
			if (DEBUG) LLog.d(Globals.TAG, CLASSTAG + ".onBluetoothConnected: Connect failed to : " + devName + ", searching again in 30 seconds");
			btDevice.closeIt();
			//mayInit = true;
		} else {
			if (DEBUG) LLog.d(Globals.TAG, CLASSTAG + ".onBluetoothConnected: Connect failed to : " + devName);
		}
	}

	@Override
	public void destroy() {
		LLog.d(Globals.TAG, CLASSTAG + ".shutDown: Closing BluetoothManager");
		if (adapter.isDiscovering())adapter.cancelDiscovery();
		Watchdog.getInstance().remove(this);

		synchronized(this) {
			if (connectThread != null) connectThread.cancel();
			connectThread = null;

			if (secureAcceptThread != null) secureAcceptThread.cancel();
			secureAcceptThread = null;

			//			if (mInsecureAcceptThread != null) {
			//				mInsecureAcceptThread.cancel();
			//				mInsecureAcceptThread = null;
			//			}
		}

		BTAcceptThread.setStateDisconnected();

		for (BTDevice device: btDevices.values()) device.closeIt();
		if (adapter != null && !wasEnabled && adapter.getState() == BluetoothAdapter.STATE_ON) {
			LLog.d(Globals.TAG, CLASSTAG + ".shutDown: Switching Bluetooth off (back to original state)");
			adapter.disable();
		}
	}

	@Override
	public void onStart() {
	}

	@Override
	public void onStop() {
	}

	@Override
	public void onResume() {
	}

	public void write(String value) {
		BTDevice device;
		if ((device = btDevices.get(BTType.BTCHAT)) != null) ((BTChatDevice)device).write(value);
	}

	@Override
	public void onWatchdogCheck(long arg0) {
		if (DEBUG) LLog.d(Globals.TAG,  CLASSTAG + ".onWatchdogCheck");
		//if (mayInit) {
		if (Globals.runMode.isRun()) { // && System.currentTimeMillis() - startTime < 3600000) {
			//				LLog.d(Globals.TAG, CLASSTAG + ".onWake: Starting again");
			init();
		} else if (DEBUG)
			LLog.d(Globals.TAG, CLASSTAG + ".onWatchdogCheck: Not starting BT search.");
		//}
	}

	@Override
	public void onWatchdogClose() {
	}

	@Override
	public void onDeviceValueChanged(DeviceStub device,
			ValueType valueType, ValueItem valueItem, boolean isNew) {
	}

	@Override
	public void onStreamFinished() {
	}

	@Override
	public void onDeviceLost() {
		if (Globals.runMode.isRun()) init();
	}

}
