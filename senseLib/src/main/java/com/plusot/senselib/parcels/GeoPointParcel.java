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

import org.osmdroid.util.GeoPoint;

import android.os.Parcel;
import android.os.Parcelable;


public class GeoPointParcel implements Parcelable {
	//private static String TAG = GeoPointParcel.class.getSimpleName();
	private int lat;
	private int lng;
	private int alt;

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(lat);
		dest.writeInt(lng);
		dest.writeInt(alt);
		
	}

	public GeoPointParcel(Parcel in) {
		lat = in.readInt();
		lng = in.readInt();
		alt = in.readInt();
	}

	public GeoPointParcel(GeoPoint point) {
		this.lat = point.getLatitudeE6();
		this.lng = point.getLongitudeE6();
		this.alt = point.getAltitude();
	}

	public static final Parcelable.Creator<GeoPointParcel> CREATOR = new Parcelable.Creator<GeoPointParcel>() {
		public GeoPointParcel createFromParcel(Parcel in) {
			return new GeoPointParcel(in);
		}

		public GeoPointParcel[] newArray(int size) {
			return new GeoPointParcel[size];
		}
	};
	
	@Override
	public String toString() {
		return "GpsPoint: " + lat + ", " + lng + ", " + alt;
	}
	
	public GeoPoint getGeoPoint() {
		return new GeoPoint(lat, lng, alt);
	}
}
