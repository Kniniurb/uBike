/*******************************************************************************
 * Copyright (c) 2012 Plusot Biketech
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Peter Bruinink - initial API and implementation and/or initial documentation
 * 
 *******************************************************************************/
package com.plusot.senselib.activities;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.plusot.common.Globals;
import com.plusot.common.share.LLog;
import com.plusot.senselib.settings.AltitudeDialogPreference;
import com.plusot.senselib.settings.MaxHeartrateDialogPreference;

public abstract class CallPreferencesActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	private static final String CLASSTAG = CallPreferencesActivity.class.getSimpleName();
	private final int preferenceXML;

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	public CallPreferencesActivity(final int preferenceXML) {
		this.preferenceXML = preferenceXML;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(preferenceXML);

		for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++){
			initSummary(getPreferenceScreen().getPreference(i));
		}
		PreferenceManager.setDefaultValues(this, preferenceXML, false);

		ListView listView = getListView();
		listView.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				ListView listView = (ListView) parent;
				ListAdapter listAdapter = listView.getAdapter();
				Object obj = listAdapter.getItem(position);
				if (obj != null && obj instanceof View.OnLongClickListener) {
					View.OnLongClickListener longListener = (View.OnLongClickListener) obj;
					return longListener.onLongClick(view);
				}
				return false;
			}
		});

	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onResume() {
		super.onResume();

		// Setup the initial values
		//mCheckBoxPreference.setSummary(sharedPreferences.getBoolean(key, false) ? "Disable this setting" : "Enable this setting");
		//mListPreference.setSummary("Current value is " + sharedPreferences.getValue(key, "")); 

		// Set up a listener whenever a key changes            
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onPause() {
		super.onPause();
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);    
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		updatePrefSummary(findPreference(key));
	}

	private void initSummary(Preference pref){
		if (pref instanceof PreferenceCategory){
			PreferenceCategory pCat = (PreferenceCategory)pref;
			for (int i=0; i < pCat.getPreferenceCount(); i++){
				initSummary(pCat.getPreference(i));
			}
		}else{
			updatePrefSummary(pref);
		}
	}

	protected void updatePrefSummary(Preference pref){
		//LLog.d(Globals.TAG, CLASSTAG + ".updatePrefSummary");

		if (pref instanceof ListPreference) {
			ListPreference listPref = (ListPreference) pref;
			//LLog.d(Globals.TAG, CLASSTAG + ".updatePrefSummary: " + pref.getSummary());
			//LLog.d(Globals.TAG, CLASSTAG + ".updatePrefSummary: " + listPref.getEntry());
			if (listPref.getEntry() != null) {
				String sum = pref.getSummary().toString();
				String edit = "" + listPref.getEntry();
				if (sum.matches(".*\\(.*\\).*")) 
					sum = sum.replaceAll("\\(.*\\)", '(' + edit + ')'); 
				else if (sum.matches(".*['].*['].*")) 
					sum = sum.replaceAll("['].*[']", "'" + edit + "'"); 
				else if (sum.matches(".*\\[.*\\].*")) 
					sum = sum.replaceAll("\\[.*\\]", "'" + edit + "'"); 
				pref.setSummary(sum);
			}
		} else if (pref instanceof EditTextPreference) {
			EditTextPreference editTextPref = (EditTextPreference) pref; 
			if (editTextPref.getText() != null) {
				String sum = pref.getSummary().toString();
				String edit = editTextPref.getText();
				if (sum.matches(".*\\(.*\\).*")) 
					sum = sum.replaceAll("\\(.*\\)", '(' + edit + ')'); 
				else if (sum.matches(".*['].*['].*")) 
					sum = sum.replaceAll("['].*[']", "'" + edit + "'"); 
				else if (sum.matches(".*\\[.*\\].*")) 
					sum = sum.replaceAll("\\[.*\\]", "'" + edit + "'"); 
				pref.setSummary(sum);
			}
		} else if (pref instanceof AltitudeDialogPreference) {
			SharedPreferences sharedPreferences = pref.getSharedPreferences();
			String edit = "" + sharedPreferences.getInt(pref.getKey(), 0);
				
			String sum = pref.getSummary().toString();
			if (sum.matches(".*\\(.*\\).*")) 
				sum = sum.replaceAll("\\(.*\\)", '(' + edit + ')'); 
			else if (sum.matches(".*['].*['].*")) 
				sum = sum.replaceAll("['].*[']", "'" + edit + "'"); 
			else if (sum.matches(".*\\[.*\\].*")) 
				sum = sum.replaceAll("\\[.*\\]", "'" + edit + "'"); 
			pref.setSummary(sum);
		} else if (pref instanceof MaxHeartrateDialogPreference) {
			SharedPreferences sharedPreferences = pref.getSharedPreferences();
			String edit = "" + sharedPreferences.getInt(pref.getKey(), 0);
				
			String sum = pref.getSummary().toString();
			if (sum.matches(".*\\(.*\\).*")) 
				sum = sum.replaceAll("\\(.*\\)", '(' + edit + ')'); 
			else if (sum.matches(".*['].*['].*")) 
				sum = sum.replaceAll("['].*[']", "'" + edit + "'"); 
			else if (sum.matches(".*\\[.*\\].*")) 
				sum = sum.replaceAll("\\[.*\\]", "'" + edit + "'"); 
			pref.setSummary(sum);
		}
	}

	@SuppressWarnings("deprecation")
	protected void setIntent(String key, final Class<?> activityClass) {

		Preference myPref = (Preference) findPreference(key);

		if (myPref == null) {
			LLog.e(Globals.TAG, CLASSTAG + ".setIntent: Could not find key: " + key);
		} else myPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				startActivity(new Intent(Globals.appContext, activityClass));
				return true;
			}

		});
	}

	@SuppressWarnings("deprecation")
	protected void removePreferenceScreen(String owner, String key) {
		PreferenceScreen myPref = (PreferenceScreen) findPreference(owner);

		if (myPref == null) {
			LLog.e(Globals.TAG, CLASSTAG + ".removePreferenceScreen: Could not find key: " + owner);
		} else {
			PreferenceScreen sub = (PreferenceScreen) findPreference(key);
			if (sub != null) myPref.removePreference(sub);
		}
	}

	@SuppressWarnings("deprecation")
	protected void disablePreferenceScreen(String owner, String key) {
		PreferenceScreen myPref = (PreferenceScreen) findPreference(owner);

		if (myPref == null) {
			LLog.e(Globals.TAG, CLASSTAG + ".disablePreferenceScreen: Could not find key: " + owner);
		} else {
			PreferenceScreen sub = (PreferenceScreen) findPreference(key);
			sub.setEnabled(false);
		}
	}
}
