package com.plusot.common.util;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;

import com.plusot.common.Globals;
import com.plusot.common.settings.PreferenceKeyInterface;
import com.plusot.common.share.LLog;

public class UserInfo {
	private static final String CLASSTAG = UserInfo.class.getSimpleName();
	private static String deviceId = null;
	private static long deviceNr = -1;

	public static class UserData {
		public String name;
		public String email;
	}

	private static UserData data = null;

	public static UserData getUserData() {
		//if (data != null) return data;
		AccountManager manager = AccountManager.get(Globals.appContext); 
		Account[] accounts = manager.getAccountsByType("com.google"); 
		if (accounts == null || accounts.length == 0) {
			accounts = manager.getAccounts();
		}
		data = new UserData();
		for (Account account : accounts) {
			LLog.d(Globals.TAG,  CLASSTAG + " Account: " + account);
			data.email = account.name;
			data.name = account.name.substring(0, account.name.indexOf("@"));
			return data;
		}
		data.email = "appuser@broxtech,com";
		data.name = "AppUser Broxtech";
		return data;
	}

	public static void resetDeviceId() {
		deviceId = null;
		deviceNr = -1;
	}

	public static long getDeviceId(PreferenceKeyInterface preferenceKey) {
		if (deviceId == null) {
			if (preferenceKey != null) {
				String imei = preferenceKey.getString();
				if (imei != "" && imei != null) deviceId = imei;
			}

			String temp = "";
			if (deviceId == null) {
				TelephonyManager telManager = (TelephonyManager) Globals.appContext.getSystemService(Context.TELEPHONY_SERVICE);
				deviceId = telManager.getDeviceId();
			}
			if (deviceId == null) {
				deviceId = Secure.getString(Globals.appContext.getContentResolver(), Secure.ANDROID_ID);
			} else {
				temp = " " + Secure.getString(Globals.appContext.getContentResolver(), Secure.ANDROID_ID);
			}
			if (deviceId == null) {
				WifiManager manager = (WifiManager) Globals.appContext.getSystemService(Context.WIFI_SERVICE);
				WifiInfo info = manager.getConnectionInfo();
				deviceId = info.getMacAddress();
			}
			if (deviceId == null) {
				deviceId = "";
				for (int i = 0; i < 12; i++) {
					int random = (int)Math.floor(35.9 * Math.random());
					if (random < 10)
						deviceId += "" + random;
					else
						deviceId += (char)('a' + random);
				}
			}
			if (preferenceKey != null) preferenceKey.set(deviceId);

			//LLog.d(Globals.TAG, CLASSTAG + " DeviceId = " + deviceId);
			if (deviceId != null) try {
				deviceNr = Math.abs(Long.parseLong(deviceId));
			} catch (NumberFormatException e) {
				/*try {
					deviceId = deviceId.replace(":","").toLowerCase();
					deviceNr = Math.abs(Long.parseLong(deviceId, 16));
				} catch (NumberFormatException e2) {*/
				deviceNr = 0;
				for (int i = 0; i < Math.min(deviceId.length(), 16); i++) {
					if (i > 0) deviceNr *= 16;
					char ch = deviceId.charAt(i);
					if (ch >= 'a' && ch <= 'z') deviceNr += (ch - 'a') + 10;
					if (ch >= '0' && ch <= '9') deviceNr += (ch - '0');
					if (i == 0 && deviceId.length() >= 16) deviceNr = (deviceNr & 0x3); 
				}
				deviceNr = Math.abs(deviceNr);
				//}
			}
			//if (Globals.testing.isTest()) ToastHelper.showToastLong("DeviceId  = " + deviceNr);
			LLog.d(Globals.TAG, CLASSTAG + "DeviceId  = " + deviceId + " (" + deviceNr + ")" + temp);
		}



		return deviceNr;
	}	

	public static String appNameVersion() {
		try {
			PackageInfo info = Globals.appContext.getPackageManager().getPackageInfo(Globals.appContext.getPackageName(), 0);
			return Globals.TAG + " " + info.versionName;
		} catch (NameNotFoundException e) {
			return Globals.TAG;
		}

	}

}
