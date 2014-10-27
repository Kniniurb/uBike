package com.plusot.senselib.activities;

import android.os.Bundle;

import com.plusot.common.Globals;
import com.plusot.common.share.LLog;
import com.plusot.senselib.R;
import com.plusot.senselib.SenseGlobals;
import com.plusot.senselib.settings.AltitudeDialogPreference;
import com.plusot.senselib.settings.AltitudeDialogPreference.OnCloseListener;
import com.plusot.senselib.settings.PreferenceKey;

public class GpsSettingsActivity extends CallPreferencesActivity {
	private static final String CLASSTAG = GpsSettingsActivity.class.getSimpleName();


	public GpsSettingsActivity() {
		super(R.xml.gps_settings);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);


		//Settings.System.putInt(getContentResolver(),Settings.System.SCREEN_OFF_TIMEOUT, DELAY);

		AltitudeDialogPreference pref = (AltitudeDialogPreference) findPreference(PreferenceKey.GPSALTOFFSET.toString());
		if (pref == null) return;
		if (SenseGlobals.lastLocation != null) pref.setRef((int)SenseGlobals.lastLocation.getAltitude());
		pref.setOnCloseListener(new OnCloseListener(){

			@Override
			public void onPositiveResult(int ref, int level) {
				PreferenceKey.ALTOFFSET.set((int)((ref + level) - SenseGlobals.lastAltitude));
				PreferenceKey.ALTOFFSETCALIBRATED.set(System.currentTimeMillis());
				LLog.d(Globals.TAG, CLASSTAG + ".onPositiveResult: Alt offset = " + ((int)((ref + level) - SenseGlobals.lastAltitude)) + " (" + (int)SenseGlobals.lastAltitude + ")");
			}
		});
	}
}
