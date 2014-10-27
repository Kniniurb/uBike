package com.plusot.senselib.store;

import com.plusot.senselib.values.ValueType;

public enum ParamType {
	ALTITUDE("alt"),
	SLOPEANGLE("ang"),
	PRESSURE("ath"),
	BEARING("brg"),
	CADENCE("cad"),
	HEARTRATE("hrm"),
	HUMIDITY("hum"),
	LATITUDE("lat"),
	LONGITUDE("lng"),
	PEDALRATIO("ped"),
	POWER("pow"),
	WHEELREVOLUTIONS("rvs"),
	SATELLITES("sat"),
	SLOPE("slp"),
	SPEED("spd"),
	TEMPERATURE("temp"),
	VERTICALSPEED("vsp"),
	WINDSPEED("windspd");

	private final String label;

	private ParamType(final String label) {
		this.label = label;
	}

	public static ParamType fromString(String label) {
		for (ParamType type : ParamType.values()) {
			if (type.label.equalsIgnoreCase(label)) return type;
		}
		return null;
	}

	public String getLabel() {
		return label;
	}
	
	public static ParamType fromValueType(ValueType valueType, int dimension) {
		switch (valueType) {
		case ACCELERATION: return null;
		case STEPS: return null;
//		case STEPSPEED: return null;
		case PULSEWIDTH: return null;
		case TEMPERATURE: return ParamType.TEMPERATURE; 
		case WINDSPEED: return ParamType.WINDSPEED;
		case AIRPRESSURE: return ParamType.PRESSURE;
		case HUMIDITY: return ParamType.HUMIDITY;
		case EFFICIENCY: return null;
		case SLOPE: return ParamType.SLOPE;
		case HEARTRATE: return ParamType.HEARTRATE;
		case WHEEL_REVS: return ParamType.WHEELREVOLUTIONS;
		case TIME: return null;
		case CADENCE: return ParamType.CADENCE;
		case POWER: return ParamType.POWER;
		case POWER_PEDAL: return ParamType.PEDALRATIO;
//		case VOLTAGE: return null;
		case ALTITUDE: return ParamType.ALTITUDE;
		case LOCATION: 
			if (dimension == 0) return ParamType.LATITUDE;
			return ParamType.LONGITUDE;
		case BEARING: return ParamType.BEARING;
		case SATELLITES: return null;
		case SPEED: return ParamType.SPEED;
		case DISTANCE: return null;
		case GAINRATIO: return null;
		case PROXIMITY: return null;
		case CRANK_TORQUE:
		case ENERGY:
		case HRZONE:
		case INTERVAL:
		case VOLTAGE:
		case WHEEL_TORQUE:
		case WINDDIRECTION:
		default:
			return null;
		}
	}
	

}
