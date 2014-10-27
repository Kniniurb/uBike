package com.plusot.senselib.online;

public enum OnlinePacketType {
	DATA,
	ECHO,
	ERROR,
	USERINFO,
	SESSION,
	ACKPACKET;

	/*public boolean hasData() {
		switch (this) {
		case DEVICEINFO:
		case DATA: 
			return true;
		default:
			return false;
		}
	}*/

	public static OnlinePacketType fromString(String text) {
		if (text != null) {
			for (OnlinePacketType type : OnlinePacketType.values()) {
				if (text.equalsIgnoreCase(type.toString())) {
					return type;
				}
			}
		}
		return null;
	}
	
	public static OnlinePacketType fromInt(int index) {
		for (OnlinePacketType type : OnlinePacketType.values()) {
			if (index == type.ordinal()) {
				return type;
			}
		}
		return null;
	}
}
