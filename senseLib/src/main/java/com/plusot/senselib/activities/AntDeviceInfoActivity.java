package com.plusot.senselib.activities;

import java.util.HashMap;
import java.util.Map;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

import com.plusot.common.Globals;
import com.plusot.common.share.LLog;
import com.plusot.senselib.R;
import com.plusot.senselib.ant.AntDevice;
import com.plusot.senselib.bluetooth.BTDevice;
import com.plusot.senselib.dialogs.DeviceInfoDialog;
import com.plusot.senselib.values.Device;

public class AntDeviceInfoActivity extends PreferenceActivity {
	private static final String CLASSTAG = AntDeviceInfoActivity.class.getSimpleName();

	private final Map<String, Device> deviceMap = new HashMap<String, Device>();

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.ant_deviceinfo);

		//fetch the item where you wish to insert the CheckBoxPreference, in this case a PreferenceCategory with key "targetCategory"
		//PreferenceCategory targetCategory = (PreferenceCategory)findPreference("ant_device_info");
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onResume() {
		PreferenceScreen root = this.getPreferenceScreen();

		int i = 0;
		final Device devices[] = Device.getDevices();
		deviceMap.clear();
		for (Device device : devices) {
			LLog.d(Globals.TAG, CLASSTAG + " Device = " + device + " " + device.isActive());
			if (device.isActive() && (device instanceof AntDevice || device instanceof BTDevice)) {
				deviceMap.put(device.getName() + "-" + device.getNumberOrAddress(), device);
				if (root.findPreference(device.toString()) == null) {
					PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(this);
					screen.setTitle(device.getName());
					screen.setKey(device.toString());
					screen.setSummary(device.getNumberOrAddress());
					screen.setOnPreferenceClickListener(new OnPreferenceClickListener() {
						@Override
						public boolean onPreferenceClick(Preference preference) {
							Device device = deviceMap.get(preference.getTitle() + "-" + preference.getSummary());
							if (device != null) new DeviceInfoDialog(AntDeviceInfoActivity.this, device).show();
							return false;
						}
					});
					root.addPreference(screen);
				}
				i++;
			}
		}

		if (i == 0) {
			PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(this);
			screen.setTitle(R.string.no_device_info_yet);
			root.addPreference(screen);
		}
		super.onResume();
	}

}
