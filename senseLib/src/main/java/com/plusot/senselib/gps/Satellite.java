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
package com.plusot.senselib.gps;

import android.location.GpsSatellite;
import android.os.Parcel;
import android.os.Parcelable;

public class Satellite implements Parcelable {
	private final boolean mHasEphemeris;
	private final boolean mHasAlmanac;
	private final boolean mUsedInFix;
	private final int mPrn;
	private final float mSnr;
	private final float mElevation;
	private final float mAzimuth;
    
    public Satellite(final GpsSatellite gpsSatellite) {
    	mHasAlmanac = gpsSatellite.hasAlmanac();
    	mHasEphemeris = gpsSatellite.hasEphemeris();
    	mUsedInFix = gpsSatellite.usedInFix();
    	mPrn = gpsSatellite.getPrn();
    	mSnr = gpsSatellite.getSnr();
    	mElevation = gpsSatellite.getElevation();
    	mAzimuth = gpsSatellite.getAzimuth();
    }
    
    public Satellite(final Parcel src) {
		mHasAlmanac = (src.readByte() == 1);
    	mHasEphemeris = (src.readByte() == 1);
    	mUsedInFix = (src.readByte() == 1);
    	mPrn = src.readInt();
    	mSnr = src.readFloat();
    	mElevation = src.readFloat();
    	mAzimuth = src.readFloat();
    }

	public static final Parcelable.Creator<Satellite> CREATOR = new Parcelable.Creator<Satellite>() {
		public Satellite createFromParcel(Parcel in) {
			return new Satellite(in);
		}

		public Satellite[] newArray(int size) {
			return new Satellite[size];
		}
	};

	public boolean hasEphemeris() {
		return mHasEphemeris;
	}

	public boolean hasAlmanac() {
		return mHasAlmanac;
	}

	public boolean usedInFix() {
		return mUsedInFix;
	}

	public int getPrn() {
		return mPrn;
	}

	public float getSnr() {
		return mSnr;
	}

	public float getElevation() {
		return mElevation;
	}

	public float getAzimuth() {
		return mAzimuth;
	}

	@Override
	public int describeContents() {
		return 0;
	}
	
	static byte boolToByte(boolean bool) {
		if (bool) return 1;
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeByte(boolToByte(mHasAlmanac));
    	dest.writeByte(boolToByte(mHasEphemeris));
    	dest.writeByte(boolToByte(mUsedInFix));
    	dest.writeInt(mPrn);
    	dest.writeFloat(mSnr);
    	dest.writeFloat(mElevation);
    	dest.writeFloat(mAzimuth);
	}
	
	@Override
	public String toString() {
		return "" + mPrn;
	}
	
	
	
}
