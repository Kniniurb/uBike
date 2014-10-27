package com.plusot.senselib.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.plusot.common.Globals;
import com.plusot.common.share.LLog;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;



/**
 * This thread runs while listening for incoming connections. It behaves
 * like a server-side client. It runs until a connection is accepted
 * (or until cancelled).
 */
public class BTAcceptThread extends Thread {
	private static final String CLASSTAG = BTAcceptThread.class.getSimpleName();
	private static final int CONNECTING = 1;
	private static final int TERMINATE = 2;
	private final BluetoothServerSocket mmServerSocket;
	private BTSocketType mSocketType;
	private static final String NAME_SECURE = "BluetoothChatSecure";
	private static final String NAME_INSECURE = "BluetoothChatInsecure";
	private static final boolean DEBUG = false;
	private static ConnectState state = ConnectState.STATE_NONE;
	private static LinkedBlockingQueue<BTSocketInfo> socketQueue = new LinkedBlockingQueue<BTSocketInfo>(); 
	private MyHandler handler = null;

	private class BTSocketInfo {
		final BluetoothSocket socket;
		final BluetoothDevice device;
		final BTSocketType type;

		BTSocketInfo(final BluetoothSocket socket,
				final BluetoothDevice device,
				final BTSocketType type) {
			this.socket = socket;
			this.device = device;
			this.type = type;
		}
	}

	private enum ConnectState {
		STATE_NONE,       // we're doing nothing
		STATE_LISTEN,     // now listening for incoming connections
		STATE_CONNECTING, // now initiating an outgoing connection
		STATE_CONNECTED;  // now connected to a remote device

	}

	public interface Listener {
		public void onBluetoothServerConnect(int what, BluetoothDevice device, BluetoothSocket socket, BTSocketType socketType);
		public void onTerminate();
	};

	public static synchronized void setStateConnected() {
		state = ConnectState.STATE_CONNECTED;
	}
	
	public static synchronized void setStateConnecting() {
		state = ConnectState.STATE_CONNECTING;
	}
	
	public static synchronized void setStateDisconnected() {
		state = ConnectState.STATE_NONE;
	}
	
	private static class MyHandler extends Handler {
		private final Listener listener;

		public MyHandler(final Listener listener) {
			this.listener = listener;
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case CONNECTING:
				BTSocketInfo socketInfo;
				if ((socketInfo = socketQueue.poll()) != null) {
					listener.onBluetoothServerConnect(msg.what, socketInfo.device, socketInfo.socket, socketInfo.type);
				}
			case TERMINATE:
				listener.onTerminate();
			}

		}
	};


	public BTAcceptThread(final Listener listener, final BluetoothAdapter adapter, boolean secure) {
		BluetoothServerSocket tmp = null;
		mSocketType = secure ? BTSocketType.SECURE:BTSocketType.INSECURE;

		// Create a new listening server socket
		try {
			if (secure) {
				tmp = adapter.listenUsingRfcommWithServiceRecord(NAME_SECURE, BTGlobals.CHAT_UUID_SECURE);
			} else {
				tmp = adapter.listenUsingInsecureRfcommWithServiceRecord(NAME_INSECURE, BTGlobals.CHAT_UUID_INSECURE);
			}
		} catch (IOException e) {
			Log.e(Globals.TAG, CLASSTAG + ".Socket Type: " + mSocketType + "listen() failed", e);
		}
		mmServerSocket = tmp;
		handler = new MyHandler(listener);
		state = ConnectState.STATE_LISTEN;
	}

	public void run() {
		setName("AcceptThread" + mSocketType + "_" + getId());
		if (DEBUG) LLog.d(Globals.TAG, CLASSTAG + "." + getName() + ".run");

		BluetoothSocket socket = null;

		// Listen to the server socket if we're not connected
		while (state != ConnectState.STATE_CONNECTED) {
			try {
				// This is a blocking call and will only return on a
				// successful connection or an exception
				socket = mmServerSocket.accept(30000);
				if (DEBUG) LLog.d(Globals.TAG, CLASSTAG + "." + getName() + ".run: Accepted " + socket.getRemoteDevice().getName());

			} catch (IOException e) {
				if (DEBUG) LLog.e(Globals.TAG, CLASSTAG + ".run: Socket accept() failed.", e);
				break;
			}

			// If a connection was accepted
			if (socket != null) {
				//				synchronized (BTChat.this) {
				switch (state) {
				case STATE_LISTEN:
				case STATE_CONNECTING:
					if (DEBUG) LLog.d(Globals.TAG, CLASSTAG + "." + getName() + ".run: Connecting to " + socket.getRemoteDevice().getName());
					socketQueue.offer(new BTSocketInfo(socket, socket.getRemoteDevice(), mSocketType));
					handler.obtainMessage(CONNECTING).sendToTarget();
					//connected(socket, socket.getRemoteDevice(), mSocketType);
					break;
				case STATE_NONE:
				case STATE_CONNECTED:
					if (DEBUG) LLog.d(Globals.TAG, CLASSTAG + "." + getName() + ".run: Closing socket " + socket.getRemoteDevice().getName());
					try {
						socket.close();
					} catch (IOException e) {
						Log.e(Globals.TAG, CLASSTAG + ".Could not close unwanted socket", e);
					}
					break;
				}
				//				}
			}
		}
		if (DEBUG) Log.i(Globals.TAG, CLASSTAG + ".END, socket Type: " + mSocketType);
		handler.obtainMessage(TERMINATE).sendToTarget();
	}

	public void cancel() {
		if (DEBUG) Log.d(Globals.TAG, CLASSTAG + ".Socket Type" + mSocketType + "cancel " + this);
		try {
			interrupt();
			mmServerSocket.close();
		} catch (IOException e) {
			Log.e(Globals.TAG, CLASSTAG + ".Socket Type" + mSocketType + "close() of server failed", e);
		}
	}
}
