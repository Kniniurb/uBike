package com.plusot.bluelib;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import com.plusot.bluelib.ble.Ble;
import com.plusot.bluelib.ble.Ble.ConnectState;
import com.plusot.bluelib.ble.Ble.GattAction;
import com.plusot.bluelib.ble.BleData;
import com.plusot.bluelib.ble.BleDataType;
import com.plusot.bluelib.ble.GattAttribute;
import com.plusot.bluelib.ble.GattAttributesActivity;
import com.plusot.bluelib.bt.BluetoothChatService;
import com.plusot.bluelib.settings.PreferenceKey;
import com.plusot.common.Globals;
import com.plusot.common.settings.PreferenceHelper;
import com.plusot.common.share.LLog;
import com.plusot.common.util.ToastHelper;
import com.plusot.common.util.Watchdog;
import com.plusot.javacommon.util.TimeUtil;

public class UBlueMain extends Activity implements Ble.Listener, OnSharedPreferenceChangeListener {
	private final static String CLASSTAG = UBlueMain.class.getSimpleName();
	private static final long SCAN_PERIOD = 10000;
	private static final int MAX_VIEWS = 9;
	private static final int REQUEST_ENABLE_BT = 8000;
	public static String EXTRA_USECHAT = "Extra_UseChat";
	public static String EXTRA_CALLER = "Extra_Caller";
	private Ble ble;
	private Handler handler;
	private Handler chatHandler = null;
	private boolean useChat = true;
	private boolean scanning = false;
	private String caller = null;
	private BluetoothAdapter bluetoothAdapter;
	private Map<String, View> deviceViews = new HashMap<String, View>();
	private int viewCount = 0;
	private View views[] = new View[MAX_VIEWS];
	private BluetoothChatService chatService = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = getIntent();
		useChat = intent.getBooleanExtra(EXTRA_USECHAT, true);
		caller = intent.getStringExtra(EXTRA_CALLER);
		if (Build.VERSION.SDK_INT >= 11 && caller != null) requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
		if (!useChat) ToastHelper.showToastLong("Called by " + caller);

		handler = new Handler();
		if (useChat) chatHandler = new ChatHandler();
		setContentView(R.layout.activity_main);

		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
			finish();
		}

		ble = Ble.getInstance();
		ble.addListener(this);
		Watchdog.getInstance();

		views[0] = findViewById(R.id.bleview1);
		views[1] = findViewById(R.id.bleview2);
		views[2] = findViewById(R.id.bleview3);
		views[3] = findViewById(R.id.bleview4);
		views[4] = findViewById(R.id.bleview5);
		views[5] = findViewById(R.id.bleview6);
		views[6] = findViewById(R.id.bleview7);
		views[7] = findViewById(R.id.bleview8);
		views[8] = findViewById(R.id.bleview9);

		final BluetoothManager bluetoothManager =
				(BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		bluetoothAdapter = bluetoothManager.getAdapter();

		updateSettings();
		PreferenceHelper.getPrefs().registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onStart() {
		super.onStart();
		Log.d(Globals.TAG, CLASSTAG + ".onStart");

		// If BT is not on, request that it be enabled.
		// setupChat() will then be called during onActivityResult
		if (!bluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
			// Otherwise, setup the chat session
		} else {
			if (useChat && chatService == null) setupChat();
			setupBle();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (useChat && chatService != null) {
			// Only if the state is STATE_NONE, do we know that we haven't started already
			if (chatService.getState() == BluetoothChatService.STATE_NONE) {
				// Start the Bluetooth chat services
				chatService.start();
			}
		}
		ble.addListener(this);
		
	}

	private void setupBle() {
		//registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
		if (ble == null) return;
		if (Ble.paired.size() > 0) for (String address: Ble.paired.keySet()){
			addView(address, Ble.paired.get(address));
			if (ble.getConnectState(address) == ConnectState.STATE_DISCONNECTED) {
				final boolean result = ble.connect(address);
				Log.d(Globals.TAG, CLASSTAG + ".onResume Paired connect request result=" + result);
			}
		} else if (ble.getCount() == 0) { // || BluetoothLe.paired.size() > ble.getCount()) {
			Log.d(Globals.TAG, CLASSTAG + ".onResume: Start scan");
			scanLeDevice(true);
		} else {
			Log.d(Globals.TAG, CLASSTAG + ".onResume: Start connect to known devices");
			for (String address : ble.getAddresses()){
				addView(address, ble.getName(address));
				if (ble.getConnectState(address) == ConnectState.STATE_DISCONNECTED) {
					final boolean result = ble.connect(address);
					Log.d(Globals.TAG, CLASSTAG + ".onResume Connect request result=" + result);
				}
			}
		}
	}

	private void setupChat() {
		if (!useChat) return;
		Log.d(Globals.TAG, CLASSTAG + ".setupChat");

		chatService = new BluetoothChatService(this, chatHandler);

		// Initialize the buffer for outgoing messages
	}


	@Override
	protected void onPause() {
		super.onPause();
		ble.removeListener(this);
		if (this.isFinishing() && useChat && caller == null) {
			if (chatService != null) chatService.stop();

			Log.d(Globals.TAG, CLASSTAG + ".onPause: finishing app."); 
			ble.closeAll();
			Watchdog.stopInstance();
		} else {
			Log.d(Globals.TAG, CLASSTAG + ".onPause"); 			
		}
		//unregisterReceiver(mGattUpdateReceiver);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		ble = null;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(Globals.TAG, CLASSTAG + ".onActivityResult " + resultCode);
		switch (requestCode) {
		//        case REQUEST_CONNECT_DEVICE_SECURE:
		//            // When DeviceListActivity returns with a device to connect
		//            if (resultCode == Activity.RESULT_OK) {
		//                connectDevice(data, true);
		//            }
		//            break;
		//        case REQUEST_CONNECT_DEVICE_INSECURE:
		//            // When DeviceListActivity returns with a device to connect
		//            if (resultCode == Activity.RESULT_OK) {
		//                connectDevice(data, false);
		//            }
		//            break;
		case REQUEST_ENABLE_BT:
			// When the request to enable Bluetooth returns
			if (resultCode == Activity.RESULT_OK) {
				// Bluetooth is now enabled, so set up a chat session
				setupBle();
			} else {
				// User did not enable Bluetooth or an error occurred
				Log.d(Globals.TAG, CLASSTAG + ".onActivityResult BT not enabled");
				Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
				finish();
			}
		}
	}
	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

		@Override
		public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
			//scanLeDevice(false);
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Log.d(Globals.TAG, CLASSTAG + ".onLeScan found " + device.getName() + " " + device.getAddress());
					if (ble != null && (Ble.paired.size() == 0 || !Ble.paired.containsKey(device.getAddress()))) {
						Log.d(Globals.TAG, CLASSTAG + ".onLeScan preferenceKey add " + device.getName() + " " + device.getAddress());
						PreferenceKey.BLE_DEVICE.addStringToSet(device.getAddress() + '|' + device.getName());
						addView(device.getAddress(), device.getName());
						if (ble.getConnectState(device.getAddress()) == ConnectState.STATE_DISCONNECTED) {
							final boolean result = ble.connect(device.getAddress());
							Log.d(Globals.TAG, CLASSTAG + ".onLeScan Connect request result for " + device.getAddress() + " = " + result);

						}
					}
				}
			});
		}
	};

	private View addView(final String address, final String name) {
		View view = null;
		if ((view = deviceViews.get(address)) != null) return view;
		viewCount %= MAX_VIEWS;
		deviceViews.put(address, views[viewCount]);
		views[viewCount].setVisibility(View.VISIBLE);
		TextView tv  = (TextView) views[viewCount].findViewById(R.id.bleDeviceAddress);
		tv.setText(address);
		tv  = (TextView) views[viewCount].findViewById(R.id.bleDeviceName);
		tv.setText(name);
		views[viewCount].setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				ble.startRead(address, GattAttribute.CSC_MEASUREMENT);
			}
		});
		final int count = viewCount;
		views[count].setTag(address);
		views[viewCount].setOnCreateContextMenuListener(new OnCreateContextMenuListener() {

			@Override
			public void onCreateContextMenu(
					ContextMenu menu, View v,
					ContextMenuInfo menuInfo) {
				//									MenuInflater inflater = getMenuInflater();
				//								    inflater.inflate(R.menu.context, menu);
				menu.setHeaderTitle(R.string.context_header);
				int index = 0;
				menu.add(count, R.id.action_info, index++, R.string.action_info);
				menu.add(count, R.id.action_delete, index++, R.string.action_delete);


			}

		});
		viewCount++;
		return views[viewCount - 1];
	}

	private void scanLeDevice(final boolean enable) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) return;
		if (enable) {
			// Stops scanning after a pre-defined scan period.
			handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					scanning = false;
					bluetoothAdapter.stopLeScan(mLeScanCallback);
					invalidateOptionsMenu();
					findViewById(R.id.progressScan).setVisibility(View.GONE);
				}
			}, SCAN_PERIOD);

			scanning = true;
			bluetoothAdapter.startLeScan(mLeScanCallback);
			findViewById(R.id.progressScan).setVisibility(View.VISIBLE);
		} else {
			scanning = false;
			bluetoothAdapter.stopLeScan(mLeScanCallback);
			findViewById(R.id.progressScan).setVisibility(View.GONE);
		}
		invalidateOptionsMenu();
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		Log.d(Globals.TAG, CLASSTAG + ".onContextItemSelected for View " + item.getGroupId());
		if (item.getGroupId() < views.length && views[item.getGroupId()] != null && views[item.getGroupId()].getTag() instanceof String) {
			String address = (String) views[item.getGroupId()].getTag();
			if (address != null) {
				int itemId = item.getItemId();
				if (itemId == R.id.action_delete) {
					ble.close(address);
					views[item.getGroupId()].setVisibility(View.GONE);
					//if (ble.hasAddress(address)) 
					PreferenceKey.BLE_DEVICE.deleteStringFromSet(address);
				} else if (itemId == R.id.action_info && ble.hasAddress(address)) {
					final Intent intent = new Intent(UBlueMain.this, GattAttributesActivity.class);
					intent.putExtra(GattAttributesActivity.EXTRAS_DEVICE_NAME, ble.getName(address));
					intent.putExtra(GattAttributesActivity.EXTRAS_DEVICE_ADDRESS, address);
					if (scanning) {
						bluetoothAdapter.stopLeScan(mLeScanCallback);
						scanning = false;
					}
					startActivity(intent);
				}
			}
		}
		return true;
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.bluemain, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_scan) {
			Ble.paired.clear();
			scanLeDevice(true);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}


	@Override
	public void onData(final String address, GattAction action, final BleData data) {
		if (data == null)
			Log.d(Globals.TAG, CLASSTAG + ".onData(" + address + ", " + action + ")");
		else
			Log.d(Globals.TAG, CLASSTAG + ".onData(" + address + ", " + action + ", data =" + data + ")");
		switch (action) {
		case ACTION_DATA_AVAILABLE:
			Log.d(Globals.TAG, CLASSTAG + ".onData case ACTION_DATA_AVAILABLE");
			if (data != null) for (BleDataType type: data.getKeys()) if (type != null && type.isValid()){ 
//				Log.d(Globals.TAG, CLASSTAG + ".onData got here");
				final View view = deviceViews.get(address);
				final BleDataType dataType = type;
				final Double value = data.get(type);
				if (value != null) {
					try {
						sendChatMessage(dataType.toJSON(value, data.getTime()).toString());	
					} catch (JSONException e) {
						Log.e(Globals.TAG, CLASSTAG + ".onData Exception in generation of JSON object", e);
					}
					switch (type) {
					case BT_TEMPERATURE:
						if (view != null) view.post(new Runnable() {
							@Override
							public void run() {
								TextView tv = (TextView) view.findViewById(R.id.bleTemperature);
								tv.setVisibility(View.VISIBLE);
								tv.setText(dataType.toString(value));

							}
						});
						break;
					case BT_BATTERYLEVEL:
						if (view != null) view.post(new Runnable() {
							@Override
							public void run() {
								TextView tv = (TextView) view.findViewById(R.id.bleBatteryLevel);
								tv.setVisibility(View.VISIBLE);
								tv.setText(dataType.toString(value));
							}
						});
						break;
					case BT_WHEELREVOLUTIONS:
						if (view != null) view.post(new Runnable() {
							@Override
							public void run() {
								view.findViewById(R.id.bleMeasurement2).setVisibility(View.VISIBLE);
								TextView tv  = (TextView) view.findViewById(R.id.bleMeasurementData2);
								tv.setText(dataType.valueString(value));
								tv  = (TextView) view.findViewById(R.id.bleMeasurementUnit2);
								tv.setText(dataType.unitString());
								tv  = (TextView) view.findViewById(R.id.bleMeasurementType2);
								tv.setText(dataType.typeString());
								tv = (TextView) view.findViewById(R.id.bleDeviceTime);
								tv.setText(TimeUtil.formatMilliTime(data.getTime(), 0));
							}
						});
						break;
					case BT_CRANKREVOLUTIONS:
					case BT_HEARTRATE:
						if (view != null) view.post(new Runnable() {
							@Override
							public void run() {
								view.findViewById(R.id.bleMeasurement).setVisibility(View.VISIBLE);
								TextView tv  = (TextView) view.findViewById(R.id.bleMeasurementData);
								tv.setText(dataType.valueString(value));
								tv  = (TextView) view.findViewById(R.id.bleMeasurementUnit);
								tv.setText(dataType.unitString());
								tv  = (TextView) view.findViewById(R.id.bleMeasurementType);
								tv.setText(dataType.typeString());
								tv = (TextView) view.findViewById(R.id.bleDeviceTime);
								tv.setText(TimeUtil.formatMilliTime(data.getTime(), 0));
							}
						});
						break;
					case BT_UNKNOWN:
						break;
					default:
						break;
					}
				}
			}
			break;
		case ACTION_GATT_CONNECTED:
			final View view = addView(address, ble.getName(address));
			view.post(new Runnable() {
				@Override
				public void run() {
					TextView tv = (TextView) view.findViewById(R.id.bleDeviceName);
					tv.setVisibility(View.VISIBLE);
					tv.setText(ble.getName(address));
				}
			});
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
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(Globals.appContext);
		for (PreferenceKey key : PreferenceKey.values()) updateSetting(prefs, key);
	}

	public void updateSetting(SharedPreferences prefs, PreferenceKey key) {
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
		}
	}	

	private void sendChatMessage(String message) {
		if (!useChat) return; 
		// Check that we're actually connected before trying anything
		if (chatService.getState() != BluetoothChatService.STATE_CONNECTED) {
			//Log.d(Globals.TAG, CLASSTAG + ".sendChatMessage Chat service not connected");
			return;
		}

		// Check that there's actually something to send
		if (message.length() > 0) {
			// Get the message bytes and tell the BluetoothChatService to write
			byte[] send = message.getBytes();
			chatService.write(send);
		}
	}

	// The Handler that gets information back from the BluetoothChatService
	private static class ChatHandler extends Handler {
		private String connectedChatDeviceName = null;

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case BluetoothChatService.MESSAGE_STATE_CHANGE:
				Log.i(Globals.TAG, CLASSTAG + ".handleMessage MESSAGE_STATE_CHANGE: " + msg.arg1);
				switch (msg.arg1) {
				case BluetoothChatService.STATE_CONNECTED:
					Log.d(Globals.TAG, CLASSTAG + ".handleMessage Chat connected");
					break;
				case BluetoothChatService.STATE_CONNECTING:
					Log.d(Globals.TAG, CLASSTAG + ".handleMessage Chat connecting");
					break;
				case BluetoothChatService.STATE_LISTEN:
				case BluetoothChatService.STATE_NONE:
					Log.d(Globals.TAG, CLASSTAG + ".handleMessage Not connected");
					break;
				}
				break;
			case BluetoothChatService.MESSAGE_WRITE:
				byte[] writeBuf = (byte[]) msg.obj;
				// construct a string from the buffer
				String writeMessage = new String(writeBuf);
				Log.d(Globals.TAG, CLASSTAG + ".handleMessage Wrote message:" + writeMessage);
				break;
			case BluetoothChatService.MESSAGE_READ:
				byte[] readBuf = (byte[]) msg.obj;
				// construct a string from the valid bytes in the buffer
				String readMessage = new String(readBuf, 0, msg.arg1);
				Log.d(Globals.TAG, CLASSTAG + ".handleMessage Read message from " + connectedChatDeviceName + ":" + readMessage);
				break;
			case BluetoothChatService.MESSAGE_DEVICE_NAME:
				// save the connected device's name
				connectedChatDeviceName = msg.getData().getString(BluetoothChatService.DEVICE_NAME);
				ToastHelper.showToastLong(Globals.appContext.getString(R.string.connected_to, connectedChatDeviceName));
				break;
			case BluetoothChatService.MESSAGE_TOAST:
				ToastHelper.showToastLong(msg.getData().getString(BluetoothChatService.TOAST));
				break;
			}
		}
	};

}
