package com.plusot.common.util;

import java.util.Locale;

public class ElapsedTime {
	
	private static TimePartAsker asker = new TimePartAsker () {

		@Override
		public String getDay() {
			return "Day";
		}

		@Override
		public String getHours() {
			return "Hours";
		}

		@Override
		public String getSeconds() {
			return "Seconds";
		}

		@Override
		public String getMinutes() {
			return "Minutes";
		}
		
	};
	
	public interface TimePartAsker {
		String getDay();
		String getSeconds();
		String getHours();
		String getMinutes();
	}
	
	public static void setTimePartAsker(TimePartAsker _asker) {
		if (_asker != null) asker = _asker;
	}
	
	
	public static String formatAuto(long value, boolean countDays, boolean speech) {
		if (countDays && value > 86400000) return asker.getDay() + " " + value / 86400000L;
		int hours = (int)(value / 3600000);
		value %= 3600000;
		int min = (int)(value / 60000);
		value %= 60000;
		int seconds = (int) (value / 1000);
		value %= 1000;
		value /= 10;
		if (hours == 0 && min == 0 && seconds == 0) return String.format(Locale.US, ".%02d", value);
		
		value /= 10;
		if (hours == 0 && min == 0) {
			if (speech)
				return String.format(Locale.US, "%d %s %d ", seconds, asker.getSeconds(), value);
			else
				return String.format(Locale.US, "%d.%01d", seconds, value);
		}
		if (hours == 0)  {
			if (speech) {
				return String.format(Locale.US, "%d $s %d %s %d ", min, asker.getMinutes(), seconds, asker.getSeconds(), value);
			} else {
				return String.format(Locale.US, "%d:%02d.%01d", min, seconds, value);
			}
		}
		if (speech)
			return String.format(Locale.US, "%d %s %d %s %d %s", hours, asker.getHours(), min, asker.getMinutes(), seconds, asker.getSeconds());
		
		return String.format(Locale.US, "%d:%02d:%02d", hours, min, seconds);
	}


	public static String format(long value, boolean countDays, boolean speech) {
		if (countDays && value > 86400000) return asker.getDay() + " " + value / 86400000L;
		int hours = (int)(value / 3600000);
		value %= 3600000;
		int min = (int)(value / 60000);
		value %= 60000;
		if (speech) {
			if (hours == 0 && min == 0) return String.format(Locale.US, "%d ", value / 1000) + asker.getSeconds();
			if (hours == 0) return String.format(Locale.US, "%d %s %d %s", min, asker.getMinutes(), value / 1000, asker.getSeconds());
			return String.format(Locale.US, "%d %s %d %s %d %s", hours, asker.getHours(), min, asker.getMinutes(), value / 1000, asker.getSeconds());
			//String.format(Locale.US, "%d:%02d:%02d", hours, min, value / 1000);
		} else {
			if (hours == 0 && min == 0) return String.format(Locale.US, "%d", value / 1000);
			if (hours == 0) return String.format(Locale.US, "%d:%02d", min, value / 1000);
			return String.format(Locale.US, "%d:%02d:%02d", hours, min, value / 1000);
		}
	}

	public static String formatMilli(long value, boolean countDays) {
		if (countDays && value > 86400000) return asker.getDay() + " " + value / 86400000L;
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
