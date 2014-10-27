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
package com.plusot.senselib.values;

import android.annotation.SuppressLint;
import android.content.Context;

import com.plusot.bluelib.ble.BleDataType;
import com.plusot.common.Globals;
import com.plusot.common.share.LLog;
import com.plusot.javacommon.util.TimeUtil;
import com.plusot.senselib.R;
import com.plusot.senselib.widget.BaseGraph;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public enum ValueType {
	/*
	 * l = long, d = double, i = integer, s = string, f = float, t = data/time
	 */
	//WEIGHT("dWGT", R.string.weight, UnitType.WEIGHT_UNIT),
	HRZONE("iZON", R.string.zone, UnitType.COUNT_UNIT),
	INTERVAL("iLAP", R.string.interval, UnitType.COUNT_UNIT),
	ENERGY("dENG", R.string.energy, UnitType.ENERGY_UNIT),
	ACCELERATION("dACC", R.string.acceleration, UnitType.ACCELERATION_UNIT),
	STEPS("iSTP", R.string.steps, UnitType.STEP_UNIT),
	VERTSPEED("dVRT", R.string.vertspeed, UnitType.SPEED_UNIT),
	//GRAVITY("dGRV", R.string.gravity, UnitType.ACCELERATION_UNIT),
	//ORIENTATION("dORI", R.string.orientation, UnitType.ANGLE_UNIT), 
	//WHEEL_PERIOD("dWPD", R.string.wheel_period, UnitType.SECONDTIME_UNIT),
	WHEEL_TORQUE("dWTQ", R.string.wheel_torque, UnitType.TORQUE_UNIT),
	CRANK_TORQUE("dCTQ", R.string.crank_torque, UnitType.TORQUE_UNIT),
	WINDDIRECTION("dWDR", R.string.winddirection, UnitType.ANGLE_UNIT), 							// degrees
	TEMPERATURE("dTMP", R.string.temperature, UnitType.TEMPERATURE_UNIT), 		
	PULSEWIDTH("iPUL", R.string.pulsewidth, UnitType.PULSEWIDTH_UNIT), 				
	WINDSPEED("dWSP", R.string.windspeed, UnitType.WIND_UNIT), 							// m/s
	AIRPRESSURE("dATH", R.string.airpressure, UnitType.PRESSURE_UNIT),					// Pa
	HUMIDITY("dHUM", R.string.humidity, UnitType.RATIO_UNIT),							// ratio
	EFFICIENCY("dEFF", R.string.efficiency, UnitType.EFFICIENCY_UNIT),					// ratio
	SLOPE("dSLP", R.string.slope, UnitType.RATIO_UNIT), 								// ratio
	//	SLOPEANGLE("dANG", R.string.slope_angle, UnitType.ANGLE_UNIT), 						// degrees
	HEARTRATE("iHRM", R.string.heart_beat, UnitType.BEATS_UNIT),						// bpm
	WHEEL_REVS("dRVS", R.string.wheel_revs, UnitType.CYCLES_UNIT),						// rpm
	TIME("tTME", R.string.time, UnitType.DATETIME_UNIT),								// milliseconds
	CADENCE("dCAD", R.string.cadence, UnitType.CYCLES_UNIT),							// rpm
	POWER("dPOW", R.string.power, UnitType.POWER_UNIT),									// W	
	POWER_PEDAL("dPED", R.string.power_pedal, UnitType.RATIO_UNIT),						// ratio
	VOLTAGE("dVLT", R.string.voltage, UnitType.VOLTAGE_UNIT),							// V
	ALTITUDE("dALT", R.string.altitude, UnitType.DISTANCE_UNIT), 						// m
	LOCATION("dLOC", R.string.location, UnitType.ANGLE_UNIT),							// degrees
	BEARING("dBRG", R.string.bearing, UnitType.ANGLE_UNIT), 							// degrees
	SATELLITES("iSAT", R.string.satellites, UnitType.COUNT_UNIT),						// #
	SPEED("dSPD", R.string.speed, UnitType.SPEED_UNIT),									// m/s
	DISTANCE("dDST", R.string.distance, UnitType.DISTANCE_UNIT),						// m
	GAINRATIO("dGRR", R.string.gearratio, UnitType.RATIO_UNIT),							// ratio
	PROXIMITY("dPRX", R.string.gearratio, UnitType.DISTANCE_UNIT);						// ratio

	private final int stringId;
	private final String shortName;
	private final String lowerCaseShortName;
	private final UnitType unitType;
	private static final String CLASSTAG = ValueType.class.getSimpleName();
	public static boolean isShowExtraValues = false;
	public static final EnumMap<ValueType, Double> raw = new EnumMap<ValueType, Double>(ValueType.class);
	public static final EnumMap<ValueType, Double> variance = new EnumMap<ValueType, Double>(ValueType.class);	
	public static final EnumMap<ValueType, Double> max = new EnumMap<ValueType, Double>(ValueType.class);	
	public static final EnumMap<ValueType, Double> min = new EnumMap<ValueType, Double>(ValueType.class);	


	private ValueType(final String shortName, final int stringId, final UnitType unitType) {
		this.shortName = shortName;
		this.lowerCaseShortName = shortName.toLowerCase(Locale.US);
		this.stringId = stringId;
		this.unitType = unitType;
	}

	public double getVariance() {
		if (variance.get(this) != null) return variance.get(this);
		return 0;
	}

	public void setVariance(double value) {
		variance.put(this, value);
	}

	public double getMax() {
		if (max.get(this) != null) return max.get(this);
		return 0;
	}

	public void setMax(double value) {
		max.put(this, value);
	}

	public double getMin() {
		if (min.get(this) != null) return min.get(this);
		return 0;
	}

	public void setMin(double value) {
		min.put(this, value);
	}

	//	public void clearVariance() {
	//		variance.remove(this);
	//	}

	public double getRaw() {
		if (raw.get(this) != null) return raw.get(this);
		return 0;
	}

	public void setRaw(double value) {
		raw.put(this, value);
	}

	//	public void clearRaw() {
	//		raw.remove(this);
	//	}

	public static void clear() {
		raw.clear();
		variance.clear();
		max.clear();
		min.clear();
	}

	public String getLabel(Context context) {
		if (context == null) {
			LLog.e(Globals.TAG, CLASSTAG + ": Context == null in ValueType.getLabel");
			return "";
		}
		return context.getString(stringId);
	}

	public Unit defaultUnit() {
		switch (this) {
		//case WEIGHT: return Unit.KG;
		case HRZONE: return Unit.COUNT;
		case ENERGY: return Unit.KILOJOULE;
		case INTERVAL: return Unit.COUNT;
		case WHEEL_TORQUE: return Unit.NM;
		case CRANK_TORQUE: return Unit.NM;
		case ACCELERATION: return Unit.MPSS;
		case STEPS: return Unit.STEPS;
		//		case STEPSPEED: return Unit.STEPSPERMINUTE;
		case PULSEWIDTH: return Unit.PULSEWIDTH;
		case TEMPERATURE: return Unit.CELCIUS; 
		case WINDSPEED: return Unit.KPH;
		case AIRPRESSURE: return Unit.MBAR;
		case HUMIDITY: return Unit.PERCENT;
		case EFFICIENCY: return Unit.EFFICIENCY;
		case SLOPE: return Unit.PERCENT;
		//		case SLOPEANGLE: return Unit.DEGREE;
		case HEARTRATE: return Unit.BPM;
		case WHEEL_REVS: return Unit.RPM;
		case TIME: return Unit.DATETIME;
		case CADENCE: return Unit.RPM;
		case POWER: return Unit.WATT;
		case POWER_PEDAL: return Unit.PERCENT;
		case VOLTAGE: return Unit.VOLTAGE;
		case ALTITUDE: return Unit.METER;
		case LOCATION: return Unit.DEGREE;
		case BEARING: return Unit.DEGREE;
		case WINDDIRECTION: return Unit.DEGREE;
		case SATELLITES: return Unit.COUNT;
		case SPEED: return Unit.KPH;
		case VERTSPEED: return Unit.MPS;
		case DISTANCE: return Unit.KM;
		case GAINRATIO: return Unit.RATIO;
		case PROXIMITY: return Unit.METER;
		}
		return null;
	}

	public Unit metricUnit() {
		switch (this) {
		//case WEIGHT: return Unit.KG;
		case HRZONE: return Unit.COUNT;
		case ENERGY: return Unit.KILOJOULE;
		case INTERVAL: return Unit.COUNT;
		case WHEEL_TORQUE: return Unit.NM;
		case CRANK_TORQUE: return Unit.NM;
		case ACCELERATION: return Unit.MPSS;
		case STEPS: return Unit.STEPS;
		//		case STEPSPEED: return Unit.STEPSPERMINUTE;
		case PULSEWIDTH: return Unit.PULSEWIDTH;
		case TEMPERATURE: return Unit.CELCIUS; 
		case WINDSPEED: return Unit.MPS;
		case AIRPRESSURE: return Unit.MBAR;
		case HUMIDITY: return Unit.PERCENT;
		case EFFICIENCY: return Unit.EFFICIENCY;
		case SLOPE: return Unit.PERCENT;
		//		case SLOPEANGLE: return Unit.DEGREE;
		case HEARTRATE: return Unit.BPM;
		case WHEEL_REVS: return Unit.RPM;
		case TIME: return Unit.DATETIME;
		case CADENCE: return Unit.RPM;
		case POWER: return Unit.WATT;
		case POWER_PEDAL: return Unit.PERCENT;
		case VOLTAGE: return Unit.VOLTAGE;
		case ALTITUDE: return Unit.METER;
		case LOCATION: return Unit.DEGREE;
		case BEARING: return Unit.DEGREE;
		case WINDDIRECTION: return Unit.DEGREE;
		case SATELLITES: return Unit.COUNT;
		case SPEED: return Unit.MPS;
		case VERTSPEED: return Unit.MPS;
		case DISTANCE: return Unit.METER;
		case GAINRATIO: return Unit.RATIO;
		case PROXIMITY: return Unit.METER;
		}
		return null;
	}

	public UnitType getUnitType() {
		return unitType;
	}

	/*public double getWeightFactor() {
		switch (this) {
		default:
			return 1;
		}
	}*/

	// milliseconds
	public long getSampleTime() { 
		switch (this) {
		case HRZONE: 
		case INTERVAL:
		case STEPS:
		case ENERGY:
		case ACCELERATION:
		case PULSEWIDTH:
		case WINDDIRECTION:
		case BEARING:
			return 0;
		case AIRPRESSURE:
			return 5000;
		case POWER:
			return 3000;
		case CADENCE:
		case SPEED:
			return 1000;
		default:
			return 1500;
		}
	}

	public int getDecimals() {
		switch (this) {
		case INTERVAL: return 0;
		case PROXIMITY:	return 1;
		case VERTSPEED: return 2;
		//case WEIGHT: return 1;
		case HRZONE: 
		case ENERGY: return 0;
		case WHEEL_TORQUE: return 1;
		case CRANK_TORQUE: return 1;
		case ACCELERATION: return 2;
		case STEPS: return 0;
		//		case STEPSPEED: return 0;
		case PULSEWIDTH: return 0;
		case TEMPERATURE: return 0; 
		case WINDSPEED: return 1;
		case HUMIDITY: return -1;
		case EFFICIENCY: return 2;
		case SLOPE: return 2;
		//		case SLOPEANGLE: return 1;
		case HEARTRATE: return 0;
		case WHEEL_REVS: return 0;
		case CADENCE: return 0;
		case POWER: return 0;
		case POWER_PEDAL: return 2;
		case VOLTAGE: return 1;
		case ALTITUDE: return 0; 
		case LOCATION: return 4;
		case BEARING: return 0;
		case WINDDIRECTION: return 0;
		case SATELLITES: return 0;
		case SPEED: return 1;
		case DISTANCE: return 1;
		case GAINRATIO: return 2;
		case AIRPRESSURE: return 1;
		case TIME: return 1;
		}
		return -1;

	}

	public boolean allowZeroOrNegativeInAvg() {
		switch(this) {
		case HRZONE: 
		case ENERGY: return false;
		case INTERVAL: return false;
		case WHEEL_TORQUE: return false;
		case CRANK_TORQUE: return false;
		case ACCELERATION: return true;
		case STEPS: return false;
		//		case STEPSPEED: return false;
		case PULSEWIDTH: return false;
		case TEMPERATURE: return true; 
		case WINDSPEED: return false;
		case AIRPRESSURE: return false;
		case HUMIDITY: return false;
		case EFFICIENCY: return false;
		case SLOPE: return true;
		//		case SLOPEANGLE: return true;
		case HEARTRATE: return false;
		case WHEEL_REVS: return false;
		case TIME: return false;
		case CADENCE: return false;
		case POWER: return false;
		case POWER_PEDAL: return false;
		case VOLTAGE: return false;
		case ALTITUDE: return true; // Mount Everest
		case LOCATION: return true;
		case BEARING: return true;
		case WINDDIRECTION: return true;
		case SATELLITES: return false;
		case SPEED: return false;
		case DISTANCE: return false;
		case GAINRATIO: return false;
		case PROXIMITY:	return false;
		case VERTSPEED: return true;
		//case WEIGHT: return false;
		}
		return false;
	}

	public double minimalRelevantValue() {
		switch(this) {
		case HRZONE: return 1.0;
		case ENERGY: return 1.0;
		case INTERVAL: return 0;
		case WHEEL_TORQUE: return 0.1;
		case CRANK_TORQUE: return 0.1;
		case ACCELERATION: return 0.01;
		case STEPS: return 1;
		//		case STEPSPEED: return 1;
		case PULSEWIDTH: return 1;
		case TEMPERATURE: return -100.0; 
		case WINDSPEED: return 0.2;
		case AIRPRESSURE: return 100;
		case HUMIDITY: return 0.05;
		case EFFICIENCY: return 0.1;
		case SLOPE:
		case HEARTRATE: return 20;
		case WHEEL_REVS: return 2.0;
		case TIME: return 0;
		case CADENCE:
		case POWER: return 2.0;
		case POWER_PEDAL:
		case VOLTAGE:
		case LOCATION:
		case WINDDIRECTION:
		case BEARING:
		case SATELLITES: return 0;
		case SPEED: return 1.0;
		case ALTITUDE: 
		case DISTANCE:
		case GAINRATIO: return 0;
		case PROXIMITY: return 0;
		case VERTSPEED: return 0.01;
		//case WEIGHT: return 0.1;
		}	
		return 0;
	}

	public double maxValue() {
		switch(this) {
		case PROXIMITY: return 1000;
		case VERTSPEED: return 10;
		//case WEIGHT: return 300;
		case HRZONE: return 6;	
		case ENERGY: return Double.MAX_VALUE;
		case INTERVAL: return Integer.MAX_VALUE;
		case WHEEL_TORQUE: return 1000;
		case CRANK_TORQUE: return 1000;
		case ACCELERATION: return 100;
		case STEPS: return 10000000;
		//		case STEPSPEED: return 400;
		case PULSEWIDTH: return 6000;
		case TEMPERATURE: return 100; 
		case WINDSPEED: return 150;
		case AIRPRESSURE: return 108500;
		case HUMIDITY: return 1;
		case EFFICIENCY: return 20;
		case SLOPE: return 0.8;
		//		case SLOPEANGLE: return 40;
		case HEARTRATE: return 220;
		case WHEEL_REVS: return 1500;
		case TIME: return TimeUtil.MAX_TIME;
		case CADENCE: return 240;
		case POWER: return 2200;
		case POWER_PEDAL: return 1;
		case VOLTAGE: return 20;
		case ALTITUDE: return 8848; // Mount Everest
		case LOCATION: return 180;
		case WINDDIRECTION: return 360;
		case BEARING: return 360;
		case SATELLITES: return 60;
		case SPEED: return 140.0 / 3.6;
		case DISTANCE: return 40000000.0;
		case GAINRATIO: return 8;
		}
		return 0;	
	}

	public double minValue() {
		switch(this) {
		case PROXIMITY: return 0;
		case VERTSPEED: return -10;
		//case WEIGHT: return 0;

		case HRZONE: return 0;
		case ENERGY: return 0;
		case INTERVAL: return 0;
		case WHEEL_TORQUE: return 0;
		case CRANK_TORQUE: return 0;
		case ACCELERATION: return 0;
		case STEPS: return 0;
		//		case STEPSPEED: return 0;
		case PULSEWIDTH: return 200;
		case TEMPERATURE: return -55; 
		case WINDSPEED: return 0;
		case AIRPRESSURE: return 50000;
		case HUMIDITY: return 0.1;
		case EFFICIENCY: return 0.1;
		case SLOPE: return -0.8;
		//		case SLOPEANGLE: return -40;
		case HEARTRATE: return 40;
		case WHEEL_REVS: return 0;
		case TIME: return TimeUtil.MIN_TIME;
		case CADENCE: return 0;
		case POWER: return 0;
		case POWER_PEDAL: return 0;
		case VOLTAGE: return 0;
		case ALTITUDE: return -420; //Dode Zee, Jordanie
		case LOCATION: return -180;
		case WINDDIRECTION: return 0;
		case BEARING: return 0;
		case SATELLITES: return -60;
		case SPEED: return 0;
		case DISTANCE: return 0;
		case GAINRATIO: return 0.8;
		default: return 0;
		}	
	}

	public boolean isStorable() {
		switch(this) {
		case PROXIMITY: return false;
		case VERTSPEED: return false;
		//case WEIGHT: return false;

		case HRZONE: return false;
		case ENERGY: return true;
		case INTERVAL: return true;
		case WHEEL_TORQUE: return true;
		case CRANK_TORQUE: return true;
		case ACCELERATION: return true;
		case STEPS: return true;
		//		case STEPSPEED: return true;
		case PULSEWIDTH: return true;
		case TEMPERATURE: return true; 
		case WINDSPEED: return true;
		case AIRPRESSURE: return true;
		case HUMIDITY: return true;
		case EFFICIENCY: return true;
		case SLOPE: return false;
		//		case SLOPEANGLE: return false;
		case HEARTRATE: return true;
		case WHEEL_REVS: return true;
		case TIME: return true;
		case CADENCE: return true;
		case POWER: return true;
		case POWER_PEDAL: return false;
		case VOLTAGE: return false;
		case ALTITUDE: return true; 
		case LOCATION: return true;
		case BEARING: return false;
		case WINDDIRECTION: return true;
		case SATELLITES: return false;
		case SPEED: return true;
		case DISTANCE: return true;
		case GAINRATIO: return true;
		default: return false;
		}	
	}

	//	public EnumSet<ValueType> getDependentOn() {
	//		switch (this) {
	//		case HRZONE: return EnumSet.of(ValueType.HEARTRATE);
	//		case ENERGY: return EnumSet.of(ValueType.POWER);
	//		case SLOPE: return EnumSet.of(ValueType.AIRPRESSURE, ValueType.SPEED);
	//		case SPEED: return EnumSet.of(ValueType.WHEEL_REVS);
	//		case DISTANCE: return EnumSet.of(ValueType.WHEEL_REVS, ValueType.SPEED);
	//		case GAINRATIO: return EnumSet.of(ValueType.WHEEL_REVS, ValueType.CADENCE, ValueType.POWER);
	//		case EFFICIENCY: return EnumSet.of(ValueType.POWER, ValueType.HEARTRATE);
	//		default:
	//			break;
	//		}
	//		return null;	
	//	}

	//	public EnumSet<ValueType> getSupports() {
	//		switch (this) {
	//		case HEARTRATE: return EnumSet.of(this, HRZONE, EFFICIENCY);
	//		case POWER: return EnumSet.of(this, ENERGY, GAINRATIO, EFFICIENCY);
	//		case SPEED: return EnumSet.of(this, SLOPE, DISTANCE);
	//		case AIRPRESSURE: return EnumSet.of(this, SLOPE);
	//		case CADENCE: return EnumSet.of(this, GAINRATIO);
	//		case WHEEL_REVS: return EnumSet.of(this, DISTANCE, SPEED, GAINRATIO);
	//		default: return EnumSet.of(this);
	//		}
	//	}

	public double deltaToNotify() {
		switch (this) {
		case VERTSPEED: return 0.1;
		//case WEIGHT: return 1;
		case HRZONE: return 1;
		case ENERGY: return 10;
		case INTERVAL: return 1;

		case WHEEL_TORQUE: return 10;
		case CRANK_TORQUE: return 10;

		case ACCELERATION: return 1;
		case STEPS: return 10;
		//		case STEPSPEED: return 10;

		case PULSEWIDTH: return 100;
		case SATELLITES: return 4;
		case TIME: return 600000;
		case LOCATION: return 0.0001;
		case SPEED: return 5 / 3.6;
		case HEARTRATE: return 10;
		case TEMPERATURE: return 10; 
		case WINDSPEED: return 10 / 3.6;
		case AIRPRESSURE: return 1000; //Pa!!!
		case HUMIDITY: return 0.1;   
		case EFFICIENCY: return 0.5;
		case SLOPE: return 0.1;
		//		case SLOPEANGLE: return 5;
		case WHEEL_REVS: return 10;
		case CADENCE: return 20;
		case POWER: return 50;
		case POWER_PEDAL: return 0.1;
		case VOLTAGE: return 0.2;
		case ALTITUDE: return 10; 
		case BEARING: return 10;
		case WINDDIRECTION: return 10;
		case DISTANCE: return 5000;
		case GAINRATIO: return 0.5;
		case PROXIMITY: return 0.1;
		}
		return 1;
	}

	public boolean isSingleValue(Presentation presentation) {
		if (getDimensions() == 1) return false;
		switch (presentation) {
		case VALUE:
			//switch (this) {
			//case ACCELERATION: return true;
			//}
			return false;
		case AVERAGE:
		case DELTA:
		case MAX:
		case DELTADOWN:
		case DELTAUP:
		case MIN:
		case NONE:
		case VALUETIME:
			return true;
		default:
			return false;
		}
	}

	/*public boolean isSerializeCurrentValue() {
		switch(this) {
		case DISTANCE:
			return true;
		default:
			return false;
		}
	}*/

	public boolean isXtraValue() {
		switch(this) {
		case VERTSPEED:
		case WHEEL_TORQUE: 
		case CRANK_TORQUE:
		case PULSEWIDTH:
		case ACCELERATION: 
		case STEPS: 
		case HRZONE: 
		case VOLTAGE:
		case AIRPRESSURE: 
		case POWER_PEDAL:
		case SATELLITES:
		case WHEEL_REVS:
		case BEARING:
		case LOCATION:
		case SLOPE:
		case ENERGY:
			return true;
		default:
			return false;
		}
	}

	public boolean mayShow(boolean showXtra) {
		switch (this) {
		case VOLTAGE: return false;
		case PROXIMITY: return false;
		default: 
			if (showXtra) return true;
			return !isXtraValue();
		}
	}

	public Presentation presentAsMax() {
		switch (this) {
		case ENERGY:
		case DISTANCE:
		case LOCATION:
			return Presentation.NONE;
		case WINDDIRECTION:
		case BEARING:
		case TIME: 
			return Presentation.VALUE;
		default: 
			return Presentation.MAX;
		}
	}

	public Presentation presentAsAvg() {
		switch (this) {
		case ENERGY: 
		case DISTANCE:
		case LOCATION:
		case TIME:
			return Presentation.NONE;
		default:
			return Presentation.AVERAGE;
		}
	}

	public boolean handleAsValue() {
		switch (this) {
		case PROXIMITY: return false;
		case VOLTAGE: return false;
		default: return true;
		}
	}

	public boolean isChatable() {
		switch (this) {
		case TIME: return false;
		case PROXIMITY: return false;
		case VOLTAGE: return false;
		default: return true;
		}
	}



	public Presentation presentAsDeltaUp() {
		switch (this) {
		case HEARTRATE:
			if (ValueType.isShowExtraValues) return Presentation.VARIANCE;
			return Presentation.NONE;
		case PULSEWIDTH:
			return Presentation.VARIANCE;
		case POWER:
			return Presentation.NORMALIZED;
			//case HRZONE:
		case ALTITUDE:
			return Presentation.DELTAUP;
		default:
			return Presentation.NONE;
		}
	}

	public Presentation presentAsDeltaDown() {
		switch (this) {
		case POWER:
			return Presentation.VALUETIME;
		case HEARTRATE:
			if (ValueType.isShowExtraValues) return Presentation.CHANGE;
			return Presentation.NONE;
			//case HRZONE:
		case ALTITUDE:
			return Presentation.DELTADOWN;
		default:
			return Presentation.NONE;
		}
	}

	public BaseGraph.AngleLocation presentAsAngle() {
		switch (this) {
		case WINDSPEED:
			if (raw.get(ValueType.WINDDIRECTION) != null && raw.get(ValueType.WINDDIRECTION) != -1) return BaseGraph.AngleLocation.LOWERLEFT;
			return BaseGraph.AngleLocation.NONE;
		case WINDDIRECTION:
		case BEARING:
			return BaseGraph.AngleLocation.MID;
		default:
			return BaseGraph.AngleLocation.NONE;
		}
	}

	public Unit getValueTimeUnit() {
		switch (this) {
		case POWER:
			return Unit.KILOJOULE;
		default:
			return defaultUnit();
		}
	}

	public String getValueTimeText(Context context) {
		switch (this) {
		case POWER:
			return context.getString(R.string.overview_energy);
		default:
			return context.getString(R.string.valuetime);
		}
	}

	public float presentAsAngle(float angle) {
		switch (this) {
		case BEARING:
			return -angle;
		case WINDSPEED:
			return (float) WINDDIRECTION.getRaw();
		case WINDDIRECTION:
			return angle;
		default:
			return angle;
		}
	}

	public Presentation presentByDefault() {
		switch (this) {
		case DISTANCE: return Presentation.MAX;
		case WINDDIRECTION: return Presentation.NONE;
		case BEARING: return Presentation.NONE;
		case TIME: return Presentation.DELTA;
		default:
			return Presentation.VALUE;
		}
	}

	public boolean sendAsBitmap(boolean showXtra) {
		switch (this) {
		case LOCATION:
		case TIME: 
			return false;
		default:
			if (showXtra) return true;
			return !isXtraValue();
		}
	}

	public int getDimensions() {
		switch(this) {
		/*		case MAGNETICFIELD:
		case ACCELERATION:
		case GRAVITY:
		case ORIENTATION:
			return 3;
		 */
		case LOCATION:
			return 2;
		default:
			return 1;
		}
	}

	public double getMinimalChange() {
		switch (this) {
		case ENERGY: return 5;
		case INTERVAL: return 1;
		case WHEEL_TORQUE: return 0.1;
		case CRANK_TORQUE: return 0.1;
		case ACCELERATION: return 0.01;
		case STEPS: return 1;
		//		case STEPSPEED: return 1;
		case PULSEWIDTH: return 1.0;
		case TEMPERATURE: return 1.0;
		case WINDSPEED: return 0.5;
		case AIRPRESSURE: return 1.0;
		case HUMIDITY: return 0.01;
		case EFFICIENCY: return 0.01;
		case SLOPE: return 0.5;
		case HEARTRATE: return 2;
		case WHEEL_REVS: return 1;
		case TIME: return 10;
		case CADENCE: return 2;
		case POWER: return 2;
		case POWER_PEDAL: return 0.01;
		case VOLTAGE: return 0.2;
		case ALTITUDE: return 10;
		case LOCATION: return 0.00001;
		case WINDDIRECTION: return 1.0;
		case BEARING: return 1.0;
		case SATELLITES: return 1;
		case SPEED: return 0.5;
		case DISTANCE: return 5;
		case GAINRATIO: return 0.1;
		case HRZONE: return 0.1;
		case PROXIMITY: return 0.1;
		case VERTSPEED: return 0.1;
		//case WEIGHT: return 0.1;
		}
		return 0.1;

	}

	public double getMinimalAnomaly() {
		switch (this) {
		case ENERGY: return Double.MAX_VALUE;
		case INTERVAL: return 10;
		case WHEEL_TORQUE: return 25;
		case CRANK_TORQUE: return 25;
		case ACCELERATION: return 40;
		case STEPS: return Double.MAX_VALUE;
		//		case STEPSPEED: return 100;
		case PULSEWIDTH: return 1000;
		case TEMPERATURE: return 5;
		case WINDSPEED: return 10;
		case AIRPRESSURE: return 10;
		case HUMIDITY: return 0.1;
		case EFFICIENCY: return 0.05;
		case SLOPE: return 5;
		case HEARTRATE: return 10;
		case WHEEL_REVS: return 10;
		case TIME: return 10;
		case CADENCE: return 20;
		case POWER: return 20;
		case POWER_PEDAL: return 0.1;
		case VOLTAGE: return 0.5;
		case ALTITUDE: return 5;
		case LOCATION: return 0.001;
		case WINDDIRECTION: return 30.0;
		case BEARING: return 5.0;
		case SATELLITES: return 50;
		case SPEED: return 5;
		case DISTANCE: return 100;
		case GAINRATIO: return 1;
		case PROXIMITY: return 100;
		case HRZONE: return 10;
		default: return Double.MAX_VALUE;
		}
	}

	public boolean showLastValuesOnly() {
		switch(this) {
		case ACCELERATION: return true;
		case PULSEWIDTH: return true;
		default: return false;
		}
	}

	public boolean isCalcVariablility() {
		switch(this) {
		case PULSEWIDTH: return true;
		default: return false;
		}
	}

	public boolean isCalcSlope() {
		switch(this) {
		case HEARTRATE: return true;
		default: return false;
		}
	}

	public boolean isVisible() {
		switch(this) {
		case INTERVAL: return false;
		default : return true;
		}
	}

	public String getShortName() {
		return shortName;
	}

	public String getLowerCaseShortName() {
		return lowerCaseShortName;
	}	

	@SuppressLint("DefaultLocale")
	public static ValueType fromShortName(String shortName) {
		for (ValueType valueType: ValueType.values()) {
			if (valueType.getShortName().toLowerCase(Locale.US).contains(shortName.toLowerCase())) return valueType;
			//if (valueType.getShortName().equalsIgnoreCase(shortName)) return valueType;
		}
		return null;
	}

	public static Map<String, Boolean> getListSelected(Context context, Set<String> valuesSelected, boolean showXtra) {
		Map<String, Boolean> map = new TreeMap<String, Boolean>(); 
		for (ValueType valueType : ValueType.values()) {
			if (valueType.isXtraValue() && !showXtra) continue;
			boolean selected = false;
			if (valuesSelected != null) if (valuesSelected.contains(valueType.getShortName())) {
				selected = true;
			}
			map.put(valueType.getLabel(context), selected);
			//}
		}
		return map;
	}

	public static Map<ValueType, Boolean> getListSelected(Context context, Map<String, Boolean> map) {
		Map<ValueType, Boolean> set = new HashMap<ValueType, Boolean>();
		for (String mapItem : map.keySet()) {
			LLog.d(Globals.TAG, CLASSTAG + ".getListSelected: MapItem = " + mapItem + " = " + map.get(mapItem));
		}
		for (ValueType valueType : ValueType.values()) {
			Boolean bool = map.get(valueType.getLabel(context));
			if (bool != null) {
				set.put(valueType, bool);
			}
		}
		return set;
	}

	public static Set<ValueType> getListSelected(Set<String> strs) {
		Set<ValueType> set = new HashSet<ValueType>();

		for (ValueType valueType : ValueType.values()) {
			if (strs.contains(valueType.shortName))	set.add(valueType);
		}
		return set;
	}

	public boolean isZeroAtPause() {
		switch (this) {
		case VERTSPEED: return true;
		case ACCELERATION: return true;
		case AIRPRESSURE: break;
		case ALTITUDE: break;
		case BEARING: break;
		case CADENCE: return true;
		case CRANK_TORQUE: return true;
		case DISTANCE: break;
		case EFFICIENCY: return true;
		case ENERGY: break;
		case GAINRATIO: break;
		case HEARTRATE: break;
		case HRZONE: break;
		case HUMIDITY: break;
		case INTERVAL: break;
		case LOCATION: break;
		case POWER: return true;
		case POWER_PEDAL: break;
		case PROXIMITY: break;
		case PULSEWIDTH: break;
		case SATELLITES: break;
		case SLOPE: break;
		case SPEED: return true;
		case STEPS: return true;
		case TEMPERATURE: break;
		case TIME: break;
		case VOLTAGE: break;
		//case WEIGHT: break;
		case WHEEL_REVS: break;
		case WHEEL_TORQUE: return true;
		case WINDDIRECTION: break;
		case WINDSPEED: break;
		}
		return false;
	}

	public static ValueType getValueType(BleDataType bleDataType) {
		switch (bleDataType) {
		case BT_BATTERYLEVEL: return ValueType.VOLTAGE;
		case BT_CRANKREVOLUTIONS: return ValueType.CADENCE;
		case BT_HEARTRATE: return ValueType.HEARTRATE;
		case BT_TEMPERATURE: return ValueType.TEMPERATURE;
		case BT_UNKNOWN: return null;
		case BT_WHEELREVOLUTIONS: return ValueType.WHEEL_REVS;
		}
		return null;
	}

	public JSONObject toJSON(ValueItem item) throws JSONException {
		JSONObject json = new JSONObject();
		JSONArray array = new JSONArray();
		if (unitType.getUnitClass().equals(Double.class)) {
			if (getDimensions() > 1) {
				double[] values = item.getDoubleValues(true);
				for (double value: values) array.put(value);
			} else
				array.put(item.getDoubleValue());
		} else if (unitType.getUnitClass().equals(Integer.class)) {
			array.put(item.getIntValue());
		} else if (unitType.getUnitClass().equals(Float.class)) {
			array.put(item.getFloatValue());
		} else if (unitType.getUnitClass().equals(Long.class)) {
			array.put(item.getLongValue());
		} 
			
		array.put(defaultUnit().toString());
		array.put(((item.getTimeStamp() - Globals.TIMEBIAS) / 100));
		json.put(toJSONString(), array);
		return json;
	}
	
	public ValueItem fromJSON(JSONArray array) throws JSONException {
		Object obj = null;
		int index = 0;
		if (unitType.getUnitClass().equals(Double.class)) {
			if (getDimensions() > 1) {
				double[] values = new double[getDimensions()];
				for (int i = 0; i < values.length; i++) {
					values[i] = array.getDouble(index++);
				}
				obj = values;
			} 
			obj = array.getDouble(index++);
		} else if (unitType.getUnitClass().equals(Integer.class)) {
			obj = array.getInt(index++);
		} else if (unitType.getUnitClass().equals(Float.class)) {
			obj = (float) array.getDouble(index++);
		} else if (unitType.getUnitClass().equals(Long.class)) {
			obj = array.getLong(index++);
		} 
		long timeStamp = array.getLong(++index) * 100 + Globals.TIMEBIAS;
		
		return new ValueItem(obj, timeStamp);
		
	}

    public String toJSONString() {
        return "VT_" + toString();
    }

    public String toDbString() {
        return toString().toLowerCase();
    }

    public static ValueType fromJSONString(String value) {
		for (ValueType type: ValueType.values())
			if (type.toJSONString().equals(value)) return type;
		return null;
	}

    public int dbScaler() {
        switch(this) {
            case HRZONE:
            case INTERVAL:
            case ENERGY:
            case ACCELERATION:
            case STEPS:
            case VERTSPEED:
            case WHEEL_TORQUE:
            case CRANK_TORQUE:
            case WINDDIRECTION:
            case TEMPERATURE:
            case PULSEWIDTH:
            case WINDSPEED:
            case AIRPRESSURE:
            case HUMIDITY:
            case EFFICIENCY:
            case SLOPE:
            case HEARTRATE:
            case WHEEL_REVS:
            case TIME:
            case CADENCE:
            case POWER:
            case POWER_PEDAL:
            case VOLTAGE:
            case ALTITUDE:
            case LOCATION:
            case BEARING:
            case SATELLITES:
            case SPEED:
            case DISTANCE:
            case GAINRATIO:
            case PROXIMITY:
            default:
                return 1000;
        }
    }

    public boolean hasTotal() {
        switch(this) {
            case ENERGY:
            case DISTANCE:
                return true;
            default:
                return false;
        }
    }

    public boolean hasMax() {
        switch(this) {
            case HEARTRATE:
            case ALTITUDE:
                return true;
            default:
                return false;
        }
    }

    public boolean hasDeltaTotal() {
        switch(this) {
            case ALTITUDE:
                return true;
            default:
                return false;
        }
    }
}
