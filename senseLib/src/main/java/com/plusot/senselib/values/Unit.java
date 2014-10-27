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

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import android.content.Context;

import com.plusot.common.util.ElapsedTime;
import com.plusot.javacommon.util.Format;
import com.plusot.javacommon.util.Scaler;
import com.plusot.javacommon.util.TimeUtil;
import com.plusot.senselib.R;

public enum Unit{
	KG(R.string.kg, R.string.kg_speech, R.string.kg_choice, UnitType.WEIGHT_UNIT),
	VOLTAGE(R.string.volt, R.string.volt_speech, R.string.volt_choice, UnitType.VOLTAGE_UNIT),
	CELCIUS(R.string.celcius, R.string.celcius_speech, R.string.celcius_choice, UnitType.TEMPERATURE_UNIT),
	FAHRENHEIT(R.string.fahrenheit, R.string.fahrenheit_speech, R.string.fahrenheit_choice, UnitType.TEMPERATURE_UNIT),
	INHG(R.string.inhg, R.string.inhg_speech, R.string.inhg_choice, UnitType.PRESSURE_UNIT, 1.0 / 3386.389),
	HPA(R.string.hpa, R.string.hpa_speech, R.string.hpa_choice, UnitType.PRESSURE_UNIT, 1.0 / 100),
	PSI(R.string.psi, R.string.psi_speech, R.string.psi_choice, UnitType.PRESSURE_UNIT, 1.0 / 6894.755),
	MBAR(R.string.mb, R.string.mb_speech, R.string.mb_choice, UnitType.PRESSURE_UNIT, 1.0 / 100),
	EFFICIENCY(R.string.effic, R.string.effic_speech, R.string.effic_choice , UnitType.EFFICIENCY_UNIT),
	DATETIME(R.string.datetime, R.string.datetime_speech, R.string.datetime_choice , UnitType.DATETIME_UNIT, "H:mm:ss"),
	SECONDTIME(R.string.datetime, R.string.datetime_speech, R.string.datetime_choice , UnitType.SECONDTIME_UNIT, 1.0),
	DEGREE(R.string.degree, R.string.degree_speech, R.string.degree_choice , UnitType.ANGLE_UNIT),
	RPM(R.string.rpm, R.string.rpm_speech, R.string.rpm_choice , UnitType.CYCLES_UNIT, 1.0), 
	BPM(R.string.bpm, R.string.bpm_speech, R.string.bpm_choice , UnitType.BEATS_UNIT),
	PULSEWIDTH(R.string.pulse, R.string.pulse_speech, R.string.pulse_choice , UnitType.PULSEWIDTH_UNIT),
	STEPS(R.string.stepsnumber, R.string.steps_speech, R.string.steps_choice , UnitType.STEP_UNIT),
//	STEPSPERMINUTE(R.string.stepsperminute, R.string.stepsperminute_speech, R.string.stepsperminute_choice , UnitType.STEPSPEED_UNIT),
	COUNT(R.string.count, R.string.count_speech, R.string.count_choice , UnitType.COUNT_UNIT),
	TESLA(R.string.tesla, R.string.tesla_speech, R.string.tesla_choice , UnitType.MAGNETICFIELD_UNIT),
	MPSS(R.string.mpss, R.string.mpss_speech, R.string.mpss_choice , UnitType.ACCELERATION_UNIT),
	FPSS(R.string.fpss, R.string.fpss_speech, R.string.fpss_choice , UnitType.ACCELERATION_UNIT, 3.280840),
	KPH(R.string.kph, R.string.kph_speech, R.string.kph_choice , UnitType.SPEED_UNIT, 3.6),
	MPS(R.string.mps, R.string.mps_speech, R.string.mps_choice , UnitType.SPEED_UNIT),
	MPH(R.string.mph, R.string.mph_speech, R.string.mph_choice , UnitType.SPEED_UNIT, 2.236936),
	FPS(R.string.fps, R.string.fps_speech, R.string.fps_choice , UnitType.SPEED_UNIT, 3.280840),
	FPM(R.string.fpm, R.string.fpm_speech, R.string.fpm_choice , UnitType.SPEED_UNIT, 3.280840 * 60.0),
	KNOT(R.string.knot, R.string.knot_speech, R.string.knot_choice , UnitType.SPEED_UNIT, 1.943844),
	BEAUFORT(R.string.bft,R.string.beaufort_speech,R.string.beaufort_choice, UnitType.SPEED_UNIT),
	METER(R.string.meter, R.string.meter_speech, R.string.meter_choice , UnitType.DISTANCE_UNIT),
	KM(R.string.km, R.string.km_speech, R.string.km_choice , UnitType.DISTANCE_UNIT, 0.001),
	INCH(R.string.inch,R.string.inch_speech, R.string.inch_choice , UnitType.DISTANCE_UNIT, 39.3700787),
	FEET(R.string.feet, R.string.feet_speech, R.string.feet_choice , UnitType.DISTANCE_UNIT, 3.2808399),
	YARD(R.string.yard, R.string.yard_speech, R.string.yard_choice , UnitType.DISTANCE_UNIT, 1.0936),
	MILE(R.string.mile, R.string.mile_speech, R.string.mile_choice , UnitType.DISTANCE_UNIT, 0.000621371192),
	JOULE(R.string.joule, R.string.joule_speech, R.string.joule_choice , UnitType.ENERGY_UNIT),
	KILOJOULE(R.string.kilojoule, R.string.kilojoule_speech, R.string.kilojoule_choice , UnitType.ENERGY_UNIT, 0.001),
	WATT(R.string.watt, R.string.watt_speech, R.string.watt_choice , UnitType.POWER_UNIT),
	PK(R.string.pk, R.string.pk_speech, R.string.pk_choice , UnitType.POWER_UNIT, 0.0013596216173),
	HP(R.string.hp, R.string.hp_speech, R.string.hp_choice , UnitType.POWER_UNIT, 0.00134048257373),
	NM(R.string.nm, R.string.nm_speech, R.string.nm_choice , UnitType.TORQUE_UNIT, 1.0),
	PERCENT(R.string.percent, R.string.percent_speech, R.string.percent_choice , UnitType.RATIO_UNIT, 100.0),
	RATIO(R.string.ratio, R.string.ratio_speech, R.string.ratio_choice , UnitType.RATIO_UNIT, 1.0),
	NONE(R.string.none, R.string.dummy, R.string.dummy, UnitType.COUNT_UNIT, 1.0);

	private final String PARTS_DELIMITER = "  ";
	private final int stringId;
	private final int speechId;
	private final int choiceId;
	private final Number multiplier;
	private final UnitType unitType;
	private final String pattern;
	private static Map<Unit, Scaler> scalers = new EnumMap<Unit, Scaler>(Unit.class);

	public class UnitScaler implements Scaler {
		final Unit unit;

		private UnitScaler(final Unit unit) {
			this.unit = unit;
		}

		@Override
		public Number onScale(Number value) {
			return unit.scale(value);
		}

		@Override
		public Number onScaleBack(Number value) {
			return unit.scaleBack(value);
		}
	}

	private Unit(final int stringId, final int speechId, final int choiceId, final UnitType unitType, final Number multiplier, final String pattern) {
		this.stringId = stringId;
		this.speechId = speechId;
		this.choiceId = choiceId;
		this.multiplier = multiplier;
		this.unitType = unitType;
		this.pattern = pattern;
	}

	private Unit(final int stringId, final int speechId, final int choiceId, final UnitType unitType, final Number multiplier) {
		this(stringId, speechId, choiceId, unitType, multiplier, null);
	}

	private Unit(final int stringId, final int speechId, final int choiceId, final UnitType unitType, final String pattern) {
		this(stringId, speechId, choiceId, unitType, null, pattern);
	}

	private Unit(final int stringId, final int speechId, final int choiceId, final UnitType unitType) {
		this(stringId, speechId, choiceId, unitType, null, null);
	}


	public String getLabel(Context context) {
		return context.getText(stringId).toString();
	}

	public String getSpeechLabel(Context context) {
		return context.getText(speechId).toString();
	}

	public String getChoiceLabel(Context context) {
		return context.getText(choiceId).toString();
	}

	/*public float getMultiplierAsFloat() {
		if (this.equals(Unit.SECONDTIME)) return 0.001f;
		if (multiplier == null) return 1.0f;
		return multiplier.floatValue();
	}

	public Number getMultiplier() {
		if (multiplier == null) return 1.0;
		return multiplier;
	}*/



	public Scaler getScaler() {
		Scaler scaler = scalers.get(this);
		if (scaler != null) return scaler;
		scaler = new UnitScaler(this);
		scalers.put(this, scaler);
		return scaler;
	}

	public Number scale(Number value) {
		switch (this) {
		case BEAUFORT:
			return Math.pow(value.doubleValue() / 0.836, 2.0 / 3.0);
		case FAHRENHEIT:
			return value.doubleValue() * 1.8 + 32.0;
		default:
			if (unitType.getUnitClass().equals(Long.class)) {
				return value.longValue();
			} else if (unitType.getUnitClass().equals(Integer.class)) {
				return value.intValue();
			} else if (unitType.getUnitClass().equals(String.class)) {
				return 0;
			} else if (unitType.getUnitClass().equals(Double.class) || unitType.getUnitClass().equals(Float.class)) {
				if (multiplier == null) return value.doubleValue();
				return multiplier.doubleValue() * value.doubleValue();	
			} else
				return 0;
		}
	}
	
	public double scale(double value) {
		switch (this) {
		case BEAUFORT:
			return Math.pow(value / 0.836, 2.0 / 3.0);
		case FAHRENHEIT:
			return value * 1.8 + 32.0;
		default:
			if (unitType.getUnitClass().equals(Long.class)) {
				return value;
			} else if (unitType.getUnitClass().equals(Integer.class)) {
				return value;
			} else if (unitType.getUnitClass().equals(String.class)) {
				return 0;
			} else if (unitType.getUnitClass().equals(Double.class) || unitType.getUnitClass().equals(Float.class)) {
				if (multiplier == null) return value;
				return multiplier.doubleValue() * value;	
			} else
				return 0;
		}
	}

	public Number scaleBack(Number value) {
		switch (this) {
		case BEAUFORT:
			return Math.pow(value.doubleValue(), 1.5) * 0.836;
		case FAHRENHEIT:
			return (value.doubleValue() - 32.0) / 1.8;
		default:
			if (unitType.getUnitClass().equals(Long.class)) {
				return value.longValue();
			} else if (unitType.getUnitClass().equals(Integer.class)) {
				return value.intValue();
			} else if (unitType.getUnitClass().equals(String.class)) {
				return 0;
			} else if (unitType.getUnitClass().equals(Double.class) || unitType.getUnitClass().equals(Float.class)) {
				if (multiplier == null) return value.doubleValue();
				return value.doubleValue() / multiplier.doubleValue();	
			} else
				return 0;
		}
	}

	public String getFormated(ValueItem value, int decimals, boolean speech) {
		return getFormated(value, decimals, null, speech);
	}
	
	public String getFormated(ValueItem value, int decimals) {
		return getFormated(value, decimals, null, false);
	}

	public String getFormated(Object obj, int decimals, boolean speech) {
		return getFormated(new ValueItem(obj, System.currentTimeMillis()), decimals, null, speech);
	}
	
	public String getFormated(Object obj, int decimals) {
		return getFormated(new ValueItem(obj, System.currentTimeMillis()), decimals, null, false);
	}

	public String getFormated(ValueItem value, int decimals, String delim, boolean speech) {
		if (unitType.getUnitClass().equals(Long.class)) {
			switch (unitType) {
			case DATETIME_UNIT:
				if (pattern != null) {
					String result;
					if (value.getLongValue() < TimeUtil.YEAR) {
						//if (pattern.contains(".SSS"))
						//	result = Util.formatElapsedMilli(value.getLongValue(), Globals.appContext, false);
						//else
							result = ElapsedTime.format(value.getLongValue(), false, speech);
					} else
						result = TimeUtil.formatTime(value.getLongValue(), pattern);
					/*if (pattern.contains(".SSS") && decimals < 3 && decimals > 0) {
						result = result.substring(0, result.length() - 3 + decimals);
					} else if (pattern.contains(".SSS") && decimals == 0) {
						result = result.substring(0, result.length() - 4);
					}*/
					return result;
				} 
				return TimeUtil.formatTime(value.getLongValue());
			case SECONDTIME_UNIT:
				return ElapsedTime.formatAuto(value.getLongValue(), false, speech);			
			default:
				return String.valueOf(value.getLongValue());
			}
		} else if (unitType.getUnitClass().equals(Integer.class)) {
			double[] doubles = value.getDoubleValues(false);
			if (doubles != null && doubles.length > 1) {
				StringBuilder sb = new StringBuilder();
				for (double d: doubles) {
					if (sb.length() > 0) {
						if (delim == null)
							sb.append(PARTS_DELIMITER);
						else
							sb.append(delim);
					}
					sb.append(Format.format(d, 0));
				}
				return sb.toString().trim();
			}
			return String.valueOf(value.getIntValue());
		} else if (unitType.getUnitClass().equals(String.class)) {
			return value.getStringValue();
		} else if (unitType.getUnitClass().equals(Double.class) || unitType.getUnitClass().equals(Float.class)) {
			double[] doubles = value.getDoubleValues(false);
			if (doubles == null) {
				double d = scale(value.getDoubleValue());
				return Format.format(d, decimals);
			} else {
				StringBuilder sb = new StringBuilder();
				for (double d: doubles) {
					if (sb.length() > 0) {
						if (delim == null)
							sb.append(PARTS_DELIMITER);
						else
							sb.append(delim);
					}
					sb.append(Format.format(scale(d), decimals));
				}
				return sb.toString().trim();
			}
		} else
			return "unknown unit class";
	}




	public static EnumSet<Unit> getValidUnits(UnitType unitType) {
		if (unitType == null) return EnumSet.noneOf(Unit.class);
		switch (unitType) {
		case WEIGHT_UNIT: return EnumSet.of(Unit.KG);
		case STEP_UNIT: return EnumSet.of(Unit.STEPS);
//		case STEPSPEED_UNIT: return EnumSet.of(Unit.STEPSPERMINUTE);
		case PULSEWIDTH_UNIT: return EnumSet.of(Unit.PULSEWIDTH);
		case PRESSURE_UNIT: return EnumSet.of(Unit.HPA, Unit.INHG, Unit.PSI, Unit.MBAR);
		case WIND_UNIT: return EnumSet.of(Unit.KPH, Unit.MPS, Unit.MPH, Unit.FPS, Unit.FPM, Unit.KNOT, Unit.BEAUFORT);
		case TEMPERATURE_UNIT: return EnumSet.of(Unit.CELCIUS, Unit.FAHRENHEIT);
		case VOLTAGE_UNIT: return EnumSet.of(Unit.VOLTAGE);
		case DATETIME_UNIT: return EnumSet.of(Unit.DATETIME);
		case SECONDTIME_UNIT: return EnumSet.of(Unit.SECONDTIME);
		case MAGNETICFIELD_UNIT: return EnumSet.of(Unit.TESLA);
		case ACCELERATION_UNIT: return EnumSet.of(Unit.MPSS, Unit.FPSS);
		case SPEED_UNIT: return EnumSet.of(Unit.KPH, Unit.MPS, Unit.MPH, Unit.FPS, Unit.KNOT);
		case DISTANCE_UNIT: return EnumSet.of(Unit.KM, Unit.MILE, Unit.METER, Unit.FEET, Unit.INCH, Unit.YARD);
		case ANGLE_UNIT: return EnumSet.of(Unit.DEGREE);
		case CYCLES_UNIT: return EnumSet.of(Unit.RPM);
		case BEATS_UNIT: return EnumSet.of(Unit.BPM);
		case COUNT_UNIT: return EnumSet.of(Unit.COUNT);
		case POWER_UNIT: return EnumSet.of(Unit.WATT, Unit.PK, Unit.HP);
		case TORQUE_UNIT: return EnumSet.of(Unit.NM);
		case RATIO_UNIT: return EnumSet.of(Unit.RATIO, Unit.PERCENT);
		case EFFICIENCY_UNIT: return EnumSet.of(Unit.RATIO, Unit.PERCENT);
		case ENERGY_UNIT: return EnumSet.of(Unit.JOULE, Unit.KILOJOULE);
		}
		return EnumSet.noneOf(Unit.class);
	}

	public static int getValidUnitsCount(UnitType unitType) {
		return getValidUnits(unitType).size();
	}

	public static String[] getValidUnits(UnitType unitType, Context context) {
		if (unitType == null) return null;
		EnumSet<Unit> units = getValidUnits(unitType);
		List<String> list = new ArrayList<String>();
		for (Unit unit : units) {
			list.add(unit.getChoiceLabel(context));
		}
		return list.toArray(new String[0]);
	}

	public static Unit getUnitByChoiceString(String value, Context context) {
		for (Unit unit: Unit.values()) {
			if (unit.getChoiceLabel(context).equals(value)) return unit;
		}
		return null;
	}

	public static Unit getNextUnit(Unit unit, UnitType unitType, Context context) {
		if (unitType == null) return null;
		EnumSet<Unit> units = getValidUnits(unitType);
		int i = 0, iStop = -1;
		Unit firstUnit = null;
		for (Unit tempUnit : units) {
			if (firstUnit == null) firstUnit = tempUnit;
			if (tempUnit.equals(unit)) iStop = i + 1;
			if (iStop == i) {
				return tempUnit;
			}
			i++;
		}
		if (firstUnit != null) return firstUnit;
		return unit;
	}

	public UnitType getUnitType() {
		return unitType;
	}

	public static Unit fromString(String unitStr) {
		if (unitStr == null) return null;
		for (Unit unit: Unit.values()) {
			if (unit.toString().equals(unitStr)) return unit;
		}
		return null;
	}

}
