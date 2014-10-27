package com.plusot.senselib.online;

import java.util.Map;


public class OnlineUserPacket extends OnlineMapPacket {

	public OnlineUserPacket(final String deviceId, final Map<String, String> data) {
		super(OnlinePacketType.USERINFO, deviceId, data);
	}

	public OnlineUserPacket(final String deviceId, final String user, final String email, final String displayName, final String model, final boolean privacy) {
		super(OnlinePacketType.USERINFO, deviceId, "name", user);
		setValue("email", email);				
		setValue("display", displayName);				
		setValue("isowner", "true");				
		setValue("model", model);		
		setValue("privacy", Boolean.valueOf(privacy).toString());				
	}

}
