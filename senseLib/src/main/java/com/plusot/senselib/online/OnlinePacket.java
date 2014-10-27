package com.plusot.senselib.online;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class OnlinePacket {
	private final OnlinePacketType packetType;
	private final String deviceId;
	private long imei;
	
	public OnlinePacket(final OnlinePacketType packetType, final String deviceId) {
		this.packetType = packetType;
		this.deviceId = deviceId;
		try {
			this.imei = Long.valueOf(deviceId);
		} catch (NumberFormatException e) {
			try {
				this.imei = Long.valueOf(deviceId, 16);
			} catch (NumberFormatException e2) {
				this.imei = 0L;
			}
		}
 	}
	
	public OnlinePacket(final OnlinePacketType packetType, final long imei) {
		this.packetType = packetType;
		this.deviceId = String.valueOf(imei);
		this.imei = imei;
 	}
	
	public boolean hasData() {
		return false;
	}
	
	public String getDeviceId() {
		return deviceId;
	}
	
	public long getImei() {
		return imei;
	}
	
	public JSONObject asJSONObject() throws JSONException {
		JSONObject json = new JSONObject();
		return json;
	}

	public OnlinePacketType getPacketType() {
		return packetType;
	}
	
	public static OnlinePacket factory(final OnlinePacketType packetType, final String deviceId) {
		return new OnlinePacket(packetType, deviceId);
	}
	
	public static OnlinePacket factory(OnlinePacketType packetType, final String deviceId, final Map<String, String> map) {
		switch (packetType) {
		case ECHO:
		case ERROR:
			return new OnlinePacket(packetType, deviceId);
		case USERINFO:
			return new OnlineUserPacket(deviceId, map);
		case DATA:
			return new OnlineDataPacket(deviceId, map);
		case SESSION:
			return new OnlineSessionPacket(deviceId, map);
		case ACKPACKET:
			return new OnlineAckPacket(deviceId, map);
		default:
			return null;
		}
	}
	
	@Override
	public String toString() {
		return "Packet: " + packetType.toString() + " from " + deviceId;
	}
	

}
