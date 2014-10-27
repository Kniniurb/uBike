package com.plusot.senselib.bluetooth;

import android.bluetooth.BluetoothDevice;

import com.plusot.common.Globals;
import com.plusot.common.share.LLog;

public enum BTType {
	BTCHAT,
	KESTREL,
	HXM;

    private static final boolean DEBUG = false;

    public static BTType fromString(String value, BTTypeMajor major) {
		if (value == null) return null;
		if (DEBUG) LLog.d(Globals.TAG, BTType.class.getSimpleName() + ".fromString Name : " + value + " Major " + major);
		switch (major) {
		case COMPUTER:
		case PHONE:
				//if (value.startsWith("Nexus")) 
			return BTType.BTCHAT;
		case HEALTH:
		case IMAGING:
		case MISC:
		case NETWORKING:
		case PERIPHERAL:
		case TOY:
		case WEARABLE:
		case AUDIO_VIDEO:
			break;
		case UNCATEGORIZED:
		default:
			if (	value.startsWith("HXM")) 
				return BTType.HXM;
			if (	value.startsWith("K4000") || value.startsWith("K4200") || 
					value.startsWith("K4300") || value.startsWith("K4400") || 
					value.startsWith("K4500") || 
					value.startsWith("CSR - bc4") || value.startsWith("00:06:66")) 
				return BTType.KESTREL;	
			break;
		}
		return null;
	}

	public static BTType fromDevice(BluetoothDevice device) {
		String name = BTHelper.getBTName(device);
		if (name == null) return null;
		BTTypeMajor major = BTTypeMajor.fromDevice(device);
		if (major == null) return null;

		return fromString(name, major);
	}
}
