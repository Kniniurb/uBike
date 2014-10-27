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

import android.os.Parcelable;

public abstract class ParcelAbstract implements Parcelable {

	/*public Parcelable createInstance(ParcelType type, Parcelable parcel) {
		switch (type) {
		case PARCEL_LOCATION: return new ParcelLocation(parcel);
		case PARCEL_PROVIDER: return new ParcelProvider(parcel);
		}
		
	}*/
	
	public abstract ParcelType getParcelType();

}
