package com.plusot.senselib.values;

import com.plusot.common.util.Calc;
import com.plusot.javacommon.util.Format;
import com.plusot.javacommon.util.MathVector;
import com.plusot.javacommon.util.NumberUtil;
import com.plusot.javacommon.util.StringUtil;
import com.plusot.javacommon.util.TimeUtil;
import com.plusot.senselib.widget.XY;

public class ValueItem {		
	private long timeStamp;
	private Object value;
	//private int tag;

	public ValueItem(final Object value, final long timeStamp/*, final int tag*/) { //, final ValueType valueType) {
		this.value = value;
		this.timeStamp = timeStamp;
		//this.tag = tag;
	}

	public ValueItem clone() {
		return new ValueItem(NumberUtil.cloneObject(value), timeStamp); //, tag);
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	public ValueItem cloneToSingleDimension() {
		double[] doubles = getDoubleValues(true);
		if (doubles.length <= 1) return this;
		double avg = 0;
		for (int i = 0; i < doubles.length; i++)
			avg += doubles[i];
		avg /= doubles.length;
		return new ValueItem((Double) avg, timeStamp); //, tag);
	}

	public void minOf(ValueItem other) {
		if (value == null) return;
		if (value instanceof double[]) {
			double[] otherValues = other.getDoubleValues(true);
			double[] myValues =  getDoubleValues(true);
			double myProd = 0;
			double otherProd = 0;
			for (int i = 0; i < Math.min(otherValues.length, myValues.length); i++) {
				myProd += myValues[i] * myValues[i];
				otherProd += otherValues[i] * otherValues[i];		
			}	
			if (otherProd < myProd) {
				timeStamp = other.timeStamp;
				//tag = other.tag;
				value = otherValues;
			}
		} else if (value instanceof float[]) {
			float[] otherValues = other.getFloatValues(true);
			float[] myValues =  getFloatValues(true);
			float myProd = 0;
			float otherProd = 0;
			for (int i = 0; i < Math.min(otherValues.length, myValues.length); i++) {
				myProd += myValues[i] * myValues[i];
				otherProd += otherValues[i] * otherValues[i];		
			}	
			if (otherProd < myProd) {
				timeStamp = other.timeStamp;
				//tag = other.tag;
				value = otherValues;
			}
		} else if (value instanceof Double) {
			Double otherValue = other.getDoubleValue();
			Double myValue =  getDoubleValue();
			if (otherValue < myValue)  {
				value = otherValue;
				timeStamp = other.timeStamp;
				//tag = other.tag;
			}
		} else if (value instanceof Long) {
			Long otherValue = other.getLongValue();
			Long myValue =  getLongValue();
			if (otherValue < myValue)  {
				value = otherValue;
				timeStamp = other.timeStamp;
				//tag = other.tag;
			}
		} else if (value instanceof Float) {
			Float otherValue = other.getFloatValue();
			Float myValue =  getFloatValue();
			if (otherValue < myValue)  {
				value = otherValue;
				timeStamp = other.timeStamp;
				//tag = other.tag;
			}
		} else if (value instanceof Integer) {
			Float otherValue = other.getFloatValue();
			Float myValue =  getFloatValue();
			if (otherValue < myValue) {
				value = otherValue;
				timeStamp = other.timeStamp;
				//tag = other.tag;
			}
		} else if (value instanceof String) {
			value = other.getStringValue();
			timeStamp = other.timeStamp;
			//tag = other.tag;
		}
	}

	public void maxOf(ValueItem other) {
		if (value == null) return;
		if (value instanceof double[]) {
			double[] otherValues = other.getDoubleValues(true);
			double[] myValues =  getDoubleValues(true);
			double myProd = 0;
			double otherProd = 0;
			for (int i = 0; i < Math.min(otherValues.length, myValues.length); i++) {
				myProd += myValues[i] * myValues[i];
				otherProd += otherValues[i] * otherValues[i];		
			}	
			if (otherProd > myProd) {
				timeStamp = other.timeStamp;
				//tag = other.tag;
				value = otherValues;
			}
		} else if (value instanceof float[]) {
			float[] otherValues = other.getFloatValues(true);
			float[] myValues =  getFloatValues(true);
			float myProd = 0;
			float otherProd = 0;
			for (int i = 0; i < Math.min(otherValues.length, myValues.length); i++) {
				myProd += myValues[i] * myValues[i];
				otherProd += otherValues[i] * otherValues[i];		
			}	
			if (otherProd > myProd) {
				timeStamp = other.timeStamp;
				//tag = other.tag;
				value = otherValues;
			}
		} else if (value instanceof Double) {
			Double otherValue = other.getDoubleValue();
			Double myValue =  getDoubleValue();
			if (otherValue > myValue)  {
				value = otherValue;
				timeStamp = other.timeStamp;
				//tag = other.tag;
			}
		} else if (value instanceof Long) {
			Long otherValue = other.getLongValue();
			Long myValue =  getLongValue();
			if (otherValue > myValue)  {
				value = otherValue;
				timeStamp = other.timeStamp;
				//tag = other.tag;
			}
		} else if (value instanceof Float) {
			Float otherValue = other.getFloatValue();
			Float myValue =  getFloatValue();
			if (otherValue > myValue)  {
				value = otherValue;
				timeStamp = other.timeStamp;
				//tag = other.tag;
			}
		} else if (value instanceof Integer) {
			Float otherValue = other.getFloatValue();
			Float myValue =  getFloatValue();
			if (otherValue > myValue) {
				value = otherValue;
				timeStamp = other.timeStamp;
				//tag = other.tag;
			}
		} else if (value instanceof String) {
			value = other.getStringValue();
			timeStamp = other.timeStamp;
			//tag = other.tag;
		}
	}

	public double[] getDelta(ValueItem other) {
		double[] otherDoubles = other.getDoubleValues(true);
		double[] doubles = this.getDoubleValues(true);
		double[] result = new double[Math.min(doubles.length, otherDoubles.length)];
		for (int i = 0; i < result.length; i++) {
			result[i] = doubles[i] - otherDoubles[i];
		}
		return result;
	}

	public boolean deltaLargerThan(ValueItem other, double value, long minTimeDelta) {
		double[] deltas = getDelta(other);
		if (Math.abs(this.timeStamp - other.timeStamp) > minTimeDelta) return true;
		for (int i = 0; i < deltas.length; i++) {
			if (Math.abs(deltas[i]) > value) return true;
		}
		return false;
	}

	public double getLengthOrAvg(int dimensions) { //, boolean allowZeroOrNegative) {
		MathVector m = new MathVector(getDoubleValues(true));
		if (dimensions > 1) return m.length();
		//if (allowZeroOrNegative) 
		return m.avg();
		//return m.avgNoZeroOrNegative();
	}

	public double[] getDoubleValues(final boolean acceptSingleValue) {
		return NumberUtil.getDoubleValues(value, acceptSingleValue);
	}

	public float[] getFloatValues(final boolean acceptSingleValue) {
		if (value instanceof double[]) {
			float[] result = new float[((double[]) value).length];
			int i = 0;
			for (double f : (double[]) value) {
				result[i++] = ((Double)f).floatValue();
			}
			return result;
		}

		if (value instanceof float[]) {
			float[] result = new float[((float[]) value).length];
			System.arraycopy((float[]) value, 0, result, 0, ((float[]) value).length); 
			return result;
		}
		if (acceptSingleValue) {
			return new float[] {getFloatValue()};
		}
		return null;
	}

	/*public float[] getFloatValues(final Float baseValue, final boolean acceptSingleValue) {
		int iAdd = 0;
		if (baseValue != null) iAdd = 1;
		if (value instanceof double[]) {
			float[] result = new float[((double[]) value).length + iAdd];
			if (iAdd == 1) result[0] = baseValue;
			int i = iAdd;
			for (double f : (double[]) value) {
				result[i++] = ((Double)f).floatValue();
			}
			return result;
		}

		if (value instanceof float[]) {
			float[] result = new float[((float[]) value).length + iAdd];
			System.arraycopy((float[]) value, 0, result, iAdd, ((float[]) value).length); 
			if (iAdd == 1) result[0] = baseValue;
			return result;
		}
		if (acceptSingleValue) {
			if (iAdd == 1) {
				return new float[] {baseValue, getFloatValue()};
			} else
				return new float[] {getFloatValue()};
		}
		return null;
	}*/

	public XY getXY(int tag) {
		XY xy = new XY(timeStamp);
		float[] floats = getFloatValues(false);
		if (floats == null)
			xy.setY(tag, getFloatValue());
		else
			xy.setY(floats);
		return xy;
	}

	public double getDoubleValue() {
		return NumberUtil.getDoubleValue(value);
	}

	public float getFloatValue() {
		if (value instanceof Double ) return ((Double) value).floatValue();
		if (value instanceof Float ) return (Float) value;
		if (value instanceof Long ) return ((Long) value).floatValue();
		if (value instanceof Integer ) return ((Integer) value).floatValue();
		if (value instanceof String) try {
			return Float.valueOf((String) value);
		} catch (NumberFormatException e) {
		}
		float[] floats = getFloatValues(false);
		if (floats != null) {
			return Calc.length(floats);
		}
		return 0;
	}

	public long getLongValue() {
		if (value instanceof Long) return (Long) value;
		if (value instanceof Integer) return ((Integer) value).longValue();
		if (value instanceof Double) return ((Double) value).longValue();
		if (value instanceof Float ) return ((Float) value).longValue();
		if (value instanceof String) try {
			return Long.valueOf((String) value);
		} catch (NumberFormatException e) {
		}
		return 0;
	}

	public String getStringValue() {
		double[] doubles = getDoubleValues(false);
		if (doubles != null) {
			return StringUtil.toString(doubles, ",");
		}
		if (value instanceof String) return (String) value;
		if (value instanceof Long) return String.valueOf((Long) value);
		if (value instanceof Float) return String.valueOf((Float) value);
		if (value instanceof Integer) return String.valueOf((Integer) value);
		if (value instanceof Double) return Format.format((Double) value, 4);

		return "";
	}

	public int getIntValue() {
		if (value instanceof Integer) return (Integer) value;
		if (value instanceof Long) return ((Long) value).intValue();
		if (value instanceof Float) return ((Float) value).intValue();
		if (value instanceof Double) return ((Double) value).intValue();
		if (value instanceof String) try {
			return Integer.valueOf((String) value);
		} catch (NumberFormatException e) {
		}
		return 0;
	}

	public Short getShortValue() {
		if (value instanceof Integer) return ((Integer) value).shortValue();
		if (value instanceof Long) return ((Long) value).shortValue();
		if (value instanceof Float) return ((Float) value).shortValue();
		if (value instanceof Double) return ((Double) value).shortValue();
		if (value instanceof String) try {
			return Short.valueOf((String) value);
		} catch (NumberFormatException e) {
		}
		return 0;
	}

	public long getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(long value) {
		timeStamp = value;
	}

	/*public int getTag() {
		return tag;
	}*/

	@Override
	public String toString() {
		return "" + timeStamp + ',' + StringUtil.toStringXL(value, ";"); // + ',' + tag;
	}

	public String toHumanString() {
		return TimeUtil.formatTime(timeStamp) + ',' + StringUtil.toStringXL(value, ";"); // + ',' + tag;
	}

	public static ValueItem fromString(String string, Class<?> someClass) {
		String[] strings = string.split(",");
		if (strings.length < 2) return null;
		long timeStamp = StringUtil.toLong(strings[0], null);
		Object obj = StringUtil.toObject(strings[1].split(";"), someClass, null);
		if (obj == null) return null;
		//int tag = Util.toInt(strings[2], 1);
		return new ValueItem(obj, timeStamp); //, tag);

	}

	public static ValueItem fromZero(Class<?> someClass, int dimension) {

		long now = System.currentTimeMillis();
		if (dimension == 1) {
			if (someClass.equals(Double.class)) {
				return new ValueItem(Double.valueOf(0), now);
			} else if (someClass.equals(Float.class)) {
				return new ValueItem(Float.valueOf(0), now);
			} else if (someClass.equals(Integer.class)) {
				return new ValueItem(Integer.valueOf(0), now);
			} else if (someClass.equals(Long.class)) {
				return new ValueItem(Long.valueOf(0), now);
			} else if (someClass.equals(String.class)) {
				return new ValueItem("0", now);

			} 
		} else {
			if (someClass.equals(Double.class)) {		
				double[] values = new double[dimension];
				for (int i = 0; i < dimension; i++) values[i] = 0;
				return new ValueItem(values, now);
			} else if (someClass.equals(Float.class)) {
				float[] values = new float[dimension];
				for (int i = 0; i < dimension; i++) values[i] = 0;
				return new ValueItem(values, now);
			} else if (someClass.equals(Integer.class)) {
				int[] values = new int[dimension];
				for (int i = 0; i < dimension; i++) values[i] = 0;
				return new ValueItem(values, now);
			} else if (someClass.equals(Long.class)) {
				long[] values = new long[dimension];
				for (int i = 0; i < dimension; i++) values[i] = 0;
				return new ValueItem(values, now);
			} else if (someClass.equals(String.class)) {
				String[] values = new String[dimension];
				for (int i = 0; i < dimension; i++) values[i] = "0";
				return new ValueItem(values, now);
			}
		}
		return null;

	}

}
