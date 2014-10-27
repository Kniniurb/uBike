package com.plusot.senselib.online;

public enum OnlineActionType {
	ACK,			//acknowledgement on message
	ACKGET,   //acknowledgement of reception, but with request for ...
	NACK,		//not acknowledged message
	NACKGET, //not acknowledged, with request for ...
	//		GET("get"),         //get request for information
	POST;       //post of data

	public static OnlineActionType fromString(String text) {
		if (text != null) {
			for (OnlineActionType actionType : OnlineActionType.values()) {
				if (text.equalsIgnoreCase(actionType.toString())) {
					return actionType;
				}
			}
		}
		return null;
	}
}
