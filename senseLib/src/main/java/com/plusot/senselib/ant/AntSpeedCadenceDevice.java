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

import com.plusot.javacommon.util.Format;
import com.plusot.senselib.SenseGlobals;
import com.plusot.senselib.values.Device;
import com.plusot.senselib.values.ValueType;

public class AntSpeedCadenceDevice extends AntDevice {
	//	private static final String CLASSTAG = BikeSpeedCadenceDevice.class.getSimpleName();

	private int prevs[] = {Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
	private double cadence = 0;
	private double revs = 0;
	private long lastCall = 0;

	public AntSpeedCadenceDevice(AntMesgCallback callback) {
		super(AntProfile.BIKESPEEDCADENCE, callback);
		
		
	}
//	public AntBikeSpeedCadenceDevice(final AntManager mrg, final Context context, final int sub) {
//		super(mrg, DeviceType.CADENCESPEED_SENSOR, AntDeviceProfile.BIKESPEEDCADENCE, sub, EnumSet.of(ValueType.CADENCE, ValueType.WHEEL_REVS, ValueType.SPEED));
//	}

	@Override
	public boolean decode(byte[] antMsg) {
		if (!super.decode(antMsg)) return false;

		long now = System.currentTimeMillis();
		if (now - lastCall > ANT_MSG_TIMEOUT) for (int i = 0; i < prevs.length; i++) prevs[i] = Integer.MIN_VALUE;
		lastCall = now;
		
		int evs[] = new int[4];
		for (int i = 0; i < 4; i++) evs[i] = (0xFF & antMsg[PAGE_OFFSET + 2 * i]) + ((0xFF & antMsg[PAGE_OFFSET + 2 * i + 1]) << 8);
		if (prevs[0] != Integer.MIN_VALUE) {	
			if (evs[0] != prevs[0]) {
				int revCount = (0xFFFF + evs[1] - prevs[1]) % 0xFFFF;
				int timeCount = (0xFFFF + evs[0] - prevs[0]) % 0xFFFF;
				cadence = 60.0 * 1024 * revCount / timeCount;
				if (cadence <= ValueType.CADENCE.maxValue() && cadence >= ValueType.CADENCE.minValue()) fireOnDeviceValueChanged(ValueType.CADENCE, cadence, now);
			}
			if (evs[2] != prevs[2]) {
				int revCount = (0xFFFF + evs[3] - prevs[3]) % 0xFFFF;
				int timeCount = (0xFFFF + evs[2] - prevs[2]) % 0xFFFF;
				revs = 60.0 * 1024 * revCount / timeCount;
				if (revs <= ValueType.WHEEL_REVS.maxValue() && revs >= ValueType.WHEEL_REVS.minValue()) fireOnDeviceValueChanged(ValueType.WHEEL_REVS, revs, now );
				double speed = revs * 0.001 * SenseGlobals.wheelCirc / 60.0;
				if (speed <= ValueType.SPEED.maxValue() && speed >= ValueType.SPEED.minValue()) fireOnDeviceValueChanged(ValueType.SPEED, speed, now);
				//rawValue = "" + Format.format(cadence) + " /s, " + Format.format(speed, 1) + " m/s";
			}
			rawValue = "" + Format.format(cadence, 0) + " rpm\n" + Format.format(3.6 * revs * 0.001 * SenseGlobals.wheelCirc / 60.0, 0) + " kph";
		}
		for (int i = 0; i < 4; i++) prevs[i] = evs[i];

		return true;
	}

	@Override
	public void fadeOut(ValueType type) {
		long now = System.currentTimeMillis();
		switch (type) {
		case CADENCE: 
			if (cadence != Double.NaN && Math.abs(cadence) > 1.0) {
				cadence *= Device.FADEOUT_FACTOR;
				if (cadence < 1.0) cadence = 0;
				fireOnDeviceValueChanged(ValueType.CADENCE, cadence, now, false);
			}
			break;
		case WHEEL_REVS:
			if (revs != Double.NaN && Math.abs(revs) > 1.0) {
				revs *= Device.FADEOUT_FACTOR;
				if (revs < 1.0) revs = 0;
				fireOnDeviceValueChanged(ValueType.WHEEL_REVS, revs, now, false);
				fireOnDeviceValueChanged(ValueType.SPEED, revs * 0.001 * SenseGlobals.wheelCirc / 60.0, now, false);

			}
			break;
		default:
			break;
		}
	}
}
