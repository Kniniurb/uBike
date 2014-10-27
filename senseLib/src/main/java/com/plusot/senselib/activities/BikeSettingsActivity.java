package com.plusot.senselib.activities;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;

import com.plusot.common.Globals;
import com.plusot.common.share.LLog;
import com.plusot.common.util.SleepAndWake;
import com.plusot.senselib.R;
import com.plusot.senselib.settings.PreferenceKey;

public class BikeSettingsActivity extends CallPreferencesActivity {
	private static String CLASSTAG = BikeSettingsActivity.class.getSimpleName();
	

	public BikeSettingsActivity() {
		super(R.xml.bike_settings);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		ListPreference listPref = (ListPreference) findPreference("bikeprofile_preference");
		//		listPrefBike.setOnPreferenceClickListener(new OnPreferenceClickListener() {
		//			@Override
		//			public boolean onPreferenceClick(Preference preference) {
		//				setListPreferenceData(listPrefBike);		
		//				return false;
		//			}
		//		});
		listPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference arg0, Object newValue) {
				if (!(newValue instanceof String)) return false;
				new SleepAndWake(wakeListener, 50);
				return true;
			}

		});
		EditTextPreference editPref = (EditTextPreference) findPreference("bikeprofilename_preference");
		//		editPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
		//			@Override
		//			public boolean onPreferenceClick(Preference preference) {
		//				editPref.setText(prefs.getProfileName());
		//				return false;
		//			}
		//		});
		editPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference arg0, Object newValue) {
				if (!(newValue instanceof String)) return true;
				LLog.d(Globals.TAG, CLASSTAG + ".editPref.onPreferenceChange: " + newValue);
				PreferenceKey.setProfileName((String) newValue);
				new SleepAndWake(wakeListener, 50);
				return true;
			}
		});

		listPref = (ListPreference) findPreference("wheelsize_preference");
		listPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
				if (!(newValue instanceof String)) return true;
				LLog.d(Globals.TAG, CLASSTAG + ".onPreferenceChange: " + newValue);
				enableWheelInMM(((String)newValue).equals("0"));
				PreferenceKey.WHEELSIZE.set((String)newValue);
				new SleepAndWake(wakeListener, 50);
				return true;
			}

		});
		editPref = (EditTextPreference) findPreference("wheel_preference");
		editPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference arg0, Object newValue) {
				if (!(newValue instanceof String)) return true;
				LLog.d(Globals.TAG, CLASSTAG + ".editPref.onPreferenceChange: " + newValue);
				PreferenceKey.WHEELCIRC.set((String) newValue);				
				new SleepAndWake(wakeListener, 50);
				return false;
			}

		});	
		update();
	}

	private SleepAndWake.Listener wakeListener = new SleepAndWake.Listener() {

		@Override
		public void onWake() {
			update();

		}

	};

	@SuppressWarnings("deprecation")
	private void update() {
		EditTextPreference editPref = (EditTextPreference) findPreference("bikeprofilename_preference");
		editPref.setText(PreferenceKey.getProfileName());
		updatePrefSummary(editPref);

		ListPreference listPref = (ListPreference) findPreference("bikeprofile_preference");
		setListPreferenceData(listPref);
		updatePrefSummary(listPref);

		listPref = (ListPreference) findPreference("wheelsize_preference");
		listPref.setValue(PreferenceKey.WHEELSIZE.getString());
		updatePrefSummary(listPref);

		editPref = (EditTextPreference) findPreference("wheel_preference");
		editPref.setText("" + PreferenceKey.getWheelCirc());
		updatePrefSummary(editPref);
	}

	protected void setListPreferenceData(ListPreference lp) {
		CharSequence[] entries = { 
				PreferenceKey.PROFILENAME1.getString(),
				PreferenceKey.PROFILENAME2.getString(),
				PreferenceKey.PROFILENAME3.getString(),
				PreferenceKey.PROFILENAME4.getString(),
				PreferenceKey.PROFILENAME5.getString()};
		CharSequence[] entryValues = {"1", "2", "3", "4", "5"};
		lp.setEntries(entries);
		lp.setDefaultValue("1");
		lp.setEntryValues(entryValues);
	}

	@Override
	protected void onResume() {
		LLog.d(Globals.TAG, CLASSTAG + ".onResume");
		enableWheelInMM();
		super.onResume();
	}

	private void enableWheelInMM() { }

	@SuppressWarnings("deprecation")
	private void enableWheelInMM(boolean enabled) {
		EditTextPreference pref = (EditTextPreference) findPreference(PreferenceKey.WHEELCIRC.toString());
		if (pref != null) {
			pref.setEnabled(enabled);
			//pref.setText
		}

	}
}
