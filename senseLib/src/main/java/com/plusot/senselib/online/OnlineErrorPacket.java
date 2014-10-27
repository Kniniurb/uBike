package com.plusot.senselib.online;


public class OnlineErrorPacket extends OnlineMapPacket {
	
	public OnlineErrorPacket(final String deviceId, final String name, final String value) {
		super(OnlinePacketType.ERROR, deviceId, name, value);
		
	}
}
