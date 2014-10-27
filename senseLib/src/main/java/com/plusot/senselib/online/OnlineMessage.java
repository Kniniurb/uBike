package com.plusot.senselib.online;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class OnlineMessage {
	private static int msgIdCounter = 0;
	private final int msgId;
	private final OnlineActionType action;
	private final OnlinePacket packet;

	public static class OnlineDecodeException extends Exception {
		private static final long serialVersionUID = 6164495196007864592L;

		public OnlineDecodeException(String message,
                Throwable cause) {
			super(message, cause);
		}

	};

	public OnlineMessage(final int msgId, final OnlineActionType action, final OnlinePacket packet) {
		this.msgId = msgId;
		this.action = action;
		this.packet = packet;
	}

	public OnlineMessage(final int msgId, final OnlineActionType action, final OnlinePacketType packetType, final String deviceId)  {
		this.msgId = msgId;
		this.action = action;
		this.packet = OnlinePacket.factory(packetType, deviceId);
	}

	public OnlineMessage(final OnlineActionType action, final OnlinePacket packet) {
		this(++msgIdCounter, action, packet);
	}

	public OnlineMessage(final OnlineActionType action, final OnlinePacketType packetType, final String deviceId)  {
		this(++msgIdCounter, action, packetType, deviceId);
	}

	@Override
	public String toString() {
		if (action == null && packet == null) return "" + msgId;
		if (action == null) return "" + msgId + " " + packet.toString();
		if (packet == null) return "" + msgId + " " + action.toString();
		return "" + msgId + " " + action.toString() + ", " + packet.toString();
	}

	public JSONObject toJSON() throws JSONException {
		JSONObject json = new JSONObject();
		json.put(OnlineMessageElement.MSGID.toString(), msgId);
		json.put(OnlineMessageElement.DEVICE.toString(), packet.getDeviceId());
		json.put(OnlineMessageElement.ACT.toString(), action.toString());
		OnlinePacketType type = packet.getPacketType();
		if (type != null) {
			json.put(OnlineMessageElement.TYPE.toString(), type.toString());
		}
		if (packet.hasData()) {
			json.put(OnlineMessageElement.PACKET.toString(), packet.asJSONObject());
		}
		return json;
	}

	public static JSONObject toJSON(Collection<OnlineMessage> coll) throws JSONException {
		JSONObject json = new JSONObject();
		List<JSONObject> objs = new ArrayList<JSONObject>();
		for (OnlineMessage msg: coll) {
			JSONObject obj = msg.toJSON();
			if (obj != null) objs.add(obj);
		}
		json.put(OnlineMessageElement.BLOCKS.toString(), objs);
		return json;
	}

	public static String decode(String value) throws OnlineDecodeException {
		try {
			return URLDecoder.decode(value, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new OnlineDecodeException("OnlineDecodeException in OnlineMessage.decode caused by " + value, e);
		} catch (Exception e) {
			return value;
		}
	}

	public static String toEncodedString(String value) throws UnsupportedEncodingException {
		return URLEncoder.encode(value, "UTF-8");
	}

	public static OnlineMessage[] fromJSON(String jsonString) throws JSONException, OnlineDecodeException {
		if (jsonString == null || jsonString.length() == 0) return null;
		JSONObject json;
		List<OnlineMessage> objs = new ArrayList<OnlineMessage>();
		//try {
		json = new JSONObject(decode(jsonString));
		//} catch (JSONException e)
		if (json.has(OnlineMessageElement.BLOCKS.toString())) {
			JSONArray array = json.getJSONArray(OnlineMessageElement.BLOCKS.toString());
			for (int i = 0; i < array.length(); i++) {
				JSONObject obj = array.getJSONObject(i);
				OnlineMessage msg = fromJSON(obj);
				if (msg != null) objs.add(msg);
			}
			return objs.toArray(new OnlineMessage[0]);
		} else {
			OnlineMessage msg = fromJSON(json);
			if (msg != null)
				return new OnlineMessage[] {msg};
			else
				return null;
		}

	}

	private static OnlineMessage fromJSON(JSONObject json) throws JSONException {
		@SuppressWarnings("unchecked")
		Iterator<String> keys = json.keys();
		int msgid = 0;
		String deviceId = null;
		String actionStr = null;
		String packetTypeStr = null;
		//String data = null;
		Map<String, String> dataMap = null;
		while (keys.hasNext()) {
			String key = keys.next();
			if (key.equals(OnlineMessageElement.MSGID.toString())) {
				try {
					msgid = Integer.valueOf(json.getInt(key));
				} catch (NumberFormatException e) {
					msgid = -1;
				}

			} else if (key.equals(OnlineMessageElement.DEVICE.toString())) {
				deviceId = json.getString(key);
			} else if (key.equals(OnlineMessageElement.ACT.toString())) {
				actionStr = json.getString(key);
			} else if (key.equals(OnlineMessageElement.TYPE.toString())) {
				packetTypeStr = json.getString(key);
			} else if (key.equals(OnlineMessageElement.PACKET.toString())) {
				//data = value;
				JSONObject jsonData = json.getJSONObject(key);
				@SuppressWarnings("unchecked")
				Iterator<String> dataKeys = jsonData.keys();
				dataMap = new HashMap<String, String>();
				while (dataKeys.hasNext()) {
					String dataKey = dataKeys.next();
					dataMap.put(dataKey, jsonData.getString(dataKey));
				}

				//JSONObject jsonData= new JSONObject
				//GWT.log("Data = " + data);
			}
		}

		if (actionStr == null || packetTypeStr == null) return null;
		OnlinePacketType packetType = OnlinePacketType.fromString(packetTypeStr);
		if (dataMap == null)
			return new OnlineMessage(msgid, OnlineActionType.fromString(actionStr), packetType, deviceId);
		OnlinePacket packet = OnlinePacket.factory(packetType, deviceId, dataMap);
		if (packet == null)
			return new OnlineMessage(msgid, OnlineActionType.fromString(actionStr), packetType, deviceId);
		return new OnlineMessage(msgid, OnlineActionType.fromString(actionStr), packet);
	}
	/*
	public static OnlineMessage fromJSON(String jsonString) throws JSONException, NumberFormatException {
		JSONObject json = new JSONObject(decode(jsonString));
		@SuppressWarnings("unchecked")
		Iterator<String> keys = json.keys();
		int msgid = 0;
		String deviceId = null;
		String actionStr = null;
		String packetTypeStr = null;
		//String data = null;
		Map<String, String> dataMap = null;
		while (keys.hasNext()) {
			String key = keys.next();
			if (key.equals(OnlineMessageElement.MSGID.toString())) {
				msgid = Integer.valueOf(json.getInt(key));
			} else if (key.equals(OnlineMessageElement.DEVICE.toString())) {
				deviceId = json.getString(key);
			} else if (key.equals(OnlineMessageElement.ACT.toString())) {
				actionStr = json.getString(key);
			} else if (key.equals(OnlineMessageElement.TYPE.toString())) {
				packetTypeStr = json.getString(key);
			} else if (key.equals(OnlineMessageElement.PACKET.toString())) {
				//data = value;
				JSONObject jsonData = json.getJSONObject(key);
				@SuppressWarnings("unchecked")
				Iterator<String> dataKeys = jsonData.keys();
				dataMap = new HashMap<String, String>();
				while (dataKeys.hasNext()) {
					String dataKey = dataKeys.next();
					dataMap.put(dataKey, jsonData.getString(dataKey));
				}

				//JSONObject jsonData= new JSONObject 
				//GWT.log("Data = " + data);
			}
		}
		if (actionStr == null || packetTypeStr == null) return null;
		OnlinePacketType packetType = OnlinePacketType.fromString(packetTypeStr);
		if (dataMap == null)
			return new OnlineMessage(msgid, OnlineActionType.fromString(actionStr), packetType, deviceId);
		return new OnlineMessage(msgid, OnlineActionType.fromString(actionStr), OnlinePacket.factory(packetType, deviceId, dataMap));
	}*/

	public int getMsgId() {
		return msgId;
	}

	public OnlineActionType getAction() {
		return action;
	}

	public OnlinePacket getPacket() {
		return packet;
	}


}
