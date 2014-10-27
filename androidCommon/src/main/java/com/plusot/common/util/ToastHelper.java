package com.plusot.common.util;

import android.widget.Toast;

import com.plusot.common.Globals;

public class ToastHelper {
	public static void showToastLong(String value) {
		//if (toast != null) toast.cancel();
		Toast toast = Toast.makeText(
				Globals.appContext, 
				value, 
				Toast.LENGTH_LONG);
		toast.show();
	}

	public static void showToastLong(int value) {
		//if (toast != null) toast.cancel();
		Toast toast = Toast.makeText(
				Globals.appContext, 
				Globals.appContext.getString(value), 
				Toast.LENGTH_LONG);
		toast.show();
	}

	public static void showToastShort(String value) {
		//if (toast != null) toast.cancel();
		Toast toast = Toast.makeText(
				Globals.appContext, 
				value, 
				Toast.LENGTH_SHORT);
		toast.show();
	}
	
	public static void showToastShort(int value, int gravity, int xOffset, int yOffset) {
		//if (toast != null) toast.cancel();
		Toast toast = Toast.makeText(
				Globals.appContext, 
				Globals.appContext.getString(value), 
				Toast.LENGTH_SHORT);
		if (gravity != -1) toast.setGravity(gravity, xOffset, yOffset);
		toast.show();
	}
	
	public static void showToastShort(int value) {
		showToastShort( value, -1, 0, 0);
	}
	
	public static void showToastShort(int value, int gravity) {
		showToastShort(value, gravity, 0, 0);
	}

}
