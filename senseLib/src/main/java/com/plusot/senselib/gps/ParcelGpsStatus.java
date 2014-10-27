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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.os.Parcel;
import android.os.Parcelable;

import com.plusot.common.share.LLog;
import com.plusot.javacommon.util.StringUtil;
import com.plusot.senselib.parcels.ParcelAbstract;
import com.plusot.senselib.parcels.ParcelType;

public class ParcelGpsStatus extends ParcelAbstract {
	private static String TAG = ParcelGpsStatus.class.getSimpleName();

	/* These package private values are modified by the LocationManager class */
	private final int timeToFirstFix;
	private final int event;
	private Satellite satellites[];


	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(event);
		dest.writeParcelableArray(satellites, flags);
		//dest.writeParcelableArray(satellites, flags);
		dest.writeInt(timeToFirstFix);
	}

	public ParcelGpsStatus(Parcel in) {
		event = in.readInt();
		Parcelable[] object = in.readParcelableArray(Satellite.class.getClassLoader());
		if (object != null) {
			LLog.i(TAG, "Object not null");
			if (object.length > 0) {
				int iLength = 0;
				while (object[iLength] != null) iLength++;
				satellites = new Satellite[iLength];
				for (int i = 0; i < iLength; i++) {
					if (object[i] != null && object[i] instanceof Satellite) {
					satellites[i] = (Satellite) object[i];		
					}
				}
			}
		}
		else
			satellites = null;
		timeToFirstFix = in.readInt();
	}

	public ParcelGpsStatus(int event, GpsStatus status) {
		this.event = event;
		timeToFirstFix = status.getTimeToFirstFix();
		int max = status.getMaxSatellites();
		if (max > 0) {
			List<Satellite> list = new ArrayList<Satellite>();
			//satellites = new Satellite[max];
			Iterator<GpsSatellite> it = status.getSatellites().iterator();
			while (it.hasNext()) {
				list.add(new Satellite(it.next()));
			}	
			if (list.size() > 0) {
				//LLog.i(TAG, "Satellites available: " + list.size());
				satellites = list.toArray(new Satellite[0]);
			} else {
				satellites = null;
			}
		} else 
			satellites = null;
	}

	public static final Parcelable.Creator<ParcelGpsStatus> CREATOR = new Parcelable.Creator<ParcelGpsStatus>() {
		public ParcelGpsStatus createFromParcel(Parcel in) {
			return new ParcelGpsStatus(in);
		}

		public ParcelGpsStatus[] newArray(int size) {
			return new ParcelGpsStatus[size];
		}
	};

	@Override
	public ParcelType getParcelType() {
		return ParcelType.PARCEL_GPSSTATUS;
	}

	@Override
	public String toString() {
		if (satellites == null) {
			return "GpsStatus: null, event = " + event + ", timetofirstfix = " + timeToFirstFix;
		}
		return "GpsStatus: " + StringUtil.toString(satellites) + ", event = " + event + ", timetofirstfix = " + timeToFirstFix;
	}

	public Satellite[] getSatellites() {
		return satellites;
	}

	public int getTimeToFirstFix() {
		return timeToFirstFix;
	}

	public int getEvent() {
		return event;
	}
		
}
