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

public enum DeviceManufacturer {
	GARMIN("Garmin", 1),
	GARMIN_FR("Garmin fr",	2),
	ZEPHYR("Zephyr", 3),
	DAYTON("Dayton", 4),
	IDT("Idt", 5),
	SRM("SRM", 6),
	QUARQ("Quarq", 7),
	IBIKE("iBike", 8),
	SARIS("Saris", 9),
	SPARK_HK("Spark_hk", 10),
	TANITA("Tanita", 11),
	ECHOWELL("Echowell", 12),
	DYNASTREAM_OEM("Dynastream OEM", 13),
	NAUTILUS("Nautilus", 14),
	DYNASTREAM("Dynastream", 15),
	TIMEX("Timex", 16),
	METRIGEAR("Metrigear", 17),
	XELIC("Xelic", 18),
	BEURER("Beurer", 19),
	CARDIOSPORT("Cardiosport", 20),
	A_AND_D("A&D", 21),
	HMM("HMM", 22),
	SUUNTO("Suunto", 23),
	THITA_ELEKTRONIK("Thita Elektronik", 24),
	GPULSE("G.Pulse", 25),
	CLEAN_MOBILE("Clean Mobile", 26),
	PEDEL_BRAIN("Pedal Brain", 27),
	PEAKSWARE("Peaksware", 28),
	SAXONAR("Saxonar", 29),
	LEMOND_FITNESS("Lemond Fitness", 30),
	DEXCOM("Dexcom", 31),
	WAHOO_FITNESS("Wahoo Fitness", 32),
	OCTAN_FITNESS("Octane Fitness", 33),
	ARCHINOETICS("Archinoetics", 34),
	THE_HURT_BOX("The Hurt Box", 35),
	CITIZEN_SYSTEMS("Citizen Systems", 36),
	OSYNCE("Osynce", 38),
	HOLUX("Holux", 39),
	CONCEPT2("Concept2", 40),
	ONE_GIANT_LEAP("One Giant Leap", 42),
	ACE_SENSOR("ace_sensor", 43),
	BRIM_BROTHERS("Brim Brothers", 44),
	XPLOVA("Xplova", 45),
	PERCEPTION_DIGITAL("Perception digital", 46),
	BF1SYSTEMS("bf1systems", 47),
	PIONEER("Pioneer", 48),
	KESTREL("Kestrel", 5001),
	UNKNOWN("Unknown", -1),
	PLUSOT("Plusot", 99099);

	private final String label;
	private final int id;
	
	private DeviceManufacturer(final String label, final int id) {
		this.label = label;
		this.id = id;
	}
	
	public static DeviceManufacturer getById(int id){
		for (DeviceManufacturer manufacturer: DeviceManufacturer.values()) {
			if (manufacturer.id == id) return manufacturer;
		}
		return DeviceManufacturer.UNKNOWN;
	}

	public String getLabel() {
		//if (this.equals(AntManufacturer.UNKNOWN)) return label + " " + id; 
		return label;
	}
}
