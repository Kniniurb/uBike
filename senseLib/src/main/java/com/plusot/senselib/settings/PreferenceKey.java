package com.plusot.senselib.settings;

import android.annotation.TargetApi;
import android.os.Build;

import com.plusot.common.Globals;
import com.plusot.common.settings.PreferenceHelper;
import com.plusot.common.settings.PreferenceKeyInterface;
import com.plusot.common.share.LLog;
import com.plusot.javacommon.util.Format;
import com.plusot.javacommon.util.TimeUtil;
import com.plusot.senselib.R;
import com.plusot.senselib.SenseGlobals;
import com.plusot.senselib.values.Unit;
import com.plusot.senselib.values.ValueType;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public enum PreferenceKey implements PreferenceKeyInterface { 
	AUTOSWITCH("autoswitch_preference", 5000, Integer.class, PreferenceDefaults.FLAG_ISARRAY),
	ALTOFFSET("altoffset_preference", Integer.MIN_VALUE, Integer.class),
	ALTOFFSETCALIBRATED("altoffsetcalibrated_preference", 0, Long.class),
	BARO_ADJUST("gpsaltbaroadjust_preference", false, Boolean.class),
    POST_URL("urlpost_preference", "http://bikesenses.nl:9090/SensesOnline/data?", String.class),
    SHARE_URL("urlpost_preference", "http://bikesenses.nl/ub.php", String.class),
    BLE_DEVICE("ble_device", null, String.class, PreferenceDefaults.FLAG_ISSTRINGSET),
//	BLE_DEVICEADDRESS("ble_address", null, String.class, PreferenceDefaults.FLAG_ISSTRINGSET),
	CLICKSWITCH("click_preference", false, Boolean.class),
	CRANKFREQOFFSET("crankfreqoffset_preference", 0, Integer.class),
	DELAY("delay_preference", 10000, Integer.class),
	DEVICENUMBER("devicenumber_preference", 0, Integer.class, PreferenceDefaults.FLAG_ISPROFILE),
	DIM("maydim_preference", DimOption.DIM.toString(), String.class),
	DISPLAYNAME("displayname_preference", "Display name", String.class),
	IMEI("imei_preference","", String.class),
	FITFILELIST("fitfilelist_preference", "", String.class),
	GPSALTOFFSET("gpsaltoffset_preference", 0, Integer.class),
	GPSINTERVAL("gpsinterval_preference", 10000, Integer.class, PreferenceDefaults.FLAG_ISARRAY),
	GPSMINDISTANCE("gpsmindistance_preference", 2, Integer.class),
	GPSON("gpson_preference", true, Boolean.class),
	HASMAP("hasmap_preference", true, Boolean.class),
	HTTPPOST("http_preference", PreferenceDefaults.HTTPPOST_DEFAULT, Boolean.class),
	LASTLOCATION_LONG("lastlocation_preference_lng", 5.830994f, Double.class),
	LASTLOCATION_LAT("lastlocationlat_preference_lat", 51.917168f, Double.class),
	LASTTIMESENTHTTP("lasttimesenthttp_preference", 0, Long.class),
	MAX_HEARTRATE("maxheartrate_preference", 180, Integer.class),
	MIN_HEARTRATE("minheartrate_preference", 80, Integer.class),
	PASSWORD("password_preference", PreferenceDefaults.PASSWORD_DEFAULT, String.class),
	PROFILE("bikeprofile_preference", 1, Integer.class, PreferenceDefaults.FLAG_ISARRAY),
	PROFILENAME1("PROFILE_NAME1", Globals.appContext.getString(R.string.profile1), String.class),
	PROFILENAME2("PROFILE_NAME2", Globals.appContext.getString(R.string.profile2), String.class),
	PROFILENAME3("PROFILE_NAME3", Globals.appContext.getString(R.string.profile3), String.class),
	PROFILENAME4("PROFILE_NAME4", Globals.appContext.getString(R.string.profile4), String.class),
	PROFILENAME5("PROFILE_NAME5", Globals.appContext.getString(R.string.profile5), String.class),
	REPLAYINTERVAL("replayinterval_preference", 100,Integer.class, PreferenceDefaults.FLAG_ISARRAY),
	SHAREPRIVACY("shareprivacy_preference", false, Boolean.class),
	SRMLASTID("srmlastid_preference", 0, Integer.class),
	STOPSTATE("stopstate_preference", SenseGlobals.StopState.STOP.ordinal(), Integer.class),
	STORAGE("storage_preference", null, Set.class),
	TESTING("test_preference", Globals.TestLevel.NONE.getLabel(), String.class),
	TILESOURCE("tilesource_preference", 0, Integer.class, PreferenceDefaults.FLAG_ISARRAY),
	TPUSER("trainingpeaks_user", PreferenceDefaults.DEFAULTTPUSER, String.class),
	TPPASS("trainingpeaks_password", PreferenceDefaults.DEFAULTTPPASSWORD, String.class),
	UNIT("unit_preference_", "", String.class),
	USEPAIRING("usepairing_preference", false, Boolean.class, PreferenceDefaults.FLAG_ISPROFILE),
	VOICEON("voiceon_preference", false, Boolean.class),
	VOICEINTERVAL("voiceinterval_preference", 120000, Integer.class, PreferenceDefaults.FLAG_ISARRAY),
	VOICEONCHANGE("voicechange_preference", false, Boolean.class),
	VOICEVALUES("voicevalue_preference", new HashSet<String>(), Set.class),
	WHEELCIRC("wheel_preference", "2100", String.class, PreferenceDefaults.FLAG_ISPROFILE),
	WHEELSIZE("wheelsize_preference", 2100026, Integer.class, PreferenceDefaults.FLAG_ISARRAY | PreferenceDefaults.FLAG_ISPROFILE),
	WINDOWS("windows_preference", PreferenceDefaults.WINDOWS_DEFAULT, Integer.class, PreferenceDefaults.FLAG_ISARRAY),
	XTRAVALUES("xtravalues_preference", false, Boolean.class),
	//	RANDOM("RANDOM", -1, Integer.class),
	//	DAY("DAY", -1, Integer.class).
	//	public static final EnumSet<FileType> DEFAULT_FILETYPES = EnumSet.of(FileType.FIT, FileType.PWX, FileType.GCSV);
	;

	private static final String CLASSTAG = PreferenceKey.class.getSimpleName();
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
		switch (this){
		case VOICEVALUES: 
			if (defaultV == null || (defaultV instanceof Set<?> && ((Set<?>)defaultV).size() == 0))  {
				Set<String> set = new HashSet<String>();
				if (set.size() == 0) {
					set.add(ValueType.SPEED.getShortName());
					set.add(ValueType.HEARTRATE.getShortName());
				}
				defaultV = set;
			}
			break;
			default:
				break;
		}
		return defaultV;
	}

	public static int getProfile() {
		return PreferenceKey.PROFILE.getInt();
	}

	public static DimOption getDimmable() { 
		return DimOption.fromString(DIM.getString());
	}

	public static void setDimmable(DimOption value) { 
		DIM.set(value.toString());
	}

	public static Globals.TestLevel isTesting() { 
		String test  = TESTING.getString();
		Globals.testing = Globals.TestLevel.fromString(test);
		return Globals.testing;
	}

	public static void setProfileName(String value) {
		switch (getProfile()) {
		default:
		case 1: PreferenceKey.PROFILENAME1.set(value); break;
		case 2: PreferenceKey.PROFILENAME2.set(value); break;
		case 3: PreferenceKey.PROFILENAME3.set(value); break;
		case 4: PreferenceKey.PROFILENAME4.set(value); break;
		case 5: PreferenceKey.PROFILENAME5.set(value); break;
		}	
	}

	public static String getProfileName() {
		switch (getProfile()) {
		default:
		case 1: return PreferenceKey.PROFILENAME1.getString();
		case 2: return PreferenceKey.PROFILENAME2.getString();
		case 3: return PreferenceKey.PROFILENAME3.getString();
		case 4: return PreferenceKey.PROFILENAME4.getString();
		case 5: return PreferenceKey.PROFILENAME5.getString();
		}	
	}


	public static void setStopState(SenseGlobals.StopState buttonState) {
		SenseGlobals.stopState = buttonState;
		STOPSTATE.set(SenseGlobals.stopState.ordinal());
	}


	public static SenseGlobals.StopState getStopState() {
		SenseGlobals.stopState = SenseGlobals.StopState.fromInt(STOPSTATE.getInt());
		return SenseGlobals.stopState;
	}


	public static void setUnit(ValueType valueType, Unit unit){ 
		//LLog.d(Globals.TAG, CLASSTAG + ".setUnit: " + valueType.toString() + " = " + unit.toString());
		PreferenceHelper.set(UNIT.label + valueType.toString(), unit.toString());
	}

	public static Unit getUnit(ValueType valueType){ 
		Unit unit = Unit.fromString(PreferenceHelper.get(UNIT.label + valueType.toString(), (String)null));
		if (unit == null) {
			//LLog.d(Globals.TAG, CLASSTAG + ".getUnit: " + valueType.toString() + " = null");
			return valueType.defaultUnit();
		}
		//LLog.d(Globals.TAG, CLASSTAG + ".getUnit: " + valueType.toString() + " = " + unit.toString());
		return unit;
	}

	public static void addFitFileList(String value) {
		String str = FITFILELIST.getString();
		LLog.d(Globals.TAG, CLASSTAG + ".addFitFileList, add: " + value + " to " + str);
		if (str.contains(value)) return;
		if (str != null && str.length() > 0)
			str += "#" + value;
		else
			str = value;
		LLog.d(Globals.TAG, CLASSTAG + ".addFitFileList: " + str);
		FITFILELIST.set(str);
	}

	public static int getWheelCirc(){ 
		SenseGlobals.wheelCirc = 2100;
		try {
			SenseGlobals.wheelCirc = WHEELSIZE.getInt() / 1000;
			if (SenseGlobals.wheelCirc == 0) SenseGlobals.wheelCirc = Integer.valueOf(WHEELCIRC.getString());
		} catch (NumberFormatException e) {
		}
		LLog.d(Globals.TAG, CLASSTAG + ".getWheelCircumference = " + SenseGlobals.wheelCirc);
		return SenseGlobals.wheelCirc;
	}

	//	public static String getRawWheelSize(){ 
	//		return WHEELSIZE.getString();	
	//	}


	public static boolean isWheelCircCustom(){ 
		return WHEELSIZE.getInt() == 0;
	}

	public static int getDeviceNumber(int deviceType) {
		return PreferenceHelper.get(DEVICENUMBER.toString() + deviceType + "_" + getProfile(), 0);
	}

	public static void setDeviceNumber(int deviceType, int number) {
		PreferenceHelper.set(DEVICENUMBER.toString() + deviceType + "_" + getProfile(), number);
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static EnumSet<FileType> getStorageFiles() {
		if (Build.VERSION.SDK_INT < 11) return PreferenceDefaults.DEFAULT_FILETYPES;
		if (!PreferenceHelper.contains(STORAGE.label)) return PreferenceDefaults.DEFAULT_FILETYPES;
		//		LLog.d(Globals.TAG, CLASSTAG + " Prefs = " + StringUtil.toString(prefs.getAll()));
		EnumSet<FileType> eset = EnumSet.noneOf(FileType.class);
		Set<String> set = new HashSet<String>();
		for (FileType type : PreferenceDefaults.DEFAULT_FILETYPES) set.add(type.toString());
		set = PreferenceHelper.getStringSet(STORAGE.toString(), set);
		FileType type;
		for (String str: set) if ((type = FileType.fromString(str)) != null) {
			eset.add(type);
		}
		return eset;
	}

//	public static Set<String> getVoiceValueStrs() {
//		Set<String> set = VOICEVALUES.getStringSet();
//		if (set.size() == 0) {
//			set.add(ValueType.SPEED.getShortName());
//			set.add(ValueType.HEARTRATE.getShortName());
//		}
//		return set;
//	}

	//	public static Set<String> getVoiceValueStrs() {
	//		Set<String> set = PreferenceHelper.get(VOICEVALUES.toString(), VOICEVALUES.getDefaultSet());
	//		if (set.size() == 0) {
	//			set.add(ValueType.SPEED.getShortName());
	//			set.add(ValueType.HEARTRATE.getShortName());
	//		}
	//		return set;
	//	}

	public static Set<ValueType> getVoiceValues() {
		Set<String> voices = VOICEVALUES.getStringSet();
		return ValueType.getListSelected(voices);
	}

	public static void setVoiceValues(Set<ValueType> set) {
		Set<String> newSet = new HashSet<String>(); 
		for (ValueType type: set) {
			newSet.add(type.getShortName());
		}
		VOICEVALUES.set(newSet);
	}

	private static long gpsTimeRef = 0;


	public static void checkAltOffsets(String caller) {
		if (gpsTimeRef != SenseGlobals.lastLocationTime && SenseGlobals.lastLocationTime > 0) {
			long now = System.currentTimeMillis();
			if (	SenseGlobals.lastAltitudeTime > 0 && 
					now - ALTOFFSETCALIBRATED.getLong() > TimeUtil.DAY && 
					SenseGlobals.lastLocation != null && 
					now - SenseGlobals.lastLocationTime < 5000 && 
					SenseGlobals.lastLocation.hasAccuracy() && SenseGlobals.lastLocation.getAccuracy() < 50 &&
					Math.abs(GPSALTOFFSET.getInt() + SenseGlobals.lastLocation.getAltitude() - (SenseGlobals.lastAltitude + ALTOFFSET.getInt())) > 2 * SenseGlobals.lastLocation.getAccuracy()) {
				LLog.d(Globals.TAG, caller + ".checkAltOffsets: GPS Altitude = " + Format.format(SenseGlobals.lastLocation.getAltitude(), 1) + " m (" + GPSALTOFFSET.getInt() +
						"), accuracy = " + SenseGlobals.lastLocation.getAccuracy() +
						", diff GPS / Baro = " + (int)(GPSALTOFFSET.getInt() + SenseGlobals.lastLocation.getAltitude() - (SenseGlobals.lastAltitude + ALTOFFSET.getInt())));
				LLog.d(Globals.TAG, caller + ".checkAltOffsets: Altitude offset set to " + (int)(GPSALTOFFSET.getInt() + SenseGlobals.lastLocation.getAltitude() - SenseGlobals.lastAltitude));
				ALTOFFSET.set((int)(GPSALTOFFSET.getInt() + SenseGlobals.lastLocation.getAltitude() - SenseGlobals.lastAltitude));
			}
			//			if (SenseGlobals.lastLocation != null && now - SenseGlobals.lastLocationTime < 5000 && SenseGlobals.lastLocation.hasAccuracy() ) {
			//				LLog.d(Globals.TAG, caller + ".checkAltOffsets: GPS Altitude = " + Format.format(SenseGlobals.lastLocation.getAltitude(), 1) + " m (" + getGpsAltOffset() +
			//						"), accuracy = " + SenseGlobals.lastLocation.getAccuracy() +
			//						", diff GPS / Baro = " + (int)(getGpsAltOffset() + SenseGlobals.lastLocation.getAltitude() - (SenseGlobals.lastAltitude + getAltOffset())));
			//			}
		}
		gpsTimeRef = SenseGlobals.lastLocationTime;
	}

	/*
	 * Generic PreferenceKey methods
	 */

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

	@SuppressWarnings("unchecked")
	public Set<String> getStringSet() {
		if (getDefault() instanceof Set<?>) return PreferenceHelper.getStringSet(this.toString(), (Set<String>)getDefault());
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
		if ((flags & PreferenceDefaults.FLAG_ISPROFILE) > 0) {
			return this.toString() + getProfile();
		}
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