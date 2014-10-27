package com.plusot.senselib.bluetooth;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

import com.plusot.bluelib.ble.Ble;
import com.plusot.bluelib.ble.Ble.ConnectState;
import com.plusot.bluelib.ble.Ble.GattAction;
import com.plusot.bluelib.ble.BleData;
import com.plusot.bluelib.ble.BleDataType;
import com.plusot.common.Globals;
import com.plusot.common.settings.PreferenceHelper;
import com.plusot.common.share.LLog;
import com.plusot.common.util.SleepAndWake;
import com.plusot.common.util.ToastHelper;
import com.plusot.senselib.R;
import com.plusot.senselib.settings.PreferenceKey;
import com.plusot.senselib.values.Manager;
import com.plusot.senselib.values.ValueType;


@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BleManager implements Manager, Ble.Listener, OnSharedPreferenceChangeListener {
	private static final String CLASSTAG = BleManager.class.getSimpleName();
	private static final long SCAN_PERIOD = 10000;
	private Ble ble = null;
	private Handler handler;
	private BluetoothAdapter bluetoothAdapter;
	private BleDevice bleDevice;
	private BluetoothAdapter.LeScanCallback leScanCallback = null;

	
	public BleManager() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) return;
		handler = new Handler();
		ble = Ble.getInstance();
		ble.addListener(this);
		PreferenceHelper.getPrefs().registerOnSharedPreferenceChangeListener(this);
		updateSettings();
		leScanCallback = new BluetoothAdapter.LeScanCallback() {
			@Override
			public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
				Log.d(Globals.TAG, CLASSTAG + ".onLeScan found " + device.getName() + " " + device.getAddress());
				if (ble != null && (Ble.paired.size() == 0 || Ble.paired.containsKey(device.getAddress()))) {
					PreferenceKey.BLE_DEVICE.addStringToSet(device.getAddress() + '|' + device.getName());
					if (ble.getConnectState(device.getAddress()) == ConnectState.STATE_DISCONNECTED) {
						final boolean result = ble.connect(device.getAddress());
						Log.d(Globals.TAG, CLASSTAG + ".onLeScan Connect request result for " + device.getAddress() + " = " + result);
					}
				}
			}
		};
	}

	private BleDevice getBleDevice() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) return null;
		if (bleDevice == null) {
			bleDevice = new BleDevice();
			//Device.addDevice(bleDevice);
		}
		return bleDevice;
	}

	@Override
	public boolean init() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) return false;
		LLog.d(Globals.TAG, CLASSTAG + ".init");
	
		handler = new Handler();
		if (!Globals.appContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			ToastHelper.showToastLong(R.string.ble_not_supported);
			return false;
		}

		ble = Ble.getInstance();
		ble.addListener(this);

		final BluetoothManager bluetoothManager = (BluetoothManager) Globals.appContext.getSystemService(Context.BLUETOOTH_SERVICE);
		bluetoothAdapter = bluetoothManager.getAdapter();

		updateSettings();
		PreferenceHelper.getPrefs().registerOnSharedPreferenceChangeListener(this);
		return true;
	}

	@Override
	public void onStart() {
		Log.d(Globals.TAG, CLASSTAG + ".onStart");
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) return;
		
		if (!bluetoothAdapter.isEnabled()) {
			//Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			//startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
		} else {
			setupBle();
		}
	}


	private void setupBle() {
		//registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
		if (ble == null) return;
		if (ble.getCount() == 0 || Ble.paired.size() > ble.getCount()) {
			Log.d(Globals.TAG, CLASSTAG + ".onResume: Start scan");
			scanLeDevice(true);
		} else {
			Log.d(Globals.TAG, CLASSTAG + ".onResume: Start connect to known devices");
			for (String address : ble.getAddresses()){
				if (ble.getConnectState(address) == ConnectState.STATE_DISCONNECTED) {
					final boolean result = ble.connect(address);
					Log.d(Globals.TAG, CLASSTAG + ".onResume Connect request result=" + result);
				}
			}
		}
	}

	@Override
	public void destroy() {

		Log.d(Globals.TAG, CLASSTAG + ".destroy"); 
		if (ble == null) return;
		ble.closeAll();
		ble = null;
		//Watchdog.stopInstance();

	}

	private void scanLeDevice(final boolean enable) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) return;
		if (enable) {
			Ble.paired.clear();

			// Stops scanning after a pre-defined scan period.
			handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					//scanning = false;
					bluetoothAdapter.stopLeScan(leScanCallback);
					LLog.d(Globals.TAG, CLASSTAG + ".scanLeDevice stopped");
				}
			}, SCAN_PERIOD);

			//scanning = true;
			bluetoothAdapter.startLeScan(leScanCallback);
			LLog.d(Globals.TAG, CLASSTAG + ".scanLeDevice started");
		} else {
			//scanning = false;
			bluetoothAdapter.stopLeScan(leScanCallback);
			LLog.d(Globals.TAG, CLASSTAG + ".scanLeDevice stopped");
		}
	}

	@Override
	public void onData(final String address, GattAction action, final BleData data) {
		Log.d(Globals.TAG, CLASSTAG + ".onData(" + address + ", " + action + ", " + data + ")");
		switch (action) {
		case ACTION_DATA_AVAILABLE:
			if (data != null) for (BleDataType type: data.getKeys()) if (type != null && type.isValid()){ 
				final Double value = data.get(type);
				BleDevice bleDevice = getBleDevice();
				ValueType valueType;
				if (bleDevice != null && value != null && (valueType = ValueType.getValueType(type))!= null) {
					if (valueType.getUnitType().getUnitClass().equals(Integer.class))
						bleDevice.onData(valueType, Integer.valueOf(value.intValue()), data.getTime());	
					else if (valueType.getUnitType().getUnitClass().equals(Long.class))
						bleDevice.onData(valueType, Long.valueOf(value.longValue()), data.getTime());	
					else if (valueType.getUnitType().getUnitClass().equals(Float.class))
						bleDevice.onData(valueType, Float.valueOf(value.floatValue()), data.getTime());	
					else 
						bleDevice.onData(valueType, value, data.getTime());	
				}
			}
			break;
		case ACTION_GATT_CONNECTED:
			new SleepAndWake(new SleepAndWake.Listener() {

				@Override
				public void onWake() {
					ToastHelper.showToastLong(ble.getName(address));
					
				}
				
			}, 100);
			break;
		case ACTION_GATT_DISCONNECTED:
			break;
		case ACTION_GATT_SERVICES_DISCOVERED:
			break;
		}


	}

	@Override
	public void onRawData(String address, GattAction action, byte[] data) {
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		LLog.d(Globals.TAG, CLASSTAG + " Preference changed: " + key); // + "\n"
		PreferenceKey prefKey = PreferenceKey.fromString(key);
		if (prefKey != null) updateSetting(sharedPreferences, prefKey);
	}

	public void updateSettings() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(Globals.appContext);
		for (PreferenceKey key : PreferenceKey.values()) updateSetting(prefs, key);
	}

	public void updateSetting(SharedPreferences prefs, PreferenceKey key) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) return;
		switch(key) {
		case BLE_DEVICE:
			int i = 0;
			String deviceStr;
			while ((deviceStr = key.getStringFromSet(i++)) != null) {
				String[] parts = deviceStr.split("\\|");
				if (parts.length >= 2){
					LLog.d(Globals.TAG,  CLASSTAG + ".updateSetting add address: " + deviceStr) ;
					Ble.paired.put(parts[0], parts[1]);
				}	
			}	
			break;
		default:
			break;
		}
	}
	
	@Override
	public void onStop() {
	}
	
	@Override
	public void onResume() {
		Log.d(Globals.TAG, CLASSTAG + ".onResume");
	}

//	@Override
//	public EnumSet<DeviceType> supportedDevices() {
//		return EnumSet.of(DeviceType.UBLUE);
//	}

}
