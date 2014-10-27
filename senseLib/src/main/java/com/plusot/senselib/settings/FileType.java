package com.plusot.senselib.settings;


public enum FileType{
	CSV,
	GCSV,
	PWX,
	GPX,
	FIT,
	SRM,
	SCR;

	public static FileType fromString(String str) {
		for (FileType type: FileType.values()) if (type.toString().equalsIgnoreCase(str)) return type;
		return null;
	}
}
