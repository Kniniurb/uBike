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
package com.plusot.common.util;

import android.location.Location;

public class Calc {
	
	public static double bearing(Location loc1, Location loc2) { 
		//http://www.movable-type.co.uk/scripts/latlong.html
		double lat1 = loc1.getLatitude() * Math.PI / 180;
		double lng1 = loc1.getLongitude() * Math.PI / 180;
		double lat2 = loc2.getLatitude() * Math.PI / 180;
		double lng2 = loc2.getLongitude() * Math.PI / 180;
		//double dLat = lat2 - lat1;
		double dLon = lng2 - lng1;

		double y = Math.sin(dLon) * Math.cos(lat2);
		double x = Math.cos(lat1)*Math.sin(lat2) -
				Math.sin(lat1)*Math.cos(lat2)*Math.cos(dLon);
		return Math.atan2(y, x) * 180 / Math.PI;
	}
	
	/*public static double distance(Location loc1, Location loc2) {
		//Haversine formula
		double EARTH_RADIUS = 6367.45;
		double lat1 = loc1.getLatitude() * Math.PI / 180;
		double lng1 = loc1.getLongitude() * Math.PI / 180;
		double lat2 = loc2.getLatitude() * Math.PI / 180;
		double lng2 = loc2.getLongitude() * Math.PI / 180;
		double dLat = lat2 - lat1;
		double dLon = lng2 - lng1;
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
		return EARTH_RADIUS*c;
	}*/

	public static double absChange(double d1, double d2) {
		double delta = Math.abs(d1 - d2);
		double mean = (Math.abs(d1) + Math.abs(d2)) / 2;
		if (mean == 0 && delta > 0) return 1;
		if (mean == 0 && delta == 0) return 0;
		return delta / mean;
	}
	
	public static double length(double d[]) {
		double sum = 0;
		for (int i = 0; i < d.length; i++) {
			sum += d[i] * d[i];
		}
		return Math.sqrt(sum);
	}
	
	public static float length(float d[]) {
		float sum = 0;
		for (int i = 0; i < d.length; i++) {
			sum += d[i] * d[i];
		}
		return (float) Math.sqrt(sum);
	}

	public static double[] unitVector(double d[]) {
		double v[] = new double[d.length];
		double length = length(d);
		for (int i = 0; i < d.length; i++) {
			v[i] = d[i] / length;
		}
		return v;
	}


}
