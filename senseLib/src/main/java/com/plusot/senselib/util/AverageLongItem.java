package com.plusot.senselib.util;

public class AverageLongItem extends AverageItem<Long> {

	public AverageLongItem(final String tag, final long sampleDelta, final double minimalAnomaly, final boolean hasVariance, final boolean hasSlope, final long slopeDelta) {
		super(tag, sampleDelta, minimalAnomaly, hasVariance, hasSlope, slopeDelta);
	}
	
	@Override
	public boolean addObject(String tag, Object value, long timestamp, final long releaseDelta) {
		if (value instanceof Long) {
			return addItem(tag, (Long) value, timestamp, releaseDelta);
		}
		return false;
	}

	@Override
	public Long getAverage(final long avgDelta) {
		double d = calcAverage(avgDelta);
		return Long.valueOf(Double.valueOf(d).longValue());
	}
}
