package com.plusot.common.settings;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

import com.plusot.common.Globals;
import com.plusot.common.share.LLog;

public class PreferenceHelper {
	public static final String CLASSTAG = PreferenceHelper.class.getSimpleName();

	private static SharedPreferences prefs = null;


	public static synchronized SharedPreferences getPrefs() {
		if (prefs == null) prefs = PreferenceManager.getDefaultSharedPreferences(Globals.appContext);
		return prefs;
	}

	public static void setFromListArray(String key, int value){ 
		getPrefs();
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(key, String.valueOf(value));
		editor.commit();
	}


	public static int getFromListArray(String key, int defaultValue){ 
		getPrefs();
		if (!prefs.contains(key)) setFromListArray(key, defaultValue);
		try {
			return Integer.valueOf(prefs.getString(key, String.valueOf(defaultValue)));
		} catch (NumberFormatException e) {
			LLog.e(Globals.TAG, ".getFromListArray Could not convert " + prefs.getString(key, String.valueOf(defaultValue)) + " to integer for " + key);
			return defaultValue;
		} catch (ClassCastException e) {
			LLog.e(Globals.TAG, ".getFromListArray Could not convert to integer for " + key, e);
			return defaultValue;
		}
	}

	public static String get(String key, String defaultValue) {
		getPrefs();
		if (prefs.contains(key)) return prefs.getString(key, defaultValue);
		prefs.edit().putString(key, defaultValue).commit();
		return defaultValue;
	}


	public static double get(String key, double defaultValue) {
		getPrefs();
		if (prefs.contains(key)) return prefs.getFloat(key, Double.valueOf(defaultValue).floatValue());
		prefs.edit().putFloat(key, (float) defaultValue).commit();
		return defaultValue;
	}

	public static double get(String key, float defaultValue) {
		getPrefs();
		if (prefs.contains(key)) return prefs.getFloat(key, defaultValue);
		prefs.edit().putFloat(key, defaultValue).commit();
		return defaultValue;
	}

	public static int get(String key, int defaultValue) {
		getPrefs();
		if (prefs.contains(key)) return prefs.getInt(key, defaultValue);
		prefs.edit().putInt(key, defaultValue).commit();
		return defaultValue;
	}

	public static long get(String key, long defaultValue) {
		getPrefs();
		if (prefs.contains(key)) return prefs.getLong(key, defaultValue);
		prefs.edit().putLong(key, defaultValue).commit();
		return defaultValue;
	}

	public static boolean get(String key, boolean defaultValue) {
		getPrefs();
		if (prefs.contains(key)) return prefs.getBoolean(key, defaultValue);
		prefs.edit().putBoolean(key, defaultValue).commit();
		return defaultValue;
	}


	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static Set<String> getStringSet(String key, Set<String> defaultSet) {
		getPrefs();

		if (Build.VERSION.SDK_INT >= 11) try {
			
			LLog.d(Globals.TAG,  CLASSTAG + ".getStringSet " + key);
			if (prefs.contains(key)) {
				Set<String> set = new HashSet<String>();
				Set<String> storedSet = prefs.getStringSet(key, defaultSet);
				if (storedSet != null) set.addAll(storedSet);
				return set;
			}
			prefs.edit().putStringSet(key, defaultSet).commit();
			return defaultSet;
		} catch (ClassCastException e) {
			LLog.d(Globals.TAG, CLASSTAG + ".getStringSet Invalid cast");
		}
		String strings[] = null;
		if (defaultSet == null) {
			String str = get(key, (String)null);
			if (str != null && str.length() > 0) strings = str.split(";");
		} else {
			StringBuilder sb = new StringBuilder();
			for (String tmp : defaultSet) {
				if (sb.length() > 0) sb.append(";");
				sb.append(tmp);
			}
			String str = get(key, sb.toString());
			if (str != null && str.length() > 0) strings = str.split(";");
		}
		if (strings == null) return defaultSet;
		Set<String> set = new HashSet<String>();
		for (String s : strings){
			set.add(s);
		}

		return set;
	}

	public static List<String> get(String key, List<String> defaultList) {
		getPrefs();

		String strings[] = null;
		if (defaultList == null) {
			String str = get(key, (String)null);
			if (str != null && str.length() > 0) strings = str.split(";");
		} else {
			StringBuilder sb = new StringBuilder();
			for (String tmp : defaultList) {
				if (sb.length() > 0) sb.append(";");
				sb.append(tmp);
			}
			String str = get(key, sb.toString());
			if (str != null && str.length() > 0) strings = str.split(";");
		}
		if (strings == null) return defaultList;
		List<String> list = new ArrayList<String>();
		for (String s : strings){
			list.add(s);
		}

		return list;
	}
	
	public static String get(String key, int index, String defaultValue) {
		getPrefs();
		List<String> list = get(key, (List<String>) null);
		if (list == null) {
			set(key, index, defaultValue);
			return defaultValue;
		}
		if (index < 0) return defaultValue;
		if (index >= list.size()) {
			set(key, index, defaultValue);
			return defaultValue;
		}
		return list.get(index);
	}

	public static String[] getStrings(String key, String[] defaultValue) {
		getPrefs();
		if (defaultValue == null) {
			String str = get(key, (String)null);
			return str.split(";");
		} 
		StringBuilder sb = new StringBuilder();
		for (String tmp : defaultValue) {
			if (sb.length() > 0) sb.append(";");
			sb.append(tmp);
		}
		String str = get(key, sb.toString());
		return str.split(";");
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static void setStringSet(String key, Set<String> set) {
		getPrefs();
		if (Build.VERSION.SDK_INT >= 11) {
			prefs.edit().putStringSet(key, set).commit();
		} else {
			StringBuilder sb = new StringBuilder();
			for (String tmp : set) {
				if (sb.length() > 0) sb.append(";");
				sb.append(tmp);
			}
			set(key, sb.toString());
		}
	}

	public static void set(String key, List<String> list) {
		getPrefs();
		StringBuilder sb = new StringBuilder();
		for (String tmp : list) {
			if (sb.length() > 0) sb.append(";");
			sb.append(tmp);
		}
		set(key, sb.toString());
	}

//	private static List<String> _set(String key, int index, String value) {
//		getPrefs();
//		List<String> list = get(key, (List<String>) null);
//		if (list == null) {
//			list = new ArrayList<String>();
//			for (int i = 0; i < index; i++) list.add("");
//		}
//		if (index >= list.size()) for (int i = list.size(); i <= index; i++) list.add("");
//		list.set(index, value);
//		StringBuilder sb = new StringBuilder();
//		for (String tmp : list) {
//			if (sb.length() > 0) sb.append(";");
//			sb.append(tmp);
//		}
//		set(key, sb.toString());
//		return list;
//	}
	
	public static boolean addToSet(String key, String value) {
		Set<String> localSet = getStringSet(key, (Set<String>) null);
		if (localSet == null) {
			localSet = new HashSet<String>();
		} else if (localSet.contains(value)) return false;
		localSet.add(value);
		setStringSet(key, localSet);
		return true;
	}
	
	public static void deleteFromSet(String key, String value) {
		Set<String> set = getStringSet(key, (Set<String>) null);
		if (set == null) return;
		for (String string : set) {
			if (string.startsWith(value)) {
				set.remove(string);
				break;
			}
		}
		setStringSet(key, set);
	}
	
	public static String getStringFromSet(String key, int index) {
		Set<String> set = getStringSet(key, (Set<String>) null);
		if (set != null) {
			String[] array = set.toArray(new String[0]);
			if (index >= 0 && index < array.length) return array[index];
		}
		return null;
	}
	
	public static void set(String key, int index, String value) {
		getPrefs();
		List<String> list = get(key, (List<String>) null);
		if (list == null) {
			list = new ArrayList<String>();
			if (index > 0) for (int i = 0; i < index; i++) list.add("");
		}
		if (index < 0) {
			if (list.contains(value)) return;
			list.add(value);
		} else {
			if (index >= list.size()) for (int i = list.size(); i <= index; i++) list.add("");
			list.set(index, value);
		}
		StringBuilder sb = new StringBuilder();
		for (String tmp : list) {
			if (sb.length() > 0) sb.append(";");
			sb.append(tmp);
		}
		set(key, sb.toString());
	}
	
//	public static void set(String key, int index, String value) {
//		_set(key, index, value);
//	}

	public static void set(String key, long value){ 
		getPrefs();
		SharedPreferences.Editor editor = prefs.edit();
		editor.putLong(key, value);
		editor.commit();
	}

	public static boolean contains(String key){ 
		getPrefs();
		return prefs.contains(key);
	}

	public static void set(String key, String value){ 
		getPrefs();
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(key, value);
		editor.commit();
	}

	public static void set(String key, boolean value){ 
		getPrefs();
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(key, value);
		editor.commit();
	}

	public static void set(String key, int value){ 
		getPrefs();
		SharedPreferences.Editor editor = prefs.edit();
		editor.putInt(key, value);
		editor.commit();
	}

	public static void set(String key, double value){ 
		getPrefs();
		SharedPreferences.Editor editor = prefs.edit();
		editor.putFloat(key, (float) value);
		editor.commit();
	}

}
