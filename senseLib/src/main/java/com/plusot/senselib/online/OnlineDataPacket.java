package com.plusot.senselib.online;

import java.util.Map;

public class OnlineDataPacket extends OnlineMapPacket {
	public static final String TIME = "time";
	public static final String SESSIONID = "sessionid";

	public OnlineDataPacket(final String deviceId, final String time, final int sessionId, final Map<String, String> data) {
		super(OnlinePacketType.DATA, deviceId, data);
		setValue(TIME, time);
		setValue(SESSIONID, "" + sessionId);
		
	}
	
	public OnlineDataPacket(final String deviceId, final Map<String, String> data) {
		super(OnlinePacketType.DATA, deviceId, data);
	}
	
	public void setSessionId(int id) {
		setValue(SESSIONID, "" + id);
	}
	
	public Integer getSessionId() {
		String value = getValue(SESSIONID);
		if (value == null) return null;
		try {
			return Integer.valueOf(value);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
