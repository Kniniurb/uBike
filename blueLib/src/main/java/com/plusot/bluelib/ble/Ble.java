/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.plusot.bluelib.ble;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.plusot.common.Globals;
import com.plusot.common.share.LLog;
import com.plusot.common.util.Watchdog;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class Ble implements Watchdog.Watchable{
	private final static String CLASSTAG = Ble.class.getSimpleName();
	private final static String FILLTAG = "       ";
	private static Ble bluetoothLe;
	public static Map<String, String> paired = new HashMap<String, String>();
	
	private BluetoothManager mBluetoothManager;
	private BluetoothAdapter mBluetoothAdapter;
	private Map<String, LeDevice> devices = new HashMap<String, LeDevice>();
	private Map<BluetoothGatt, LeDevice> gatts = new HashMap<BluetoothGatt, LeDevice>();
	private final Context context;
	private EnumMap<GattAttribute, Measurement> measurements = new EnumMap<GattAttribute, Measurement>(GattAttribute.class);

	public enum ConnectState {
		STATE_DISCONNECTED,
		STATE_CONNECTING,
		STATE_CONNECTED;
	}

	public enum GattAction {
		ACTION_GATT_CONNECTED ,
		ACTION_GATT_DISCONNECTED,
		ACTION_GATT_SERVICES_DISCOVERED,
		ACTION_DATA_AVAILABLE;
	}

	public class LeDevice {
		private ConnectState state = ConnectState.STATE_DISCONNECTED;
		private final BluetoothGatt gatt;
		private final String address;
		private final String name;
		private boolean servicesDiscovered = false;
		private BleData data = new BleData();
		private EnumMap<GattAttribute, BluetoothGattCharacteristic> characteristics = new EnumMap<GattAttribute, BluetoothGattCharacteristic>(GattAttribute.class);

		public LeDevice(final String address, final String name, final BluetoothGatt gatt) {
			this.gatt = gatt;
			this.address = address;
			this.name = name;
		}
		public ConnectState getState() {
			return state;
		}
		public void setState(ConnectState state) {
			this.state = state;
		}
		public String getAddress() {
			return address;
		}
		public void addCharacteristic(GattAttribute attr, BluetoothGattCharacteristic characteristic) {
			characteristics.put(attr, characteristic);
		}
		public BluetoothGattCharacteristic getCharacteristic(GattAttribute attr) {
			return characteristics.get(attr);	
		}
		
		public int getBatteryLevel() {
			Double value;
			if ((value = data.get(BleDataType.BT_BATTERYLEVEL)) == null) return -1;

			return value.intValue();
		}
	}

	private final Set<Listener> listeners = new HashSet<Listener>();

	public interface Listener {
		public void onData(String address, GattAction action, BleData data);
		public void onRawData(String address, GattAction action, byte[] data);
	}

	private Ble() {
		this.context = Globals.appContext;
		Watchdog.getInstance().add(this, 5000);
	}

	public static Ble getInstance() {
		if (bluetoothLe == null) {
			bluetoothLe = new Ble();
			bluetoothLe.initialize();
		}
		return bluetoothLe;

	}

	public void addListener(Listener listener) {
		listeners.add(listener);
	}

	public void removeListener(Listener listener) {
		listeners.remove(listener);
	}

	private void fire(String address, GattAction action) {
		for (Listener listener: listeners) listener.onData(address, action, null);
	}

	// Implements callback methods for GATT events that the app cares about.  For example,
	// connection change and services discovered.
	private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
			LeDevice leDevice = gatts.get(gatt);
			if (newState == BluetoothProfile.STATE_CONNECTED) {
				if (leDevice != null) {
					leDevice.setState(ConnectState.STATE_CONNECTED);
				}
				fire(gatt.getDevice().getAddress(), GattAction.ACTION_GATT_CONNECTED);
				Log.i(Globals.TAG, CLASSTAG + ".onConnectionStateChange Connected to GATT server for " + gatt.getDevice().getAddress());
				// Attempts to discover services after successful connection.
				Log.i(Globals.TAG, CLASSTAG + ".onConnectionStateChange Attempting to start service discovery:" + gatt.discoverServices() + " for " + gatt.getDevice().getAddress());

			} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
				if (leDevice != null) leDevice.setState(ConnectState.STATE_DISCONNECTED);
				Log.i(Globals.TAG, CLASSTAG + ".onConnectionStateChange Disconnected from GATT server for " + gatt.getDevice().getAddress());
				fire(gatt.getDevice().getAddress(), GattAction.ACTION_GATT_DISCONNECTED);
			}
		}

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			LeDevice leDevice = gatts.get(gatt);
			if (leDevice == null) return;
			if (status == BluetoothGatt.GATT_SUCCESS) {
				handleGattServices(leDevice, getSupportedGattServices(leDevice.address));
				fire(leDevice.address, GattAction.ACTION_GATT_SERVICES_DISCOVERED);
				leDevice.servicesDiscovered = true;
			} else {
				Log.w(Globals.TAG, CLASSTAG + ".onServicesDiscovered received: " + status);
			}
		}

		@Override
		public void onCharacteristicRead(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic,
				int status) {
			LeDevice leDevice = gatts.get(gatt);
			if (leDevice == null) return;
			//if (status == BluetoothGatt.GATT_SUCCESS) {
			fire(leDevice, GattAction.ACTION_DATA_AVAILABLE, characteristic);
			//}
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic) {
//			Log.d(Globals.TAG, CLASSTAG + ".onCharacteristicChanged");
			LeDevice leDevice = gatts.get(gatt);
			if (leDevice == null) return;
			fire(leDevice, GattAction.ACTION_DATA_AVAILABLE, characteristic);
		}
	};

	private void handleGattServices(final LeDevice device, List<BluetoothGattService> gattServices) {
		Log.d(Globals.TAG,  CLASSTAG + ".handleGattServices for " + device.address);
		for (BluetoothGattService gattService : gattServices) {
			List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
			UUID uuid = gattService.getUuid();
			Log.d(Globals.TAG,  FILLTAG + " Service = " + uuid + ", " + GattAttribute.stringFromUUID(uuid, GattAttributeType.SERVICE));
			for (BluetoothGattCharacteristic characteristic : gattCharacteristics) {				
				uuid = characteristic.getUuid();
				GattAttribute attr = GattAttribute.fromUUID(uuid);
				device.addCharacteristic(attr, characteristic);
				Log.d(Globals.TAG,  FILLTAG + " Characteristic = " + uuid + ", " + GattAttribute.stringFromUUID(uuid, GattAttributeType.CHARACTERISTIC));
				if (attr.isReadable()) startRead(device.address, characteristic);
			}      
		}    
	}

	public void startRead(String address, GattAttribute attr) {
		LeDevice leDevice = devices.get(address);
		if (leDevice == null) return;
		startRead(leDevice.address, leDevice.getCharacteristic(attr));
	}

	private void startRead(String address, BluetoothGattCharacteristic characteristic) {
		if (characteristic == null || address == null) {
			Log.d(Globals.TAG,  CLASSTAG + ".startRead address or characteristic = null");
			return;
		}
		Log.d(Globals.TAG,  CLASSTAG + ".startRead for " + address + ": " + GattAttribute.fromUUID(characteristic.getUuid()));

		final int prop = characteristic.getProperties();
		if ((prop | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
			// If there is an active notification on a characteristic, clear
			// it first so it doesn't update the data field on the user interface.
			//			if (notifyCharacteristic != null) {
			//				Log.d(Globals.TAG,  CLASSTAG + ".startHrmRead: Switching previous notification off");
			//				ble.setCharacteristicNotification(address, notifyCharacteristic, false);
			//				notifyCharacteristic = null;
			//			}
			Log.d(Globals.TAG,  CLASSTAG + ".startRead: Read characteristic");
			readCharacteristic(address, characteristic);
		}
		if ((prop | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
			Log.d(Globals.TAG,  CLASSTAG + ".startRead: Notify");
			//			notifyCharacteristic = hrmCharacteristic;
			setCharacteristicNotification(address, characteristic, true);
		}
	}

	private void fire(final LeDevice device, final GattAction action,
			final BluetoothGattCharacteristic characteristic) {
		GattAttribute attr = GattAttribute.fromUUID(characteristic.getUuid());
		Measurement measurement =  measurements.get(attr);
		if (measurement == null) switch(attr) {
		case CSC_MEASUREMENT:
            measurement = new SpeedCadenceMeasurement();
            LLog.d(Globals.TAG, CLASSTAG + " CSC Measurement");
            break;
		case HEARTRATE_MEASUREMENT: measurement = new HeartrateMeasurement(); break;
		case BATTERY_LEVEL: measurement = new BatteryLevelMeasurement(); break;
		case TEMPERATURE_MEASUREMENT: measurement = new TemperatureMeasurement(); break;
		default:
			byte[] bytes = characteristic.getValue();
			for (Listener listener: listeners) listener.onRawData(device.address, action, bytes);
			break;
		}
		if (measurement != null) {
			measurements.put(attr, measurement);
			BleData data = measurement.onMeasurement(device, action, characteristic);
			device.data.merge(data);
			for (Listener listener: listeners) listener.onData(device.address, action, data);	
		}
	}

	public boolean initialize() {
		if (mBluetoothManager == null) {
			mBluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
			if (mBluetoothManager == null) {
				Log.e(Globals.TAG, CLASSTAG + ".initialize Unable to initialize BluetoothManager.");
				return false;
			}
		}

		mBluetoothAdapter = mBluetoothManager.getAdapter();
		if (mBluetoothAdapter == null) {
			Log.e(Globals.TAG, CLASSTAG + ".initialize Unable to obtain a BluetoothAdapter.");
			return false;
		}
		return true;
	}

	public boolean connect(final String address) {
		if (mBluetoothAdapter == null || address == null) {
			Log.w(Globals.TAG, CLASSTAG + ".connect BluetoothAdapter not initialized or unspecified address for " + address);
			return false;
		}

		// Previously connected device.  Try to reconnect.
		LeDevice leDevice;
		if ((leDevice = devices.get(address)) != null) {
			Log.d(Globals.TAG, CLASSTAG + ".connect Trying to use an existing BluetoothGatt for connection to " + address);
			if (leDevice.gatt.connect()) {
				leDevice.setState(ConnectState.STATE_CONNECTING);
				Log.d(Globals.TAG, CLASSTAG + ".connecting to " + address);
				return true;
			} else {
				return false;
			}
		}

		BluetoothDevice device = null;
		try {
			device = mBluetoothAdapter.getRemoteDevice(address);
		} catch(IllegalArgumentException e) {
			Log.e(Globals.TAG, CLASSTAG + ".connect Illegal argument exception with address = " + address);
			
		}
		if (device == null) {
			Log.w(Globals.TAG, CLASSTAG + ".connect Device not found. Unable to connect to " + address);
			return false;
		}
		// We want to directly connect to the device, so we are setting the autoConnect
		// parameter to false.
		BluetoothGatt gatt = device.connectGatt(context, false, gattCallback);
		leDevice = new LeDevice(address, gatt.getDevice().getName(), gatt);
		Log.d(Globals.TAG, CLASSTAG + ".connect Trying to create a new connection to " + address);
		devices.put(address, leDevice);
		gatts.put(gatt, leDevice);
		leDevice.setState(ConnectState.STATE_CONNECTING);
		return true;
	}

	/**
	 * Disconnects an existing connection or cancel a pending connection. The disconnection result
	 * is reported asynchronously through the
	 * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
	 * callback.
	 */
	public void disconnect(String address) {
		LeDevice leDevice;
		if (mBluetoothAdapter == null || (leDevice = devices.get(address)) == null) {
			Log.w(Globals.TAG, CLASSTAG + ".disconnect BluetoothAdapter not initialized");
			return;
		}
		leDevice.gatt.disconnect();
	}

	/**
	 * After using a given BLE device, the app must call this method to ensure resources are
	 * released properly.
	 */
	public void close(String address) {
		LeDevice leDevice;
		if ((leDevice = devices.get(address)) == null) {
			return;
		}
		if (leDevice.gatt != null) {
			leDevice.gatt.close();
			gatts.remove(leDevice.gatt);
		}
		devices.remove(address);
	}

	public void closeAll() {
		for (LeDevice leDevice: devices.values()) {
			leDevice.gatt.close();
		}
		devices.clear();
		gatts.clear();
	}

	/**
	 * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
	 * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
	 * callback.
	 *
	 * @param characteristic The characteristic to read from.
	 */
	public void readCharacteristic(String address, BluetoothGattCharacteristic characteristic) {
		LeDevice leDevice;
		if (mBluetoothAdapter == null || (leDevice = devices.get(address)) == null) {
			Log.w(Globals.TAG, CLASSTAG + "readCharacteristic BluetoothAdapter not initialized for " + address);
			return;
		}
		GattAttribute attr = GattAttribute.fromUUID(characteristic.getUuid());
		
		Log.d(Globals.TAG, CLASSTAG + ".readCharacteristic " + attr+ " for " + address);
		if (!leDevice.gatt.readCharacteristic(characteristic)) {
			Log.d(Globals.TAG, CLASSTAG + ".readCharacteristic Could not start " + attr + " for " + address);
		}
	}

	/**
	 * Enables or disables notification on a give characteristic.
	 *
	 * @param characteristic Characteristic to act on.
	 * @param enabled If true, enable notification.  False otherwise.
	 */
	public void setCharacteristicNotification(String address, BluetoothGattCharacteristic characteristic,
			boolean enabled) {
		LeDevice leDevice;
		if (mBluetoothAdapter == null || (leDevice = devices.get(address)) == null) {
			Log.w(Globals.TAG, CLASSTAG + ".setCharacteristicNotification BluetoothAdapter not initialized for " + address);
			return;
		}
		GattAttribute attr = GattAttribute.fromUUID(characteristic.getUuid());
		Log.d(Globals.TAG, CLASSTAG + ".setCharacteristicNotification " + attr + " for " + address + " to " + enabled);
		leDevice.gatt.setCharacteristicNotification(characteristic, enabled);

		GattAttribute descriptor = null;
		if ((descriptor = attr.getDescriptor()) != null) {
			Log.d(Globals.TAG, CLASSTAG + ".setCharacteristicNotification: Set descriptor");
			BluetoothGattDescriptor gattDescriptor = characteristic.getDescriptor(descriptor.getUuid());
			gattDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
			leDevice.gatt.writeDescriptor(gattDescriptor);
		} 
	}

	/**
	 * Retrieves a list of supported GATT services on the connected device. This should be
	 * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
	 *
	 * @return A {@code List} of supported services.
	 */
	public List<BluetoothGattService> getSupportedGattServices(String address) {
		LeDevice leDevice;
		if (mBluetoothAdapter == null || (leDevice = devices.get(address)) == null) {
			Log.w(Globals.TAG, CLASSTAG + ".getSupportedGattServices BluetoothAdapter not initialized for " + address);
			return null;
		}
		return leDevice.gatt.getServices();
	}

	public ConnectState getConnectState(String address) {
		LeDevice leDevice;
		if ((leDevice = devices.get(address)) == null) {
			return ConnectState.STATE_DISCONNECTED;
		}

		return leDevice.getState();
	}

	public int getBatteryLevel(String address) {
		LeDevice leDevice;
		if ((leDevice = devices.get(address)) == null) {
			return -1;
		}
		return leDevice.getBatteryLevel();
		
	}
	
	public String getName(String address) {
		LeDevice leDevice;
		if ((leDevice = devices.get(address)) == null) {
			return "";
		}
		return leDevice.name;
		
	}
	
	public boolean hasAddress(String address) {
		if (devices.get(address) == null) return false;
		return true;
	}
	
	public int getCount() {
		return devices.size();
	}

	public boolean isServicesDiscovered(String address) {
		LeDevice leDevice;
		if ((leDevice = devices.get(address)) == null) {
			return false;
		}

		return leDevice.servicesDiscovered;
	}

	@Override
	public void onWatchdogCheck(long count) {
		for (LeDevice leDevice : devices.values()) {
			BluetoothGattCharacteristic characteristic;
			if (leDevice.getBatteryLevel() == -1 && (characteristic = leDevice.getCharacteristic(GattAttribute.BATTERY_LEVEL)) != null) {
				startRead(leDevice.address, characteristic);
			}
		}

	}

	@Override
	public void onWatchdogClose() {
		Log.d(Globals.TAG, CLASSTAG + ".onWatchdogClose");
		closeAll();
		bluetoothLe = null;

	}
	
	public Set<String> getAddresses() {
		return devices.keySet();
	}


}
