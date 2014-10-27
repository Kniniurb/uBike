package com.plusot.senselib.online;

import java.util.Map;

public class OnlineAckPacket extends OnlineMapPacket {
	private static final String SESSIONTIME = "time";
	private static final String SESSIONID = "sessionid";
	private static final String MAXMSGID = "maxmsgid";
	private static final String ORGPACKET = "orgpacket";


	public OnlineAckPacket(final String deviceId, final Map<String, String> data) {
		super(OnlinePacketType.ACKPACKET, deviceId, data);
	}

	public OnlineAckPacket(final String deviceId, final OnlinePacketType packetType) {
		super(OnlinePacketType.ACKPACKET, deviceId, ORGPACKET, packetType.toString());
		//if (sessionTime != null) setSessionTime(sessionTime);
	}

	public Long getSessionTime() {
		String value = getValue(SESSIONTIME);
		if (value == null) return null;
		try {
			return Long.valueOf(value);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public void setSessionTime(long time) {
		setValue(SESSIONTIME, "" + time);
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

	public void setSessionId(int id) {
		setValue(SESSIONID, "" + id);
	}

	public Integer getMaxMsgId() {
		String value = getValue(MAXMSGID);
		if (value == null) return null;
		try {
			return Integer.valueOf(value);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public void setMaxMsgId(int id) {
		setValue(MAXMSGID, "" + id);
	}

	public OnlinePacketType getOrgPacketType() {
		String value = getValue(ORGPACKET);
		return OnlinePacketType.fromString(value);
	}
	
	public void setOrgPacketType(OnlinePacketType type) {
		setValue(ORGPACKET, type.toString());
	}

}
