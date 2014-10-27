package com.plusot.bluelib.ble;

import android.bluetooth.BluetoothGattCharacteristic;

import com.plusot.bluelib.ble.Ble.GattAction;
import com.plusot.bluelib.ble.Ble.LeDevice;

public class BatteryLevelMeasurement implements Measurement{

	@Override
	public BleData onMeasurement(final LeDevice device, final GattAction action, final BluetoothGattCharacteristic characteristic) {
		final int batteryLevel = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
		BleData data = new BleData();
		data.add(BleDataType.BT_BATTERYLEVEL, Double.valueOf(batteryLevel));
		return data;
	}

}
