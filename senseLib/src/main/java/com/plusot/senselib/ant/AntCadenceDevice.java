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

import com.plusot.common.Globals;
import com.plusot.common.share.LLog;
import com.plusot.javacommon.util.Format;
import com.plusot.senselib.values.Device;
import com.plusot.senselib.values.ValueType;

public class AntCadenceDevice extends AntDevice {
	private static final String CLASSTAG = AntCadenceDevice.class.getSimpleName();

	private int prevEventTime = Integer.MIN_VALUE;
	private int prevCadenceCount = Integer.MIN_VALUE;
	private long lastCall = 0;

	private double revs = 0;
	
	public AntCadenceDevice(AntMesgCallback callback) {
		super(AntProfile.BIKECADENCE, callback);
	}

//	public AntCadenceDevice(final AntManager mrg, final Context context, final int sub) {
//		super(mrg, DeviceType.CADENCE_SENSOR, AntDeviceProfile.BIKECADENCE, sub, EnumSet.of(ValueType.CADENCE));		
//	}

	@Override
	public boolean decode(byte[] antMsg) {
		if (!super.decode(antMsg)) return false;

		if ((antMsg[PAGE_OFFSET] & (byte) 0x80) == (byte) 0x80) {
			//LLog.d(Globals.TAG, CLASSTAG + ".decode: Page change toggle");
		}
		
		long now = System.currentTimeMillis();
		if (now - lastCall > ANT_MSG_TIMEOUT) {
			prevEventTime = Integer.MIN_VALUE;
			prevCadenceCount = Integer.MIN_VALUE;
		}
		lastCall = now;


		byte page = (byte)(antMsg[PAGE_OFFSET] & (byte) 0x7F); 
		switch (page) {
		case 0x0:
			break;
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
		default:
			LLog.e(Globals.TAG, CLASSTAG + ".decode: Unknown page: " + page);
			return true;
		}
		
		int eventTime = (0xFF & antMsg[PAGE_OFFSET + 4]) + ((0xFF & antMsg[PAGE_OFFSET + 5]) << 8);
		if (eventTime < prevEventTime) prevEventTime -= 65536;
		int revCount = (0xFF & antMsg[PAGE_OFFSET + 6]) + ((0xFF & antMsg[PAGE_OFFSET + 7]) << 8);
		if (revCount < prevCadenceCount) prevCadenceCount -= 65536;
		if (eventTime > prevEventTime && revCount > prevCadenceCount && prevEventTime != Integer.MIN_VALUE) {
			revs = 60.0 * 1024 * (revCount - prevCadenceCount) / (eventTime - prevEventTime);
			fireOnDeviceValueChanged(ValueType.CADENCE, revs, System.currentTimeMillis());
			rawValue = "" + Format.format(revs, 0) + " rpm";
			//LLog.d(Globals.TAG, CLASSTAG + ".decode: EventTime = " + eventTime + " " + prevEventTime + ", RevCount = " + revCount + " " + prevCadenceCount  + ", Revs = " + revs);
			
		}
		//LLog.d(Globals.TAG, CLASSTAG + ".decode: EventTime = " + eventTime + " " + prevEventTime + ", RevCount = " + revCount + " " + prevCadenceCount  + ", Revs = " + revs);
		prevCadenceCount = revCount;
		prevEventTime = eventTime;
		return true;
	}

	@Override
	public void fadeOut(ValueType type) {
		long now = System.currentTimeMillis();
		if (type.equals(ValueType.CADENCE) && revs != Double.NaN && Math.abs(revs) > 1.0) {
			revs *= Device.FADEOUT_FACTOR;
			if (revs < 1.0) revs = 0;
			fireOnDeviceValueChanged(ValueType.CADENCE, revs, now, false);	
		}
	}

	
}
