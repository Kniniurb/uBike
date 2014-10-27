package com.plusot.senselib.util;


public class AverageDoubleItem extends AverageItem<Double> {
	//private final static String CLASSTAG = AverageDoubleItem.class.getSimpleName();

	public AverageDoubleItem(final String tag, final long sampleDelta, final double minimalAnomaly, final boolean hasVariance, final boolean hasSlope, final long slopeDelta) {
		super(tag, sampleDelta, minimalAnomaly, hasVariance, hasSlope, slopeDelta);
	}
	
	@Override
	public boolean addObject(String tag, Object value, long timestamp, final long releaseDelta) {
		if (value instanceof Double) {
			return addItem(tag, (Double) value, timestamp, releaseDelta);
		}
		//LLog.d(Globals.TAG, CLASSTAG + " Wrong type");
		return false;
	}

	@Override
	public Double getAverage(final long avgDelta) {
		double d = calcAverage(avgDelta);
		return Double.valueOf(d);
	}

}
