package com.plusot.senselib.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.view.View;
import android.view.View.OnLongClickListener;

import com.plusot.common.Globals;
import com.plusot.common.settings.PreferenceHelper;
import com.plusot.common.util.SleepAndWake;
import com.plusot.common.util.ToastHelper;
import com.plusot.senselib.R;
import com.plusot.senselib.settings.PreferenceKey;
import com.plusot.senselib.twitter.TwitterConst;
import com.plusot.senselib.util.SenseUserInfo;

public class ShareSettingsActivity extends CallPreferencesActivity {
	private Preference linkPref;
	private CheckBoxPreference sharePref;

	public ShareSettingsActivity() {
		super(R.xml.share_settings);
		//super(R.xml.share_settings_test);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		LongCheckBoxPreference checkPref = (LongCheckBoxPreference) findPreference(PreferenceKey.HTTPPOST.toString());
		if (checkPref != null) {
			checkPref.setOnLongClickListener(new OnLongClickListener() {

				@Override
				public boolean onLongClick(View v) {
					if (Globals.testing.isTest()) startActivity(new Intent(ShareSettingsActivity.this, UrlSettingsActivity.class));	
					return false;
				}});
		}

		checkPref = (LongCheckBoxPreference) findPreference(PreferenceKey.SHAREPRIVACY.toString());
		if (checkPref != null) {
			checkPref.setOnLongClickListener(new OnLongClickListener() {

				@Override
				public boolean onLongClick(View v) {
					if (findPreference(PreferenceKey.IMEI.toString()) != null) return false;
					EditTextPreference pref = new EditTextPreference(ShareSettingsActivity.this);
					pref.setKey(PreferenceKey.IMEI.toString());
					pref.setTitle(R.string.title_imei_preference);
					pref.setSummary(R.string.summary_imei_preference);
					pref.setText(PreferenceKey.IMEI.getString(String.valueOf(SenseUserInfo.getDeviceId() )));
					getPreferenceScreen().addPreference(pref);
					return true;
				}});
		}


		linkPref = (Preference) findPreference("http_link");
		sharePref = (CheckBoxPreference) findPreference(PreferenceKey.SHAREPRIVACY.toString());
		setTitle();

		linkPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				startActivity(new Intent(Intent.ACTION_VIEW, 
						Uri.parse(PreferenceKey.SHARE_URL.getString() + "?device=" + SenseUserInfo.getDeviceId())));
				return true;
			}
		});
		sharePref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				new SleepAndWake(new SleepAndWake.Listener() {
					@Override
					public void onWake() {
						setTitle();

					}
				}, 250);				
				return true;
			}
		});
		Preference myPref = (Preference) findPreference("twitter_reset");
		if (myPref != null) myPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				PreferenceHelper.set(TwitterConst.PREF_KEY_TOKEN, ""); 
				PreferenceHelper.set(TwitterConst.PREF_KEY_SECRET, ""); 
				ToastHelper.showToastLong(R.string.twitter_reset);
				return true;
			}
		});

	}

	private void setTitle() {
		linkPref.setTitle(PreferenceKey.SHARE_URL.getString());
		linkPref.setSummary(PreferenceKey.SHARE_URL.getString() + "?device=" + SenseUserInfo.getDeviceId());
	}
}
