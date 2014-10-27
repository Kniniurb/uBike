package com.plusot.bluelib.ble;

import android.bluetooth.BluetoothGattCharacteristic;

import com.plusot.bluelib.ble.Ble.GattAction;
import com.plusot.bluelib.ble.Ble.LeDevice;

public interface Measurement {
	
	public abstract BleData onMeasurement(final LeDevice device, final GattAction action, final BluetoothGattCharacteristic characteristic);

}
