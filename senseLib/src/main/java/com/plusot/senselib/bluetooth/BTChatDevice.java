package com.plusot.senselib.bluetooth;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.bluetooth.BluetoothSocket;

import com.plusot.bluelib.ble.BleDataType;
import com.plusot.common.Globals;
import com.plusot.common.share.LLog;
import com.plusot.javacommon.util.StringUtil;
import com.plusot.javacommon.util.TimeUtil;
import com.plusot.senselib.ant.DeviceManufacturer;
import com.plusot.senselib.values.DeviceType;
import com.plusot.senselib.values.ValueItem;
import com.plusot.senselib.values.ValueType;

public class BTChatDevice extends BTDevice {
	private static final String CLASSTAG = BTChatDevice.class.getSimpleName();
	
	public BTChatDevice(String deviceName) {
		super(DeviceType.BTCHAT, deviceName);	
		manufacturer = DeviceManufacturer.PLUSOT;
		productId = "btChat";
		deviceName = "btChat";
	}
	
	@Override
	public void onBluetoothReadBytes(BTConnectedThread sender, byte[] msg) {
		active = System.currentTimeMillis();
		LLog.d(Globals.TAG, CLASSTAG + ".onBluetoothReadBytes: " + StringUtil.toHexString(msg)); 
	}
	
	@Override
	public void onBluetoothRead(BTConnectedThread sender, String msg) {
		active = System.currentTimeMillis();
		LLog.d(Globals.TAG, CLASSTAG + ".onBluetoothRead: " + msg);
		msg.replace("}{", "}\n{");
		String[] parts = msg.split("\n");
		for (String part : parts) try {
			JSONObject json = new JSONObject(part);
			LLog.d(Globals.TAG, CLASSTAG + ".onBluetoothRead: processing " + part);
			
			if (part.startsWith("{\"BT_")) {
				fromBTJSON(json);
			} else {
				fromJSON(json);
			}
		} catch (JSONException e) {
			LLog.e(Globals.TAG, CLASSTAG + ".onBluetoothRead Could not create JSON object from " + part, e);
		}
	}
	
	private void fromBTJSON(JSONObject obj) throws JSONException {
		BleDataType type;
		Object sub;
		JSONArray array = obj.names();
		if (array != null) for (int i = 0; i < array.length(); i++ ) {
			String name = array.getString(i);
			if ((type = BleDataType.fromString(name)) != null && (sub = obj.get(name)) instanceof JSONArray) {
				JSONArray values = (JSONArray) sub;
				if (values.length() >= 3) {
					double value = values.getDouble(0);
					//String unit = values.getString(1);
					String time = values.getString(2);
					long timeStamp = TimeUtil.parseTime(time, "HH:mm:ss");
					ValueType valueType;
					if ((valueType = ValueType.getValueType(type)) != null) 
						this.fireOnDeviceValueChanged(valueType, value, timeStamp);
				}
			}
		}
	}
	
	private void fromJSON(JSONObject obj) throws JSONException {
		ValueType type;
		Object sub;
		JSONArray array = obj.names();
		if (array != null) for (int i = 0; i < array.length(); i++ ) {
			String name = array.getString(i);
			if ((type = ValueType.fromJSONString(name)) != null && (sub = obj.get(name)) instanceof JSONArray) {
				JSONArray values = (JSONArray) sub;
				if (values.length() >= 3) {
					ValueItem item = type.fromJSON(values);
					this.fireOnDeviceValueChanged(type, item.getValue(), item.getTimeStamp());
				}
			}
		}
	}
	
	public void write(String value) {
		if (connectedThread != null) {
			LLog.d(Globals.TAG, CLASSTAG + ".write: " + value);
			connectedThread.write(value);
		}
	}

	@Override
	protected BTConnectedThread createConnectedThread(BluetoothSocket socket) {
		BTConnectedThread thread = new BTConnectedThread(this, socket, null, BTConnectedThread.Type.STRINGS);
		BTAcceptThread.setStateConnected();
		return thread;
	}

}