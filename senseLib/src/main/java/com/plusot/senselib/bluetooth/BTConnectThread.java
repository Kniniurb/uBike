package com.plusot.senselib.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;

import com.plusot.common.Globals;
import com.plusot.common.share.LLog;

import java.io.IOException;

public class BTConnectThread extends Thread {
	private static final String CLASSTAG = BTConnectThread.class.getSimpleName();
    private static final boolean DEBUG = false;

	public static final int CONNECTED = 1;
	public static final int CONNECT_FAILED = 2;
	
	private final BluetoothSocket socket;
	private final BluetoothAdapter adapter;
	private final MyHandler handler;
	private BTSocketType socketType;


	public interface Listener {
		public void onBluetoothConnect(BluetoothDevice device, BluetoothSocket socket, BTSocketType socketType);
		public void onBluetoothConnectFailed(BluetoothDevice device);//(BluetoothConnectThread sender);
	};

	private static class MyHandler extends Handler {
		private final Listener listener;
		//private final BTConnectThread thread;
		private final BluetoothSocket socket;
		private final BluetoothDevice device;
		private final BTSocketType socketType;

		public MyHandler(/*final BTConnectThread thread, */final Listener listener, final BluetoothSocket socket, final BTSocketType socketType, final BluetoothDevice device) {
			this.listener = listener;
			//this.thread = thread;
			this.socket = socket;
			this.socketType = socketType;
			this.device = device;

		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case CONNECT_FAILED:
				listener.onBluetoothConnectFailed(device);
				break;
			case CONNECTED:
				listener.onBluetoothConnect(device, socket, socketType);
				break;
			}
		}
	};

	public BluetoothSocket getBluetoothSocket(BluetoothDevice device) throws IOException {
		switch (BTTypeMajor.fromInt(device.getBluetoothClass().getMajorDeviceClass())) {
		case COMPUTER:
		case PHONE:
			//if (secure) 
			socketType = BTSocketType.SECURE;
			BTAcceptThread.setStateConnecting();
			return device.createRfcommSocketToServiceRecord(BTGlobals.CHAT_UUID_SECURE);
			//else
			//	return device.createInsecureRfcommSocketToServiceRecord(MY_UUID_INSECURE);
		default:
			socketType = BTSocketType.INSECURE;
			try {
				return device.createInsecureRfcommSocketToServiceRecord(BTGlobals.BS_UUID);
			} catch (IOException e) {
				LLog.d(Globals.TAG, "Unable to create insecure connection", e);
			}
			return device.createRfcommSocketToServiceRecord(BTGlobals.BS_UUID);
		}
	};

	public BTConnectThread(Listener listener, BluetoothAdapter adapter, BluetoothDevice device) {
		setName("ConnectThread-" + BTHelper.getBTName(device));
		this.adapter = adapter;
		BluetoothSocket tmp = null;

		// Get a BluetoothSocket for a connection with the
		// given BluetoothDevice
		try {
			tmp = getBluetoothSocket(device);
		} catch (IOException e) {
			LLog.e(Globals.TAG, CLASSTAG + ".create() failed", e);
		}
		socket = tmp;
		handler = new MyHandler(listener, socket, socketType, device);
	}



	@Override
	public void run() {
		//LLog.d(Globals.TAG, CLASSTAG + ".run: BEGIN");
		setName("ConnectThread_" + getId());

		adapter.cancelDiscovery();

		// Make a connection to the BluetoothSocket
		try {
			// This is a blocking call and will only return on a successful connection or an exception
			socket.connect();
		} catch (IOException e) {
			if (DEBUG) LLog.e(Globals.TAG, CLASSTAG + "." + getName() + ".run: Bluetooth connection failed: " + e.getMessage());
			try {
				socket.close();
			} catch (IOException e2) {
				LLog.e(Globals.TAG, CLASSTAG + ".run: Unable to close() socket during connection failure", e2);
			}
			// Start the service over to restart listening mode
			handler.obtainMessage(CONNECT_FAILED).sendToTarget();
			return;
		}

		// Start the connected thread
		handler.obtainMessage(CONNECTED).sendToTarget();
	}

	public void cancel() {
		try {
			interrupt();
			socket.close();
		} catch (IOException e) {
			LLog.e(Globals.TAG, CLASSTAG + ".cancel: Close() of connect socket failed", e);
		}
	}

}
