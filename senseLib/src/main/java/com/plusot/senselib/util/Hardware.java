package com.plusot.senselib.util;

import java.lang.reflect.Field;

import com.plusot.javacommon.util.TimeUtil;

public class Hardware {
	private static String model = null;
	
	private static String addBuildField(String string, android.os.Build build, String fieldName)  {
		Class<android.os.Build> buildClass = android.os.Build.class;
		Field field;
		try {
			field = buildClass.getField(fieldName);
			Object obj = field.get(new android.os.Build()); 
			if (obj != null) {
				if (obj instanceof Long) {
					TimeUtil.formatDate((Long) obj);
				}
				return string + "\n   " + fieldName + " = " + obj;
			}
		} catch (SecurityException e) {

		} catch (NoSuchFieldException e) {

		} catch (IllegalArgumentException e) {

		} catch (IllegalAccessException e) {

		} 
		return string;
	}
	
	private static String buildField(android.os.Build build, String fieldName)  {
		Class<android.os.Build> buildClass = android.os.Build.class;
		try {
			Field field = buildClass.getField(fieldName);
			Object obj = field.get(new android.os.Build()); 
			if (obj != null) {
				if (obj instanceof Long) {
					return TimeUtil.formatDate((Long) obj);
				}
				return (String)obj;
			}
		} catch (SecurityException e) {

		} catch (NoSuchFieldException e) {

		} catch (IllegalArgumentException e) {

		} catch (IllegalAccessException e) {

		} 
		return "";
	}
	
	public static String getModel()  { 
		if (model != null) return model;
		android.os.Build build = new android.os.Build();
		model = buildField(build, "MANUFACTURER"); 
		model += " " + buildField(build, "DEVICE"); 
		return model; 
	} 
	
	public static String getInfo()  { 
		android.os.Build build = new android.os.Build();
		String info = addBuildField("", build, "BOARD"); 
		info = addBuildField(info, build, "BOOTLOADER"); 
		info = addBuildField(info, build, "BRAND"); 
		info = addBuildField(info, build, "CPU_ABI"); 
		info = addBuildField(info, build, "CPU_ABI2"); 
		info = addBuildField(info, build, "DEVICE"); 
		info = addBuildField(info, build, "DISPLAY"); 
		info = addBuildField(info, build, "FINGERPRINT"); 
		info = addBuildField(info, build, "HARDWARE"); 
		info = addBuildField(info, build, "HOST"); 
		info = addBuildField(info, build, "ID"); 
		info = addBuildField(info, build, "MANUFACTURER"); 
		info = addBuildField(info, build, "MODEL"); 
		info = addBuildField(info, build, "PRODUCT"); 
		info = addBuildField(info, build, "RADIO"); 
		info = addBuildField(info, build, "SERIAL"); 
		info = addBuildField(info, build, "TAGS"); 
		info = addBuildField(info, build, "TIME"); 
		info = addBuildField(info, build, "TYPE"); 
		info = addBuildField(info, build, "USER"); 
		return info; 
	} 

}
