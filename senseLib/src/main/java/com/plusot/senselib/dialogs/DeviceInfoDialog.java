package com.plusot.senselib.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;

import com.plusot.common.Globals;
import com.plusot.common.share.LLog;
import com.plusot.common.util.ElapsedTime;
import com.plusot.common.util.Watchdog;
import com.plusot.javacommon.util.Format;
import com.plusot.senselib.R;
import com.plusot.senselib.ant.AntPowerDevice;
import com.plusot.senselib.ant.BatteryStatus;
import com.plusot.senselib.store.SRMLog;
import com.plusot.senselib.util.Util;
import com.plusot.senselib.values.Device;

public class DeviceInfoDialog extends Dialog implements /*Device.DeviceListener,*/ Watchdog.Watchable {
	private static final String CLASSTAG = DeviceInfoDialog.class.getSimpleName();

	private final Device device;
//	private TimeDevice timer;
//	private long lastUpdate = 0;

	public DeviceInfoDialog(Context context, final Device device) {
		//super(context, android.R.style.Theme_Translucent_NoTitleBar);
		super(context, R.style.Theme_TranslucentDialog);
		this.device = device;

	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.deviceinfo);
		setTitle(device.getName());
		setInfo();

		/*Button button = (Button) this.findViewById(R.id.deviceinfoButton);
		button.setOnClickListener(new android.view.View.OnClickListener() {

			@Override
			public void onClick(View v) {
				setInfo();
			}
		});*/
//		ValueViewManager mgr = ValueViewManager.getInstance();
//		if (mgr != null) {
//			LLog.d(Globals.TAG, CLASSTAG + ".onCreate Views found");
//			TimeManager timeMgr = (TimeManager) mgr.getManager(ManagerType.TIME_MANAGER);
//			if (timeMgr != null) {
//				LLog.d(Globals.TAG, CLASSTAG + ".onCreate TimeManager found");
//				timer = timeMgr.getDevice();
//				if (timer != null) timer.addListener(this);
//			}
//		}
		Watchdog.getInstance().add(this, 500);
		
	}


	@Override
	protected void onStart() {
		LLog.d(Globals.TAG, CLASSTAG + ".onStart");
		super.onStart();
	}
	
	@Override
	protected void onStop() {
		LLog.d(Globals.TAG, CLASSTAG + ".onStop");
		Watchdog.getInstance().remove(this);
		super.onStop();
	}

	private void setInfo() {
		Util.setText(this, R.id.deviceinfo_title, device.getName());
		Util.setText(this, R.id.manufacturer, device.getManufacturer().getLabel());
		Util.setText(this, R.id.model, "" + device.getProductId());
		Util.setText(this, R.id.revision, "" + device.getRevision());

		if (device.getSoftwareVersion().equals("")) {
			findViewById(R.id.row_software).setVisibility(View.GONE);
		} else {
			findViewById(R.id.row_software).setVisibility(View.VISIBLE);
			Util.setText(this, R.id.software, "" + device.getSoftwareVersion());
		}

		if (device.getSerialNumber().equals("")) {
			findViewById(R.id.row_serialnumber).setVisibility(View.GONE);
		} else {
			findViewById(R.id.row_serialnumber).setVisibility(View.VISIBLE);
			Util.setText(this, R.id.serialnumber, "" + device.getSerialNumber());
		}


		if (device.getDeviceNumber() == -1) {
			findViewById(R.id.row_channel).setVisibility(View.GONE);
			findViewById(R.id.row_devicenumber).setVisibility(View.GONE);
			findViewById(R.id.row_address).setVisibility(View.VISIBLE);
			Util.setText(this, R.id.address, "" + device.getAddress());
		} else {
			findViewById(R.id.row_address).setVisibility(View.GONE);
			findViewById(R.id.row_channel).setVisibility(View.VISIBLE);
			Util.setText(this, R.id.channel, "" + device.getAddress());
			Util.setText(this, R.id.devicenumber, "" + device.getDeviceNumber());
			findViewById(R.id.row_devicenumber).setVisibility(View.VISIBLE);
		}

		BatteryStatus status = device.getBatteryStatus();
		if (status != null && !status.equals(BatteryStatus.UNKNOWN)) {
			findViewById(R.id.row_batterystatus).setVisibility(View.VISIBLE);
			Util.setText(this, R.id.batterystatus, status.getLabel(this.getContext()));
		} else
			findViewById(R.id.row_batterystatus).setVisibility(View.GONE);

		Util.setText(this, R.id.voltage, device.getBattery());
		if (device.getOperatingTime() == 0) {
			findViewById(R.id.row_alive).setVisibility(View.GONE);

		} else {
			findViewById(R.id.row_alive).setVisibility(View.VISIBLE);
			Util.setText(this, R.id.alive, ElapsedTime.format(device.getOperatingTime() * 1000, true, false));
		}
		if (device instanceof AntPowerDevice) {
			SRMLog srmLog = SRMLog.getInstance();
			if (srmLog != null) {
				double offset = srmLog.getOffset();
				if (offset != 0) {
					this.findViewById(R.id.srm_offset_row).setVisibility(View.VISIBLE);
					Util.setText(this, R.id.srm_offset, Format.format(offset, 2));
				}
				double slope = srmLog.getSlope();
				if (slope != 0) {
					this.findViewById(R.id.srm_slope_row).setVisibility(View.VISIBLE);
					Util.setText(this, R.id.srm_slope, Format.format(slope, 2));

				}
			}
		} else {
			this.findViewById(R.id.srm_slope_row).setVisibility(View.GONE);
			this.findViewById(R.id.srm_offset_row).setVisibility(View.GONE);
		}
		if (device.getRawValue() == null) {
			findViewById(R.id.raw_value_row).setVisibility(View.GONE);

		} else {
			findViewById(R.id.raw_value_row).setVisibility(View.VISIBLE);
			Util.setText(this, R.id.raw_value, device.getRawValue());
		}
	}

	private String rawValue = null;
	@Override
	public void onWatchdogCheck(long count) {
		
		LLog.d(Globals.TAG, CLASSTAG + ".onWatchdogCheck");
		if (rawValue != null && rawValue.equals(device.getRawValue())) return;
		rawValue = device.getRawValue();
		setInfo();
	}

	@Override
	public void onWatchdogClose() {
		// TODO Auto-generated method stub
		
	}

	
//
//	@Override
//	public boolean supportsValueType(ValueType valueType) {
//		return true;
//	}
//
//	@Override
//	public void onDeviceValueChanged(Device device, ValueType valueType,
//			ValueItem valueItem, int tag) {
//		long now = System.currentTimeMillis();
//		if (now - lastUpdate > 1000) {
//			setInfo();
//			lastUpdate = now;
//		}
//	}
}
