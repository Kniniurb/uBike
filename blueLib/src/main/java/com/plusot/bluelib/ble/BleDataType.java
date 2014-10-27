package com.plusot.bluelib.ble;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.plusot.bluelib.R;
import com.plusot.common.Globals;
import com.plusot.javacommon.util.Format;
import com.plusot.javacommon.util.TimeUtil;

public enum BleDataType {
	BT_TEMPERATURE(R.string.temperature, 0),
	BT_HEARTRATE(R.string.heartrate, 0),
	BT_BATTERYLEVEL(R.string.batterylevel, 0),
	BT_WHEELREVOLUTIONS(R.string.wheelrevolutions, 0),
	BT_CRANKREVOLUTIONS(R.string.crankrevolutions, 0),
	BT_UNKNOWN(R.string.unknown, 0),
	;
	
	final int label;
	final int decimals;
	
	
	private BleDataType(final int label, int decimals) {
		this.label = label;
		this.decimals = decimals;
	}

	public Unit getUnit() {
		switch (this) {
		case BT_TEMPERATURE:
			return Unit.CELSIUS;
		case BT_BATTERYLEVEL:
			return Unit.VOLT;
		case BT_CRANKREVOLUTIONS:
			return Unit.RPM;
		case BT_HEARTRATE:
			return Unit.BPM;
		case BT_UNKNOWN:
			return Unit.NONE;
		case BT_WHEELREVOLUTIONS:
			return Unit.RPM;
		}
		return Unit.NONE;
	}
	
	public boolean isValid() {
		return this != BT_UNKNOWN;
	}
	
	public String toString(double value) {
		return Globals.appContext.getString(label) + " " + Format.format(value, decimals) + " " + getUnit().toString();
	}
	
	public JSONObject toJSON(double value, long time) throws JSONException {
		JSONObject json = new JSONObject();
		JSONArray array = new JSONArray();
		array.put(value);
		array.put(unitString());
		array.put(TimeUtil.formatMilliTime(time, 1));
		json.put(toString(), array);
		return json;
	}
	
	public String unitString() {
		return getUnit().toString();
	}
	
	public String valueString(double value) {
		return Format.format(value, decimals);
	}
	
	public String typeString() {
		return Globals.appContext.getString(label); 
	}
	
	public static BleDataType fromString(String value) {
		for (BleDataType type: BleDataType.values())
			if (type.toString().equals(value)) return type;
		return null;
	}
}