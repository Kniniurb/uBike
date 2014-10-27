package com.plusot.senselib.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.View;
import android.widget.TableRow;
import android.widget.TextView;

import com.plusot.common.Globals;
import com.plusot.javacommon.util.TimeUtil;
import com.plusot.senselib.R;
import com.plusot.senselib.SenseGlobals;
import com.plusot.senselib.http.HttpSender;
import com.plusot.senselib.settings.PreferenceKey;
import com.plusot.senselib.values.Unit;
import com.plusot.senselib.values.Value;
import com.plusot.senselib.values.ValueType;

public class SplashDialog extends Dialog {
	private boolean cancelable = true;

	public SplashDialog(Context context, boolean cancelable) {
		super(context, R.style.SplashScreen);
		this.cancelable = cancelable;
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		//requestWindowFeature(Window.FEATURE_NO_TITLE); 

		setContentView(SenseGlobals.splashLayout); //R.layout.splash);
		setCancelable(cancelable);

		if (SenseGlobals.isBikeApp) {
			Value.saveTotals();
			TextView tv = (TextView) findViewById(SenseGlobals.splash_release_id);
			PackageInfo info;
			try {
				info = getContext().getPackageManager().getPackageInfo(getContext().getPackageName(), 0);
				if (tv != null) {
					if (Globals.testing.isTest())
						tv.setText(Globals.TAG + " " + info.versionName + " TEST");
					else
						tv.setText(Globals.TAG + " " + info.versionName);
				}
			} catch (NameNotFoundException e) {
				if (tv != null) tv.setVisibility(View.GONE);
			}
			Value value = null;
			if ((value = Value.getValue(ValueType.TIME, false))!= null) {
				if ((tv = (TextView) findViewById(SenseGlobals.splash_rides_id)) != null)
					tv.setText("" + Value.getRides());
				if ((tv= (TextView) findViewById(SenseGlobals.splash_time_id)) != null)
					tv.setText(value.printValue((Long)Value.getTotalTime(), false));
			}
			if ((value = Value.getValue(ValueType.DISTANCE, false))!= null) {
				if ((tv = (TextView) findViewById(SenseGlobals.splash_distance_id)) != null)
					tv.setText(value.printValue((Double)Value.getTotalDistance(), false));
				if ((tv = (TextView) findViewById(SenseGlobals.splash_distance_unit_id)) != null)
					tv.setText(value.getUnitLabel(getContext()));
			}
			if ((value = Value.getValue(ValueType.POWER, false))!= null) {
				if ((tv = (TextView) findViewById(SenseGlobals.splash_energy_id)) != null)
					tv.setText(Unit.KILOJOULE.getFormated((Double)Value.getTotalEnergy(),0, false));
				if ((tv = (TextView) findViewById(SenseGlobals.splash_energy_unit_id)) != null)
					tv.setText(Unit.KILOJOULE.getLabel(getContext()));
			}
			if ((value = Value.getValue(ValueType.ALTITUDE, false))!= null) {
				if ((tv = (TextView) findViewById(SenseGlobals.splash_ascent_id)) != null)
					tv.setText(value.printValue((Double)Value.getTotalAscent(), false));
				if ((tv = (TextView) findViewById(SenseGlobals.splash_ascent_unit_id)) != null)
					tv.setText(value.getUnitLabel(getContext()));
				if ((tv = (TextView) findViewById(SenseGlobals.splash_altitude_id)) != null)
					tv.setText(value.printValue((Double)Value.getHighestPeak(), false));
				if ((tv = (TextView) findViewById(SenseGlobals.splash_altitude_unit_id)) != null)
					tv.setText(value.getUnitLabel(getContext()));
			}
			if (HttpSender.getLastTimeSent() > 0 && PreferenceKey.HTTPPOST.isTrue()) {
				if ((tv = (TextView) findViewById(SenseGlobals.splash_lasttimesent_id)) != null)
					tv.setText(TimeUtil.formatTimeShort(HttpSender.getLastTimeSent()));
				TableRow row = (TableRow) findViewById(SenseGlobals.splashrow_lasttimesent_id);
				if (row != null) {
					row.setVisibility(View.VISIBLE);
				}

			}
		}

	}	
}
