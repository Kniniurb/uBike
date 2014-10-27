package com.plusot.bluelib.ble;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Set;

import com.plusot.javacommon.util.TimeUtil;

public class BleData {
	private EnumMap<BleDataType, Double> data = new EnumMap<BleDataType, Double>(BleDataType.class);
	private long time;
	
	public BleData() {
		time = System.currentTimeMillis();
	}
	
	public void add(BleDataType type, Double value) {
		data.put(type, value);
	}

	public long getTime() {
		return time;
	}			
	
	public Set<BleDataType> getKeys() {
		return data.keySet();
	}
	
	public Collection<Double> getValues() {
		return data.values();
	}
	
	public Double get(BleDataType type) {
		return data.get(type);
	}
	
	public void merge(BleData otherData) {
		Double value;
		for (BleDataType type : otherData.data.keySet()) {
			if ((value = otherData.data.get(type)) != null) data.put(type, value);
		}
		time = Math.max(time, otherData.time);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (BleDataType type: data.keySet()) {
			Double value = data.get(type);
			if (value != null) {
				if (sb.length() > 0) sb.append(",");
				sb.append(type.toString(value));
			}
			
		}
		sb.append(", ").append(TimeUtil.formatTime(getTime()));
		return sb.toString();
	}

}
