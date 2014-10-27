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

import android.content.Context;

import com.plusot.senselib.R;

public enum BatteryStatus {
	NEW(R.string.battery_new, 1),
	GOOD(R.string.battery_good, 2),
	OK(R.string.battery_ok, 3),
	LOW(R.string.battery_low, 4),
	CRITICAL(R.string.battery_critical, 5),
	UNKNOWN(R.string.battery_unknown, -1);
	
	private final int resId;
	private final int id;
	
	private BatteryStatus(final int resId, final int id) {
		this.resId = resId;
		this.id = id;
	}
	
	public String getLabel(Context context) {
		return context.getString(resId);
	}
	
	public static BatteryStatus getById(int id) {
		for (BatteryStatus status: BatteryStatus.values()) {
			if (status.id == id) return status;
		}
		return BatteryStatus.UNKNOWN;
	}

}
