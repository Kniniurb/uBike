package com.plusot.senselib.bluetooth;

import android.bluetooth.BluetoothSocket;

import com.plusot.common.Globals;
import com.plusot.common.share.LLog;
import com.plusot.common.share.SimpleLog;
import com.plusot.common.share.SimpleLogType;
import com.plusot.javacommon.util.Format;
import com.plusot.javacommon.util.StringUtil;
import com.plusot.javacommon.util.TimeUtil;
import com.plusot.senselib.SenseGlobals;
import com.plusot.senselib.ant.DeviceManufacturer;
import com.plusot.senselib.values.DeviceType;
import com.plusot.senselib.values.ValueType;

public class HxmDevice extends BTDevice {
	private static final String CLASSTAG = HxmDevice.class.getSimpleName();
	
	private int pFirmwareId = 0;
	private int pHardwareId = 0;
	private int pCharge = 0;
	private int pBeat = -1;
	private int pIntDistance = 0;
	private float totDistance = 0f;
	private float pSpeed = -1f;

	private int pStrides = -1;
	private int totStrides = 0;
	private long lastStridesTime = -1;

	public HxmDevice(final String deviceName) {
		super(DeviceType.ZEPHYRHEARTRATEMONITOR, deviceName);	
		manufacturer = DeviceManufacturer.ZEPHYR;
		productId = "HxM BT";
	}

	private int crc8PushByte(int crc, final int ch) {
		crc ^= ch;
		for (int i = 0; i < 8; i++) {
			if ((crc & 1) == 1) {
				crc >>>= 1; crc ^= 0x8C;
			} else {
				crc >>>= 1;
			}
		} 
		return crc;
	}

	private int crc8(byte[] block, int pos, int count) {
		int crc = 0;
		for (int i = 0; i < Math.min(block.length - pos, count); i++) {
			crc = crc8PushByte(crc, 0xFF & block[pos + i]);
		}
		return crc;
	}

	private int getInt16(byte[] msg, int pos) {
		return (0xFF & msg[pos]) + ((0xFF & msg[pos + 1]) << 8);	
	}


	private String getAscii(byte[] msg, int pos) {
		return "" + (char)msg[pos] + (char)msg[pos + 1];	
	}

	private int getInt8(byte[] msg, int pos) {
		return (0xFF & msg[pos]);	
	}

	@Override
	public void onBluetoothReadBytes(BTConnectedThread sender, byte[] msg) {
		active = System.currentTimeMillis();
		if (msg.length < 6) 
			LLog.e(Globals.TAG, CLASSTAG + ".onBluetoothReadBytes: Msg too short (" + StringUtil.toHexString(msg) + ")!!!!!!"); 
		else {
			long now = System.currentTimeMillis();
			int crc = crc8(msg, 3, msg.length - 5);
			int crc2 = (0xFF & msg[msg.length - 2]);
			if (crc != crc2) LLog.d(Globals.TAG, CLASSTAG + ".onBluetoothReadBytes: CRC failed (" + crc + ", " + crc2 +  ")!!!!!!"); 

			if ((0xFF & msg[1]) == 0x26 && (0xFF & msg[2]) == 55 && msg.length >= 60) {
				int firmwareId = getInt16(msg, 3);
				String firmwareVersion = getAscii(msg, 5);
				int hardwareId = getInt16(msg, 7);
				String hardwareVersion = getAscii(msg, 9);
				int charge = getInt8(msg, 11);
				int hr = getInt8(msg, 12);
				int beat = getInt8(msg, 13);
				int deltaBeat = 14;
				if (pBeat != -1) deltaBeat = Math.min((0xFF + beat - pBeat) % 0xFF, 14);
				int stamp = 0;
				int prevStamp = getInt16(msg, 14);
				int[] intervals = new int[14];
				for (int i = 0; i < 14; i++) {
					stamp = getInt16(msg, 16 + i * 2);
					intervals[i] = (0xFFFF + prevStamp - stamp) %  0xFFFF;
					if (intervals[i] == 0 && i < deltaBeat) deltaBeat = i; 
					prevStamp = stamp;
				}
				int intDistance = getInt16(msg, 50);
				float speed = 1.0f / 256f * getInt16(msg, 52);
				int strides = getInt8(msg, 54);
				StringBuilder result = new StringBuilder();
				result.append('{');
				if (pFirmwareId != firmwareId) {
					result.append("\"firmware\":\"").append("9500." + firmwareId + ".V" + firmwareVersion).append("\",");
					softwareVersion = "9500." + firmwareId + ".V" + firmwareVersion;
				}
				if (pHardwareId != hardwareId) {
					result.append("\"hardware\":\"").append("9800." + hardwareId + ".V" + hardwareVersion).append("\",");
					revision = "9800." + hardwareId + ".V" + hardwareVersion;
				}
				if (pCharge != charge) {
					result.append("\"charge\":").append(charge).append(",");
					battery = "" + charge + "%";
				}
				result.append("\"hr\":").append(hr).append(",");
				result.append("\"beat\":").append(beat).append(",");
				int totalInter = 0;
				if (deltaBeat > 0) {
					result.append("\"inter\":[");
					for (int i = 0; i < deltaBeat; i++) {
						result.append(intervals[i]);
						totalInter += intervals[i];
						if (i < deltaBeat - 1) result.append(",");
					}
					result.append("],");
				}
				if (pIntDistance != intDistance) {
					int id = (4096 + intDistance - pIntDistance) % 4096;
					totDistance += 1.0f / 16f * id; 
					result.append("\"distance\":").append(Format.format(totDistance, 1)).append(",");
				}
				if (pSpeed != speed) {
					result.append("\"speed\":").append(speed).append(",");
					if (SenseGlobals.lastLocation == null || Math.abs(now - SenseGlobals.lastLocationTime) > 60000 || SenseGlobals.lastLocation.getSpeed() < 0.9f * speed)
						fireOnDeviceValueChanged(ValueType.SPEED, (double) (speed), now);
				}
				if (pStrides != strides && pStrides != -1) {
					int delta = (128 + strides - pStrides) % 128;
					totStrides += delta;
					result.append("\"strides\":").append(totStrides).append(",");
					fireOnDeviceValueChanged(ValueType.STEPS, delta, now);
					if (lastStridesTime != -1)
						fireOnDeviceValueChanged(ValueType.CADENCE, 60000.0 * delta / (now - lastStridesTime), now);
					lastStridesTime = now;	

				}
				if (totDistance > 1000)
					rawValue = "" + hr + " bpm\n" + Format.format(totDistance / 1000, 0) + " km\n" + Format.format(3.6 * speed, 1) + " kph\n" + totStrides; 
				else
					rawValue = "" + hr + " bpm\n" + Format.format(totDistance, 0) + " m\n" + Format.format(3.6 * speed, 1) + " kph\n" + totStrides + " strides"; 
				result.append("\"time\":\"" + TimeUtil.formatTime(now) + "\"");
				result.append('}');

				if (SenseGlobals.activity.equals(SenseGlobals.ActivityMode.RUN) && SenseGlobals.hxmTesting) SimpleLog.getInstance(SimpleLogType.JSON, "hxm").log(result.toString());
				//else
				//	LLog.d(Globals.TAG, CLASSTAG + ".HxmDevice.onBluetoothReadBytes: " + result.toString());	
				pCharge = charge;
				pHardwareId = hardwareId;
				pFirmwareId = firmwareId;
				pBeat = beat;
				pStrides = strides;
				pSpeed = speed;
				pIntDistance = intDistance;
				for (int i = deltaBeat - 1; i >=  0; i--) {
					fireOnDeviceValueChanged(ValueType.PULSEWIDTH, intervals[i], now - totalInter);
					totalInter -= intervals[i];
				}
				fireOnDeviceValueChanged(ValueType.HEARTRATE, hr, now);

			} else
				LLog.d(Globals.TAG, CLASSTAG + ".onBluetoothReadBytes: " + StringUtil.toHexString(msg));

		}
	}

	@Override
	public void onBluetoothRead(BTConnectedThread sender, String msg) {
		active = System.currentTimeMillis();
		LLog.d(Globals.TAG, CLASSTAG + ".onBluetoothRead: " + StringUtil.toHexString(msg.getBytes()));
	}

	@Override
	protected BTConnectedThread createConnectedThread(BluetoothSocket socket) {
		return new BTConnectedThread(this, socket, null, BTConnectedThread.Type.WITH_STARTENDBYTE, new byte[] {(byte)0x2, (byte) 0x3}, 2, 5);
	}


}