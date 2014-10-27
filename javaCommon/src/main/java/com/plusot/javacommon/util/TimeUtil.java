package com.plusot.javacommon.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class TimeUtil {
	//private static final String CLASSTAG = TimeUtil.class.getSimpleName();
	public static final long HOUR = 3600000;
	public static final long DAY = 86400000;
	public static final long YEAR = 365 * 86400000;
	public static final long MIN_TIME = getTime(2012, 7, 1, 0, 0, 0); 
	public static final long MAX_TIME = getTime(2028, 7, 1, 0, 0, 0); 

	public static int getCurrentYear() {
		Calendar cal = Calendar.getInstance();
//        String timezoneID = TimeZone.getDefault().getID();
//        cal.setTimeZone(TimeZone.getTimeZone(timezoneID));
        return cal.get(Calendar.YEAR);   
	}
	
	public static long getTime(int year, int month, int day, int hour, int minute, int second) {
		Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT")); 
		cal.set(year, month, day, hour, minute, second); 
		return cal.getTime().getTime();
	}

	public static int getOffset() {
		return  TimeZone.getDefault().getOffset(System.currentTimeMillis());
	}

	public static String formatTime(long value, String pattern) {
		final SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
		//longDateFormat.setTimeZone(UTC_TIMEZONE);
		return format.format(new Date(value));
	}

	public static String formatTimeUTC(long value, String pattern) {
		final SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
		format.setTimeZone(TimeZone.getTimeZone("UTC"));
		return format.format(new Date(value));
	}

	public static String formatTimeUTC(long value) {
		return formatTimeUTC(value, "yyyy-MM-dd HH:mm:ss");
	}

	//	public static String formatTimeNL(long value) {
	//		final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	//		format.setTimeZone(TimeZone.getTimeZone("Europe/Amsterdams"));
	//		return format.format(new Date(value));
	//	}

	public static String formatTime(long value) {
		return formatTime(value, "yyyy-MM-dd HH:mm:ss");
	}
	
	public static String formatTime() {
		return formatTime(System.currentTimeMillis(), "yyyy-MM-dd HH:mm:ss");
	}
	
	public static String formatTimeT(long time) {
		return formatTime(time, "yyyy-MM-dd HH:mm:ss").replace(' ', 'T');
	}
	
	public static String formatFileTime() {
		return formatTime(System.currentTimeMillis(), "yyyyMMdd-HHmmss");
	}

	public static String formatTimeShort(long value) {
		String dateStr = formatDate(value);
		long now = System.currentTimeMillis();
		if (!dateStr.equals(formatDate(now))) return dateStr;
		return formatTime(value, "HH:mm:ss");
	}

	public static String formatDate(long value) {
		return formatTime(value, "yyyy-MM-dd");
	}
	
	public static String formatDateShort(long value) {
		return formatTime(value, "yyyyMMdd");
	}

	public static String formatMilli() {
		return formatMilli(-1) ;
	}
	
	public static String formatMilli(long value) {
		if (value == -1) value = System.currentTimeMillis();
		return formatTime(value, "yyyy-MM-dd HH:mm:ss") + "." + String.format(Locale.US, "%03d", value % 1000);
	}

	public static String formatMilliTime(long value) {
		return formatTime(value, "HH:mm:ss") + "." + String.format(Locale.US, "%03d", value % 1000);
	}

	public static String formatMilli(long value, String pattern) {
		return formatTime(value, pattern) + "." + String.format(Locale.US, "%03d", value % 1000);
	}

	public static String formatMilli(long value, int decimals) {
		return TimeUtil.formatMilli(value, "yyyy-MM-dd HH:mm:ss", decimals);
	}
	
	public static String formatMilliTime(long value, int decimals) {
		return TimeUtil.formatMilli(value, "HH:mm:ss", decimals);
	}

	public static String formatMilli(long value, String pattern, int decimals) {
		switch (decimals) {
		case 0: return formatTime(value, pattern);
		case 1: return formatTime(value, pattern) + "." + String.format(Locale.US, "%01d", (value % 1000) / 100);
		case 2: return formatTime(value, pattern) + "." + String.format(Locale.US, "%02d", (value % 1000) / 10);
		default: return formatTime(value, pattern) + "." + String.format(Locale.US, "%03d", value % 1000);
		}
	}

	public static long parseTime(String value, String pattern) {
		final SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
		Date date;
		try {
			date = format.parse(value);
		} catch (ParseException e) {
			return 0;
		}
		//longDateFormat.setTimeZone(UTC_TIMEZONE);
		return date.getTime();
	}

	public static long parseTimeUTC(String value, String pattern) {
		int milli = 0;
		if (pattern.contains(".S")) {
			String[] valueParts = value.split("\\.");
			String[] patternParts = pattern.split("\\.");
			for (int i = 0; i < Math.min(patternParts.length, valueParts.length); i++) {
				if (patternParts[i].startsWith("S")) {
					String milliPart = (valueParts[i] + "000").substring(0, 3);
					try {
						milli = Integer.parseInt(milliPart);
					} catch (NumberFormatException e) {
						milli = 0;
					}
					switch (patternParts[i].length()) {
					case 3: pattern = pattern.replace(".SSS", ""); break;
					case 2: pattern = pattern.replace(".SS", ""); break;
					default:
					case 1: pattern = pattern.replace(".S", ""); break;
					}

				}
			}
		}
		final SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
		format.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date date;
		try {
			date = format.parse(value);
		} catch (ParseException e) {
			return 0;
		}
		return date.getTime() + milli;
	}

	public static long parseTimeUTC(String value) {
		final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
		format.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date date;
		try {
			date = format.parse(value);
		} catch (ParseException e) {
			return 0;
		}
		return date.getTime();
	}

	public static long parseTime(String value) {
		return parseTime(value, "yyyy-MM-dd HH:mm:ss");
	}

	public static long timeToUTC(long time) {
		//		String sTime = formatTime(time);
		//		return parseTimeToZone(sTime, "UTC");
		return time + getOffset();
	}

	public static long timeToLocal(long time) {
		return time - getOffset();
	}

	public static String formatElapsedMilli(long value, boolean countDays) {
		if (countDays && value > 86400000) return value / 86400000L + " days";
		int hours = (int)(value / 3600000);
		value %= 3600000;
		int min = (int)(value / 60000);
		value %= 60000;
		int seconds = (int) (value / 1000);
		value %= 1000;
		if (hours == 0 && min == 0 && seconds == 0) return String.format(Locale.US, ".%d", value);
		if (hours == 0 && min == 0) return String.format(Locale.US, "%d.%03d", seconds, value);
		if (hours == 0) return String.format(Locale.US, "%d:%02d.%03d", min, seconds, value);
		return String.format(Locale.US, "%d:%02d:%02d.%03d", hours, min, seconds, value);
	}


}
