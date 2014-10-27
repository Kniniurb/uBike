package com.plusot.javacommon.util;

import java.util.Locale;

public class Format {
	public static String format(double value, int decimals) {
		if (decimals < 0) return format(value);
		/*if (Math.abs(value) < 1.0 && decimals == 0) decimals = 1;
		if (Math.abs(value) < 0.1 && decimals <= 1) decimals = 2;
		if (Math.abs(value) < 0.01 && decimals <= 2) decimals = 3;
		 */
		String format = "%1." + decimals + "f";
		return String.format(Locale.US, format, value).trim();
	}

	public static String format(double value) {
		int decimals = 0;
		double power = Math.log10(Math.abs(value));
		if (power > 1.5) 
			decimals = 0;
		else
			decimals = 2 - (int) Math.round(power);

		String format = "%1." + decimals + "f";
		return String.format(Locale.US, format, value);
	}
	
	public static String format4(double value) {
		return String.format(Locale.US, "%1.4f", value);
	}

	public static String format2(double value) {
		return String.format(Locale.US, "%1.2f", value);
	}


}
