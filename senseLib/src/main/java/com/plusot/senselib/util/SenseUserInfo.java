package com.plusot.senselib.util;

import com.plusot.common.util.UserInfo;
import com.plusot.senselib.settings.PreferenceKey;

public class SenseUserInfo extends UserInfo {
	
	public static long getDeviceId() {
		return UserInfo.getDeviceId(PreferenceKey.IMEI);
	}

}
