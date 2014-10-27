/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.plusot.bluelib.ble;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import com.plusot.bluelib.R;
import com.plusot.bluelib.ble.Ble.ConnectState;
import com.plusot.bluelib.ble.Ble.GattAction;
import com.plusot.common.Globals;
import com.plusot.javacommon.util.StringUtil;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class GattAttributesActivity extends Activity implements Ble.Listener {
	private final static String CLASSTAG = GattAttributesActivity.class.getSimpleName();

	public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
	public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
	public boolean connected = false;
	private boolean isServicesDisplayed = false;

	private TextView mConnectionState;
	private TextView mDataField;
	private TextView mInfoField;
	private String mDeviceName;
	private String mDeviceAddress;
	private Ble ble;
	private BleData data = new BleData();

	private ExpandableListView mGattServicesList;
	private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
			new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
	private BluetoothGattCharacteristic mNotifyCharacteristic;

	private final String LIST_NAME = "NAME";
	private final String LIST_UUID = "UUID";

	@Override
	public void onData(final String address, final GattAction action, final BleData newData) {
		if (!address.equals(mDeviceAddress)) return;
		if (newData == null) 
			Log.d(Globals.TAG, CLASSTAG + ".onData(" + address + ", " + action + ")");
		else
			Log.d(Globals.TAG, CLASSTAG + ".onData(" + address + ", " + action + ", " + newData + ")");
		data.merge(newData);
		runOnUiThread(new Runnable() {
			@Override
			public void run() {

				switch (action) {
				case ACTION_DATA_AVAILABLE:
					if (!connected) updateConnectionState(R.string.connected, true);
					displayData(data.toString());
					if (!isServicesDisplayed) displayGattServices(ble.getSupportedGattServices(mDeviceAddress));								
					break;
				case ACTION_GATT_CONNECTED:
					updateConnectionState(R.string.connected, true);
					break;
				case ACTION_GATT_DISCONNECTED:
					updateConnectionState(R.string.disconnected, false);
					clearUI();
					ble.close(mDeviceAddress);
					break;
				case ACTION_GATT_SERVICES_DISCOVERED:
					displayGattServices(ble.getSupportedGattServices(mDeviceAddress));
					break;

				}
			}
		});

	}                  


	// If a given GATT characteristic is selected, check for supported features.  This sample
	// demonstrates 'Read' and 'Notify' features.  See
	// http://d.android.com/reference/android/bluetooth/BluetoothGatt.html for the complete
	// list of supported characteristic features.
	private final ExpandableListView.OnChildClickListener servicesListClickListner =
			new ExpandableListView.OnChildClickListener() {
		@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
		@Override
		public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
				int childPosition, long id) {
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) return true; 
			if (mGattCharacteristics != null) {
				final BluetoothGattCharacteristic characteristic =
						mGattCharacteristics.get(groupPosition).get(childPosition);
				final int charaProp = characteristic.getProperties();
				if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
					// If there is an active notification on a characteristic, clear
					// it first so it doesn't update the data field on the user interface.
					if (mNotifyCharacteristic != null) {
						ble.setCharacteristicNotification(mDeviceAddress, mNotifyCharacteristic, false);
						mNotifyCharacteristic = null;
					}
					ble.readCharacteristic(mDeviceAddress, characteristic);
				}
				if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
					mNotifyCharacteristic = characteristic;
					ble.setCharacteristicNotification(mDeviceAddress, characteristic, true);
				}
				return true;
			}
			return false;
		}
	};

	private void clearUI() {
		mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
		mDataField.setText(R.string.no_data);
		mInfoField.setText(R.string.no_data);
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ble = Ble.getInstance();
		ble.addListener(this);
		setContentView(R.layout.activity_gattattributes);

		final Intent intent = getIntent();
		mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
		mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

		// Sets up UI references.
		((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
		mGattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
		mGattServicesList.setOnChildClickListener(servicesListClickListner);
		mConnectionState = (TextView) findViewById(R.id.connection_state);
		mDataField = (TextView) findViewById(R.id.data_value);
		mInfoField = (TextView) findViewById(R.id.info_value);

		getActionBar().setTitle(mDeviceName + " 1");
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	protected void onResume() {
		super.onResume();
		ble.addListener(this);
		
		if (ble.getConnectState(mDeviceAddress) != ConnectState.STATE_CONNECTED)
			ble.connect(mDeviceAddress);
		else if (ble.isServicesDiscovered(mDeviceAddress))
			displayGattServices(ble.getSupportedGattServices(mDeviceAddress));								
	}

	@Override
	protected void onPause() {
		ble.removeListener(this);
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		ble.removeListener(this);
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.gatt_activity, menu);
		if (connected) {
			menu.findItem(R.id.menu_connect).setVisible(false);
			menu.findItem(R.id.menu_disconnect).setVisible(true);
		} else {
			menu.findItem(R.id.menu_connect).setVisible(true);
			menu.findItem(R.id.menu_disconnect).setVisible(false);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == R.id.menu_connect) {
			ble.connect(mDeviceAddress);
			return true;
		} else if (itemId == R.id.menu_disconnect) {
			ble.disconnect(mDeviceAddress);
			return true;
		} else if (itemId == android.R.id.home) {
			onBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void updateConnectionState(final int resourceId, final boolean connected) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				GattAttributesActivity.this.connected = connected;
				mConnectionState.setText(resourceId);
				invalidateOptionsMenu();
			}
		});
	}

	private void displayData(String data) {
		if (data != null) {
			mDataField.setText(data);
		}
	}

	private void displayInfo(String info) {
		if (info != null) {
			mInfoField.setText(info);
		}
	}

	// Demonstrates how to iterate through the supported GATT Services/Characteristics.
	// In this sample, we populate the data structure that is bound to the ExpandableListView
	// on the UI.
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
	private void displayGattServices(List<BluetoothGattService> gattServices) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) return; 
		if (gattServices == null) return;
		isServicesDisplayed = true;
		UUID uuid;
		ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
		ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
		= new ArrayList<ArrayList<HashMap<String, String>>>();
		mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

		// Loops through available GATT Services.
		for (BluetoothGattService gattService : gattServices) {
			HashMap<String, String> currentServiceData = new HashMap<String, String>();
			uuid = gattService.getUuid();
			currentServiceData.put(LIST_NAME, GattAttribute.stringFromUUID(uuid, GattAttributeType.SERVICE));
			currentServiceData.put(LIST_UUID, uuid.toString());
			gattServiceData.add(currentServiceData);

			ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
					new ArrayList<HashMap<String, String>>();
			List<BluetoothGattCharacteristic> gattCharacteristics =
					gattService.getCharacteristics();
			ArrayList<BluetoothGattCharacteristic> charas =
					new ArrayList<BluetoothGattCharacteristic>();

			// Loops through available Characteristics.
			for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
				charas.add(gattCharacteristic);
				HashMap<String, String> currentCharaData = new HashMap<String, String>();
				uuid = gattCharacteristic.getUuid();
				currentCharaData.put(
						LIST_NAME, GattAttribute.stringFromUUID(uuid, GattAttributeType.CHARACTERISTIC));
				currentCharaData.put(LIST_UUID, uuid.toString());
				gattCharacteristicGroupData.add(currentCharaData);
			}
			mGattCharacteristics.add(charas);
			gattCharacteristicData.add(gattCharacteristicGroupData);
		}

		SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
				this,
				gattServiceData,
				android.R.layout.simple_expandable_list_item_2,
				new String[] {LIST_NAME, LIST_UUID},
				new int[] { android.R.id.text1, android.R.id.text2 },
				gattCharacteristicData,
				android.R.layout.simple_expandable_list_item_2,
				new String[] {LIST_NAME, LIST_UUID},
				new int[] { android.R.id.text1, android.R.id.text2 }
				);
		mGattServicesList.setAdapter(gattServiceAdapter);
	}

	@Override
	public void onRawData(String address, GattAction action, byte[] data) {
		if (data != null) displayInfo(StringUtil.toHexString(data));	
	}
}
