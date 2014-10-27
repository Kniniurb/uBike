package com.plusot.senselib.activities;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.text.InputType;

import com.plusot.senselib.R;

public class BikeSettingsCustomActivity extends CallPreferencesActivity {

	public BikeSettingsCustomActivity() {
		super(R.xml.bike_settingscustom);
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		EditTextPreference prefEditText = (EditTextPreference) findPreference("wheel_preference");
		prefEditText.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);

	}
}
