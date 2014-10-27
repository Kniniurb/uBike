package com.plusot.javacommon.util;

import java.util.Collection;
import java.util.Map;

public class StringUtil {

	public static String toString(Object array[], String delim) {
		if (array == null) return "";
		StringBuffer sb = new StringBuffer();
		for (Object object : array) {
			if (object != null) {
				if (sb.length() > 0) sb.append(delim);
				sb.append(object.toString());

			}
		}
		return sb.toString();
	}

	public static String toString(Object array[]) {
		return toString(array, " ");
	}

	public static String toString(float array[], String delim) {
		if (array == null) return "";
		StringBuffer sb = new StringBuffer();
		for (float object : array) {
			if (sb.length() > 0) sb.append(delim);
			sb.append("" + object);
		}
        sb.append(" float");
		return sb.toString();
	}

	public static String toString(int array[], String delim, double scale, int decimals) {
		if (array == null) return "";
		StringBuffer sb = new StringBuffer();
		for (int object : array) {
			if (sb.length() > 0) sb.append(delim);
			if (scale != 1) 
				sb.append(Format.format(scale * object, decimals));
			else
				sb.append(object);
		}
		return sb.toString();
	}

	public static String toString(int array[], double scale, int decimals) {
		return toString(array, " ", scale, decimals);
	}

	public static String toString(int array[], String delim) {
		if (array == null) return "";
		StringBuffer sb = new StringBuffer();
		for (int object : array) {
			if (sb.length() > 0) sb.append(delim);
			sb.append("" + object);
		}
		return sb.toString();
	}

	public static String toString(long array[], String delim) {
		if (array == null) return "";
		StringBuffer sb = new StringBuffer();
		for (long object : array) {
			if (sb.length() > 0) sb.append(delim);
			sb.append("" + object);
		}
		return sb.toString();
	}

	public static String toString(String array[], String delim) {
		if (array == null) return "";
		StringBuffer sb = new StringBuffer();
		for (String object : array) {
			if (sb.length() > 0) sb.append(delim);
			sb.append("" + object);
		}
		return sb.toString();
	}

	public static String toString(Collection<?> coll, String delim) {
		if (coll == null) return "";
		StringBuffer sb = new StringBuffer();
		for (Object object : coll) {
			if (object instanceof Collection<?>) {
				sb.append(toString((Collection<?>)object, delim));
			} else {
				if (sb.length() > 0) sb.append(delim);
				sb.append("" + object.toString());
			}
		}
		return sb.toString();
	}

	public static String toString(float array[]) {
		return toString(array, " ");
	}

	public static String toString(double array[], String delim) {
		return toString(array, delim, false, -1);
	}

	public static String toString(double array[], String delim, boolean format, int decimals) {
		if (array == null) return "";
		StringBuffer sb = new StringBuffer();
		for (double object : array) {
			if (sb.length() > 0) sb.append(delim);
			if (format)
				sb.append(Format.format(object, decimals));
			else
				sb.append("" + object);
		}
		return sb.toString();
	}


	public static String toString(double array[]) {
		return toString(array, " ", false, -1);
	}

	public static String toString(Map<String, ?> map) {
		StringBuilder sb = new StringBuilder();

		for (String key : map.keySet()) {
			if (sb.length() != 0) sb.append("\n");
			sb.append(key).append(" = ");
			if (map.get(key) != null) sb.append(map.get(key).toString());
		}
		return sb.toString();
	}

	/* 
	 * toString(Object) applies only to objects used in this application!!! 
	 */
	public static String toStringXL(Object object, String delim) {
		if (object instanceof double[]) {
			return toString((double[]) object, delim);
		} else if (object instanceof float[]) {
			return toString((float[]) object, delim);
		}  else if (object instanceof int[]) {
			return toString((int[]) object, delim);
		}  else if (object instanceof long[]) {
			return toString((long[]) object, delim);
		} else
			return object.toString();
	}


	public static String toHexString(byte data) {
		return String.format("%02X", data & 0xFF);
	}

	public static String toHexString(byte[] data, int len, boolean longFormat) {
		if (null == data) {
			return "no data";
		}

		StringBuffer hexString = new StringBuffer();
		for(int i = 0;i < Math.min(data.length, len); i++) {
			if (longFormat) hexString.append("[");
			hexString.append(String.format("%02X", data[i] & 0xFF));
			if (longFormat) hexString.append("]"); else hexString.append(" ");
		}
		return hexString.toString().trim();
	}

	public static String toHexString(byte[] data, int len) {
		return toHexString(data, len, false);
	}	

	public static String toHexString(byte[] data) {
		return toHexString(data, Integer.MAX_VALUE, false);
	}

	public static String toReadableString(byte[] data) {
		return toReadableString(data, data.length, true);
	}
	
	public static String toReadableString(byte[] data, boolean longFormat) {
		return toReadableString(data, data.length, longFormat);
	}

	public static String toReadableString(byte[] data, int len, boolean longFormat) {
		if (null == data) {
			return "no data";
		}

		StringBuffer hexString = new StringBuffer();
		for(int i = 0;i < Math.min(len, data.length); i++) {
			if ((data[i] & 0xFF) >= 32 && (data[i] & 0xFF) <= 127) { 
				hexString.append((char)data[i]);
			} else {
				if (longFormat) hexString.append("[");
				hexString.append(String.format("%02X", data[i] & 0xFF));
				if (longFormat) hexString.append("]"); else hexString.append(" ");
			}
		}
		return hexString.toString().trim();
	}
	
	public static String toReadableString(byte[] data, int st, int len, boolean longFormat) {
		if (null == data) {
			return "no data";
		}
		if (len + st > data.length) len = data.length - st;
		if (len <= 0) return "";
		StringBuffer hexString = new StringBuffer();
		for(int i = 0;i < len; i++) {
			if ((data[i + st] & 0xFF) >= 32 && (data[i + st] & 0xFF) <= 127) { 
				hexString.append((char)data[i + st]);
			} else {
				if (longFormat) hexString.append("[");
				hexString.append(String.format("%02X", data[i + st] & 0xFF));
				if (longFormat) hexString.append("]"); else hexString.append(" ");
			}
		}
		return hexString.toString().trim();
	}

	public static String toReadableString(String data, boolean longFormat) {
		if (null == data) {
			return "no data";
		}

		StringBuffer hexString = new StringBuffer();
		for(int i = 0;i < data.length(); i++) {
			if ((data.charAt(i) & 0xFF) >= 32 && (data.charAt(i) & 0xFF) <= 127) { 
				hexString.append(data.charAt(i));
			} else {
				if (longFormat) hexString.append("[");
				hexString.append(String.format("%02X", data.charAt(i) & 0xFFFF));
				if (longFormat) hexString.append("]"); else hexString.append(" ");
			}
		}
		return hexString.toString().trim();
	}

	public static String trimAll(String msg) {
		return msg.replace((char)0xB0, ' ').replaceAll("[>\uFFFD \r\n]", "");
	}


	public static String[] toStringArray(Collection<String[]> collection) {
		if (collection == null) return null;
		String strings[] = new String[collection.size()];
		int i = 0;
		for (String[] item : collection) {
			strings[i++] = toString(item, ";");
		}
		return strings;
	}

	public static double toDouble(String value, Scaler scaler) {
		if (value == null) return 0.0;
		try {
			if (scaler == null) return Double.valueOf(value);
			return scaler.onScaleBack(Double.valueOf(value)).doubleValue();
		} catch (NumberFormatException e) {
			return 0.0;
		}
	}

	public static double[] toDoubles(String[] values, Scaler scaler) {
		if (values == null) return null;
		double[] doubles = new double[values.length];
		int i = 0;
		for (String value: values) try {
			if (scaler == null)
				doubles[i] = Double.valueOf(value);
			else
				doubles[i] = scaler.onScaleBack(Double.valueOf(value)).doubleValue();
			i++;
		} catch (NumberFormatException e) {
			doubles[i++] = 0;
		}
		return doubles;
	}

	public static float[] toFloats(String[] values, Scaler scaler) {
		if (values == null) return null;
		float[] floats = new float[values.length];
		int i = 0;
		for (String value: values) try {
			if (scaler == null)
				floats[i] = Float.valueOf(value);
			else
				floats[i] = scaler.onScaleBack(Float.valueOf(value)).floatValue();
			i++;
		} catch (NumberFormatException e) {
			floats[i++] = 0f;
		}
		return floats;
	}

	public static float toFloat(String value, Scaler scaler) {
		if (value == null) return 0f;
		try {
			if (scaler == null) return Float.valueOf(value);
			return scaler.onScaleBack(Float.valueOf(value)).floatValue();
		} catch (NumberFormatException e) {
			return 0f;
		}
	}

	public static int toInt(String value, Scaler scaler) {
		if (value == null) return 0;
		try {
			if (scaler == null)
				return Integer.valueOf(value);	
			else
				return scaler.onScaleBack(Integer.valueOf(value)).intValue();
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	public static int[] toInts(String[] values, Scaler scaler) {
		if (values == null) return null;
		int[] ints = new int[values.length];
		int i = 0;
		for (String value: values) try {
			if (scaler == null)
				ints[i] = Integer.valueOf(value);
			else
				ints[i] = scaler.onScaleBack(Integer.valueOf(value)).intValue();
			i++;
		} catch (NumberFormatException e) {
			ints[i++] = 0;
		}
		return ints;
	}

	public static long[] toLongs(String[] values, Scaler scaler) {
		if (values == null) return null;
		long[] longs = new long[values.length];
		int i = 0;
		for (String value: values) try {
			if (scaler == null)
				longs[i] = Long.valueOf(value);
			else
				longs[i] = scaler.onScaleBack(Long.valueOf(value)).longValue();
			i++;
		} catch (NumberFormatException e) {
			longs[i++] = 0;
		}
		return longs;
	}

	public static long toLong(String value, Scaler scaler) {
		if (value == null) return 0;
		try {
			if (scaler == null) return Long.valueOf(value);
			return scaler.onScaleBack(Long.valueOf(value)).longValue();
		} catch (NumberFormatException e) {
		}
		return 0;
	}

	public static Object toObject(String[] values, Class<?> someClass, Scaler scaler) {
		if (values == null || values.length == 0) return null;
		if (values.length == 1) {
			return toObject(values[0], someClass, scaler);
		} else if (someClass.equals(Double.class)) {
			return toDoubles(values, scaler);
		} else if (someClass.equals(Float.class)) {
			return toFloats(values, scaler);
		} else if (someClass.equals(Integer.class)) {
			return toInts(values, scaler);
		} else if (someClass.equals(Long.class)) {
			return toLongs(values, scaler);
		} else if (someClass.equals(String.class)) {
			return values;
		}
		return null;
	}

	public static Object toObject(String value, Class<?> someClass, Scaler scaler) {
		if (value == null) return null;
		if (someClass.equals(Double.class)) {
			try {
				if (scaler == null)	return Double.parseDouble(value);
				return scaler.onScaleBack(Double.parseDouble(value));
			} catch (NumberFormatException e) {
				return Double.valueOf(0.0);
			}
		} else if (someClass.equals(Float.class)) {
			try {
				if (scaler == null)	return Float.valueOf(value);
				return scaler.onScaleBack(Float.valueOf(value));
			} catch (NumberFormatException e) {
				return Float.valueOf(0f);
			}
		} else if (someClass.equals(Integer.class)) {
			try {
				if (scaler == null)	return Integer.valueOf(value);
				return scaler.onScaleBack(Integer.valueOf(value));
			} catch (NumberFormatException e) {
				try {
					if (scaler == null)	return Double.valueOf(value).intValue();
					return scaler.onScaleBack(Double.valueOf(value)).intValue();
				} catch (NumberFormatException e2) {
					return Integer.valueOf(0);
				}
			}
		} else if (someClass.equals(Long.class)) {
			try {
				if (scaler == null)	return Long.valueOf(value);
				return scaler.onScaleBack(Long.valueOf(value));
			} catch (NumberFormatException e) {
				try {
					if (scaler == null)	return Double.valueOf(value).longValue();
					return scaler.onScaleBack(Double.valueOf(value)).longValue();
					//return d.longValue();
				} catch (NumberFormatException e2) {
					return Long.valueOf(0);
				}
			}
		} else if (someClass.equals(String.class)) {
			return value;
		}
		return null;
	}



}
