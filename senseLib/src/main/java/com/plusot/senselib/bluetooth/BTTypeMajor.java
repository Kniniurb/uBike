package com.plusot.senselib.bluetooth;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;

public enum BTTypeMajor {
	COMPUTER(BluetoothClass.Device.Major.COMPUTER),
	MISC(BluetoothClass.Device.Major.MISC),
	PHONE(BluetoothClass.Device.Major.PHONE),
	NETWORKING(BluetoothClass.Device.Major.NETWORKING ),
	AUDIO_VIDEO(BluetoothClass.Device.Major.AUDIO_VIDEO),
	PERIPHERAL(BluetoothClass.Device.Major.PERIPHERAL),
	IMAGING(BluetoothClass.Device.Major.IMAGING),
	WEARABLE(BluetoothClass.Device.Major.WEARABLE),
	TOY(BluetoothClass.Device.Major.TOY),
	HEALTH(BluetoothClass.Device.Major.HEALTH),
	UNCATEGORIZED(BluetoothClass.Device.Major.UNCATEGORIZED);


	private final int type;
	private BTTypeMajor(final int type) {
		this.type = type;
	}
	
	public static BTTypeMajor fromInt(int type) {
		for (BTTypeMajor major: BTTypeMajor.values()) {
			if (major.type == type) return major;
		}
		return null;
	}
	
	public static BTTypeMajor fromDevice(BluetoothDevice device) {
		BluetoothClass bluetoothClass = device.getBluetoothClass();
		if (bluetoothClass != null) return fromInt(bluetoothClass.getMajorDeviceClass());
		return null;
	}

}
