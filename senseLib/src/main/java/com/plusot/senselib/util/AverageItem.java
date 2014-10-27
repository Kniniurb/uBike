package com.plusot.senselib.util;

import com.plusot.javacommon.util.Format;

import java.util.ArrayList;
import java.util.List;

public abstract class AverageItem<T extends Number> {
	private static final String CLASSTAG = AverageItem.class.getSimpleName();

	protected final long sampleDelta;
	protected long lastAvgDelta = 0;
	private final boolean hasVariance;
	protected final long slopeDelta;
	private final boolean hasSlope;
	private double slope = 0;
	protected List<Item> items = new ArrayList<Item>();
	protected long lastCall = 0;
	private static final double PEAKFACTOR = 4;
	private double variance = 0;
	private double avg = 0;
	private boolean changed = false;
	private final double minimalAnomaly;
	private final String tag;
	private LogListener listener = null;

	protected class Item {
		public T value;
		public long timestamp;
		public Item(T value, long timestamp) {
			this.value = value;
			this.timestamp = timestamp;
		}

	}

	public interface LogListener {
		public void onLog(String classTag, String log);
	}

	public AverageItem(final String tag, final long sampleDelta, final double minimalAnomaly, final boolean hasVariance, final boolean hasSlope, final long slopeDelta) {
		this.sampleDelta = sampleDelta;
		this.hasVariance = hasVariance;
		this.minimalAnomaly = minimalAnomaly;
		this.tag = tag;
		this.hasSlope = hasSlope;
		this.slopeDelta = slopeDelta;
	}

	public void setLogListener(final LogListener listener) {
		this.listener = listener;
	}

	public void clear() {
		items.clear();
		lastCall = 0;
	}

	public static AverageItem<?> getInstance(Class<?> valueClass, final String tag, final long sampleDelta, final double minimalAnomaly, final boolean hasVariance, final boolean hasSlope, final long slopeDelta) {
		if (valueClass.equals(Integer.class)) return new AverageIntegerItem(tag, sampleDelta, minimalAnomaly, hasVariance, hasSlope, slopeDelta);
		if (valueClass.equals(Float.class)) return new AverageFloatItem(tag, sampleDelta, minimalAnomaly, hasVariance, hasSlope, slopeDelta);
		if (valueClass.equals(Double.class)) return new AverageDoubleItem(tag, sampleDelta, minimalAnomaly, hasVariance, hasSlope, slopeDelta);
		if (valueClass.equals(Long.class)) return new AverageLongItem(tag, sampleDelta, minimalAnomaly, hasVariance, hasSlope, slopeDelta);
		return null;
	}

	public abstract boolean addObject(String tag, Object value, long timestamp, final long releaseDelta);

	protected boolean addItem(String tag, T value, long timestamp, long releaseDelta) {
		releaseDelta = Math.min(sampleDelta, releaseDelta);
		if (items.size() > 0 && timestamp == items.get(items.size() - 1).timestamp) {
			//if (tag.equals("SPEED")) LLog.d(Globals.TAG,  CLASSTAG + " Time = same for " + tag);
			return false;
		}
		changed = true;
		items.add(new Item(value, timestamp));
		long refTimestamp = items.get(items.size() - 1).timestamp;
		if (items.size() > 10) while (refTimestamp - items.get(0).timestamp > 15 * sampleDelta / 10) items.remove(0);
		//if (tag.equals("SPEED")) LLog.d(Globals.TAG,  CLASSTAG + " Time delta = " +  Math.abs(refTimestamp - lastCall) + " <> " + delta);
		return Math.abs(refTimestamp - lastCall) > releaseDelta;
	}

	public abstract T getAverage(final long avgDelta);

	protected double calcAverage(long avgDelta) {
		if (avgDelta > sampleDelta || avgDelta == 0) avgDelta = sampleDelta;
		if (!changed && lastAvgDelta == avgDelta) return avg;
		lastAvgDelta = avgDelta;
		if (items.size() == 0) {
			avg = 0;
			return 0;
		}
		long refTimestamp = items.get(items.size() - 1).timestamp;
		lastCall = refTimestamp;
		if (items.size() == 1) {
			T value = items.get(0).value;
			return value.doubleValue();
		}

		long timesum = 0;
		int nr = 0;
		double avgChange = 0;
		double prevChange = -1;

		for (int i = items.size() - 1; i > 0; i--) {
			Item item = items.get(i);
			Item prevItem = items.get(i - 1);
			double maxChange = 0;
			double change =  Math.abs(1.0 * (item.value.doubleValue() - prevItem.value.doubleValue()) / ( item.timestamp - prevItem.timestamp));
			maxChange = Math.max(maxChange, change);
			//LLog.d(Globals.TAG, CLASSTAG + ".change[" + j + "]========= " + change);
			avgChange += change;
			if (maxChange > 0) nr++;
			if (refTimestamp - prevItem.timestamp > avgDelta) break;			
		}
		avgChange /= nr;


		for (int i = items.size() - 1; i > 0; i--) {
			Item item = items.get(i);
			Item prevItem = items.get(i - 1);
			if (item.timestamp - prevItem.timestamp > 0) {
				double change = 1.0 * (item.value.doubleValue() - prevItem.value.doubleValue()) / ( item.timestamp - prevItem.timestamp);
				double refChange = Math.max(minimalAnomaly, PEAKFACTOR * avgChange);
				if (change > refChange && (prevChange > refChange || prevChange == -1)) {
					double value = item.value.doubleValue();
					if (listener != null) listener.onLog(
							CLASSTAG,
							".calcAverage: Peak removed for " + tag + "[" + i + " of " + items.size() + "("+ nr +")]: " + 
									Format.format(value) + ", change = " + change + ", avg change = " + avgChange
							);
					//iRemoved++;
					items.remove(i);
					break;
				}
				prevChange = change;	
			}
			if (refTimestamp - prevItem.timestamp > avgDelta) break;			
		}

		for (int i = items.size() - 1; i > 0; i--) {
			Item item = items.get(i);
			Item prevItem = items.get(i - 1);
			double value = item.value.doubleValue();
			double prevValue = prevItem.value.doubleValue();
			if (refTimestamp - prevItem.timestamp > avgDelta && item.timestamp - prevItem.timestamp > 0) {
				double dt = item.timestamp - refTimestamp + avgDelta;
				double slope = 1.0 * (value - prevValue) / (item.timestamp - prevItem.timestamp);
				avg += 0.5 *  (slope * (refTimestamp - avgDelta - prevItem.timestamp) + prevValue + value) * (dt);	
				timesum += dt;
				break;
			} else {
				avg += 0.5 * (value + prevValue) * (item.timestamp - prevItem.timestamp);
				timesum += item.timestamp - prevItem.timestamp; 
			}
		}

		avg /= timesum;

		if (hasVariance) { 
			variance = 0;
			int count = 0;
			double sumPower2 = 0;
			for (int i = items.size() - 1; i > 0; i--) {
				Item item = items.get(i);
				double value = item.value.doubleValue();
				sumPower2 += (value - avg) * (value - avg);
				count++;
				if (refTimestamp - item.timestamp > avgDelta) break;
			}
			if (count > 1) {
				variance = Math.pow(sumPower2 / (count - 1), 0.5);
				//				LLog.d(Globals.TAG, CLASSTAG + ".calcAverage: avg = " + avg + ", var = " + variance + ", count = " + count); 
			}
		}
		if (hasSlope) { 
			slope = 0;
			//			long prevTimestamp = 0;
			Item item = items.get(items.size() - 1);
			double prevValue = item.value.doubleValue();
			long prevTime = item.timestamp;
			double maxSlope = 0;
			double minSlope = 0;
			double refValue = item.value.doubleValue();
			double localSlope = 0;
			long timeD = 0;
			for (int i = items.size() - 1; i > 0; i--) {
				item = items.get(i - 1);
				double value = item.value.doubleValue();
				timeD = refTimestamp - item.timestamp;
				if (timeD > 0 && refValue != 0 && value != 0 && prevTime - item.timestamp > 0) {
					localSlope *= 0.75;
					localSlope += 0.25 * (prevValue - value) / (prevTime - item.timestamp);
					if (localSlope > maxSlope) maxSlope = localSlope;
					else if (localSlope < minSlope) minSlope = localSlope;
					slope = (refValue - value) / timeD;
				}
				if (timeD > slopeDelta) break;
				prevValue = value;
				prevTime = item.timestamp;
			}
			if (timeD < slopeDelta) 
				slope = 0;
			//			else if (slope < 0) {
			//				if (Math.abs(maxSlope) > 0.5 * Math.abs(slope)) slope = 0;
			//				//if (Math.abs(minSlope) > 5 * Math.abs(slope)) slope = 0;
			//			} else {
			//				if (Math.abs(minSlope) > 0.5 * Math.abs(slope)) slope = 0;
			//			}
			slope *= 60000;
			//			slope = Math.abs(slope);
			//			LLog.d(Globals.TAG, CLASSTAG + ".calcAverage: slope = " + slope + " max = " + 60000 * maxSlope + " min = " + 60000 * minSlope); 
		}
		//if (iRemoved > 0) LLog.d(Globals.TAG, CLASSTAG + ".calcAverage: Average for " + tag + ": " + StringUtil.toString(avg));
		changed = false;
		return avg;
	}

	public double getVariance(final long avgDelta) {
		calcAverage(avgDelta);
		return variance;
	}
	
	public double getVariance() {
		calcAverage(0);
		return variance;
	}

	public double getSlope(final long avgDelta) {
		calcAverage(avgDelta);
		return slope;
	}
	
	public double getSlope() {
		calcAverage(0);
		return slope;
	}

}
