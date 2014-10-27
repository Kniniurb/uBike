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


public enum UnitType {
	VOLTAGE_UNIT(Double.class),
	PRESSURE_UNIT(Double.class),
	WIND_UNIT(Double.class),
	TEMPERATURE_UNIT(Double.class),
	EFFICIENCY_UNIT(Double.class),
	DATETIME_UNIT(Long.class),
	PULSEWIDTH_UNIT(Integer.class),
	SECONDTIME_UNIT(Long.class),
	SPEED_UNIT(Double.class),
	MAGNETICFIELD_UNIT(Float.class),
	ACCELERATION_UNIT(Float.class),
	DISTANCE_UNIT(Double.class),
	ANGLE_UNIT(Double.class),
	CYCLES_UNIT(Double.class),
	BEATS_UNIT(Integer.class),
	WEIGHT_UNIT(Integer.class),
	COUNT_UNIT(Integer.class),
	POWER_UNIT(Double.class),
	ENERGY_UNIT(Double.class),
	TORQUE_UNIT(Double.class),
	RATIO_UNIT(Double.class),
	STEP_UNIT(Integer.class);
//	STEPSPEED_UNIT(Double.class);

	private final Class<?> unitClass;


	private UnitType(final Class<?> unitClass) {
		this.unitClass = unitClass;
	}
	
	public boolean isNumeric() {
		switch (this) {
		case WEIGHT_UNIT:
		case STEP_UNIT:
//		case STEPSPEED_UNIT:	
		case PULSEWIDTH_UNIT:
		case VOLTAGE_UNIT:
		case PRESSURE_UNIT:
		case WIND_UNIT:
		case TEMPERATURE_UNIT:
		case EFFICIENCY_UNIT:
		case SECONDTIME_UNIT: 
		case SPEED_UNIT: 
		case ACCELERATION_UNIT: 
		case MAGNETICFIELD_UNIT: 
		case DISTANCE_UNIT: 
		case ANGLE_UNIT: 
		case CYCLES_UNIT: 
		case BEATS_UNIT: 
		case COUNT_UNIT: 
		case POWER_UNIT:
		case TORQUE_UNIT: 
		case ENERGY_UNIT:
		case RATIO_UNIT: return true;
		case DATETIME_UNIT: return false;
		}
		return true;
	}

	public Class<?> getUnitClass() {
		return unitClass;
	}

}
