package com.plusot.senselib.online;

import java.util.Map;

public class OnlineEchoPacket extends OnlineMapPacket {

	public OnlineEchoPacket(final String deviceId, final Map<String, String> data) {
		super(OnlinePacketType.ECHO, deviceId, data);
		
	}
	
	public OnlineEchoPacket(final String deviceId, final String name, final String value) {
		super(OnlinePacketType.ECHO, deviceId, name, value);
		
	}
}
