package com.plusot.common.util;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;

public class ActivityUtil {
	private static boolean portret = true;
	
	public static boolean isPortret() {
		return portret;	
	}
	
	public static void lockScreenOrientation(Activity a) {
		if(a.getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED || a.getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_BEHIND || a.getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_USER) {
			switch (a.getResources().getConfiguration().orientation) {
			case Configuration.ORIENTATION_LANDSCAPE:
				a.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
				portret = false;
				break;
			case Configuration.ORIENTATION_PORTRAIT:
				a.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
				portret = true;
				break;
//			case Configuration.O.ORIENTATION_SQUARE:
//				a.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
//				break;
			default:
				break;
			}
			//a.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		} else {
			a.setRequestedOrientation(a.getRequestedOrientation());
		}
		//Globals.screenLock = true;
	}
	
	public static void unlockScreenOrientation(Activity a) {
		a.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
		//Globals.screenLock = false;
	}

}
