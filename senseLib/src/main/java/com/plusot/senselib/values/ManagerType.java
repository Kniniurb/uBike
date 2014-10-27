package com.plusot.senselib.values;

import java.util.EnumSet;

public enum ManagerType {
	ANT_MANAGER,
	GPS_MANAGER,
	TIME_MANAGER,
	SENSOR_MANAGER,
	BLUETOOTH_MANAGER,
	BLUETOOTHLE_MANAGER;
	
	public static EnumSet<ManagerType> supportedManagers = EnumSet.noneOf(ManagerType.class);
}