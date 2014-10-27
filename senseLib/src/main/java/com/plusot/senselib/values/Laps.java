package com.plusot.senselib.values;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

public class Laps {
	private static List<Lap> laps = new ArrayList<Lap>();
	private static int currentLap = 0;
	private static LapDevice lapDevice = new LapDevice();
	
	public enum ValueAggregate {
		AVG, 
		MAX,
		MIN,
		VALUETIME,
		//		NORMALIZED,
		DELTA
	}
	public static class Lap {
		private EnumMap<ValueType, EnumMap<ValueAggregate, Double>> lapData = new EnumMap<ValueType, EnumMap<ValueAggregate, Double>>(ValueType.class);
		private final long startTime;
		private long endTime;

		public Lap(final long startTime) {
			this.startTime = startTime;
		}

		public void setEndTime(long endTime) {
			this.endTime = endTime;
		}

		public void setLapData(ValueType type, ValueAggregate aggregate, double value) {
			EnumMap<ValueAggregate, Double> data = lapData.get(type);
			if (data == null) {
				lapData.put(type, new EnumMap<ValueAggregate, Double>(ValueAggregate.class));
				data = lapData.get(type);
			}
			data.put(aggregate, value);
			endTime = System.currentTimeMillis();
		}

		public void addLapData(ValueType type, ValueAggregate aggregate, double value) {
			EnumMap<ValueAggregate, Double> data = lapData.get(type);
			if (data == null) {
				lapData.put(type, new EnumMap<ValueAggregate, Double>(ValueAggregate.class));
				data = lapData.get(type);
			}
			switch (aggregate) {
			case MAX: 
			case MIN: 
				data.put(ValueAggregate.MAX, Math.max(value,getLapData(type, ValueAggregate.MAX, true)));
				data.put(ValueAggregate.MIN, Math.min(value,getLapData(type, ValueAggregate.MIN, true))); 
				break;
			case DELTA: 
				double delta = value + getLapData(type, aggregate, true);
				data.put(aggregate, delta);
				if (delta > 0) lapData.get(type).put(ValueAggregate.AVG, getLapData(type, ValueAggregate.VALUETIME, true) / delta);
				break;
			default: data.put(aggregate, value + getLapData(type, aggregate, true)); break;
			}
			endTime = System.currentTimeMillis();
		}

		public Double getLapData(ValueType type, ValueAggregate aggregate, boolean replaceNull) {
			if (lapData.get(type) == null || lapData.get(type).get(aggregate) == null) {
				if (replaceNull) switch (aggregate) {
				case MAX: return -Double.MAX_VALUE;
				case MIN: return Double.MAX_VALUE;
				default: return Double.valueOf(0); 
				} else 
					return null;
			}
			return lapData.get(type).get(aggregate);
		}

		public long getStartTime() {
			return startTime;
		}

		public long getEndTime() {
			return endTime;
		}

		public boolean has(ValueType type) {
			return lapData.get(type) != null;
		}


		public boolean hasData() {
			return lapData.size() > 0;
		}

		public void clear() {
			lapData.clear();
			endTime = 0;
		}
	}

	public static class LapDevice extends Device {
		
		public LapDevice() {
			super(DeviceType.INTERVAL);
		}

		@Override
		public void fadeOut(ValueType type) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public boolean isActive() {
			return true;
		}
		
	}
	public static void newLap(long startTime) {
		if (laps.size() > 0) {
			Lap lap = laps.get(laps.size() - 1);
			lap.setEndTime(startTime);
			if (!lap.hasData()) return;
		}
		laps.add(new Lap(startTime));
		currentLap = laps.size() - 1;
		lapDevice.fireOnDeviceValueChanged(ValueType.INTERVAL, currentLap, startTime);
		//DataLog.getInstance().log("lap", "" + currentLap, System.currentTimeMillis());
	}

	public static int getLaps() {
		return laps.size();
	}
	
	
	public static int getCurrentLap() {
		return currentLap;
	}


	public static boolean has(int lapIndex, ValueType type) {
		if (lapIndex < 0 || lapIndex >= laps.size()) return false;

		return laps.get(lapIndex).has(type);
	}

	public static void setAggregate(ValueType type, ValueAggregate aggregate, double value) {
		if (!isSupported(type, aggregate)) return;
		Lap lap = laps.get(currentLap);
		if (lap == null) {
			newLap(System.currentTimeMillis());
			lap = laps.get(currentLap);
		}
		if (lap == null) return;

		lap.setLapData(type, aggregate, value);
	}

	public static void addAggregate(ValueType type, ValueAggregate aggregate, double value) {
		if (!isSupported(type, aggregate)) return;
		Lap lap  = null;
		if (laps.size() > 0) 
			lap = laps.get(currentLap);
		if (lap == null) {
			newLap(System.currentTimeMillis());
			lap = laps.get(currentLap);
		}
		if (lap == null) return;

		lap.addLapData(type, aggregate, value);
	}

	public static Double getAggregate(int lapIndex, ValueType type, ValueAggregate aggregate) {
		if (lapIndex < 0 || lapIndex >= laps.size()) return null;
		Lap lap = laps.get(lapIndex);
		return lap.getLapData(type, aggregate, false);
	}

	public static long getDuration(int lapIndex) {
		if (lapIndex < 0 || lapIndex >= laps.size()) return 0;
		Lap lap = laps.get(lapIndex);
		return Math.max(lap.getEndTime() - lap.getStartTime(), 0);
	}

	public static boolean isSupported(ValueType type, ValueAggregate aggregate) {
		switch (type) {

		case POWER:
		case SPEED:
		case HEARTRATE:
		case CADENCE:
			switch(aggregate) {
			case VALUETIME:
			case DELTA:
			case MIN:
			case MAX:
			case AVG: 
				return true;
			default :
				return false; 
			}
		default:
			return false;


		}
	}
	
	public static LapDevice getLapDevice() {
		return lapDevice;
	}

	public static void clear() {
		for (Lap lap: laps) {
			lap.clear();
		}
		laps.clear();
		currentLap = 0;
	}

}
