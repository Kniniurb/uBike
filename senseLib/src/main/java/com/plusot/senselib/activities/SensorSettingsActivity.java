package com.plusot.senselib.activities;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;

import com.plusot.common.Globals;
import com.plusot.common.util.ToastHelper;
import com.plusot.senselib.R;
import com.plusot.senselib.sensors.AndroidSensorManager;
import com.plusot.senselib.values.Manager;
import com.plusot.senselib.values.ManagerType;

public class SensorSettingsActivity extends CallPreferencesActivity {

	
	public SensorSettingsActivity() {
		super(R.xml.sensor_settings);
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Preference myPref = (Preference) findPreference("set_level");
		myPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				if (Globals.app == null) return false;
				if (Manager.managers.get(ManagerType.SENSOR_MANAGER) == null) return false;
				((AndroidSensorManager)Manager.managers.get(ManagerType.SENSOR_MANAGER)).setLevel();
				ToastHelper.showToastLong(R.string.level_message);
				return true;
			}
		});
	}
}
