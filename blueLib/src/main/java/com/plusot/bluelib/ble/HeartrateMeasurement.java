package com.plusot.bluelib.ble;

import android.bluetooth.BluetoothGattCharacteristic;

import com.plusot.bluelib.ble.Ble.GattAction;
import com.plusot.bluelib.ble.Ble.LeDevice;

public class HeartrateMeasurement implements Measurement{

	@Override
	public BleData onMeasurement(final LeDevice device, final GattAction action, final BluetoothGattCharacteristic characteristic) {
		int flag = characteristic.getProperties();
		int format = -1;
		if ((flag & 0x01) != 0) {
			format = BluetoothGattCharacteristic.FORMAT_UINT16;
			//Log.d(Globals.TAG, CLASSTAG + " Heart rate format UINT16.");
		} else {
			format = BluetoothGattCharacteristic.FORMAT_UINT8;
			//Log.d(Globals.TAG, CLASSTAG + " Heart rate format UINT8.");
		}
		final int heartRate = characteristic.getIntValue(format, 1);
		//Log.d(Globals.TAG, CLASSTAG + " Received heart rate: " + heartRate);
		BleData data = new BleData();
		data.add(BleDataType.BT_HEARTRATE, Double.valueOf(heartRate));
		return data;
	}

}
