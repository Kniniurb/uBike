/*******************************************************************************
 * Copyright (c) 2012 Plusot Biketech
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Peter Bruinink - initial API and implementation and/or initial documentation
 * 
 *******************************************************************************/
package com.plusot.senselib.ant;


import java.util.ArrayList;
import java.util.List;

import com.plusot.common.Globals;
import com.plusot.common.share.LLog;
import com.plusot.common.util.ToastHelper;
import com.plusot.javacommon.util.Format;
import com.plusot.senselib.R;
import com.plusot.senselib.SenseGlobals;
import com.plusot.senselib.values.ValueType;

public class AntStrideDevice extends AntDevice {
	private static final String CLASSTAG = AntStrideDevice.class.getSimpleName();

	private static final boolean testing = false;
	private long lastCall = 0;
	private long totalSteps = 0;
	private int pStrideCount = -1;
	private Status podStatus = Status.POD_OK;

	public enum Status {
		LOCATION_LACES((byte)0x0, (byte) 0xC0),
		LOCATION_MIDSOLE((byte)0x40, (byte) 0xC0),
		LOCATION_OTHER((byte)0x80, (byte) 0xC0),
		LOCATION_ANKLE((byte)0xC0, (byte) 0xC0),
		BATTERY_NEW((byte)0x0, (byte) 0x30),
		BATTERY_GOOD((byte)0x10, (byte) 0x30),
		BATTERY_OK((byte)0x20, (byte) 0x30),
		BATTERY_LOW((byte)0x30, (byte) 0x30),
		POD_OK((byte)0x0, (byte) 0xC),
		POD_ERROR((byte)0x4, (byte) 0xC),
		POD_WARNING((byte)0x8, (byte) 0xC),
		POD_RESERVED((byte)0xC, (byte) 0xC),
		USE_INACTIVE((byte)0x0, (byte) 0x3),
		USE_ACTIVE((byte)0x1, (byte) 0x3),
		USE_RESERVED1((byte)0x2, (byte) 0x3),
		USE_RESERVED2((byte)0x3, (byte) 0x3);
		private final byte bits;
		private final byte check;
		private Status(final byte bits, final byte check) {
			this.bits = bits;
			this.check = check;
		}

		public boolean isBatteryStatus() {
			switch (this) {
			case BATTERY_LOW:
			case BATTERY_OK:
			case BATTERY_GOOD:
			case BATTERY_NEW:
				return true;
			default:
				return false;
			}	
		}

		public static Status[] fromBits(byte bits) {
			List<Status> states = new ArrayList<Status>(); 
			for (Status status: Status.values()) {
				if (status.bits == (bits & status.check)) {
					states.add(status);
				}		
			}
			return states.toArray(new Status[0]);
		}




		public static String toStaticString(Status[] states) {	
			StringBuilder sb = new StringBuilder();
			for (Status status : states) {
				if (sb.length() > 0) sb.append(", ");
				sb.append(status.toString());
			}
			return sb.toString();
		}	
	}

	private void processStatus(Status[] states) {
		for (Status status: states) switch (status) {
		case LOCATION_LACES:
		case LOCATION_MIDSOLE:
		case LOCATION_OTHER:
		case LOCATION_ANKLE:
			break;
		case BATTERY_NEW:
			batteryStatus = BatteryStatus.NEW;
			break;
		case BATTERY_GOOD:
			batteryStatus = BatteryStatus.GOOD;
			break;
		case BATTERY_OK:
			batteryStatus = BatteryStatus.OK;
			break;
		case BATTERY_LOW:
			if (!batteryStatus.equals(BatteryStatus.LOW)) 
				ToastHelper.showToastLong(
						Globals.appContext.getString(R.string.stridesensor) + ": " + 
								Globals.appContext.getString(R.string.batterystatus) + " = " + 
								Globals.appContext.getString(R.string.battery_low));
			batteryStatus = BatteryStatus.LOW;
			break;
		case POD_OK:
			podStatus = status;
			break;
		case POD_ERROR:
			if (!podStatus.equals(Status.POD_ERROR)) 
				ToastHelper.showToastLong(R.string.stridesensor_error);
			podStatus = status;
			break;	
		case POD_WARNING:
			if (!podStatus.equals(Status.POD_WARNING)) 
				ToastHelper.showToastLong(R.string.stridesensor_warning);
			podStatus = status;
			break;
		case POD_RESERVED:
		case USE_INACTIVE:
		case USE_ACTIVE:
		default:
			break;
		}
	}

	public AntStrideDevice(AntMesgCallback callback) {
		super(AntProfile.STRIDE, callback);
	}
//	public AntStrideDevice(final AntManager mgr, final Context context, final int sub) {
//		super(mgr, DeviceType.STRIDE_SENSOR, AntDeviceProfile.STRIDE, sub, EnumSet.of(ValueType.STEPS, ValueType.CADENCE, ValueType.SPEED));
//	}

	@Override
	public boolean decode(byte[] antMsg) {
		if (!super.decode(antMsg)) return false;

		long now = System.currentTimeMillis();
		if (now - lastCall > 30000) {
			pStrideCount = -1;
		}
		lastCall = now;

		double speedFraction = 0;

		byte page = (byte)(antMsg[PAGE_OFFSET] & (byte) 0x7F); 
		switch (page) {
		case 0x1:
			//			double timeFraction =  1.0 * (0xFF & antMsg[PAGE_OFFSET + 1]) / 200;
			//			int time = (0xFF & antMsg[PAGE_OFFSET + 2]);
			//			int distance = (0xFF & antMsg[PAGE_OFFSET + 3]);
			//			double distanceFraction =  1.0 * ((0xF0 & antMsg[PAGE_OFFSET + 4]) >> 4) / 16;
			int speed = (0x0F & antMsg[PAGE_OFFSET + 4]);
			speedFraction =  1.0 * (0xFF & antMsg[PAGE_OFFSET + 5]) / 256;
			speedFraction += speed;
			int strideCount = (0xFF & antMsg[PAGE_OFFSET + 6]);
			if (pStrideCount != -1 && pStrideCount != strideCount) {
				int delta = (256 + strideCount - pStrideCount) % 256;
				totalSteps += delta;
				
				if (testing) LLog.d(Globals.TAG, CLASSTAG + " Total steps = " + totalSteps);
				fireOnDeviceValueChanged(ValueType.STEPS, delta, now);
			}
			pStrideCount = strideCount;

			//double latency = 1.0 *  (0xFF & antMsg[PAGE_OFFSET + 7]) / 32;
			//LLog.d(Globals.TAG, CLASSTAG + " StrideCount = " + strideCount + ", latency = " + latency);

			break;
		case 0x2:
			int cadence = (0xFF & antMsg[PAGE_OFFSET + 3]);
			double cadenceFraction = 1.0 * ((0xF0 & antMsg[PAGE_OFFSET + 4]) >> 4) / 16;
			cadenceFraction += cadence;
			fireOnDeviceValueChanged(ValueType.CADENCE, cadenceFraction, now);
			rawValue = "" + Format.format(cadenceFraction, 1) + " rpm";

			speed = (0x0F & antMsg[PAGE_OFFSET + 4]);
			speedFraction =  1.0 * (0xFF & antMsg[PAGE_OFFSET + 5]) / 256;
			speedFraction += speed;
			byte statusBits = antMsg[PAGE_OFFSET + 7];
			Status[] states = Status.fromBits(statusBits);
			//LLog.d(Globals.TAG, CLASSTAG + " Status = " + Status.toStaticString(states) + " (0x"+ String.format("%X", (0xFF & statusBits)) + ")");
			processStatus(states);
			break;
		case 0x3:
			cadence = (0xFF & antMsg[PAGE_OFFSET + 3]);
			cadenceFraction = 1.0 * ((0xF0 & antMsg[PAGE_OFFSET + 4]) >> 4) / 16;
			cadenceFraction += cadence;
			fireOnDeviceValueChanged(ValueType.CADENCE, cadenceFraction, now);
			rawValue = "" + Format.format(cadenceFraction, 1) + " rpm";

			speed = (0x0F & antMsg[PAGE_OFFSET + 4]);
			speedFraction =  1.0 * (0xFF & antMsg[PAGE_OFFSET + 5]) / 256;
			speedFraction += speed;
			int calories = (0xFF & antMsg[PAGE_OFFSET + 6]);
			LLog.d(Globals.TAG, CLASSTAG + " Calories = " + calories);

			statusBits = antMsg[PAGE_OFFSET + 7];
			states = Status.fromBits(statusBits);
			//LLog.d(Globals.TAG, CLASSTAG + " Status = " + Status.toStaticString(states) + " (0x"+ String.format("%X", (0xFF & statusBits)) + ")");
			processStatus(states);
			break;
		default:
			LLog.d(Globals.TAG, CLASSTAG + " Page " + page);
		}

		if (SenseGlobals.lastLocation == null || Math.abs(now - SenseGlobals.lastLocationTime) > 60000 || SenseGlobals.lastLocation.getSpeed() < 0.9f * speedFraction)
		fireOnDeviceValueChanged(ValueType.SPEED, speedFraction, now);

		return true;
	}

	@Override
	public void fadeOut(ValueType type) {

	}

}
