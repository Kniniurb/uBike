package com.plusot.senselib.settings;

import java.util.EnumSet;

public class PreferenceDefaults {
	public static final String DEFAULTTPUSER = "_user_";
	public static final String DEFAULTTPPASSWORD = "pass";
	public static final int WINDOWS_DEFAULT = 4;
	public static final boolean HTTPPOST_DEFAULT = true;
	public static final String PASSWORD_DEFAULT = "password";
	public static final EnumSet<FileType> DEFAULT_FILETYPES = EnumSet.of(FileType.FIT, FileType.PWX, FileType.GCSV);
	public static final long FLAG_ISARRAY = 1;
	public static final long FLAG_ISPROFILE = 2;
	public static final long FLAG_ISSTRINGSET = 4;
	
	
}
