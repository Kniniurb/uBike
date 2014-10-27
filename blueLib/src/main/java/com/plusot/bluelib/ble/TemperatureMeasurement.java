package com.plusot.bluelib.ble;

import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import com.plusot.bluelib.ble.Ble.GattAction;
import com.plusot.bluelib.ble.Ble.LeDevice;
import com.plusot.common.Globals;
import com.plusot.javacommon.util.StringUtil;

public class TemperatureMeasurement implements Measurement{
	private final static String CLASSTAG = TemperatureMeasurement.class.getSimpleName();
	

	@Override
	public BleData onMeasurement(final LeDevice device, final GattAction action, final BluetoothGattCharacteristic characteristic) {
		byte[] bytes = characteristic.getValue();
//		int flag = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
		float celsius = characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_FLOAT, 1);
//		if ((flag & 0x01) != 0) {
//			celsius = (celsius - 32) * 5 / 9;
//		} 
		Log.d(Globals.TAG, CLASSTAG + ".onMeasurement " + StringUtil.toHexString(bytes) + " Celsius = " + celsius);
		BleData data = new BleData();
		data.add(BleDataType.BT_TEMPERATURE, Double.valueOf(celsius));
		return data;
	}

}
