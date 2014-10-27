package com.plusot.bluelib.ble;

import android.bluetooth.BluetoothGattCharacteristic;

import com.plusot.bluelib.ble.Ble.GattAction;
import com.plusot.bluelib.ble.Ble.LeDevice;

public class SpeedCadenceMeasurement implements Measurement{
//	private final static String CLASSTAG = SpeedCadenceMeasurement.class.getSimpleName();
	private int prevWheelRevolutions = -1;
	private int prevCrankRevolutions = -1;
	private int prevWheelEventTime = -1;
	private int prevCrankEventTime = 1;
	private double wheelRpm = 0;
	private double crankRpm = 0;


	@Override
	public BleData onMeasurement(final LeDevice device, final GattAction action, final BluetoothGattCharacteristic characteristic) {
//		byte[] bytes = characteristic.getValue();
//		Log.d(Globals.TAG, CLASSTAG + "Received CSC " + StringUtil.toHexString(bytes));
		int flag = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
		boolean hasWheelRevolutions = false;
		boolean hasCrankRevolutions = false;
		if ((flag & 0x01) != 0) hasWheelRevolutions = true;
		if ((flag & 0x02) != 0) hasCrankRevolutions = true;
		int index = 1;
		BleData data = new BleData();
		if (hasWheelRevolutions) {
			final int wheelRevolutions = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, index);
			index += 4;
			final int wheelEventTime = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, index);
			index += 2;
//			Log.d(Globals.TAG, CLASSTAG + " Received wheel revolutions: " + wheelRevolutions + ", " + Format.format(wheelEventTime, 3));
			if (prevWheelRevolutions != -1 && prevWheelEventTime != wheelEventTime) {
				int divRevolutions = (int)((4294967296L + wheelRevolutions - prevWheelRevolutions) % 4294967296L);
				double divTime = 1.0 / 1024 * ((65536 + wheelEventTime - prevWheelEventTime) % 65536);
				wheelRpm *= 0.8;
				wheelRpm += 0.2 * 60.0 * divRevolutions / divTime;

				data.add(BleDataType.BT_WHEELREVOLUTIONS, wheelRpm);
			} else if (prevWheelEventTime == wheelEventTime && prevWheelRevolutions == wheelRevolutions) {
				data.add(BleDataType.BT_WHEELREVOLUTIONS, 0.0);
			}
			prevWheelEventTime = wheelEventTime;
			prevWheelRevolutions = wheelRevolutions;


		}
		if (hasCrankRevolutions) {
			final int crankRevolutions = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, index);
			index += 2;
			final int crankEventTime = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, index);
//			Log.d(Globals.TAG, CLASSTAG + " Received crank revolutions: " + crankRevolutions + ", " + Format.format(crankEventTime, 3));
			if (prevCrankRevolutions != -1 && prevCrankEventTime != crankEventTime) {
				int divRevolutions = (int)((4294967296L + crankRevolutions - prevCrankRevolutions) % 4294967296L);
				double divTime = 1.0 / 1024 * ((65536 + crankEventTime - prevCrankEventTime) % 65536);
				crankRpm *= 0.8;
				crankRpm += 0.2 * 60.0 * divRevolutions / divTime;
				data.add(BleDataType.BT_CRANKREVOLUTIONS, crankRpm);
			} else if (prevCrankEventTime == crankEventTime && prevCrankRevolutions == crankRevolutions) {
				data.add(BleDataType.BT_CRANKREVOLUTIONS, 0.0);
			}
			prevCrankEventTime = crankEventTime;
			prevCrankRevolutions = crankRevolutions;

		}
		return data;
	}

}
