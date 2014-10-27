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

package com.plusot.senselib;


import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.graphics.Point;
import android.location.LocationManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.support.v4.app.FragmentActivity;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.plusot.common.Globals;
import com.plusot.common.settings.PreferenceHelper;
import com.plusot.common.share.LLog;
import com.plusot.common.share.LogMail;
import com.plusot.common.share.SimpleLog;
import com.plusot.common.share.SimpleLogType;
import com.plusot.common.util.ActivityUtil;
import com.plusot.common.util.FileExplorer;
import com.plusot.common.util.SleepAndWake;
import com.plusot.common.util.ToastHelper;
import com.plusot.common.util.UserInfo;
import com.plusot.common.util.Watchdog;
import com.plusot.common.util.Watchdog.ProcessInfo;
import com.plusot.javacommon.util.Format;
import com.plusot.javacommon.util.TimeUtil;
import com.plusot.senselib.activities.AntSettingsActivity;
import com.plusot.senselib.activities.BikeSettingsActivity;
import com.plusot.senselib.activities.SettingsActivity;
import com.plusot.senselib.activities.ShareSettingsActivity;
import com.plusot.senselib.ant.AntDeviceManager;
import com.plusot.senselib.bluetooth.BTManager;
import com.plusot.senselib.bluetooth.BleManager;
import com.plusot.senselib.dialogs.DispatchKeyListener;
import com.plusot.senselib.dialogs.InputDialog;
import com.plusot.senselib.dialogs.LapsDialog;
import com.plusot.senselib.dialogs.MultiChoiceDialog;
import com.plusot.senselib.dialogs.PopupDialog;
import com.plusot.senselib.dialogs.SplashDialog;
import com.plusot.senselib.gps.GpsManager;
import com.plusot.senselib.http.HttpSender;
import com.plusot.senselib.http.TrainingPeaksHttpUpload;
import com.plusot.senselib.http.TrainingPeaksHttpUpload.Result;
import com.plusot.senselib.mail.Bitmapable;
import com.plusot.senselib.mail.SendMail;
import com.plusot.senselib.mail.SendStravaMail;
import com.plusot.senselib.osmdroid.ClickableOverlay;
import com.plusot.senselib.sensors.AndroidSensorManager;
import com.plusot.senselib.settings.FileType;
import com.plusot.senselib.settings.PreferenceDefaults;
import com.plusot.senselib.settings.PreferenceKey;
import com.plusot.senselib.store.DataLog;
import com.plusot.senselib.store.GpxReader;
import com.plusot.senselib.store.GpxReader.ResultType;
import com.plusot.senselib.store.PwxLog;
import com.plusot.senselib.store.ZipFiles;
import com.plusot.senselib.time.TimeManager;
import com.plusot.senselib.twitter.Tweet;
import com.plusot.senselib.util.SenseUserInfo;
import com.plusot.senselib.values.Device;
import com.plusot.senselib.values.DeviceListener;
import com.plusot.senselib.values.DeviceStub;
import com.plusot.senselib.values.DeviceType;
import com.plusot.senselib.values.Laps;
import com.plusot.senselib.values.Manager;
import com.plusot.senselib.values.ManagerType;
import com.plusot.senselib.values.Unit;
import com.plusot.senselib.values.Value;
import com.plusot.senselib.values.ValueItem;
import com.plusot.senselib.values.ValueType;
import com.plusot.senselib.values.ValueView;
import com.plusot.senselib.widget.FitLabelView;
import com.plusot.senselib.widget.MyMapView;

import org.json.JSONException;
import org.osmdroid.views.MapView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SenseMain
extends FragmentActivity 
implements PopupDialog.Listener, MultiChoiceDialog.Listener, Bitmapable, DispatchKeyListener, DeviceListener, 
Watchdog.Watchable, Watchdog.TopListener, OnSharedPreferenceChangeListener, SendMail.Listener, ValueView.Listener {

	private static final String CLASSTAG = SenseMain.class.getSimpleName();
	public static final int VALUE_POPUP = 1;
	public static final int VALUESELECT_POPUP = 2;
	public static final int UNIT_POPUP = 3;
	public static final int YEAR_POPUP = 4;
	public static final int MONTH_POPUP = 5;
	public static final int DAY_POPUP = 6;
	public static final int TIMES_POPUP = 7;
	public static final int START_POPUP = 8;
	public static final int MOVESTART_POPUP = 9;
	public static final int STOP_POPUP = 10;
	public static final int FULLSTOP_POPUP = 11;
	public static final int STEP_POPUP = 12;
	public static final int ARGOSSTOP_POPUP = 13;
	public static final int SPEECHSELECT_POPUP = 14;
	public static final int SHARE_POPUP = 15;
	public static final int INTERVAL_POPUP = 16;
	public static final int GPX_POPUP = 17;
	public static final int MAX_VIEWS_ADDED = 8;

	private static final int RESULT_TIMESET = 401;
	private static final int RESULT_STEP1 = 402;
	private static final int RESULT_GPS = 403;
	private static final int RESULT_BLUETOOTH = 404;
	private static int MAPVIEW_ID = 8000;


	public static final String ARGOS_MENTALSTATEFILE = "mental";
	//public boolean creating = false;

	public static final int VIEW_ID_BASE = 4000;
	public int viewsAdded = 0;

	private SenseApp app;
	private String logYear = null;
	private int logMonth = 0;
	private int logDay = 0;
	private Integer[] logMonths = null;
	private Integer[] logDays = null;
	//private String[] logTimes = null;
	private FileExplorer fileExplorer = null;
	private PowerManager.WakeLock wakeLock = null;
	private boolean clickSwitch = true;
	private boolean alive = false;
	private int watchId = -1;
	private AlertDialog calibrateDialog = null;
	//	private boolean menuVisible = true;
	private long menuVisibleClicked = System.currentTimeMillis();
	private int step = 0;
	private int argosStopStep = 0;
	private int argosRecovery = 0;
	private int shouldFinish = 0;
	private Value popupValue = null;
	private boolean moveStartBusy = false;
	private boolean stepsDone = false;
	private MyMapView map = null;
	private ViewGroup[] vgs = null;
	private int startOptions = 3;
	private NotificationManager noteManager;
	private static final int NOTIFY_ME_ID = 34561;
	private SparseArray<ValueView> views = new SparseArray<ValueView>();



	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		shouldFinish = 0;
		if (Build.VERSION.SDK_INT >= 11) requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY); //.FEATURE_ACTION_BAR_OVERLAY);
		Log.d(Globals.TAG, CLASSTAG + ".onCreate:\n" +
				"   ******************************************************\n" +
				"   *                                                    *\n" +
				"   *                                                    *\n" +
				"   *                  SenseMain Started                 *\n" +
				"   *                                                    *\n" +
				"   *                                                    *\n" +
				"   ******************************************************");

		if (Build.VERSION.SDK_INT < 14 || ViewConfiguration.get(this).hasPermanentMenuKey()) requestWindowFeature(Window.FEATURE_NO_TITLE); 
		setContentView(R.layout.main);
		ActivityUtil.lockScreenOrientation(this);

		app = (SenseApp) getApplication();
		app.activityCreated(SenseMain.this);
		init(getIntent(), true);

		watchId = Watchdog.addProcessS(CLASSTAG);	

		String state = android.os.Environment.getExternalStorageState();
		if(!state.equals(android.os.Environment.MEDIA_MOUNTED))  {
			AlertDialog alertDialog = new AlertDialog.Builder(SenseMain.this).create();  
			alertDialog.setTitle(R.string.no_sd_title);  
			alertDialog.setMessage(SenseMain.this.getString(R.string.no_sd_message));  
			alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, SenseMain.this.getString(R.string.button_confirm), new DialogInterface.OnClickListener() {  
				public void onClick(DialogInterface dialog, int which) {  
					return;  
				} });
			alertDialog.show();   
		}
		if (SenseGlobals.screenLock) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); //ActivityUtil.lockScreenOrientation(SenseMain.this);
		updateSettings();
		PreferenceHelper.getPrefs().registerOnSharedPreferenceChangeListener(this);
		LLog.i(Globals.TAG, CLASSTAG + ".onCreate. Wakelock acquired");

		Device.setListener(SenseMain.this);
		HttpSender.checkSession();
		if (System.currentTimeMillis() - Value.getSessionTime() > 3600000L * 12 && SenseGlobals.stopState.equals(SenseGlobals.StopState.PAUZE)) {
			closeValues(SenseGlobals.ActivityMode.STOP, false);
			PreferenceKey.setStopState(SenseGlobals.StopState.STOP);
		}
		if (SenseApp.firstActivity && SenseGlobals.loadSplash) {
			//creating = true;
			showSplashScreen(true);
			if (SenseGlobals.isBikeApp) {
				View view = this.findViewById(R.id.main_layout);
				if (view != null) view.setVisibility(View.INVISIBLE);
			}
		}
		SenseApp.firstActivity = false;
		Watchdog.getInstance().add(this,  1000);
		Watchdog.getInstance().add(this);

		//for (PreferenceKey prefKey : PreferenceKey.values()) updateSetting(prefs, prefKey);
		//if (PreferenceKey.HASMAP.isTrue()) addMap();
		try {
			PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
			setTitle(Globals.appName + " " + info.versionName);
		} catch (NameNotFoundException ignored) {

		}
		File file = new File(SenseGlobals.getGpxPath()); 
		if (!file.isDirectory() && !file.mkdirs()) {
			Log.w(Globals.TAG, CLASSTAG + " Could not create: " + SenseGlobals.getGpxPath());
		}
	}


	@Override
	public void onNewIntent(Intent intent) {
		init(intent, false);
	}

	public void init(Intent intent, boolean start) {
		if (intent != null) {
			String scheme = intent.getScheme();
			LLog.d(Globals.TAG, CLASSTAG + ".onActivityCreated: Scheme = " + scheme);
			if (scheme != null && scheme.equals("file")) {
				try {
					InputStream is = getContentResolver().openInputStream(intent.getData());
					LLog.d(Globals.TAG, CLASSTAG + ".onActivityCreated: Reading CSV");
					if (start) init(false);
					DataLog dataLog = DataLog.getInstance();
					SenseGlobals.loadSplash = false;
					if (dataLog != null) dataLog.readCsv(is, this);
					return;
				} catch (FileNotFoundException e) {
					LLog.e(Globals.TAG, CLASSTAG + ".onActivityCreated: Could not getContentResolver()", e);
				}
			}
		}
		if (start) init(true);
	}

	private boolean init(boolean deserialize) {	
		if (Manager.managers.size() > 0) {
			LLog.d(Globals.TAG, CLASSTAG + ".init: Start GPS only");
			Manager.managers.get(ManagerType.GPS_MANAGER).init();
			return false;
		}
		Manager.managers.clear();
		Device.clearDevices();
		LLog.d(Globals.TAG, CLASSTAG + ".init: Building from scratch");
		if (ManagerType.supportedManagers.contains(ManagerType.ANT_MANAGER))
			Manager.managers.put(ManagerType.ANT_MANAGER, new AntDeviceManager());
		Manager.managers.put(ManagerType.GPS_MANAGER, new GpsManager());
		Manager.managers.put(ManagerType.TIME_MANAGER, new TimeManager());
		if (ManagerType.supportedManagers.contains(ManagerType.SENSOR_MANAGER))
			Manager.managers.put(ManagerType.SENSOR_MANAGER, new AndroidSensorManager());
		if (ManagerType.supportedManagers.contains(ManagerType.BLUETOOTH_MANAGER))
			Manager.managers.put(ManagerType.BLUETOOTH_MANAGER, new BTManager());
		if (ManagerType.supportedManagers.contains(ManagerType.BLUETOOTHLE_MANAGER))
			Manager.managers.put(ManagerType.BLUETOOTHLE_MANAGER, new BleManager());

		for (Manager mgr : Manager.managers.values()) mgr.init();

		//Device.addDevice(
		Laps.getLapDevice();//);
		Value.initDevices(deserialize);

		noteManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notifyMe();
		return true;
	}


	private void finishApp() {
		for (Manager manager : Manager.managers.values()) manager.destroy();
		views.clear();
		clearNotification();

		Watchdog.getInstance().remove((Watchdog.Watchable)this);
		Watchdog.getInstance().remove((Watchdog.TopListener)this);

		LLog.d(Globals.TAG, CLASSTAG + ".onPause is finishing app and closing log.");
		Manager.managers.clear();
		Device.clearDevices();
		app.finish(CLASSTAG + ".onPause");
		if (wakeLock != null && wakeLock.isHeld()) {
			LLog.d(Globals.TAG, CLASSTAG + ".finish releasing WakeLock"); 	
			wakeLock.release();
			wakeLock = null;
		}
		Watchdog.removeProcessS(watchId, CLASSTAG);
	}

	@Override
	protected void onPause() {

		LLog.d(Globals.TAG, CLASSTAG + ".onPause");
		alive = false;
		saveConfiguration();

		if (isFinishing()) finishApp();

		super.onPause();
	}

	@SuppressLint("Wakelock")
	@Override
	protected void onResume() {
		LLog.d(Globals.TAG, CLASSTAG + ".onResume");
		super.onResume();

		loadConfiguration();

		for (Manager deviceMgr: Manager.managers.values()){
			if (deviceMgr != null) deviceMgr.onResume();
		} 


		for (int i = 0; i < views.size(); i++) {
			ValueView view = views.valueAt(i);
			if (view == null) continue;
			Set<Value> values = view.getValues();
			view.resume();
			for (Value value : values) value.update(); //, ChangeFlag.ON_RESUME);
		}

		if (HttpSender.isHttpPost) {
			LLog.d(Globals.TAG, CLASSTAG + ".onResume: Sending user info");
			HttpSender sender = HttpSender.getInstance(); 
			if (sender != null) sender.sendUser();
		}
		alive = true;
		if (popupToShow != -1) 
			new SleepAndWake(new SleepAndWake.Listener() {

				@Override
				public void onWake() {
					//if (popupToShow == STEP_POPUP) step = SenseGlobals.argos ? 0 : 1;
					showPopupDialog(popupToShow, VIEW_ID_BASE + viewsAdded - 1, null);
				}
			}, 300);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		alive = true;
		super.onRestoreInstanceState(savedInstanceState);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		alive = false;
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onStart() {
		LLog.d(Globals.TAG, CLASSTAG + ".onStart");

		for (Manager deviceMgr: Manager.managers.values()){
			if (deviceMgr != null) deviceMgr.onStart();
		} 
		super.onStart();
	}

	@Override
	protected void onStop() {
		alive = false;
		LLog.d(Globals.TAG, CLASSTAG + ".onStop");
		//		for (Manager deviceMgr: Manager.managers.values()){
		//			if (deviceMgr != null) deviceMgr.onStop(); //may be needed for ANT+????
		//		} 
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		LLog.d(Globals.TAG, CLASSTAG + ".onDestroy");

		if (Globals.runMode.isRun()) {
			alive = false;
			saveConfiguration();

			finishApp();
		}
		super.onDestroy();
	}

	@SuppressWarnings("deprecation")
	public void notifyMe() {
		String version = "";
		try {
			PackageInfo info = Globals.appContext.getPackageManager().getPackageInfo(Globals.appContext.getPackageName(), 0);
			version = " " + info.versionName;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}

		Notification note = new Notification(SenseGlobals.notifyIcon,
				Globals.TAG + version,
				System.currentTimeMillis()); 
		//String[] hints = Globals.appContext.getResources().getStringArray(R.array.hint_items);
		//String hint = hints[(int)(Math.random() * (hints.length - 1))];
		PendingIntent intent = PendingIntent.getActivity(Globals.appContext, 0, new Intent(Globals.appContext, SenseGlobals.mainClass), 0);
		note.setLatestEventInfo(Globals.appContext, Globals.TAG + version, Globals.appContext.getString(R.string.hello), intent);
		//note.ledOnMS = 1000;
		//note.ledOffMS = 1000;
		//note.ledARGB = 0xFFFFFFFF;
		//note.tickerText = hint;
		//note.number=++noteCount;
		note.flags |= Notification.FLAG_ONGOING_EVENT;
		//note.vibrate=new long[] {20L, 50L}; 

		noteManager.notify(NOTIFY_ME_ID, note);
	}

	public void clearNotification() {
		noteManager.cancel(NOTIFY_ME_ID); 
	}

	private void addOnValueClick(View v, final int viewId) {
		//LLog.d(Globals.TAG, "Value view " + viewId + " click added");
		if (v != null) {
			v.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (viewHasValue(viewId)) {
						// Util.showToastShort(WindSensActivity.this,
						// getString(R.string.hint_long_click));
					} else {
						showMultiChoiceDialog(VALUESELECT_POPUP, viewId); 
					}
				}
			});
		}
	}


	@SuppressWarnings("deprecation")
	public void showPopupDialog(int dialogId, int viewId, String[] items) {// ,
		if (!alive) {
			if (dialogId == STEP_POPUP) popupToShow = STEP_POPUP;
			return; 
		}
		int iEnd = 3;
        int timeOut = 0;
        int defaultChoice = 0;
		boolean cancelable = true;
		popupToShow = -1;
		String title = null;
		Map <String, String> lookup = null;
		if (items == null)
			items = getResources().getStringArray(
					R.array.value_popup_dialog_items);
		switch (dialogId) {
		case VALUE_POPUP:
			title = getString(R.string.valuepopup);
			items = getResources().getStringArray(
					R.array.value_popup_dialog_items);

			if (viewHasValue(viewId)) {
				popupValue = viewGetCurrentValue(viewId);
				title = popupValue.getValueType(this);
				if (popupValue.getValueType().equals(ValueType.POWER)) {
					items = Arrays.copyOf(items, items.length + 1);
					items[items.length - 1] = getString(R.string.calibrate);
				} else if (popupValue.getValueType().equals(ValueType.SLOPE)) {
					items = Arrays.copyOf(items, items.length + 1);
					items[items.length - 1] = getString(R.string.set_level);
				}  
			}

			break;
		case VALUESELECT_POPUP:
			title = getString(R.string.valuepopup);
			items = Value.getValuePopupList(this, PreferenceKey.XTRAVALUES.isTrue());
			break;
		case UNIT_POPUP:
			title = getString(R.string.unitpopup);
			if (items == null || items.length == 0)
				ToastHelper.showToastLong(R.string.no_unit_choice);
			break;
		case YEAR_POPUP:
			title = getString(R.string.yearpopup);
			if (fileExplorer == null) {
				items = null;
			} else {
				items = fileExplorer.getYears();
			}
			if (items == null || items.length == 0) {
				ToastHelper.showToastLong(R.string.no_year_choice);
				return;
			} else if (items.length > 1) {
				break;
			}
			logYear = items[0];
			dialogId = MONTH_POPUP;
		case MONTH_POPUP:
			title = getString(R.string.monthpopup);
			if (fileExplorer == null) {
				items = null;
			} else {
				logMonths = fileExplorer.getMonths(logYear);
				items = FileExplorer.getMonths(logYear, logMonths);
			}
			if (items == null || items.length == 0) {
				ToastHelper.showToastLong(getString(R.string.no_month_choice, logYear));
				return;
			} else if (items.length > 1) {
				break;
			}
			logMonth = logMonths[0];
			LLog.d(Globals.TAG, CLASSTAG + ".showPopupDialog: logMonth = "
					+ logMonth);
			dialogId = DAY_POPUP;
		case DAY_POPUP:
			title = getString(R.string.daypopup);
			if (fileExplorer == null) {
				items = null;
			} else {
				logDays = fileExplorer.getDays(logYear, logMonth);
				items = FileExplorer.getDays(logYear, DateUtils.getMonthString(
						logMonth - 1, DateUtils.LENGTH_LONG), logDays);
			}
			break;
		case TIMES_POPUP:
			title = getString(R.string.timespopup);
			if (fileExplorer == null) {
				items = null;
			} else {
				items = fileExplorer.getTimes(logYear, logMonth, logDay);
			}
			break;
		case MOVESTART_POPUP:
			lastAskForActivityStart = System.currentTimeMillis();
			if (moveStartBusy) {
				title = null;
			} else {	
				moveStartBusy = true;
				title = getString(R.string.movestartpopup);
				items = getResources().getStringArray(R.array.start_popup_dialog_items);
				if (SenseGlobals.stopState.equals(SenseGlobals.StopState.STOP))
					items = Arrays.copyOfRange(items, 1, iEnd);
				else
					items = Arrays.copyOfRange(items, 0, iEnd);
				cancelable = false;
			}
			break;
		case STEP_POPUP:
			lastAskForActivityStart = System.currentTimeMillis();
            timeOut = 10000;
			switch (step) {
			case 0:
				title = getString(R.string.recoverypopup);
				items = getResources().getStringArray(R.array.fitnessorrecovery_popup_dialog_items);
				cancelable = false;
				break;
			case 1:
				title = getString(R.string.step1settingspopup);
				List<String> list = new ArrayList<String>();
				String profile = PreferenceKey.getProfileName();
				list.add(getString(R.string.step1_bikeprofile, profile));
				//list.add(getString(R.string.popitem_wheelcirumference, prefs.getWheelCirc()));
				if (PreferenceKey.USEPAIRING.isTrue())
					list.add(getString(R.string.popitem_pairing_on, profile) );
				else
					list.add(getString(R.string.popitem_pairing_off, profile) );
				BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
				if (adapter != null && adapter.isEnabled()) 
					list.add(getString(R.string.popitem_bluetooth_on) );
				else
					list.add(getString(R.string.popitem_bluetooth_off) );
				list.add(getString(R.string.step_continue));
				items = list.toArray(new String[0]);
				cancelable = false;
				break;
			case 2:
				if (PreferenceKey.XTRAVALUES.isTrue()) {
					title = getString(R.string.startpopup);
					iEnd = 3;	
				}	else {
					title = getString(R.string.step2startpopup);
					iEnd = 4;	
				}
				cancelable = false;
				if (title == null) title = getString(R.string.startpopup);
				items = getResources().getStringArray(R.array.start_popup_dialog_items);
				if (SenseGlobals.stopState.equals(SenseGlobals.StopState.STOP)) {
					items = Arrays.copyOfRange(items, 1, iEnd);
					startOptions = 3;
				} else {
					items = Arrays.copyOfRange(items, 0, iEnd);
					startOptions = 4;
				}
				break;
			}
			break;
		case START_POPUP:
			lastAskForActivityStart = System.currentTimeMillis();
			if (title == null) title = getString(R.string.startpopup);
            timeOut = 10000;
			items = getResources().getStringArray(R.array.start_popup_dialog_items);
			if (SenseGlobals.stopState.equals(SenseGlobals.StopState.STOP))
				items = Arrays.copyOfRange(items, 1, iEnd);
			else
				items = Arrays.copyOfRange(items, 0, iEnd);
			break;
		case ARGOSSTOP_POPUP:
			cancelable = false;
			switch (argosStopStep) {
			case 0:
				title = getString(R.string.intensitypopup);
				items = getResources().getStringArray(R.array.intensity_popup_dialog_items);
				break;
			case 1:
				title = getString(R.string.fitnesspopup);
				items = getResources().getStringArray(R.array.fitnessorrecovery_popup_dialog_items);
				break;
			}
			break;
		case FULLSTOP_POPUP:
			lastAskForActivityStart = System.currentTimeMillis();
			title = getString(R.string.stoppopup);
			items = getResources().getStringArray(R.array.stop_popup_dialog_items);
			break;
		case STOP_POPUP:
			lastAskForActivityStart = System.currentTimeMillis();
			title = getString(R.string.stoppopup);
			items = getResources().getStringArray(R.array.stop_popup_dialog_items);
			break;
		case SHARE_POPUP:
			title = getString(R.string.sharepopup);
			items = getResources().getStringArray(R.array.share_popup_dialog_items);
			if (Globals.testing.isNoTest()) items = Arrays.copyOf(items, items.length - 1);
			break;
		case INTERVAL_POPUP:
			title = getString(R.string.menu_interval);
			items = getResources().getStringArray(R.array.interval_popup_dialog_items);
			break;
		case GPX_POPUP:
			title = getString(R.string.menu_gpx);
			FileExplorer.FileList list = FileExplorer.getFileList(new String[] { Globals.getDataPath(),  Globals.getDownloadPath(), SenseGlobals.getGpxPath()},
					new String[] {".gpx"}, false);
			items = list.items;
			lookup = list.lookup;
			break;
		}
		if (title != null && items != null && items.length > 0) {
			if (!PopupDialog.showPopupDialog(getFragmentManager(), title, dialogId, viewId, items, lookup, cancelable, timeOut, defaultChoice) && dialogId == STEP_POPUP) popupToShow = STEP_POPUP;
		}
	}

	private void calibrate() {
		AntDeviceManager ant = (AntDeviceManager) Manager.managers.get(ManagerType.ANT_MANAGER);
		if (ant != null) ant.calibratePower();
	}

	private void showTweetWarning() {
		AlertDialog alertDialog = new AlertDialog.Builder(this).create();  
		alertDialog.setTitle(R.string.warn_tweet_cloud_title);  
		alertDialog.setMessage(getString(R.string.warn_tweet_cloud_msg));  
		alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.button_confirm), new DialogInterface.OnClickListener() {  
			public void onClick(DialogInterface dialog, int which) {  
				return;  
			} });
		alertDialog.show();   
	}

	@SuppressWarnings("deprecation")
	public void onPopupResult(int dialogId, int viewId, int iWhich, int itemsSize, String sWhich, String sTag) { // , int tag) {
		switch (dialogId) {
		case VALUE_POPUP:
			switch (iWhich) {
			/*case 0:
				showPopupDialog(VALUESELECT_POPUP, viewId, null); // , 0);
				break;*/
			case 0:
				showMultiChoiceDialog(VALUESELECT_POPUP, viewId);
				break;
			case 1:
				//				if (SenseGlobals.isLite) 
				//					showLiteWarning();
				//				else 
				if (PreferenceKey.VOICEON.isTrue()) 
					showMultiChoiceDialog(SPEECHSELECT_POPUP, viewId);
				else
					warnNoSpeech();
				break;
			case 2:
				if (popupValue == null) {
					popupValue = viewGetCurrentValue(viewId);
				}
				if (popupValue != null) {
					final String[] units = Unit.getValidUnits(popupValue.getValueType().getUnitType(), this);
					showPopupDialog(UNIT_POPUP, viewId, units); 
				}
				break;
			case 3:
				if (sWhich.equals(getString(R.string.calibrate))) {
					calibrate();
				} else if (sWhich.equals(getString(R.string.set_level))) {
					if (Manager.managers.get(ManagerType.SENSOR_MANAGER) == null) return;
					((AndroidSensorManager)Manager.managers.get(ManagerType.SENSOR_MANAGER)).setLevel();
					ToastHelper.showToastLong(R.string.level_message);
				}
				break;
			}
			break;
		case UNIT_POPUP:
			final Unit unit = Unit.getUnitByChoiceString(sWhich, this);
			if (popupValue != null) {
				popupValue.setUnit(unit);
			}
			break;
		case VALUESELECT_POPUP:
			viewSetValue(viewId, Value.getValueByString(this, sWhich));
			ToastHelper.showToastShort(getString(R.string.hint_long_click));
			saveConfiguration();
			break;
		case YEAR_POPUP:
			logYear = sWhich;
			showPopupDialog(MONTH_POPUP, viewId, null); // , 0);
			break;
		case MONTH_POPUP:
			logMonth = logMonths[iWhich];
			LLog.d(Globals.TAG, CLASSTAG + ".showPopupDialog: logMonth = "
					+ logMonth);
			showPopupDialog(DAY_POPUP, viewId, null); // , 0);
			break;
		case DAY_POPUP:
			logDay = logDays[iWhich];

			if (fileExplorer != null) {
				switch (fileExplorer.getTag()) {
				case FileExplorer.ACTIVITY_TAG:	
					showPopupDialog(TIMES_POPUP, viewId, null); 
					break;
				}
			}
			break;
		case TIMES_POPUP:
			if (fileExplorer != null) {
				switch (fileExplorer.getTag()) {
				case FileExplorer.ACTIVITY_TAG:	
					stopActivity("onPopupResult", false);
					ToastHelper.showToastLong(
							getString(R.string.chosen_file) + " = "
									+ logDay
									+ " "
									+ DateUtils.getMonthString(logMonth - 1,
											DateUtils.LENGTH_LONG) + " " + logYear + " " + sWhich);

					DataLog dataLog =  DataLog.getInstance();
					if (dataLog != null) {
						String file = fileExplorer.getFileList(logYear, logMonth, logDay, sWhich);
						if (file != null) {
							SenseGlobals.replaySession  = logYear + String.format("%02d%02d", logMonth, logDay) + "-" + sWhich.substring(0,2) + sWhich.substring(3,5) + sWhich.substring(6,8);
							LLog.d(Globals.TAG, CLASSTAG + ".onPopupResult: ReplaySession = " + SenseGlobals.replaySession);
							dataLog.readCsv(new String[] {file}, this);
						}
					}
					//					if (showMap) showLocation();
					break;
				}
			}
			break;
		case STEP_POPUP:
			switch (step) {
			case 0:
				argosRecovery = 6 + iWhich;
				step = 1;
				showPopupDialog(STEP_POPUP, VIEW_ID_BASE + viewsAdded - 1, null);
				break;
			case 1:
				LLog.d(Globals.TAG, CLASSTAG + ".onPopuResult: STEP1SETTINGS_POPUP = " + iWhich);
				//ActivityUtil.lockScreenOrientation(this);
				switch (iWhich){
				case 0:
					startActivityForResult(new Intent(this, BikeSettingsActivity.class), RESULT_STEP1);
					break;	
				case 1:
					startActivityForResult(new Intent(SenseMain.this, AntSettingsActivity.class), RESULT_STEP1);
					break;
				case 2:
					startActivityForResult(new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS), RESULT_STEP1);
					break;
				default:
					//enableAnt();
					step = 2;
					showPopupDialog(STEP_POPUP, VIEW_ID_BASE + viewsAdded - 1, null);
					break;
				}
				break;
			case 2:
				if (iWhich == startOptions - 1) 
					startUpDialogs("STEP2START_POPUP");
				else {
					View view = this.findViewById(R.id.main_layout);
					if (view != null) view.setVisibility(View.VISIBLE);

					if (iWhich >= 0 && iWhich <= startOptions - 3) startActivity(iWhich == startOptions - 3);
					stepsDone = true;
				}
				lastAskForActivityStart = System.currentTimeMillis();
				break;
			}
			break;
		case MOVESTART_POPUP:
			moveStartBusy  = false;
			lastAskForActivityStart = System.currentTimeMillis();
		case START_POPUP:
			if (iWhich >= 0 && iWhich <= itemsSize - 2) startActivity(iWhich == itemsSize - 2);

			break;
		case ARGOSSTOP_POPUP:
			switch (argosStopStep) {
			case 0:
				SimpleLog.getInstance(SimpleLogType.TEXT, ARGOS_MENTALSTATEFILE).log("recovery=" + argosRecovery);
				SimpleLog.getInstance(SimpleLogType.TEXT, ARGOS_MENTALSTATEFILE).log("intensity=" + (6 + iWhich));
				argosStopStep = 1;
				showPopupDialog(ARGOSSTOP_POPUP, VIEW_ID_BASE + viewsAdded - 1, null);

				break;
			case 1:
				SimpleLog.getInstance(SimpleLogType.TEXT, ARGOS_MENTALSTATEFILE).log("fitness=" + (6 + iWhich));
				new InputDialog(this, 666, R.string.remarkspopup_title, R.string.remarkspopup_description, "", R.string.remarkspopup_hint,
						new InputDialog.Listener() {

					@Override
					public void onClose(int id, String temp) {
						SimpleLog.getInstance(SimpleLogType.TEXT, ARGOS_MENTALSTATEFILE).log("remark=" + temp);
						argosStopStep = 2;
						new SleepAndWake(new SleepAndWake.Listener() {
							@Override
							public void onWake() {
								shouldFinish =  (shouldFinish == 1) ? 2 : 0;
								sendMail(true);
							}
						}, 100);	
					}
				}).show();
				break;
			}
			break;

		case FULLSTOP_POPUP:
			lastAskForActivityStart = System.currentTimeMillis();

			if (iWhich == 0) {
				ToastHelper.showToastLong(R.string.toast_pause);
				pauseActivity("onPopupResult");
				finishIt();
			} else {
				stopActivity("onPopupResult", true);
			}
			//			if (dialogId == FULLSTOP_POPUP) finishIt();

			break;
		case STOP_POPUP:
			lastAskForActivityStart = System.currentTimeMillis();
			if (iWhich == 0) {
				ToastHelper.showToastLong(R.string.toast_pause);
				pauseActivity("onPopupResult");
			} else {
				stopActivity("onPopupResult", false);
			}
			break;
		case SHARE_POPUP:
			switch (iWhich) {
			case 0: this.sendMail(false); break;
			case 1: 
				//				if (SenseGlobals.isLite) {
				//					this.showLiteWarning();
				//				} else 
				if (!PreferenceKey.HTTPPOST.isTrue())
					showTweetWarning();
				else {
					String link = getString(R.string.tweet_msg, 
							Globals.TAG, PreferenceKey.SHARE_URL.getString() + "?device=" + SenseUserInfo.getDeviceId(),
							TimeUtil.formatTime(System.currentTimeMillis(), "EEE dd MMM HH:mm:ss"));
					int session = HttpSender.getSession();
					if (session >= 0) link += "&session=" + session;
					new Tweet(this, null).send(link); 
				}
				break;
			case 2: 
				//				if (SenseGlobals.isLite)
				//					this.showLiteWarning();
				//				else
				this.sendTP();
				break;
			case 3:
				if (!Value.fileTypes.contains(FileType.FIT)) {
					this.warnNoFit();
					break;
				}
				switch (new SendStravaMail(this).send()) {
				case SUCCES:
					break;
				case NOACCOUNT:
					this.warnAccount();
					break;
				case NOFILES:
					this.warnNoFilesToday();
					break;
				}
				break;
			case 4:
				StringBuffer buf = new StringBuffer();
				buf.append("\nApp version: " + UserInfo.appNameVersion() + "\n");
				buf.append("Android version: " + Build.VERSION.RELEASE+ "\n");
				buf.append("Android incremental: "+Build.VERSION.INCREMENTAL+ "\n");
				buf.append("Android SDK: "+Build.VERSION.SDK_INT+ "\n");
				//buf.append("FINGERPRINT: "+Build.FINGERPRINT+ "\n");
				buf.append("Device manufacturer: "+Build.MANUFACTURER+ "\n");
				buf.append("Device brand: "+Build.BRAND+ "\n");
				buf.append("Device model: "+Build.MODEL+ "\n");
				buf.append("Device board: "+Build.BOARD+ "\n");
				buf.append("Device id: "+Build.DEVICE);

				new LogMail(this, getString(R.string.reportmail, TimeUtil.formatTime(System.currentTimeMillis()), buf), getString(R.string.mail_sending), getString(R.string.no_mail_to_send), null);
			}
			break;
		case INTERVAL_POPUP:
			switch (iWhich) {
			case 0: newInterval(); break;
			case 1: new LapsDialog(this, Laps.getLaps() - 1,false).show();
			}
			break;
		case GPX_POPUP:
			ToastHelper.showToastLong(getString(R.string.get_gpx_file, sWhich + " " + sTag));
			getGPX(sTag);
			break;
		}


	}

	public void showMultiChoiceDialog(int dialogId, int viewId) {
		// Value value = null;
		String title = "";
		Map<String, Boolean> items = null;

		switch (dialogId) {
		case VALUESELECT_POPUP:
			// multi = true;
			title = getString(R.string.valuepopup);
			final EnumSet<ValueType> valueSet = viewGetValues(viewId);
			items = Value.getValueSelectedPopupList(this, valueSet, PreferenceKey.XTRAVALUES.isTrue());
			break;
		case SPEECHSELECT_POPUP:
			title = getString(R.string.voicevaluepopup);
			final Set<String> voiceSet = PreferenceKey.VOICEVALUES.getStringSet();
			items = ValueType.getListSelected(this, voiceSet, PreferenceKey.XTRAVALUES.isTrue());
			break;
		}
		if (items != null && items.size() > 0)
			MultiChoiceDialog.showPopupDialog(this.getFragmentManager(),
					title, dialogId, viewId, items);
	}

	@Override
	public void onMultiChoiceDialogResult(int dialogId, int viewId,
			Map<String, Boolean> map) {
		switch (dialogId) {
		case VALUESELECT_POPUP:
			for (final String which : map.keySet()) {
				if (map.get(which)) {
					LLog.d(Globals.TAG,  CLASSTAG + ".onMultiChoiceDialogResult: Checked: " + which);
					viewAddValue(viewId, Value.getValueByString(this, which));
					// Util.showToastShort(this,
					// getString(R.string.hint_long_click));
				} else {
					LLog.d(Globals.TAG,  CLASSTAG + ".onMultiChoiceDialogResult: Unchecked: " + which);
					viewRemoveValue(viewId, Value.getValueByString(this, which));
				}
			}
			saveConfiguration();
			break;
		case SPEECHSELECT_POPUP:
			Map<ValueType, Boolean> valueTypes = ValueType.getListSelected(this, map);
			for (ValueType valueType : valueTypes.keySet()) {
				Boolean bool = valueTypes.get(valueType);
				if (bool != null) {
					if (bool.booleanValue()) {
						app.addVoiceValues(valueType);
					} else
						app.removeVoiceValues(valueType);
				}
			}
			break;
		}
	}

	private void saveConfiguration() {
		LLog.d(Globals.TAG, CLASSTAG + ".saveConfiguration");
		final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this); 
		final SharedPreferences.Editor editor = settings.edit();

		for (int i = 0; i < viewsAdded; i++) {
			final EnumSet<ValueType> values = viewGetValues(i + VIEW_ID_BASE);
			int j = 0;
			if (values == null) continue;
			for (final ValueType valueType : values) {
				final String id = valueType.getShortName();
				editor.putString("view_" + (i + 1) + "_value_" + j, id);
				//LLog.d(Globals.TAG, CLASSTAG + ": Saving " + id);
				j++;
			}
			editor.putInt("view_" + (i + 1) + "_values", j);

		}
		editor.commit();
	}

	private void loadConfiguration() {
		LLog.d(Globals.TAG, CLASSTAG + ".loadConfiguration");
		int windows = PreferenceKey.WINDOWS.getInt();
		if (viewsAdded == 0 || (viewsAdded > 0 && findViewById(VIEW_ID_BASE + viewsAdded - 1) == null)) {
			viewsAdded = 0;
			for (int i = 0; i < windows; i++) {
				addView();
			}
		} else if (windows < viewsAdded) {
			while (windows < viewsAdded) {
				removeView();
			}
		} else if (windows > viewsAdded) {
			while (windows > viewsAdded) {
				addView();
			}
		}
		if (viewsAdded == 0) for (int i = 0; i < 4; i++) {
			addView();
		}
		loadValuesToViews(null);
	}

	private void loadValuesToViews(Value refValue) {
		if (refValue != null && viewHasValue(refValue)) return;
		final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this); 
		for (int i = 0; i < viewsAdded; i++) {
			String id = null;
			final int jMax = settings.getInt("view_" + (i + 1) + "_values", 0);
			if (jMax == 0) {
				if (SenseGlobals.isBikeApp) switch (i) {
				case 0: viewAddValue(i + VIEW_ID_BASE, Value.getValue(ValueType.HEARTRATE, true)); break;
				case 1: viewAddValue(i + VIEW_ID_BASE, Value.getValue(ValueType.SPEED, true)); break;
				case 2: viewAddValue(i + VIEW_ID_BASE, Value.getValue(ValueType.CADENCE, true)); break;
				//case 3: valueMgr.viewAddValue(i + VIEW_ID_BASE, Value.getValue(ValueType.AIRPRESSURE)); break;
				} else switch (i) {
				case 0: viewAddValue(i + VIEW_ID_BASE, Value.getValue(ValueType.TEMPERATURE, true)); break;
				case 1: viewAddValue(i + VIEW_ID_BASE, Value.getValue(ValueType.WINDSPEED, true)); break;
				case 2: viewAddValue(i + VIEW_ID_BASE, Value.getValue(ValueType.HUMIDITY, true)); break;
				case 3: viewAddValue(i + VIEW_ID_BASE, Value.getValue(ValueType.AIRPRESSURE, true)); break;
				}
			}
			Value value = null;
			for (int j = 0; j < jMax; j++) {
				id = settings.getString("view_" + (i + 1) + "_value_" + j, null);

				if (id == null) continue;
				LLog.d(Globals.TAG,
						CLASSTAG
						+ ".loadValuesToViews: Trying to load value from id: " + id + " to view " + (i + 1));
				value = Value.getValueByString(id);
				if (value == null) {
					LLog.d(Globals.TAG,
							CLASSTAG
							+ ".loadValuesToViews could not create value from id: "
							+ id);
				} else if (refValue == null || refValue == value) {
					viewAddValue(i + VIEW_ID_BASE, value);
				}
			}
		}
	}

	private void adjustViews() {
		vgs  = new ViewGroup[4];
		vgs[0] = (ViewGroup) findViewById(R.id.double1);
		vgs[1] = (ViewGroup) findViewById(R.id.double2);
		vgs[2] = (ViewGroup) findViewById(R.id.double3);
		vgs[3] = (ViewGroup) findViewById(R.id.double4);
		int devider = 1;
		if (this.viewsAdded <= 4) {
			if (map == null) {
				if (vgs[3].getChildCount() == 0 && vgs[1].getChildCount() == 2) {
					View view = vgs[1].getChildAt(1);
					vgs[1].removeView(view);
					vgs[3].addView(view);
				}
				if (vgs[2].getChildCount() == 0 && vgs[0].getChildCount() == 2) {
					View view = vgs[0].getChildAt(1);
					vgs[0].removeView(view);
					vgs[2].addView(view);
				}
			} else {
				if (vgs[3].getChildCount() == 1 && vgs[1].getChildCount() == 1) {
					View view = vgs[3].getChildAt(0);
					vgs[3].removeView(view);
					vgs[1].addView(view);
				}
				if (vgs[2].getChildCount() == 1 && vgs[0].getChildCount() == 1) {
					View view = vgs[2].getChildAt(0);
					vgs[2].removeView(view);
					vgs[0].addView(view);
				}	
			}
		}
		for (int i = 0; i < 4; i++) {
			if (vgs[i].getChildCount() > 0) {
				devider =  i + 1; 
				vgs[i].setVisibility(View.VISIBLE);
			} else 
				vgs[i].setVisibility(View.GONE);
		}
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT,
				1.0f / devider);
		for (int i = 0; i < 4; i++) vgs[i].setLayoutParams(params);

		Display display = getWindowManager().getDefaultDisplay();
		@SuppressWarnings("deprecation")
		int w = display.getWidth();
		@SuppressWarnings("deprecation")
		int h = display.getHeight();
		//LLog.d(Globals.TAG,  CLASSTAG + " Size = " + w + " x " + h);

		for (int i = 0; i < 8; i++) {
			if (map == null) {
				View view = findViewById(i + VIEW_ID_BASE);
				if (view == null) continue;
				params = (LinearLayout.LayoutParams) view.getLayoutParams();
				params.setMargins(1, 1, 1, 1);	
				view.setBackgroundResource(R.drawable.round_border);
				view.setLayoutParams(params);
			} else {

				int x = w / 10;
				if (w < 500) x = w / 40;
				int y = h / 6;
				switch (devider) {
				case 1: y = h / 6 + h / 2; break;
				case 2: y = h / 6; break;
				case 3: y = h / 8; break;
				case 4: y = h / 10; break;
				}
				View view = null;
				switch (i) {
				case 0: 
					view = vgs[0].getChildAt(0);
					if (view == null) break;
					params = (LinearLayout.LayoutParams) view.getLayoutParams();
					if (vgs[0].getChildCount() == 1)
						params.setMargins(0, 0, 0, y);
					else 
						params.setMargins(0, 0, x, y);	
					break;
				case 1:
					view = vgs[0].getChildAt(1);
					if (view == null) break;
					params = (LinearLayout.LayoutParams) view.getLayoutParams();
					params.setMargins(x, 0, 0, y);
					break;
				case 2:
					view = vgs[1].getChildAt(0);

					if (view == null) break;
					params = (LinearLayout.LayoutParams) view.getLayoutParams();
					if (devider > 2) {
						if (vgs[1].getChildCount() == 1)
							params.setMargins(0, y, x + w / 2, 0);
						else
							params.setMargins(0, y / 2, x, y / 2);
					} else if (vgs[1].getChildCount() == 1)
						params.setMargins(0, y, 0, 0);
					else
						params.setMargins(0, y, x, 0);
					break;
				case 3:
					view = vgs[1].getChildAt(1);
					if (view == null) break;
					params = (LinearLayout.LayoutParams) view.getLayoutParams();
					if (devider > 2) 
						params.setMargins(x, y / 2, 0, y / 2);
					else
						params.setMargins(x, y, 0, 0);
					break;
				case 4: 
					view = vgs[2].getChildAt(0);
					if (view == null) break;
					params = (LinearLayout.LayoutParams) view.getLayoutParams();
					if (devider > 3) {
						if (vgs[2].getChildCount() == 1)
							params.setMargins(0, y, x + w / 2, 0);
						else 
							params.setMargins(0, y / 2, x, y / 2);
					} else if (vgs[2].getChildCount() == 1)
						params.setMargins(0, y, 0, 0);
					else
						params.setMargins(0, y, x, 0);
					break;
				case 5:
					view = vgs[2].getChildAt(1);
					if (view == null) break;
					params = (LinearLayout.LayoutParams) view.getLayoutParams();
					if (devider > 3)
						params.setMargins(x, y / 2, 0, y / 2);
					else
						params.setMargins(x, y, 0, 0);
					break;
				case 6:
					view = vgs[3].getChildAt(0);
					if (view == null) break;
					params = (LinearLayout.LayoutParams) view.getLayoutParams();
					if (vgs[3].getChildCount() == 1)
						params.setMargins(0, y, 0, 0);
					else
						params.setMargins(0, y, x, 0);
					break;
				case 7:
					view = vgs[3].getChildAt(1);
					if (view == null) break;
					params = (LinearLayout.LayoutParams) view.getLayoutParams();
					params.setMargins(x, y, 0, 0);
					break;

				}
				if (view != null) {
					view.setLayoutParams(params);
					view.setBackgroundResource(R.drawable.transparent_border);
				}	
			}

			//View v = view.findViewById(R.id.graph_view_layout);
		}

	}

	//	private static final String[] ROTATION = new String[] { "rotation_0","rotation_90","rotation_180","rotation_270" };

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void addMap() {
		View view = findViewById(R.id.main_rootlayout);
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
				LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT);
		params.addRule(RelativeLayout.CENTER_VERTICAL);
		map = new MyMapView(this);
		LLog.d(Globals.TAG, CLASSTAG + ".addMap: Map created");
		map.setZoom(14);
		//map.setMapListener(this);
		params.setMargins(0 ,0 ,0 ,0 );
		((RelativeLayout) view).addView(map, 0, params);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && MAPVIEW_ID == 8000)
            MAPVIEW_ID = View.generateViewId();
        map.setId(MAPVIEW_ID);
		map.setOnTouchListener(new ClickableOverlay.Listener() {

			@TargetApi(Build.VERSION_CODES.HONEYCOMB)
			@Override
			public boolean onTouch(MapView mapView, MotionEvent event) {
				//LLog.d(Globals.TAG, CLASSTAG + ".Mapview.onTouch");
				if (Build.VERSION.SDK_INT >= 11) {
					ActionBar bar = getActionBar();
					if (bar != null && !bar.isShowing()) bar.show();
				}
				menuVisibleClicked = System.currentTimeMillis();

				return false;
			}

			@Override
			public boolean onClick(MapView mapView, MotionEvent event) {
				toggleValuesVisible();
				return false;
			}

		});

	}

	private boolean visible = true;
	private void toggleValuesVisible() {
		visible ^= true;
		for (int i = 0; i < viewsAdded; i++) {
			View v = findViewById(i + VIEW_ID_BASE);
			if (v == null || v.getVisibility() == View.GONE) continue;
			if (visible) {
				v.setVisibility(View.VISIBLE);
			} else {
				v.setVisibility(View.INVISIBLE);
			}
		}
	}

	private boolean large[] = new boolean []{ false, false, false, false,false, false, false, false}; 

	private void toggleLarge(View view) {
		int id = view.getId() - SenseMain.VIEW_ID_BASE;
		if (id < 0 || id > 7) return;
		large[id] ^= true;
		for (View tempView: valueViews) {
			if (view != tempView && tempView != null) {
				if (large[id])
					tempView.setVisibility(View.GONE);
				else
					tempView.setVisibility(View.VISIBLE);
			}
		}
		for (ViewGroup vg : vgs) {
			if (vg.getChildAt(0) == view || vg.getChildAt(1) == view) {

			} else {
				if (large[id]) {
					vg.setVisibility(View.GONE);
				} else if (vg.getChildCount() > 0){
					vg.setVisibility(View.VISIBLE);
				}
			}
		}

	}

	private void releaseMap() {
		if (map != null) {
			map.setVisibility(View.GONE);
			RelativeLayout view = (RelativeLayout) findViewById(R.id.main_rootlayout);
			view.removeView(map);
			map.removeAllViews();
			map = null;
			adjustViews();
		}

	}

	private View valueViews[] = new View[] { null, null, null, null, null, null, null, null};

	private void addView() {
		// ViewGroup vg = (ViewGroup) findViewById(R.id.values_layout);
		if (viewsAdded >= MAX_VIEWS_ADDED)
			return;

		ViewGroup vg;
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE || map != null) {
			switch (viewsAdded) {
			case 0:
			case 2:
				vg = (ViewGroup) findViewById(R.id.double1);
				break;
			case 1:
			case 3:
				vg = (ViewGroup) findViewById(R.id.double2);
				break;
			case 4:
			case 5:
				vg = (ViewGroup) findViewById(R.id.double3);
				break;
			case 6:
			case 7:
				vg = (ViewGroup) findViewById(R.id.double4);
				break;
			default:
				vg = (ViewGroup) findViewById(R.id.main_layout);
				break;
			}
		} else {
			switch (viewsAdded) {
			case 0:
			case 7:
				vg = (ViewGroup) findViewById(R.id.double1);
				break;
			case 1:
			case 6:
				vg = (ViewGroup) findViewById(R.id.double2);
				break;
			case 2:
			case 5:
				vg = (ViewGroup) findViewById(R.id.double3);
				break;
			case 3:
			case 4:
				vg = (ViewGroup) findViewById(R.id.double4);
				break;
			default:
				vg = (ViewGroup) findViewById(R.id.main_layout);
				break;
			}
		}

		final View view = LayoutInflater.from(this).inflate(
				R.layout.graph_view, vg, false);
		valueViews[viewsAdded] = view;

		if (Globals.testing.isTest()) {
			FitLabelView fit = (FitLabelView) view.findViewById(R.id.caption);
			fit.setPadding(0,0,0,6);
			fit.setHeightPercentage(0.24f);
		}
		view.setId(viewsAdded + VIEW_ID_BASE);
		vg.addView(view);
		View value = view.findViewById(R.id.value);

		addOnValueClick(value, VIEW_ID_BASE + viewsAdded);
		value.setOnTouchListener(new GestureTouchListener(this, view));
		viewsAdded++;
		adjustViews();

	}

	private void removeView() {
		if (viewsAdded == 0)
			return;

		View v = findViewById(VIEW_ID_BASE + --viewsAdded);
		if (v != null) { 
			ViewParent vp = v.getParent();
			if (vp instanceof ViewGroup) {
				((ViewGroup) vp).removeView(v);
				LLog.d(Globals.TAG, CLASSTAG + " View removed from viewgroup.");
				v.setVisibility(View.GONE);
			}
		}

		adjustViews();
	}

	//	private void showLocation() {
	//		/*
	//		 * http://developer.android.com/guide/appendix/g-app-intents.html
	//		 * geo:latitude,longitude geo:latitude,longitude?z=zoom
	//		 * geo:0,0?q=my+street+address geo:0,0?q=business+near+city
	//		 * google.streetview:cbll=lat,lng&cbp=1,yaw,,pitch,zoom&mz=mapZoom
	//		 */
	//		final ValueViewManager valueMgr = ValueViewManager.getInstance();
	//		if (valueMgr == null)
	//			return;
	//
	//		final Location loc = valueMgr.getLastLocation();
	//		if (loc == null) {
	//			ToastHelper.showToastLong(R.string.no_location);
	//			return;
	//
	//		}
	//		final String lat = Format.format(loc.getLatitude(), 8);
	//		final String lon = Format.format(loc.getLongitude(), 8);
	//
	//		final Uri uri = Uri.parse("geo:" + lat + "," + lon + "?z=15");
	//		final Intent intent = new Intent(Intent.ACTION_VIEW, uri, this, MapActivity.class); //new Intent(Intent.ACTION_VIEW, uri);
	//		final Values values = Values.getInstance();
	//		if (values == null)
	//			return;
	//
	//		final Value value = values.getValue(ValueType.LOCATION);
	//		if (value != null) {
	//			final List<XY> list = value.getQuickView();
	//			if (list != null && list.size() > 0) {
	//				final float[] ltt = new float[list.size()];
	//				final float[] lng = new float[list.size()];
	//
	//				int i = 0;
	//				for (final XY xy : list) {
	//					if (xy.getTag(0) == -1) {
	//						ltt[i] = xy.getY()[0];
	//						lng[i++] = xy.getY()[1];
	//					} else {
	//						ltt[i] = xy.getY()[1];
	//						lng[i++] = xy.getY()[0];
	//					}
	//				}
	//				//ToastHelper.showToastLong("Added " + i + " points to intent");
	//
	//				intent.putExtra("latitudes", ltt);
	//				intent.putExtra("longitudes", lng);
	//			}
	//		}
	//		startActivity(intent);
	//	}

	/*private void showStreetview() {
		final ValueManager valueMgr = ValueManager.getInstance();
		if (valueMgr == null)
			return;
		final Location loc = valueMgr.getLastLocation();
		if (loc == null) {
			ToastHelper.showToastLong(R.string.no_location);
			return;
		}
		final String lat = Util.format(loc.getLatitude(), 8);
		final String lon = Util.format(loc.getLongitude(), 8);

		final Uri uri = Uri.parse("google.streetview:cbll=" + lat + "," + lon
				+ "&cbp=1,0,,0,1.0&mz=15");
		startActivity(new Intent(Intent.ACTION_VIEW, uri));
	}
	 */

	private float dim = 0.5f;

	private void dimScreen(boolean up) {
		try {
			final int brightnessMode = Settings.System.getInt(
					getContentResolver(),
					Settings.System.SCREEN_BRIGHTNESS_MODE);

			if (brightnessMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
				Settings.System.putInt(getContentResolver(),
						Settings.System.SCREEN_BRIGHTNESS_MODE,
						Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
			}
		} catch (final SettingNotFoundException e) {
			LLog.e(Globals.TAG,
					CLASSTAG + ".dimScreen: Could not read settings", e);
		}

		final WindowManager.LayoutParams layoutParams = getWindow()
				.getAttributes();
		// float dim = layoutParams.screenBrightness;
		if (up) {
			dim *= 1.1f;
		} else {
			dim *= 0.9f;
		}
		if (dim > 1.0)
			dim = 1.0f;
		else if (dim < 0.01)
			dim = 0.01f;
		layoutParams.screenBrightness = dim;
		getWindow().setAttributes(layoutParams);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	private MenuItem menuItemz[] = null;

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (menu.size() < 2) super.onPrepareOptionsMenu(menu);
		menuItemz = new MenuItem[menu.size()];
		for (int i = 0; i < menu.size(); i++) {
			menuItemz[i] = menu.getItem(i);
		}
		//		if (SenseGlobals.activity.equals(SenseGlobals.ActivityMode.RUN)) {
		//			menuItemz[0].setVisible(false);
		//			menuItemz[1].setVisible(true);
		//		} else {
		//			menuItemz[0].setVisible(true);
		//			menuItemz[1].setVisible(false);
		//		}
		//		if (!Globals.isBikeApp) {
		//			MenuItem item = menuItems.get(MyMenu.MENU_STEPS);
		//			if (item != null) item.setVisible(false);
		//		}
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == R.id.action_startstop) {
			switch (SenseGlobals.activity) {
			case RUN:
				showPopupDialog(STOP_POPUP, VIEW_ID_BASE + viewsAdded - 1, null);
				return true;
			case REPLAY:
				startActivity(true);
				return true;
			default:
				showPopupDialog(START_POPUP, VIEW_ID_BASE + viewsAdded - 1, null); 
				return true;
			} 
		} else if (itemId == R.id.action_interval) {
			showPopupDialog(INTERVAL_POPUP, VIEW_ID_BASE + viewsAdded - 1, null);
			return true;
		} else if (itemId == R.id.action_settings) {
			startActivity(new Intent(this, SettingsActivity.class));
			return true;
		} else if (itemId == R.id.action_share) {
			if (SenseGlobals.isBikeApp) 
				showPopupDialog(SHARE_POPUP, VIEW_ID_BASE + viewsAdded - 1, null);
			else
				sendMail(false);
			return true;
		} else if (itemId == R.id.action_gpx) {
			this.getGPX();
			return true;
		} else if (itemId == R.id.action_history) {
			if (SenseGlobals.activity.equals(SenseGlobals.ActivityMode.RUN)) 
				warnStopActivityAtOpenHistory(FileExplorer.ACTIVITY_TAG); 
			else 
				openHistory(FileExplorer.ACTIVITY_TAG);
			return true;
		}
		//		} else if (itemId == R.id.action_ble) {
		//			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
		//				startActivity(new Intent(this, BeatzActivity.class));
		//			else
		//				warnNoBle();
		//			return true;
		//		}
		return super.onOptionsItemSelected(item);
	}

	//	private void warnNoBle() {
	//		AlertDialog alertDialog = new AlertDialog.Builder(this).create();  
	//		alertDialog.setTitle(R.string.warn_ble_not_available_title);  
	//		alertDialog.setMessage(getString(R.string.warn_ble_not_available_msg));  
	//		alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.button_confirm), new DialogInterface.OnClickListener() {  
	//			public void onClick(DialogInterface dialog, int which) {  
	//				return;  
	//			} });
	//		alertDialog.show();   
	//	}


	private long newIntervalPressed = 0;

	private void newInterval() {
		if (System.currentTimeMillis() - newIntervalPressed < 5000) return;
		if (allowHandWake) unlockScreen();
		newIntervalPressed = System.currentTimeMillis();
		if (SenseGlobals.activity.isRun()) { 
			Laps.newLap(System.currentTimeMillis());
			if (Laps.getLaps() >= 2) new LapsDialog(this, Laps.getLaps() - 2, true).show();
		} else 
			startActivity(true);
	}

	private void sendMail(boolean forceNewContents) {
		switch (SenseGlobals.activity) {
		case REPLAY: new SendMail(this, this, Value.getSummary(this), SenseGlobals.replaySession, false, this, forceNewContents); break;
		case RUN: new SendMail(this, this, Value.saveTotals(), Value.getSessionString(), true, this, forceNewContents); break;
		case STOP: new SendMail(this, this, Value.getSummary(), Value.getSessionString(), false, this, forceNewContents); break;
		}
	}

	private void sendTP() {
		if (PwxLog.getInstance() == null) return;
		//		if (Globals.activity.equals(Globals.ActivityMode.STOP)) {
		//			new AlertDialog.Builder(SensMain.this)
		//			.setTitle(R.string.trainingpeaks_upload_failed)  
		//			.setMessage(R.string.tp_result_nofile)
		//			.setPositiveButton(R.string.button_confirm, new DialogInterface.OnClickListener() {
		//				@Override
		//				public void onClick(DialogInterface dialog, int whichButton)  {												
		//					if (dialog != null) dialog.dismiss();
		//
		//				}
		//			})
		//			.create().show();  
		//			return;
		//		}
		String user = PreferenceKey.TPUSER.getString();
		if (user.equals(PreferenceDefaults.DEFAULTTPUSER)) 
			this.warnNoPassword(0);
		else
			new TrainingPeaksHttpUpload(new TrainingPeaksHttpUpload.Listener() {

				@Override
				public void onResult(final Result result) {
					new SleepAndWake(new SleepAndWake.Listener() {
						@Override
						public void onWake() {
							TextView v = (TextView) findViewById(R.id.status_view);
							v.setVisibility(View.GONE);
							if (result.success) {
								//								new AlertDialog.Builder(SensMain.this)
								//								.setTitle(R.string.trainingpeaks_upload_success)  
								//								.setMessage(getString(result.msg, String.valueOf(result.bytes)))
								//								.setPositiveButton(R.string.button_confirm, new DialogInterface.OnClickListener() {
								//									@Override
								//									public void onClick(DialogInterface dialog, int whichButton)  {												
								//										if (dialog != null) dialog.dismiss();
								//
								//									}
								//								})
								//								.create().show();  
							} else {
								new AlertDialog.Builder(SenseMain.this)
								.setTitle(R.string.trainingpeaks_upload_failed)  
								.setMessage(getString(result.msg, result.details))
								.setPositiveButton(R.string.button_confirm, new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int whichButton)  {												
										if (dialog != null) dialog.dismiss();
									}
								})
								.create().show();
							}
						}
					}, 2000);
				}

				@Override
				public void onUpdate(String value) {
					TextView v = (TextView) findViewById(R.id.status_view);
					v.setVisibility(View.VISIBLE);
					v.setText(value);

				}
			}).doSend();
	}

	private void nextValue(int viewId) {
		if (!viewHasValue(viewId)) {
			showMultiChoiceDialog(VALUESELECT_POPUP, viewId);
			//showPopupDialog(VALUESELECT_POPUP, viewId, null); 
		} else if (viewGetNextValue(viewId) == null) {	
			Value value = viewGetCurrentValue(viewId);
			if (value.hasRegistrations()) {
				value = Value.getNextValue(value, PreferenceKey.XTRAVALUES.isTrue());
				viewSetValue(viewId, value);
			} //else 
			//	showMultiChoiceDialog(VALUESELECT_POPUP, viewId);
		}
	}

	private void prevValue(int viewId) {
		if (!viewHasValue(viewId)) {
			showMultiChoiceDialog(VALUESELECT_POPUP, viewId);
			//showPopupDialog(VALUESELECT_POPUP, viewId, null); 
		} else if (viewGetPrevValue(viewId) == null)  {
			Value value = viewGetCurrentValue(viewId);
			if (value.hasRegistrations()) {
				value = Value.getPreviousValue(value, PreferenceKey.XTRAVALUES.isTrue());
				viewSetValue(viewId, value);
			} //else 
			//	showMultiChoiceDialog(VALUESELECT_POPUP, viewId);
		}
	}

	public class GestureTouchListener implements OnTouchListener {
		private GestureDetector gestureDetector;

		public GestureTouchListener(final Context context, final View view ) {
			gestureDetector = new GestureDetector(context, new SimpleOnGestureListener() {
				@Override
				public boolean onDoubleTap(MotionEvent e) {
					LLog.d(Globals.TAG, CLASSTAG + ".GestureTouchListener.onDoubleTap: " + e.getX() + ", " + e.getY() + " (" + view.getWidth() + ", " + view.getHeight() + ")");
					//					if (PreferenceKey.XTRAVALUES.isTrue()) {
					//						int viewId = view.getId();
					//						final ValueViewManager valueMgr = ValueViewManager.getInstance();
					//						if (valueMgr != null) {
					//							final Value value = valueMgr.viewGetCurrentValue(viewId);
					//							if (Unit.getValidUnitsCount(value.getValueType().getUnitType()) > 1)
					//								value.nextUnit();
					//							//else
					//							//ToastHelper.showToastLong(R.string.one_unit_only);
					//						}
					//					} else {
					int viewId = view.getId();
					showPopupDialog(VALUE_POPUP, viewId, null); // , 0);
					//					}
					return super.onDoubleTap(e);
				}

				@Override
				public void onLongPress(MotionEvent e) {
					LLog.d(Globals.TAG, CLASSTAG + ".GestureTouchListener.onLongPress: " + e.getX() + ", " + e.getY() + " (" + view.getWidth() + ", " + view.getHeight() + ")");
					int viewId = view.getId();
					showPopupDialog(VALUE_POPUP, viewId, null); // , 0);
					super.onLongPress(e);
				}



				@Override
				public boolean onSingleTapConfirmed(MotionEvent e) {
					LLog.d(Globals.TAG, CLASSTAG + ".GestureTouchListener.onSingleTapConfirmed: " + e.getX() + ", " + e.getY() + " (" + view.getWidth() + ", " + view.getHeight() + ")");
					int viewId = view.getId();
					float margin = 0.5f;					
					if (clickSwitch) {
						if (e.getX() >= (1f - margin) * view.getWidth()) {
							LLog.d(Globals.TAG, CLASSTAG + ": Click left");
							nextValue(viewId);
							return true;   

						} else if (e.getX() <= margin * view.getWidth()) {
							LLog.d(Globals.TAG, CLASSTAG + ": Click right");
							prevValue(viewId);
							return true;
						}

					} 
					toggleLarge(view);
					return super.onSingleTapConfirmed(e);
				}

				@Override
				public boolean onSingleTapUp(MotionEvent e) {
					return super.onSingleTapUp(e);
				}

				@Override
				public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
					//if (e1.getX() - e2.getX() > LARGE_MOVE) showLocation();
					return false;
				}
			});	
		}


		@TargetApi(Build.VERSION_CODES.HONEYCOMB)
		@Override
		public boolean onTouch(View view, MotionEvent event) {
			if (Build.VERSION.SDK_INT >= 11) {
				ActionBar bar = getActionBar();
				if (bar != null) bar.show();
			}
			menuVisibleClicked = System.currentTimeMillis();
			if (gestureDetector != null) return gestureDetector.onTouchEvent(event);
			return true;
		}
	};

	private void finishIt() {
		LLog.d(Globals.TAG, CLASSTAG + ".finishIt");
		ToastHelper.showToastLong(getString(R.string.app_stopped, Globals.TAG));
		ActivityUtil.unlockScreenOrientation(this);
		finish();	
	}

	@Override
	public void onBackPressed() {
		Value.serializeValues();

		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(getString(R.string.app_quitmessage, Globals.TAG))
		.setCancelable(true)
		.setPositiveButton(R.string.button_yes, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				if (!SenseGlobals.isBikeApp) 
					finishIt();
				else if (SenseGlobals.activity.equals(SenseGlobals.ActivityMode.RUN)) 
					showPopupDialog(FULLSTOP_POPUP, VIEW_ID_BASE + viewsAdded - 1, null);
				else
					finishIt();
			}
		})
		.setNegativeButton(R.string.button_no, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
			}
		})
		/*.setNeutralButton(R.string.button_quit_and_mail, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				//app.prepareFinish(CLASSTAG + ".onBackPressed");
				if (prefs.isMailInternal())
					mailsInProcess = new SendGMail(mailer, SensMain.this, SensMain.this, EXIT_MESG).send();
				else
					new SendMail(SensMain.this, SensMain.this, EXIT_MESG).send();
				//WindSensActivity.this.moveTaskToBack(true);
			}
		})*/.show();

	}

	@Override
	public View[] onRequestViews() {
		return new View[] { findViewById(com.plusot.senselib.R.id.main_layout) };
	}

	private int lastAction = 0;
	private int lastKeyCode = 0;
	private int sameActionKeyCode = 0;

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		final int action = event.getAction();
		final int keyCode = event.getKeyCode();
		boolean result = false;

		if ((lastAction == action && lastKeyCode == keyCode)) {
			switch (keyCode) {
			case KeyEvent.KEYCODE_CAMERA:
				newInterval();
				break;
			case KeyEvent.KEYCODE_POWER:
				//if (action == KeyEvent.)
				LLog.d(Globals.TAG, CLASSTAG + " Power button pressed");
				break;
			case KeyEvent.KEYCODE_VOLUME_UP:
				if (action == KeyEvent.ACTION_DOWN) {
					dimScreen(true);
				}
				result = true;
				break;
			case KeyEvent.KEYCODE_VOLUME_DOWN:
				if (action == KeyEvent.ACTION_DOWN) {
					dimScreen(false);
				}
				result = true;
				break;
			}
		} else {
			AudioManager mgr = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
			switch (keyCode) {
			case KeyEvent.KEYCODE_CAMERA:
				newInterval();
				break;
			case KeyEvent.KEYCODE_VOLUME_UP:
				if (sameActionKeyCode < 2 && lastAction == KeyEvent.ACTION_DOWN && action == KeyEvent.ACTION_UP && lastKeyCode == KeyEvent.KEYCODE_VOLUME_UP) {
					mgr.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI | AudioManager.FLAG_PLAY_SOUND);
				}
				result = true;
				break;
			case KeyEvent.KEYCODE_VOLUME_DOWN:
				if (sameActionKeyCode < 2 && lastAction == KeyEvent.ACTION_DOWN && action == KeyEvent.ACTION_UP && lastKeyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
					mgr.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI | AudioManager.FLAG_PLAY_SOUND);		
				}
				result = true;
				break;
			}
		}
		if (!result) {
			// LLog.d(Globals.TAG, CLASSTAG +
			// ".distpatchKeyEvent: Calling super with " + delta);
			result = super.dispatchKeyEvent(event);
		}
		if (lastAction == action && lastKeyCode == keyCode) {
			sameActionKeyCode++;
		} else
			sameActionKeyCode = 0;

		lastKeyCode = keyCode;
		lastAction = action;
		return result;

	}

	@Override
	public boolean onDispatchKeyEvent(KeyEvent event) {
		return dispatchKeyEvent(event);
	}

	private Dialog mSplashDialog = null;
	private int popupToShow = -1;

	protected void removeSplashScreen() {
		if (mSplashDialog != null) try {
			mSplashDialog.dismiss();
			mSplashDialog = null;
		} catch (IllegalArgumentException e) {
			LLog.e(Globals.TAG, CLASSTAG + ".removeSplashScreen: Can not dismiss dialog", e);
		}
	}

	private void startUpDialogs(String tag) {
		LLog.d(Globals.TAG, CLASSTAG + ".startUpDialogs: Started by " + tag);
		if (SenseGlobals.isBikeApp) {
			if (!SenseGlobals.argos && SenseGlobals.stopState.equals(SenseGlobals.StopState.PAUZE)) {
				View view = this.findViewById(R.id.main_layout);
				if (view != null) view.setVisibility(View.VISIBLE);
				startActivity(false);
			} else {
				step = SenseGlobals.argos ? 0 : 1;
				if (step == 1 && PreferenceKey.XTRAVALUES.isTrue()) step = 2;
				if (alive) 
					showPopupDialog(STEP_POPUP, VIEW_ID_BASE + viewsAdded - 1, null);
				else 
					popupToShow  = STEP_POPUP;
			}

		} else {
			checkBluetooth();
		} 

	}

	private void checkGPS() {
		if (PreferenceKey.GPSON.isTrue()) {
			final LocationManager manager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );

			if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
				startUpDialogs("checkGPS.providerEnabled");
				return;
			}
			final AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.dialog_gps_on)
			.setCancelable(true)
			.setPositiveButton(R.string.button_yes, new DialogInterface.OnClickListener() {
				public void onClick(final DialogInterface dialog, final int id) {
					startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), RESULT_GPS);
				}
			})
			.setNegativeButton(R.string.button_no, new DialogInterface.OnClickListener() {
				public void onClick(final DialogInterface dialog, final int id) {
					//dialog.cancel();
					startUpDialogs("checkGPS.dialog.negativeButton");
				}
			})
			.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface arg0) {
					startUpDialogs("checkGPS.dialog.cancel");
				}
			});
			final AlertDialog alert = builder.create();
			alert.show();
		} else
			startUpDialogs("checkGPS.gpsOff");
	}

	@Override
	protected void onActivityResult(int request, int result, Intent intent) {
		switch (request) {
		case RESULT_GPS: 
			if (result >= 0) new SleepAndWake(new SleepAndWake.Listener() {
				@Override
				public void onWake() {
					startUpDialogs("onActivityResult,RESULT_GPS"); 
				}
			}, 100);
			break; 
		case RESULT_TIMESET: 
			if (result >= 0) new SleepAndWake(new SleepAndWake.Listener() {
				@Override
				public void onWake() {
					checkGPS(); }
			}, 100);
			break;
		case RESULT_STEP1: 
			LLog.d(Globals.TAG, CLASSTAG + ".onActivityResult: " + result);
			if (result >= 0) new SleepAndWake(new SleepAndWake.Listener() {
				@Override
				public void onWake() {
					step = 1;
					showPopupDialog(STEP_POPUP, VIEW_ID_BASE + viewsAdded - 1, null); 
				}
			}, 100);
			//showPopupDialog(STEP1SETTINGS_POPUP, VIEW_ID_BASE + viewsAdded - 1, null); 
			break;
		case RESULT_BLUETOOTH:
			BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
			if (adapter != null && adapter.getState() != BluetoothAdapter.STATE_OFF) {
				BTManager kestrel = (BTManager) Manager.managers.get(ManagerType.BLUETOOTH_MANAGER);
				if (kestrel != null) kestrel.init();
				startActivity(true);
			}
			break;
		}
		super.onActivityResult(request, result, intent);
	}

	/**
	 * Shows the splash screen over the full Activity
	 */
	protected void showSplashScreen(boolean startApp) {
		mSplashDialog = new SplashDialog(this, false);
		mSplashDialog.show();

		if (startApp) {
			final Handler handler = new Handler();
			handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					//					while (creating) try {
					//						Thread.sleep(1000);
					//					} catch (InterruptedException e) {
					//					}
					removeSplashScreen();
					//enableAnt();

					new SleepAndWake(new SleepAndWake.Listener() {

						@Override
						public void onWake() {
							if (System.currentTimeMillis() < TimeUtil.MIN_TIME) {
								startActivityForResult(new Intent(android.provider.Settings.ACTION_DATE_SETTINGS), RESULT_TIMESET);
							} else
								checkGPS();

						}
					}, 1000);
				}
			}, 4000);
		}
	}

	private void warnStopActivityAtOpenHistory(final int tag) {
		new AlertDialog.Builder(SenseMain.this)
		.setTitle(R.string.warn_stopactivity_title)  
		.setMessage(R.string.warn_stopactivity_msg)
		.setPositiveButton(R.string.button_yes, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton)  {												
				if (dialog != null) dialog.dismiss();
				openHistory(tag);
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

	private void warnNoPassword(final int tag) {
		warn(R.string.warn_nopassword_title,R.string.warn_nopassword_msg);
	}

	private void warnNoFit() {
		warn(R.string.warn_nofit_title,R.string.warn_nofit_msg);
	}

	private void warnAccount() {
		warn(R.string.warn_noaccount_title,R.string.warn_noaccount_msg);
	}

	private void warnNoFilesToday() {
		warn(R.string.warn_nofitfiles_title,R.string.warn_nofitfiles_msg);
	}

	private void warn(final int titleId, final int msgId) {
		new AlertDialog.Builder(SenseMain.this)
		.setTitle(titleId)  
		.setMessage(msgId)
		.setPositiveButton(R.string.button_yes, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton)  {												
				if (dialog != null) dialog.dismiss();
				startActivity(new Intent(SenseMain.this, ShareSettingsActivity.class));
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

	private void openHistory(final int tag) {
		fileExplorer = new FileExplorer(Globals.getDataPath(), DataLog.SPEC, DataLog.EXT, tag);
		showPopupDialog(YEAR_POPUP, VIEW_ID_BASE + viewsAdded - 1, null); 	
	}


	private void getGPX() {
		showPopupDialog(GPX_POPUP, VIEW_ID_BASE + viewsAdded - 1, null); 	
	}

	private void getGPX(final String path) {
		//final String path = SenseGlobals.getGpxPath() + "test.gpx";
		new GpxReader(new GpxReader.Listener() {

			@Override
			public void onPoints(List<Point> list) {
				if (map != null) map.addGpx(list);

			}

			@Override
			public void onFailure(ResultType result) {
				ToastHelper.showToastLong(getString(R.string.failed_to_read, path));

			}

		}, path);
	}


	private void warnNoSpeech() {
		warn(R.string.warn_nospeech_title, R.string.warn_nospeech_msg);
	}

	private void checkBluetooth() {
		BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
		if (adapter != null && adapter.getState() != BluetoothAdapter.STATE_OFF) {
			startActivity(true);
			return;
		}
		new AlertDialog.Builder(SenseMain.this)
		.setTitle(R.string.warn_nobluetooth_title)  
		.setMessage(R.string.warn_nobluetooth_msg)
		.setPositiveButton(R.string.button_yes, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton)  {												
				if (dialog != null) dialog.dismiss();
				startActivityForResult(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS), RESULT_BLUETOOTH);	
			}
		})
		.setNegativeButton(R.string.button_no, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				if (dialog != null) dialog.dismiss();
				startActivity(true);
			}
		})
		.setOnCancelListener(new OnCancelListener() {

			@Override
			public void onCancel(DialogInterface dialog) {
				startActivity(true);
			}	
		})
		.create().show();  
	}

	private void pauseActivity(String caller) {
		LLog.d(Globals.TAG, CLASSTAG + ".stopActivity called by " + caller);
		SenseGlobals.activity = SenseGlobals.ActivityMode.STOP;
		DataLog log = DataLog.getInstance();
		if (log != null) log.cancelReadTask();
		Value.saveTotals();
		PreferenceKey.setStopState(SenseGlobals.StopState.PAUZE);
		Device.devicesToZero();
		Value.updateValues();
	}

	private void closeValues(SenseGlobals.ActivityMode prevMode, boolean closeApplication) {
		if(prevMode.equals(SenseGlobals.ActivityMode.RUN)) {
			Value.saveTotals();
			new ZipFiles(this, this).zip(true, closeApplication, !SenseGlobals.argos);
		} else if (SenseGlobals.stopState.equals(SenseGlobals.StopState.PAUZE)) {
			new ZipFiles(this, this).zip(true, closeApplication, !SenseGlobals.argos);
		}
		PreferenceKey.setStopState(SenseGlobals.StopState.STOP);
		Value.clearAllValues();

	}

	private void stopActivity(String caller, boolean closeApplication) {
		LLog.d(Globals.TAG, CLASSTAG + ".stopActivity called by " + caller);
		//Debug.stopMethodTracing();
		SenseGlobals.ActivityMode prevMode = SenseGlobals.activity;
		SenseGlobals.activity = SenseGlobals.ActivityMode.STOP;
		DataLog log = DataLog.getInstance();
		if (log != null) log.cancelReadTask();
		closeValues(prevMode, closeApplication);
		PreferenceKey.setStopState(SenseGlobals.StopState.STOP);
		Value.updateValues();

		if (SenseGlobals.argos) {
			argosStopStep = 0;
			shouldFinish = closeApplication ? 1 : 0;
			showPopupDialog(ARGOSSTOP_POPUP, VIEW_ID_BASE + viewsAdded - 1, null);
		} else if (closeApplication)
			finishIt();
		//new BatteryInfo();
	}

	private void startActivity(boolean newStart) {
		//ActivityUtil.lockScreenOrientation(this);
		//Debug.startMethodTracing();
		SenseGlobals.ActivityMode prevMode = SenseGlobals.activity;
		DataLog log = DataLog.getInstance();

		if (log != null) log.cancelReadTask();
		if (newStart) {
			//			ValueViewManager mgr = ValueViewManager.getInstance();
			//			AntDeviceManager antManager = (AntDeviceManager) mgr.getManager(ManagerType.ANT_MANAGER);		
			//			antManager.reset();
			//			LLog.d(Globals.TAG, CLASSTAG + " ANT Manager reset called !!!! !!!!");
			closeValues(prevMode, false);
			Value.newSession();
			HttpSender.newSession();
			HttpSender sender = HttpSender.getInstance();
			if (sender != null && PreferenceKey.HTTPPOST.isTrue()) sender.sendSession();
		} else {
			Device.devicesToZero();
		}
		PreferenceKey.setStopState(SenseGlobals.StopState.PAUZE);
		SenseGlobals.activity = SenseGlobals.ActivityMode.RUN;
		Value.updateValues();	
	}

	private long lastAskForActivityStart = 0;
	private boolean calibrationAsked = false;
	private long lowSaved = 0; 

	public void onDeviceValueChanged(DeviceStub device, ValueType valueType, ValueItem valueItem, boolean isNew) {
		long now = System.currentTimeMillis();

		if (valueType.handleAsValue()) {
			Value.getValue(valueType, true).onDeviceValueChanged(device, valueItem, isNew);
			if (isNew && valueType.isChatable() && device.getDeviceType() != DeviceType.BTCHAT) try {
				BTManager mgr = ((BTManager) Manager.managers.get(ManagerType.BLUETOOTH_MANAGER));
				if (mgr != null) mgr.write(valueType.toJSON(valueItem).toString());
			} catch (JSONException e) {
				LLog.e(Globals.TAG, CLASSTAG + " Could not convert value to JSON", e);
			}
		}
		switch (valueType) {
		case LOCATION:
			//LLog.d(Globals.TAG, CLASSTAG + ".onDeviceValueChanged: Location");
			if (map == null) break;
			Value locValue = Value.getValue(ValueType.LOCATION, false);
			if (locValue != null) map.setPath(locValue.getQuickView());
			break;
		case SPEED: 
			if (stepsDone && SenseGlobals.activity.equals(SenseGlobals.ActivityMode.STOP) && valueItem.getDoubleValue() > 2.0 && now - lastAskForActivityStart > 60000) {
				showPopupDialog(MOVESTART_POPUP, VIEW_ID_BASE + viewsAdded - 1, null);
				lastAskForActivityStart = now;
			}
			break;
		case POWER: 
			if (isNew && !calibrationAsked && device.getDeviceType() != DeviceType.BTCHAT) {
				calibrationAsked = true;
				calibrateDialog = new AlertDialog.Builder(SenseMain.this)
				.setTitle(R.string.dialog_calibrate_title)  
				.setMessage(R.string.dialog_calibrate_msg)
				.setPositiveButton(R.string.button_yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int whichButton)  {												
						if (dialog != null) dialog.dismiss();
						calibrate();
						calibrateDialog = null;
					}
				})
				.setNegativeButton(R.string.button_no, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int whichButton) {
						if (dialog != null) dialog.dismiss();
						calibrateDialog = null;
					}
				})
				.setOnCancelListener(new OnCancelListener() {
					@Override
					public void onCancel(DialogInterface arg0) {
						calibrateDialog = null;		
					}

				})
				.create();
				calibrateDialog.show();  
				new SleepAndWake(new SleepAndWake.Listener() {
					@Override
					public void onWake() {
						if (calibrateDialog != null) calibrateDialog.dismiss();
						calibrateDialog = null;
					}

				}, 10000);
			}
			break;
		case PROXIMITY:
			if (allowHandWake) unlockScreen();
			break;
		case VOLTAGE:
			if (valueItem.getDoubleValue() < 0.04 && System.currentTimeMillis() - lowSaved > 300000 && SenseGlobals.activity.equals(SenseGlobals.ActivityMode.RUN)) {
				new ZipFiles(this, this).zip(false, false, true);
				lowSaved = System.currentTimeMillis();
			} else if (Globals.testing.isTest()) {
				TextView v = (TextView) findViewById(R.id.status_view);
				v.setVisibility(View.VISIBLE);
				battText = "batt " + Format.format(valueItem.getDoubleValue(), 1) + "%, " + Format.format(AndroidSensorManager.batteryUsage, 1) + "%/hr";
				if (procText == null) 
					v.setText(battText);
				else
					v.setText(procText + ", " + battText);

			}

		default:

		}
	}

	private String battText = null;
	private String procText = null;

	@SuppressWarnings("deprecation")
	private void unlockScreen() {
		if (Globals.testing.isVerbose()) LLog.d(Globals.TAG, CLASSTAG + ".unLockScreen");
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
		final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		final PowerManager.WakeLock wl = pm.newWakeLock((PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE), Globals.TAG +"_WakeLock2");
		wl.acquire();
		new SleepAndWake(new SleepAndWake.Listener() {
			@Override
			public void onWake() {
				wl.release();
			}
		}, 2000);

	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private void hideMenu() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) return;
		if (menuVisibleClicked == 0) return;
		LLog.d(Globals.TAG, CLASSTAG + ".hideMenu: Hiding menu");

		menuVisibleClicked = 0;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && Globals.testing.isTest())
			getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
		ActionBar bar = this.getActionBar();
		if (bar != null) bar.hide();

	}

	private boolean allowHandWake = true;

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public void onWatchdogCheck(long count) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) return;
		if (menuVisibleClicked > 0 && System.currentTimeMillis() - menuVisibleClicked > 5000) {
			LLog.d(Globals.TAG, CLASSTAG + ".onCheck: Hiding menu");
			hideMenu();
		}

	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		LLog.d(Globals.TAG, CLASSTAG + " Preference changed: " + key); // + "\n"
		PreferenceKey prefKey = PreferenceKey.fromString(key);
		if (prefKey != null) updateSetting(sharedPreferences, prefKey, false);
	}

	public void updateSettings() {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(Globals.appContext);
		for (PreferenceKey key : PreferenceKey.values())
			updateSetting(prefs, key, true);
	}

	@SuppressLint("Wakelock")
	@SuppressWarnings("deprecation")
	public void updateSetting(SharedPreferences prefs, PreferenceKey key, boolean firstTime) {
		switch (key) {
		case AUTOSWITCH:
			if (key.getInt() > 1800000) {
				PreferenceKey.CLICKSWITCH.set(true);
			}
			break;
		case CLICKSWITCH:	
			clickSwitch = key.isTrue();
			break;
		case DIM:
			if (wakeLock != null && wakeLock.isHeld()) {
				wakeLock.release();
				wakeLock = null;
			}
			final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			allowHandWake  = false;
			switch (PreferenceKey.getDimmable()) {
			case SCREENOFF_WAKE:
				allowHandWake = true;
			case SCREENOFF_FULL:
				getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
				wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, Globals.TAG +"_WakeLock");
				wakeLock.acquire();
				break;
			case NODIM:
				getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);	
				break;
			case DIM:
			default:
				allowHandWake = true;
				getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
				wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, Globals.TAG +"_WakeLock");
				wakeLock.acquire();
				break;
			}
			break;
		case DISPLAYNAME:
			UserInfo.UserData user = UserInfo.getUserData();
			if (user == null) return;
			key.getString(user.name);
			break;
		case HASMAP: 
			new SleepAndWake(new SleepAndWake.Listener() {

				@Override
				public void onWake() {
					if (PreferenceKey.HASMAP.isTrue()) {
						addMap();
						adjustViews();
					} else {
						releaseMap();
						if (!visible) toggleValuesVisible();
					}
				}
			}, 500);

			break;
		case HTTPPOST:
			HttpSender.isHttpPost = key.isTrue();
			break;
		case STOPSTATE:
			PreferenceKey.getStopState();
			break;
		case STORAGE:
			Value.fileTypes = PreferenceKey.getStorageFiles();
			break;
		case TESTING:
			PreferenceKey.isTesting();
			break;
		case WHEELSIZE:
		case WHEELCIRC:
			PreferenceKey.getWheelCirc();

			break;
		case XTRAVALUES:
			key.isTrue();
			break;
		case IMEI:
			UserInfo.resetDeviceId();
			break;
		case TILESOURCE:
			key.get();
			if (!firstTime) {
				if (map != null) {
					RelativeLayout view = (RelativeLayout) findViewById(R.id.main_rootlayout);
					view.removeView(map);
					map.removeAllViews();
					map.setOnTouchListener((ClickableOverlay.Listener)null);
					map = null;
				}

				addMap();
				//ToastHelper.showToastLong(R.string.change_tilesource);
			}
			break;
		case BLE_DEVICE:
			key.get();
			break;
		default:
			key.get();
		}
	}


	@Override
	public void onTopUpdate(ProcessInfo info) {
		procText = "cpu = " + info.cpu + "%, idle = " + info.idle + "%";
		if (Globals.testing.isTest()) {
			TextView v = (TextView) findViewById(R.id.status_view);
			v.setVisibility(View.VISIBLE);
			if (battText == null) 
				v.setText(procText);
			else
				v.setText(procText + ", " + battText);
		}

	}

	@Override
	public void onMailComplete() {
		LLog.d(Globals.TAG, CLASSTAG + ".onMailComplete: shouldFinish = " + shouldFinish);
		if (shouldFinish > 1) finishIt();

	}

	private boolean viewHasValue(final int viewId) {
		if (views.get(viewId) == null) return false;
		return views.get(viewId).hasValues();
	}

	private boolean viewHasValue(Value value) {
		if (value == null) return false;
		for (int i = 0; i < views.size(); i++) {
			ValueView view = views.valueAt(i);
			if (view == null) continue;
			if (view.hasValue(value)) return true;
		}
		return false;
	}

	private Value viewGetCurrentValue(final int viewId) {
		if (!viewHasValue(viewId)) return null;
		return views.get(viewId).getCurrentValue();
	}

	private Value viewGetNextValue(final int viewId) {
		if (!viewHasValue(viewId)) return null;
		return views.get(viewId).nextValue();
	}

	private Value viewGetPrevValue(final int viewId) {
		if (!viewHasValue(viewId)) return null;
		return views.get(viewId).prevValue();
	}

	private EnumSet<ValueType> viewGetValues(final int viewId) {
		if (!viewHasValue(viewId)) return null;
		return views.get(viewId).getValueTypes();
	}

	private void viewSetValue(int viewId, Value value) { 
		if (value == null) return;
		ValueView view = views.get(viewId);
		if (view == null) {	
			view = new ValueView(this, viewId); 
			views.put(viewId, view);
		} 

		LLog.d(Globals.TAG, CLASSTAG + " Value view " + viewId + " value set: " + value.getValueType(this));
		view.setValue(value);
		value.update(); //, ChangeFlag.ON_SELECTFIELD);
	}

	private void viewAddValue(int viewId, Value value) { 
		//DataView obj = 
		if (value == null) return;
		ValueView view = views.get(viewId);
		if (view == null) {	
			view = new ValueView(this, viewId); 
			views.put(viewId, view);
		} else if (view.hasValue(value))
			return;

		//LLog.d(Globals.TAG, CLASSTAG + " Value view " + viewId + " value added: " + value.getValueType(activity));
		view.addValue(value);
		value.update(); //, ChangeFlag.ON_SELECTFIELD);
	}

	private void viewRemoveValue(int viewId, Value value) { 
		if (value == null) return;
		ValueView view = views.get(viewId);
		if (view == null || !view.hasValue(value)) return;

		LLog.d(Globals.TAG, CLASSTAG + " Value view " + viewId + " value removed: " + value.getValueType(this));
		view.close(value);
		Value temp = view.getCurrentValue();
		if (temp != null) temp.update(); //, ChangeFlag.ON_NEXTFIELD);

	}


	@Override
	public void onWatchdogClose() {
	}

	@Override
	public Activity getActivity() {
		return this;
	}


	@Override
	public void onStreamFinished() {
		Value.updateValues();		
	}


	@Override
	public void onDeviceLost() {
		// TODO Auto-generated method stub

	}


}