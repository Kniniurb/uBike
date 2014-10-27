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


import com.plusot.senselib.values.ValueType;

public class AntHrmDevice extends AntDevice {
//	private static final String CLASSTAG = HrmDevice.class.getSimpleName();

	private int bpm = Integer.MAX_VALUE;
	private int prevEventTime = Integer.MIN_VALUE;
	private int prevBeat = Integer.MIN_VALUE;
	private long lastCall = 0;

	public AntHrmDevice(AntMesgCallback callback) {
		super(AntProfile.HEARTRATE, callback);
	}
//	public AntHrmDevice(final AntManager mgr, final Context context, final DeviceType type, final int sub) {
//		super(mgr, type, AntDeviceProfile.HEARTRATE, sub, EnumSet.of(ValueType.HEARTRATE, ValueType.PULSEWIDTH, ValueType.HRZONE));
//		//prepareLog(ValueType.HEARTBEATS);
//	}

	@Override
	public boolean decode(byte[] antMsg) {
		if (!super.decode(antMsg)) return false;

		long now = System.currentTimeMillis();
		if (now - lastCall > 1000) {
			prevBeat = Integer.MIN_VALUE;
			prevEventTime = Integer.MIN_VALUE;
		}
		lastCall = now;

		if ((antMsg[PAGE_OFFSET] & (byte) 0x80) == (byte) 0x80) {
			//LLog.d(Globals.TAG, CLASSTAG + ".decode: Page change toggle");
		}

		byte page = (byte)(antMsg[PAGE_OFFSET] & (byte) 0x7F); 
		switch (page) {
		case 0x1:
			operatingTime = (0xFF & antMsg[PAGE_OFFSET + 1]) + 
			((0xFF & antMsg[PAGE_OFFSET + 2]) << 8) +
			((0xFF & antMsg[PAGE_OFFSET + 3]) << 16);
			operatingTime *= 2;
			break;
		case 0x2:
			int manufacturingId = (0xFF & antMsg[PAGE_OFFSET + 1]);
			manufacturer = DeviceManufacturer.getById(manufacturingId);
			serialNumber = "" + (0xFF & antMsg[PAGE_OFFSET + 2]) + ((0xFF & antMsg[PAGE_OFFSET + 3]) << 8); 
			break;
		case 0x3:
			revision = "" + (0xFF & antMsg[PAGE_OFFSET + 1]);
			softwareVersion = "" + (0xFF & antMsg[PAGE_OFFSET + 1]);
			productId = "" + (0xFF & antMsg[PAGE_OFFSET + 3]);
			break;
		case 0x4:
			//manufacturer specific data & previous heart beat event time
			break;	
		}

		int eventTime = (0xFF & antMsg[PAGE_OFFSET + 4]) + ((0xFF & antMsg[PAGE_OFFSET + 5]) << 8); 
		int beat = (0xFF & antMsg[PAGE_OFFSET + 6]);

		if (prevBeat != Integer.MIN_VALUE) {
			int pulseWidth = (0xFFFF + eventTime - prevEventTime) % 0xFFFF;
			int beats = (0xFF + beat - prevBeat) % 0xFF;
			if (beats > 0) {
				pulseWidth /= beats;
				if (pulseWidth > 200 && pulseWidth < 2000) fireOnDeviceValueChanged(ValueType.PULSEWIDTH, pulseWidth, now);
//				LLog.i(Globals.TAG, CLASSTAG + ".decode for " + getName() + ": pulse width " + pulseWidth + "(" + beat + ", " + beats + ", " + eventTime + " - " + prevEventTime + " = " + 
//						((0xFFFF + eventTime - prevEventTime) % 0xFFFF)+ ")");
				
			}
		}
		prevBeat = beat;
		prevEventTime = eventTime;

		bpm = antMsg[PAGE_OFFSET + 7] & 0xFF;
		this.rawValue = "" + bpm + " bpm";
		if (bpm != 0) fireOnDeviceValueChanged(ValueType.HEARTRATE, bpm, now);
		return true;
	}

	@Override
	public void fadeOut(ValueType type) {
// 		if (type.equals(ValueType.HEARTRATE) && bpm != Integer.MAX_VALUE && bpm > 1) {
//			bpm = (int) Math.floor(Device.FADEOUT_FACTOR * bpm);
//			if (bpm < 5) bpm = 0;
//			LLog.d(Globals.TAG, CLASSTAG + ".fadeOut");
//			fireOnDeviceValueChanged(ValueType.HEARTRATE, bpm, System.currentTimeMillis(), false);
//		}
	}

}
