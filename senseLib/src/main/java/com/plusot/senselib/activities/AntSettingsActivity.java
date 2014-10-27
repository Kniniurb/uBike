package com.plusot.senselib.activities;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;

import com.plusot.common.Globals;
import com.plusot.common.share.LLog;
import com.plusot.common.util.SleepAndWake;
import com.plusot.senselib.R;
import com.plusot.senselib.ant.AntDeviceManager;
import com.plusot.senselib.ant.AntProfile;
import com.plusot.senselib.settings.PreferenceKey;
import com.plusot.senselib.store.SRMLog;
import com.plusot.senselib.values.Manager;
import com.plusot.senselib.values.ManagerType;

public class AntSettingsActivity extends CallPreferencesActivity {
	private static final String CLASSTAG = AntSettingsActivity.class.getSimpleName();


	public AntSettingsActivity() {
		super(R.xml.ant_settings);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final LongCheckBoxPreference checkPref = (LongCheckBoxPreference) findPreference("usepairing_preference");
		checkPref.setChecked(PreferenceKey.USEPAIRING.isTrue());
		checkPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
				if (newValue instanceof Boolean) {
					LLog.d(Globals.TAG, CLASSTAG + ".onPreferenceChange for use pairing is: " + (Boolean) newValue);
					PreferenceKey.USEPAIRING.set((Boolean) newValue);
				}
				new SleepAndWake(new SleepAndWake.Listener() {
					@Override
					public void onWake() {
						AntDeviceManager antMgr = (AntDeviceManager) Manager.managers.get(ManagerType.ANT_MANAGER);
						if (antMgr != null) {
							if (PreferenceKey.USEPAIRING.isTrue()) {
								antMgr.retreivePairing();
							} else {
								antMgr.resetPairing();	
							}
						}
					}
				}, 200);

				return true;
			}
		});

		Preference myPref = (Preference) findPreference("calibrate_power");
		myPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				if (Globals.app == null) return false;
				AntDeviceManager antMgr = (AntDeviceManager) Manager.managers.get(ManagerType.ANT_MANAGER);
				if (antMgr != null) antMgr.calibratePower();
				return true;
			}
		});
		/*myPref = (Preference) findPreference("pair_ant");
		myPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				SenseApp app = Globals.app;
				if (app == null) return false;
				ValueViewManager mgr = ValueViewManager.getInstance();
				if (mgr != null) {
					mgr.resetPairing();
					ToastHelper.showToastLong(R.string.device_pairing_reset);
				}
				return true;
			}
		});*/
		setIntent("antinfo_settings",AntDeviceInfoActivity.class);
		if (SRMLog.getInstance() != null) {
			PreferenceScreen root = getPreferenceScreen();

			double offset = SRMLog.getInstance().getOffset();
			if (offset != 0) {
				PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(this);
				screen.setTitle(getString(R.string.settings_offset, offset));
				root.addPreference(screen);
			}
			double slope = SRMLog.getInstance().getSlope();
			if (slope != 0) {
				PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(this);
				screen.setTitle(getString(R.string.settings_slope, slope));
				root.addPreference(screen);
			}
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onResume() {
		LLog.d(Globals.TAG, CLASSTAG + ".onResume");
		AntDeviceManager mgr = (AntDeviceManager) Manager.managers.get(ManagerType.ANT_MANAGER);
		if (mgr != null) {
			Preference pref = findPreference("antinfo_settings");
			if (pref != null) pref.setEnabled(mgr.isEnabled());
			pref = findPreference("calibrate_power");
			if (pref != null) pref.setEnabled(mgr.isEnabled() && mgr.isActive(AntProfile.BIKEPOWER));
		}

		super.onResume();
	}

}
