package com.plusot.bluelib.settings;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.plusot.common.settings.PreferenceHelper;
import com.plusot.common.settings.PreferenceKeyInterface;


public enum PreferenceKey implements PreferenceKeyInterface { 
	BLE_DEVICE("ble_device", null, String.class, PreferenceDefaults.FLAG_ISSTRINGSET),
	;


//	private static final String CLASSTAG = PreferenceKey.class.getSimpleName();
	private final long flags;
	private final String label;
	private Object defaultV;
	private final Class<?> valueClass;

	private PreferenceKey(final String label, final Object defaultValue, final Class<?> valueClass) {
		this(label, defaultValue, valueClass, 0);
	}

	private PreferenceKey(final String label, final Object defaultValue, final Class<?> valueClass, final long flags) {
		this.label = label;
		this.defaultV = defaultValue;
		this.valueClass = valueClass;
		this.flags = flags;
	}

	private Object getDefault() {
		return defaultV;
	}

	
	public static PreferenceKey fromString(final String label) {
		for (PreferenceKey key : PreferenceKey.values()) {
			if (key.label.equals(label)) return key;
		}
		return null;
	}

	public int getDefaultIntValue() {
		if (getDefault() instanceof Integer) {
			return (Integer) getDefault();
		}
		return 0;
	}

	public boolean getDefaultBoolValue() {
		if (getDefault() instanceof Boolean) {
			return (Boolean) getDefault();
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	public Set<String> getDefaultSet() {
		if (getDefault() instanceof Set<?>) try {
			return (Set<String>) getDefault();
		} catch (Exception e) {
			return null;
		}
		return null;
	}

	public String getDefaultStringValue() {
		if (getDefault() instanceof String) {
			return (String) getDefault();
		} else if (getDefault() instanceof Integer) {
			return String.valueOf((Integer)getDefault());
		} else if (getDefault() instanceof Double) {
			return String.valueOf((Double)getDefault());
		} else if (getDefault() instanceof Float) {
			return String.valueOf((Float)getDefault());
		} else if (getDefault() instanceof Long) {
			return String.valueOf((Long)getDefault());
		} 
		return null;
	}

	public double getDefaultDoubleValue() {
		if (getDefault() != null && getDefault() instanceof Double) {
			return (Double) getDefault();
		}
		return 0;
	}

	public double getDefaultFloatValue() {
		if (getDefault() != null && getDefault() instanceof Float) {
			return (Float) getDefault();
		}
		return 0;
	}

	public EnumSet<?> getDefaultEnumSetValue() {
		if (getDefault() instanceof EnumSet<?>) {
			return (EnumSet<?>) getDefault();
		}
		return null;
	}

	public long getDefaultLongValue() {
		if (getDefault() != null && getDefault() instanceof Long) {
			return (Long) getDefault();
		}
		return 0;
	}

	public Class<?> getValueClass() {
		return valueClass;
	}

	@Override
	public String toString() {
		return label;
	}

	public boolean isTrue() {
		//LLog.d(Globals.TAG, CLASSTAG + ".getBoolean: This is " + this);
		return PreferenceHelper.get(getKey(), this.getDefaultBoolValue());
	}

	public void get() {
		if (valueClass.equals(Long.class)) {
			PreferenceHelper.get(getKey(), this.getDefaultLongValue());
		} else if (valueClass.equals(Integer.class)) {
			if ((flags & PreferenceDefaults.FLAG_ISARRAY) > 0)
				PreferenceHelper.get(getKey(), this.getDefaultStringValue());
			else
				PreferenceHelper.get(getKey(), this.getDefaultIntValue());
		} else if (valueClass.equals(Boolean.class)) {
			PreferenceHelper.get(getKey(), this.getDefaultBoolValue());
		} else if (valueClass.equals(String.class)) {
			if ((flags & PreferenceDefaults.FLAG_ISSTRINGSET) > 0)
				PreferenceHelper.getStringSet(getKey(), this.getDefaultSet());
			else
				PreferenceHelper.get(getKey(), this.getDefaultStringValue());
		} else if (valueClass.equals(Double.class)) {
			PreferenceHelper.get(getKey(), this.getDefaultDoubleValue());
		} else if (valueClass.equals(Float.class)) {
			PreferenceHelper.get(getKey(), this.getDefaultFloatValue());
		} else if (valueClass.equals(Set.class)) {
			PreferenceHelper.getStringSet(getKey(), this.getDefaultSet());
		} 	
	}

	@Override
	public String getString() {
		return PreferenceHelper.get(getKey(), this.getDefaultStringValue());
	}
	
	public String getString(int index) {
		return PreferenceHelper.get(getKey(), index, this.getDefaultStringValue());
	}

	public String getString(final String defaultValue) {
		return PreferenceHelper.get(getKey(), defaultValue);
	}

	public String[] getStrings() {
		String str = getString();
		return str.split(";");
	}

	public List<String> getStringList() {
		return Arrays.asList(getStrings());
	}

	public Set<String> getStringSet() {
		if (getDefault() instanceof Set<?>) return PreferenceHelper.getStringSet(this.toString(), getDefaultSet());
		return null;
	}
	
	public String getStringFromSet(int index) {
		if ((flags & PreferenceDefaults.FLAG_ISSTRINGSET) == 0) return null;
		return PreferenceHelper.getStringFromSet(this.toString(), index);
	}
	
	public boolean addStringToSet(String value) {
		if ((flags & PreferenceDefaults.FLAG_ISSTRINGSET) == 0) return false;
		return PreferenceHelper.addToSet(this.toString(), value);
	}
	
	public void deleteStringFromSet(String value) {
		if ((flags & PreferenceDefaults.FLAG_ISSTRINGSET) == 0) return;
		PreferenceHelper.deleteFromSet(this.toString(), value);
	}

	public long getLong() {
		//LLog.d(Globals.TAG, CLASSTAG + ".getLong: " + this);
		return PreferenceHelper.get(getKey(), this.getDefaultLongValue());
	}

	public double getDouble() {
		//LLog.d(Globals.TAG, CLASSTAG + ".getDouble: This is " + this);
		return PreferenceHelper.get(getKey(), this.getDefaultDoubleValue());
	}

	private String getKey() {
		return this.toString();
	}

	public int getInt() {
		if (!valueClass.equals(Integer.class)) return 0;
		//LLog.d(Globals.TAG, CLASSTAG + ".getInt: This is " + this);

		if ((flags & PreferenceDefaults.FLAG_ISARRAY) > 0) return PreferenceHelper.getFromListArray(getKey(), (Integer)getDefault());
		return PreferenceHelper.get(getKey(), this.getDefaultIntValue());
	}

	public void set(long value){ 
		PreferenceHelper.set(getKey(), value);
	}

	public void set(int value){ 
		PreferenceHelper.set(getKey(), value);
	}

	public void set(boolean value){ 
		PreferenceHelper.set(getKey(), value);
	}

	public void set(double value){ 
		PreferenceHelper.set(getKey(), value);
	}

	@Override
	public void set(String value){ 
		PreferenceHelper.set(getKey(), value);
	}

	public void set(Set<String> value){ 
		PreferenceHelper.setStringSet(getKey(), value);
	}
	
	public void setString(int index, String value) {
		PreferenceHelper.set(getKey(), index, value);
		
	}

	//
	//	public static void setFromListArray(PreferenceKey key, int value){ 
	//		PreferenceHelper.setFromListArray(key.toString(), value);
	//	}
	//
	//	public static void set(PreferenceKey key, long value){ 
	//		PreferenceHelper.set(key.toString(), value);
	//	}
	//
	//	public static void set(PreferenceKey key, String value){ 
	//		PreferenceHelper.set(key.toString(), value);
	//	}
	//
	//	public static void set(PreferenceKey key, boolean value){ 
	//		PreferenceHelper.set(key.toString(), value);
	//	}
	//
	//	public static void set(PreferenceKey key, int value){ 
	//		PreferenceHelper.set(key.toString(), value);
	//	}
	//
	//	public static String get(PreferenceKey key) {
	//		return PreferenceHelper.get(key.toString(), key.getDefaultStringValue());
	//	}
	//
	//	public static boolean getBoolean(PreferenceKey key) {
	//		return PreferenceHelper.getBoolean(key.toString(), key.getDefaultBoolValue());
	//	}



}