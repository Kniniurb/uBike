package com.plusot.senselib.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.plusot.common.Globals;
import com.plusot.common.share.LLog;
import com.plusot.common.util.ElapsedTime;
import com.plusot.common.util.Watchdog;
import com.plusot.senselib.R;
import com.plusot.senselib.util.Util;
import com.plusot.senselib.values.Laps;
import com.plusot.senselib.values.Value;
import com.plusot.senselib.values.ValueType;

public class LapsDialog extends Dialog implements Watchdog.Watchable {
	private static final String CLASSTAG = LapsDialog.class.getSimpleName();
	private int lap;
	private final boolean autoClose;
	private Button nextButton;
	private Button prevButton;
	private long refTime;


	public LapsDialog(Context context, int lap, boolean autoClose) {
		//super(context, android.R.style.Theme_Translucent_NoTitleBar);
		super(context, R.style.Theme_TranslucentDialog);
		this.lap = Math.max(lap, 0);
		this.autoClose = autoClose;
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.laps);
		refTime = System.currentTimeMillis();

		nextButton = (Button) this.findViewById(R.id.laps_nextbutton);
		prevButton = (Button) this.findViewById(R.id.laps_previousbutton);
		nextButton.setOnClickListener(new android.view.View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (lap < Laps.getLaps() - 1) {
					lap++;
					setInfo();
					refTime = System.currentTimeMillis();
				}
			}
		});
		prevButton.setOnClickListener(new android.view.View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (lap > 0) {
					lap--;
					setInfo();
					refTime = System.currentTimeMillis();	
				}	
			}
		});
		setInfo();
		
		Watchdog.getInstance().add(this, 1000);

		LLog.d(Globals.TAG, CLASSTAG + ".onCreate");
	}

	private void setInfo() {
		if (lap < Laps.getLaps() - 1) {
			nextButton.setEnabled(true);
			nextButton.setVisibility(View.VISIBLE);		
		} else {
			nextButton.setEnabled(false);
			nextButton.setVisibility(View.INVISIBLE);	
		}
		if (lap > 0) {
			prevButton.setEnabled(true);
			prevButton.setVisibility(View.VISIBLE);	

		} else {
			prevButton.setEnabled(false);
			prevButton.setVisibility(View.INVISIBLE);	
		}
		Context context = getContext();

		String title = context.getString(R.string.laps_title, lap + 1);
		setTitle(title);
		Util.setText(this, R.id.laps_title, title);
		Value value;
		Double val;
		if ((value = Value.getValue(ValueType.POWER, false)) != null && Laps.has(lap, ValueType.POWER)) {
			findViewById(R.id.laps_row_power).setVisibility(View.VISIBLE);
			if ((val = Laps.getAggregate(lap, ValueType.POWER, Laps.ValueAggregate.MAX)) != null)
				Util.setText(this, R.id.laps_max_power, value.printValue(val, false));
			if ((val = Laps.getAggregate(lap, ValueType.POWER, Laps.ValueAggregate.AVG)) != null)
				Util.setText(this, R.id.laps_avg_power, value.printValue(val, false));
			Util.setText(this, R.id.laps_unit_power, value.getUnitLabel(context));
		} else {
			findViewById(R.id.laps_row_power).setVisibility(View.GONE);
		}
		if ((value = Value.getValue(ValueType.CADENCE, false)) != null && Laps.has(lap, ValueType.CADENCE)) {
			findViewById(R.id.laps_row_cadence).setVisibility(View.VISIBLE);
			if ((val = Laps.getAggregate(lap, ValueType.CADENCE, Laps.ValueAggregate.MAX)) != null)
				Util.setText(this, R.id.laps_max_cadence, value.printValue(val, false));
			if ((val = Laps.getAggregate(lap, ValueType.CADENCE, Laps.ValueAggregate.AVG)) != null)
				Util.setText(this, R.id.laps_avg_cadence, value.printValue(val, false));
			Util.setText(this, R.id.laps_unit_cadence, value.getUnitLabel(context));
		} else {
			findViewById(R.id.laps_row_cadence).setVisibility(View.GONE);
		}
		if ((value = Value.getValue(ValueType.HEARTRATE, false)) != null && Laps.has(lap, ValueType.HEARTRATE)) {
			findViewById(R.id.laps_row_heartrate).setVisibility(View.VISIBLE);
			if ((val = Laps.getAggregate(lap, ValueType.HEARTRATE, Laps.ValueAggregate.MAX)) != null)
				Util.setText(this, R.id.laps_max_heartrate, value.printValue(val, false));
			if ((val = Laps.getAggregate(lap, ValueType.HEARTRATE, Laps.ValueAggregate.AVG)) != null)
				Util.setText(this, R.id.laps_avg_heartrate, value.printValue(val, false));
			Util.setText(this, R.id.laps_unit_heartrate, value.getUnitLabel(context));
		} else {
			findViewById(R.id.laps_row_heartrate).setVisibility(View.GONE);
		}
		if ((value = Value.getValue(ValueType.SPEED, false)) != null && Laps.has(lap, ValueType.SPEED)) {
			findViewById(R.id.laps_row_speed).setVisibility(View.VISIBLE);
			if ((val = Laps.getAggregate(lap, ValueType.SPEED, Laps.ValueAggregate.MAX)) != null)
				Util.setText(this, R.id.laps_max_speed, value.printValue(val, false));
			if ((val = Laps.getAggregate(lap, ValueType.SPEED, Laps.ValueAggregate.AVG)) != null)
				Util.setText(this, R.id.laps_avg_speed, value.printValue(val, false));
			Util.setText(this, R.id.laps_unit_speed, value.getUnitLabel(context));
		} else {
			findViewById(R.id.laps_row_speed).setVisibility(View.GONE);
		}

		Util.setText(this, R.id.laps_duration, ElapsedTime.format(Laps.getDuration(lap), true, false));
	}

	

	@Override
	public void onWatchdogCheck(long count) {
		if (autoClose && System.currentTimeMillis() - refTime > 10000) {
			this.dismiss();
		}
		
	}

	@Override
	public void onWatchdogClose() {
		
	}

}
