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
package com.plusot.senselib.widget;

import java.util.Locale;

public class GraphUtil {
	public static String format(double value) {
		int decimals = 0;
		double power = Math.log10(Math.abs(value));
		if (value != 0) {
			if (power > 1) 
				decimals = 0;
			else
				decimals = 2 - (int) Math.round(power);
		}
		String format = "%1." + decimals + "f";
		return String.format(Locale.US, format, value);
	}

	public static String format(double value, int decimals) {
		if (decimals < 0) return format(value);
		String format = "%1." + decimals + "f";
		return String.format(Locale.US, format, value);
	}

	public static String formatTime(long value) {
		if (value > 86400000) return "day " + value / 86400000; 
		int hours = (int)(value / 3600000);
		value %= 3600000;
		int min = (int)(value / 60000);
		value %= 60000;
		if (hours == 0 && min == 0) return String.format(Locale.US, "%d", value / 1000);
		if (hours == 0) return String.format(Locale.US, "%d:%02d", min, value / 1000);
		return String.format(Locale.US, "%d:%02d:%02d", hours, min, value / 1000);
	}

	public static int randomColor(int alpha, int intensity) {
		int color = ((0xFF & alpha) << 24);
		int[] rgb = new int[3];
		int refIntensity = 0;
		do {
			for (int i = 0; i < 3; i++) {
				refIntensity = 0;
				rgb[i] = (int) (Math.random() * 255);
				if (rgb[i] > refIntensity) refIntensity = rgb[i];
			}
		} while (Math.abs(rgb[0] - rgb[1]) < 70 || Math.abs(rgb[1] - rgb[2]) < 70 || Math.abs(rgb[0] - rgb[2]) < 70 || refIntensity == 0);


		for (int i = 0; i < 3; i++) {
			rgb[i] = rgb[i] * intensity / refIntensity;
		}
		color |= ((0xFF & rgb[0]) << 16);
		color |= ((0xFF & rgb[1]) << 8);
		color |= (0xFF & rgb[2]);
		return color;

	}

	public static int dimColor(int color, float dim) {
		int result = color;
		int multiplier = (int)(dim * 1000);
		int devider = 1000;
		int rgb[] = new int[3];
		rgb[0] = 0xFF & (color >> 16);
		rgb[1] = 0xFF & (color >> 8);
		rgb[2] = 0xFF & color;
		result = color & 0xFF000000;
		result |= ((0xFF & (rgb[0] * multiplier / devider)) << 16);
		result |= ((0xFF & (rgb[1] * multiplier / devider)) << 8);
		result |= (0xFF & (rgb[2] * multiplier / devider));
		return result;
	}

	/*public static TreeMap<Float, Float>[] split(TreeMap<Float, float[]> map) {
		List<Map<Float, Float>> result = new ArrayList<Map<Float, Float>>();
		return result.toArray(new Map<Float,Float>[0]);
	}*/

}
