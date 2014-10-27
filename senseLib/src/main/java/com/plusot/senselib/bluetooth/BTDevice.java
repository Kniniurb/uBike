package com.plusot.senselib.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import com.plusot.common.Globals;
import com.plusot.common.share.LLog;
import com.plusot.common.util.ToastHelper;
import com.plusot.senselib.R;
import com.plusot.senselib.values.Device;
import com.plusot.senselib.values.DeviceListener;
import com.plusot.senselib.values.DeviceType;
import com.plusot.senselib.values.ValueType;

public abstract class BTDevice extends Device implements BTConnectedThread.Listener {
	private final static String CLASSTAG = BTDevice.class.getSimpleName();
	protected String deviceName = null;
	protected BTConnectedThread connectedThread;
	protected long active = 0;


	public BTDevice(final DeviceType deviceType, final String deviceName) {
		super (deviceType);
		this.deviceName = deviceName;
	}

	
	public String getDeviceName() {
		return deviceName;
	}


	public void open(final BluetoothDevice device, final BluetoothSocket socket) {
		address = device.getAddress();
		active = System.currentTimeMillis();
		LLog.d(Globals.TAG, CLASSTAG + ".start: Starting " + getName() + " for " + BTHelper.getBTName(device));
		ToastHelper.showToastLong(Globals.appContext.getString(R.string.first_msg, getName()) + " " + BTHelper.getBTName(device));
		connectedThread = createConnectedThread(socket);
		//new BTConnectedThread(this, socket, null, BTConnectedThread.Type.STRINGS);
		connectedThread.start();
		opened = true;
	}
	
	protected abstract BTConnectedThread createConnectedThread(BluetoothSocket socket);

	@Override
	public void closeIt() {
		if (!opened) return;
		LLog.d(Globals.TAG, CLASSTAG + ".close for " + getName());
		if (connectedThread != null) connectedThread.close();
		connectedThread = null;
	}
	
	

	@Override
	public void onBluetoothTerminate(BTConnectedThread sender) {
		connectedThread = null;
		if (Globals.runMode.isRun()) {
			LLog.d(Globals.TAG, CLASSTAG + ".onBluetoothTerminate: Closed " + deviceName + ", will try to open again in 30 seconds.");
			for (DeviceListener listener: listeners) listener.onDeviceLost();
		}
	}

	@Override
	public void onBluetoothWrite(BTConnectedThread sender, String msg) {
		LLog.d(Globals.TAG, CLASSTAG + ".onBluetoothWrite: " + msg);
	}


	@Override
	public void onBluetoothDeviceName(BTConnectedThread sender,
			String deviceName) {
		this.deviceName = deviceName;
	}

	@Override
	public boolean isActive() {
		if (connectedThread != null && System.currentTimeMillis() - active < 15000) return true;
		return false;
	}

	@Override
	public void fadeOut(ValueType type) {
	}

}
