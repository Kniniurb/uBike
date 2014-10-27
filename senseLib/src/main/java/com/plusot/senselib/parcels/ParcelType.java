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
package com.plusot.senselib.parcels;

import com.plusot.senselib.gps.ParcelGpsStatus;

public enum ParcelType {
	//PARCEL_LOCATION("parcelLocation", ParcelLocation.class),
	//PARCEL_INFO("parcelInfo", ParcelGpsInfo.class),
	PARCEL_GPSSTATUS("parcelGpsStatus", ParcelGpsStatus.class);
	
	private final String label;
	private final Class<?> parcelClass;
	
	private ParcelType(final String label, final Class<?> parcelClass) {
		this.label = label;
		this.parcelClass = parcelClass;
	}

	@Override 
	public String toString() {
		return label;
	}
	
	public Class<?> getParcelClass() {
		return parcelClass;
	}
	
	
	public static ParcelType fromString(String label) {
		for (ParcelType parcelType: ParcelType.values()) {
			if (label.equals(parcelType.toString())) {
				return parcelType;
			}
		}
		return null;
	}
}
