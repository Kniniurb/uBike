package com.plusot.senselib.values;

import java.util.EnumSet;
import java.util.Locale;

import android.content.Context;

import com.plusot.senselib.R;

public enum DeviceType {
	TIMER(0, R.string.timedevice, "TIME"), //, EnumSet.of(ValueType.TIME)),
	CADENCE_SENSOR(1, R.string.bikecadence, "BCD"), //, EnumSet.of(ValueType.CADENCE)),
	SPEED_SENSOR(2, R.string.bikespeed, "BSP"), //, EnumSet.of(ValueType.SPEED, ValueType.WHEEL_REVS)),
	CADENCESPEED_SENSOR(3, R.string.bikespeedcadence, "BSC"), //, EnumSet.of(ValueType.SPEED, ValueType.CADENCE, ValueType.WHEEL_REVS)),
	POWER_SENSOR(4, R.string.bikepower, "POW"), 
	//, EnumSet.of(
//			ValueType.POWER, ValueType.POWER_PEDAL, ValueType.CADENCE, 
//			ValueType.CRANK_TORQUE, ValueType.WHEEL_TORQUE,
//			ValueType.ENERGY, 
//			ValueType.WHEEL_REVS, ValueType.SPEED)),
//	POWER_SENSOR2(5, R.string.bikepower2, "POW2"),
//	POWER_SENSOR3(6, R.string.bikepower3, "POW3"),
//	POWER_SENSOR4(7, R.string.bikepower4, "POW4"),
	WIND_SENSOR(5, R.string.winddevice, "WIND"), //, EnumSet.of(ValueType.WINDSPEED, ValueType.TEMPERATURE, ValueType.HUMIDITY, ValueType.AIRPRESSURE, ValueType.WINDDIRECTION)),
	AIRPRESSURE_SENSOR(6, R.string.airpressuresensor, "WIND"), //, EnumSet.of(ValueType.AIRPRESSURE, ValueType.ALTITUDE, ValueType.SLOPE, ValueType.VERTSPEED)),
	HEARTRATEMONITOR(7, R.string.heartratemonitor, "HRM"), //, EnumSet.of(ValueType.HEARTRATE, ValueType.HRZONE, ValueType.PULSEWIDTH)),
//	BLEHEARTRATEMONITOR(8, R.string.heartrate_ble, "BLEHRM", EnumSet.of(ValueType.HEARTRATE, ValueType.HRZONE)),
	SUUNTOHEARTRATEMONITOR(9, R.string.heartratesuunto, "SHRM"), //, EnumSet.of(ValueType.HEARTRATE, ValueType.HRZONE, ValueType.PULSEWIDTH)),
	BTCHAT(10, R.string.btchat, "CHAT"), //, EnumSet.of(ValueType.HEARTRATE, ValueType.HRZONE, ValueType.PULSEWIDTH, ValueType.SPEED, ValueType.WHEEL_REVS, ValueType.CADENCE, ValueType.TEMPERATURE)),
	BLE(11, R.string.ble, "BLE"), //, EnumSet.of(ValueType.HEARTRATE, ValueType.HRZONE, ValueType.PULSEWIDTH, ValueType.SPEED, ValueType.WHEEL_REVS, ValueType.CADENCE, ValueType.TEMPERATURE)),
	ZEPHYRHEARTRATEMONITOR(12, R.string.heartratezephyr, "ZHRM"), //, EnumSet.of(ValueType.HEARTRATE, ValueType.HRZONE, ValueType.PULSEWIDTH, ValueType.SPEED, ValueType.STEPS, ValueType.CADENCE)),
	GPS_SENSOR(13, R.string.locationdevice, "LOC"), //, EnumSet.of(ValueType.SPEED, ValueType.LOCATION, ValueType.SATELLITES, ValueType.SLOPE, ValueType.BEARING, ValueType.ALTITUDE)),
	MAGNETICFIELD_SENSOR(14, R.string.magneticdevice, "MAGNET"), //, EnumSet.of(/*ValueType.MAGNETICFIELD, */ValueType.SLOPE)),
	ACCELEROMETER(15, R.string.accelerometer, "ACCEL"), //, EnumSet.of(/*ValueType.GRAVITY,*/ ValueType.ACCELERATION, ValueType.SLOPE)),
	BATTERY_SENSOR(16, R.string.batterysensor, "BATT"), //, EnumSet.of(ValueType.VOLTAGE)),
	PROXIMITY_SENSOR(17, R.string.batterysensor, "PROX"), //, EnumSet.noneOf(ValueType.class)),
	STRIDE_SENSOR(18, R.string.stridesensor, "STRIDE"), //, EnumSet.of(ValueType.SPEED, ValueType.CADENCE, ValueType.STEPS)),
	INTERVAL(19, R.string.interval, "LAP"), //, EnumSet.of(ValueType.INTERVAL));
	;
	
	private final int priorityBit;
	private final int label;
	private final String shortName;
	private final String lowerCaseShortName;
	//private final EnumSet<ValueType> valueTypes;
	private static EnumSet<DeviceType> deviceTypes= EnumSet.noneOf(DeviceType.class);
	
	public static void addDeviceTypes(EnumSet<DeviceType> deviceTypes) {
		DeviceType.deviceTypes.addAll(deviceTypes);
	}
	
	public static EnumSet<DeviceType> getDeviceTypes() {
		return deviceTypes.clone();
	}
	
	public static void clearDeviceTypes() {
		deviceTypes.clear();
	}
	
	private DeviceType(final int bit, final int label, final String shortName) { //, final EnumSet<ValueType> valueTypes) {
		this.priorityBit = bit;
		this.label = label;
		this.shortName = shortName;
		this.lowerCaseShortName = shortName.toLowerCase(Locale.US);
		//this.valueTypes = valueTypes;
	}
	
	public int getPriority() {
		return (1 << priorityBit);
	}
	
	public int getPriorityBit() {
		return priorityBit;
	}
	
	public String getLabel(Context context) {
		if (label == 0) return "NULL";
		return context.getString(label);
	}
	
	public String getShortName() {
		return shortName;
	}
	
	public String getLowerCaseShortName() {
		return lowerCaseShortName;
	}
	
	public static DeviceType fromShortName(String shortName) {
		for (DeviceType type : DeviceType.values()) {
			if (type.shortName.equalsIgnoreCase(shortName)) return type;
		}
		return null;
	}
	
	public static EnumSet<DeviceType> fromTag(int tag) {
		EnumSet<DeviceType> deviceTypes = EnumSet.noneOf(DeviceType.class);
		for (DeviceType type : DeviceType.values()) {
			if ((type.priorityBit & tag) == type.priorityBit) deviceTypes.add(type);
		}
		return deviceTypes;
	}
	
	public static String[] getLabels(Context context) {
		String[] list = new String[DeviceType.values().length];
	
		for (DeviceType type : DeviceType.values()) {
			list[type.ordinal()] = type.getLabel(context);
		
		}
		return list;
	}

	/*public EnumSet<ValueType> getValueTypes() {
		return valueTypes;
	}*/
}
