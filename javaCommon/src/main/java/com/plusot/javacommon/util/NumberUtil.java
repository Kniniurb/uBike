package com.plusot.javacommon.util;

public class NumberUtil {
	
	public static Object cloneObject(Object value) {
		if (value == null) return null;
		if (value instanceof double[]) {
			double[] array =  new double[((double[]) value).length];
			System.arraycopy(value, 0, array, 0, array.length);
			return array;
		}
		if (value instanceof long[]) {
			long[] array =  new long[((long[]) value).length];
			System.arraycopy(value, 0, array, 0, array.length);
			return array;
		}
		if (value instanceof float[]) {
			float[] array =  new float[((float[]) value).length];
			System.arraycopy(value, 0, array, 0, array.length);
			return array;
		}
		if (value instanceof int[]) {
			int[] array =  new int[((int[]) value).length];
			System.arraycopy(value, 0, array, 0, array.length);
			return array;
		}
		if (value instanceof String[]) {
			String[] array =  new String[((String[]) value).length];
			System.arraycopy(value, 0, array, 0, array.length);
			return array;
		}
		if (value instanceof Double) return Double.valueOf((Double)value);
		if (value instanceof Long) return Long.valueOf((Long) value);
		if (value instanceof Float) return Float.valueOf((Float) value);
		if (value instanceof Integer) return Integer.valueOf((Integer) value);
		if (value instanceof String) return new String ((String) value);	
		return null;
	}

	public static boolean inRange(Object value, double minValue, double maxValue) {
		double[] doubles = getDoubleValues(value, true);
		if (doubles == null) return false;
		for (int i = 0; i < doubles.length; i++) {
			if (doubles[i] > maxValue || doubles[i] < minValue) return false;
		}
		return true;
	}

	public static boolean greaterThan(Object value, double minValue) {
		double[] doubles = getDoubleValues(value, true);
		if (doubles == null) return false;
		for (int i = 0; i < doubles.length; i++) {
			if (doubles[i] > minValue) return true;
		}
		return false;
	}
	
	public static double max(double[] value) {
		float max = 0;
		if (value != null) {
			for (int i = 0; i < value.length; i++) {
				max += Math.max(max,value[i]);
			}
		}
		return max;
	}

	public static double average(double[] value) {
		double avg = 0;
		if (value != null) {
			for (int i = 0; i < value.length; i++) {
				avg += value[i];
			}
			avg /= value.length;
		}
		return avg;
	}
	
	public static float average(float[] value) {
		float avg = 0;
		if (value != null) {
			for (int i = 0; i < value.length; i++) {
				avg += value[i];
			}
			avg /= value.length;
		}
		return avg;
	}
	
	public static double getDoubleValue(final Object value) {
		if (value instanceof double[]) {
			return average((double[])value);
		} 
		if (value instanceof float[]) {
			return average((float[])value);
		}
		if (value instanceof Double) return (Double) value;
		if (value instanceof Long) return ((Long) value).doubleValue();
		if (value instanceof Float) return ((Float) value).doubleValue();
		if (value instanceof Integer) return ((Integer) value).doubleValue();
		if (value instanceof String) try {
			return Double.valueOf((String) value);
		} catch (NumberFormatException e) {
		}
		return 0;
	}

	public static double[] getDoubleValues(final Object value, final boolean acceptSingleValue) {
		if (value instanceof double[]) {
			double result[] = new double[((double[]) value).length];
			System.arraycopy(value, 0, result, 0, result.length);
			return result;
		} 
		if (value instanceof float[]) {
			double[] result = new double[((float[]) value).length];
			int i = 0;
			for (float f : (float[]) value) {
				result[i++] = f;
			}
			return result;
		}
		if (acceptSingleValue) {
			return new double[] {getDoubleValue(value)};
		}
		return null;
	}
}
