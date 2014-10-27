package com.plusot.senselib.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;

import com.plusot.senselib.R;
import com.plusot.senselib.dialogs.SplashDialog;
import com.plusot.senselib.settings.PreferenceKey;
import com.plusot.senselib.values.Value;
import com.plusot.senselib.widget.MyMapView;

public class DisplaySettingsActivity extends CallPreferencesActivity {

	public DisplaySettingsActivity() {
		super(R.xml.display_settings);
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Preference myPref = (Preference) findPreference("resettotals_preference");
		myPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				warnReset();
				return true;
			}
		});
		
		ListPreference listPref = (ListPreference) findPreference(PreferenceKey.TILESOURCE.toString());
		setListPreferenceData(listPref);
		updatePrefSummary(listPref);

		
		//Settings.System.putInt(getContentResolver(),Settings.System.SCREEN_OFF_TIMEOUT, DELAY);
	}
	
	private void warnReset() {
		new AlertDialog.Builder(this)
		.setTitle(R.string.warn_reset_title)  
		.setMessage(R.string.warn_reset_msg)
		.setPositiveButton(R.string.button_yes, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton)  {												
				if (dialog != null) dialog.dismiss();
				Value.resetTotals();
				new SplashDialog(DisplaySettingsActivity.this, true).show();
			}
		})
		.setNegativeButton(R.string.button_no, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				if (dialog != null) dialog.dismiss();
			}
		})
		.create().show();  
	}
	
	protected void setListPreferenceData(ListPreference lp) {
		lp.setEntries(MyMapView.TILE_SOURCE_NAMES);
		lp.setDefaultValue("1");
		lp.setEntryValues(MyMapView.TILE_SOURCE_VALUES);
	}

}
