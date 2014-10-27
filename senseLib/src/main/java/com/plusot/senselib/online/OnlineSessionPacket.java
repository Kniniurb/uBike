package com.plusot.senselib.online;

import com.plusot.javacommon.util.TimeUtil;

import java.util.Map;


public class OnlineSessionPacket extends OnlineMapPacket {
	private static final String RIDES = "total_rides";
	private static final String TIME = "time";
	private static final String TOTAL_TIME = "total_time";
	private static final String TOTAL_DISTANCE = "total_distance";
	private static final String TOTAL_ENERGY = "total_energy";
	private static final String TOTAL_ASCENT = "total_ascent";
	private static final String PEAK = "peak";


	public OnlineSessionPacket(final String deviceId, final Map<String, String> data) {
		super(OnlinePacketType.SESSION, deviceId, data);
	}

	public OnlineSessionPacket(final String deviceId, final long time, final int rides, final long total_time, final int total_distance, final int total_energy, final int total_ascent, final int peak) {
		super(OnlinePacketType.SESSION, deviceId, TIME, "" + TimeUtil.timeToUTC(time) / 1000);
		setValue(RIDES, "" + rides);				
		setValue(TOTAL_TIME, "" + total_time);				
		setValue(TOTAL_DISTANCE, "" + total_distance);				
		setValue(TOTAL_ENERGY, "" + total_energy);				
		setValue(TOTAL_ASCENT, "" + total_ascent);				
		setValue(PEAK, "" + peak);				
	}
	
	public Integer getSecondTime() {
		String value = getValue(TIME);
		if (value == null) return null;
		try {
			Long longValue = Long.valueOf(value);
			if (longValue != null && longValue > System.currentTimeMillis() - 3650L * 86400000L) return (int) (longValue / 1000);
			return Integer.valueOf(value);
		} catch (NumberFormatException e) {
			return null;
		}
	}
	
	public String getTimeStr() {
		Integer time = getSecondTime();
		if (time == null) return null;
		return TimeUtil.formatTimeUTC(1000L * time);
	}
	
	public Integer getRides() {
		String value = getValue(RIDES);
		if (value == null) return null;
		try {
			return Integer.valueOf(value);
		} catch (NumberFormatException e) {
			return null;
		}
	}
	
	public Long getTotalTime() {
		String value = getValue(TOTAL_TIME);
		if (value == null) return null;
		try {
			return Long.valueOf(value);
		} catch (NumberFormatException e) {
			return null;
		}
	}
	
	public String getTotalTimeStr() {
		Long time = getTotalTime();
		if (time == null) return null;
		return TimeUtil.formatTime(time);
	}
	
	
	public Integer getTotalDistance() {
		String value = getValue(TOTAL_DISTANCE);
		if (value == null) return null;
		try {
			return Integer.valueOf(value);
		} catch (NumberFormatException e) {
			return null;
		}
	}
	
	public Integer getTotalEnergy() {
		String value = getValue(TOTAL_ENERGY);
		if (value == null) return null;
		try {
			return Integer.valueOf(value);
		} catch (NumberFormatException e) {
			return null;
		}
	}
	
	public Integer getTotalAscent() {
		String value = getValue(TOTAL_ASCENT);
		if (value == null) return null;
		try {
			return Integer.valueOf(value);
		} catch (NumberFormatException e) {
			return null;
		}
	}
	
	public Integer getHighestPeak() {
		String value = getValue(PEAK);
		if (value == null) return null;
		try {
			return Integer.valueOf(value);
		} catch (NumberFormatException e) {
			return null;
		}
	}

}
