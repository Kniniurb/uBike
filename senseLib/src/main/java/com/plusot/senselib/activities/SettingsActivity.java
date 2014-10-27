package com.plusot.senselib.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;

import com.plusot.bluelib.UBlueMain;
import com.plusot.common.Globals;
import com.plusot.common.share.LLog;
import com.plusot.senselib.R;
import com.plusot.senselib.dialogs.HintDialog;
import com.plusot.senselib.dialogs.SplashDialog;
import com.plusot.senselib.values.ManagerType;

public class SettingsActivity extends CallPreferencesActivity {
	private static final String CLASSTAG = SettingsActivity.class.getSimpleName();
	
	public SettingsActivity() {
		super(R.xml.settings);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (ManagerType.supportedManagers.contains(ManagerType.ANT_MANAGER)) {
			setIntent("ant_settings", AntSettingsActivity.class);
			setIntent("bike_settings", BikeSettingsActivity.class);
		}	else {
			removePreferenceScreen("settings_screen", "ant_settings");
			removePreferenceScreen("settings_screen", "bike_settings");
		}

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2)
			removePreferenceScreen("settings_screen", "ble_settings");
		else
			setUBlueIntent();
		setIntent("gps_settings", GpsSettingsActivity.class);
		setIntent("display_settings", DisplaySettingsActivity.class);
		setIntent("speech_settings", SpeechSettingsActivity.class);
		setIntent("share_settings", ShareSettingsActivity.class);
		setIntent("replay_settings", HistorySettingsActivity.class);
		Preference myPref = (Preference) findPreference("about_settings");
		if (myPref != null) myPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				new SplashDialog(SettingsActivity.this, true).show();
				//new AboutDialog(SettingsActivity.this, null).show();
				return true;
			}
		});
		myPref = (Preference) findPreference("hint_settings");
		if (myPref != null) myPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {

				new HintDialog(SettingsActivity.this, null).show();
				return true;
			}
		});
	}
	
	@SuppressWarnings("deprecation")
	private void  setUBlueIntent() {
		final String key ="ble_settings";

		Preference myPref = (Preference) findPreference(key);

		if (myPref == null) {
			LLog.e(Globals.TAG, CLASSTAG + ".setUBlueIntent: Could not find key: " + key);
		} else myPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Intent intent = new Intent(Globals.appContext,  UBlueMain.class);
				intent.putExtra(UBlueMain.EXTRA_USECHAT, false);
				intent.putExtra(UBlueMain.EXTRA_CALLER, "SenseLib");
				startActivity(intent);
				return true;
			}

		});
	}
}
