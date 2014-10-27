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

import android.os.Parcel;
import android.os.Parcelable;


public class FloatsParcel implements Parcelable {
	//private static String TAG = GeoPointParcel.class.getSimpleName();
	private float[] floats;
	
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(floats.length);
		for (float f: floats) 
			dest.writeFloat(f);
	}

	public FloatsParcel(Parcel in) {
		floats = new float[in.readInt()];
		for (int i = 0; i < floats.length; i++)
			floats[i] =in.readFloat();
	}

	public FloatsParcel(float floats[]) {
		this.floats = floats.clone();
	}

	public static final Parcelable.Creator<FloatsParcel> CREATOR = new Parcelable.Creator<FloatsParcel>() {
		public FloatsParcel createFromParcel(Parcel in) {
			return new FloatsParcel(in);
		}

		public FloatsParcel[] newArray(int size) {
			return new FloatsParcel[size];
		}
	};
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (float f : floats) {
			sb.append(" ").append(f);
		}
		return "Floats:" + sb.toString();
	}
	
	public float[] getFloats() {
		return floats.clone();
	}
}
