package com.plusot.senselib.online;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public abstract class OnlineMapPacket extends OnlinePacket {
	private Map<String, String> data = new HashMap<String, String>();
	
	public OnlineMapPacket(final OnlinePacketType type, final String deviceId, final Map<String, String> data) {
		super(type, deviceId);
		for (String dataItem: data.keySet()) {
			String dataValue = data.get(dataItem);
			if (dataValue != null && !dataValue.equals("")) this.data.put(dataItem, dataValue);		
		}		
	}
	
	public OnlineMapPacket(final OnlinePacketType type, final String deviceId, final String name, final String value) {
		super(type, deviceId);
		data.put(name, value);				
	}
	
	public boolean hasData() {
		return true;
	}
	
	protected void setValue(String name, String value) {
		data.put(name,  value);
	}
	
	public Map<String, String> getData() {
		return data;
	}
	
	protected String getValue(String key) {
		return data.get(key);
	}
	
	@Override
	public JSONObject asJSONObject() throws JSONException {
		JSONObject json = super.asJSONObject();
		for (String key: data.keySet()) {
			json.put(key, data.get(key));
		}
		return json;
	}
	
	/*@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (String key : data.keySet()) {
			sb.append(" ").append(key).append("=").append(data.get(key));
		}
		return super.toString() + " (" + sb.toString() + ")";
	}*/

}
