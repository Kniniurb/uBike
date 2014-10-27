package com.plusot.senselib.util;

public class AverageFloatItem extends AverageItem<Float> {

	public AverageFloatItem(final String tag, final long sampleDelta, final double minimalAnomaly, final boolean hasVariance, final boolean hasSlope, final long slopeDelta) {
		super(tag, sampleDelta, minimalAnomaly, hasVariance, hasSlope, slopeDelta);
	}
	
	@Override
	public boolean addObject(String tag, Object value, long timestamp, final long releaseDelta) {
		if (value instanceof Float) {
			return addItem(tag, (Float) value, timestamp, releaseDelta);
		}
		return false;
	}

	@Override
	public Float getAverage(final long avgDelta) {
		double d = calcAverage(avgDelta);
		return Float.valueOf(Double.valueOf(d).floatValue());
	}
}
