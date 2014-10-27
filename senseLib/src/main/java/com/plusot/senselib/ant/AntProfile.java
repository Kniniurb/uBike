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

import com.plusot.senselib.values.DeviceType;


public enum AntProfile {
	SUUNTODUAL((byte) 2, (byte) 0x4, (short)6554, (byte)65),
	HEARTRATE((byte) 1, (byte)0x78, (short)8070, (byte)57), 
	BIKESPEEDCADENCE((byte) 1, (byte)0x79, (short)8086, (byte)57), 
	BIKESPEED((byte) 1, (byte)0x7B, (short)8118, (byte)57), 
	BIKECADENCE((byte) 1, (byte)0x7A, (short)8102, (byte)57), 
	BIKEPOWER((byte) 1, (byte)0x0B, (short)8182, (byte)57), 
	STRIDE((byte) 1, (byte)0x7C, (short)8134, (byte)57),
	//WEIGHT((byte) 1, (byte)0x77, (short)8192, (byte)57),
	; 

	private final byte network;
	private final byte deviceType;
	private final short period;
	private final byte freq;

	private AntProfile(final byte network, final byte deviceType, final short period, final byte freq) { 
		this.network = network;
		this.deviceType = deviceType;
		this.period = period;
		this.freq = freq;
	}

	public byte getAntDeviceType() {
		return deviceType;
	}

	public short getPeriod() {
		return period;
	}

	public byte getNetwork() {
		return network;
	}	

	public byte getFreq() {
		return freq;
	}

	public byte getChannel() {
		return (byte) this.ordinal();
	}

	public static AntProfile fromChannel(byte channel) {
		if (channel < 0 || channel >= AntProfile.values().length) return null;
		return AntProfile.values()[channel];
	}

	public DeviceType getDeviceType() {
		switch (this) {
		case BIKECADENCE:
			return DeviceType.CADENCE_SENSOR;
		case BIKEPOWER:
			return DeviceType.POWER_SENSOR;
		case BIKESPEED:
			return DeviceType.SPEED_SENSOR;
		case BIKESPEEDCADENCE:
			return DeviceType.CADENCESPEED_SENSOR;
		default:
		case HEARTRATE:
			return DeviceType.HEARTRATEMONITOR;
		case STRIDE:
			return DeviceType.STRIDE_SENSOR;
		case SUUNTODUAL:
			return DeviceType.SUUNTOHEARTRATEMONITOR;
			//		case WEIGHT:
			//			break;
		}
	}

	public static AntProfile nextProfile(AntProfile current) {	
		if (current == null) return null;
		int ord = current.ordinal();
		ord++;
		if (ord < AntProfile.values().length) return AntProfile.values()[ord];
		return null;
	}

	public static AntProfile firstProfile() {	
		return AntProfile.values()[0];
	}

}

