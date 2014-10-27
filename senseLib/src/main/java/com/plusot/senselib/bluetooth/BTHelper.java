package com.plusot.senselib.bluetooth;

import android.bluetooth.BluetoothDevice;

public class BTHelper {
	public static String getBTName(BluetoothDevice device) {
		if (device == null) return null;
		if (device.getName() == null || device.getName().equals(""))
			return device.getAddress();
		return device.getName();
	}

}
