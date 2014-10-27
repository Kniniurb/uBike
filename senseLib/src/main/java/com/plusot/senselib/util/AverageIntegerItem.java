package com.plusot.senselib.util;

public class AverageIntegerItem extends AverageItem<Integer> {

	public AverageIntegerItem(final String tag, final long sampleDelta, final double minimalAnomaly, final boolean hasVariance, final boolean hasSlope, final long slopeDelta) {
		super(tag, sampleDelta, minimalAnomaly, hasVariance, hasSlope, slopeDelta);
	}

	@Override
	public boolean addObject(String tag, Object value, long timestamp, final long releaseDelta) {
		if (value instanceof Integer) {
			return addItem(tag, (Integer) value, timestamp, releaseDelta);
		}
		return false;
	}
	
	
	@Override
	public Integer getAverage(final long avgDelta) {
		double d = calcAverage(avgDelta);
		return Integer.valueOf(Double.valueOf(d).intValue());
	}
}
