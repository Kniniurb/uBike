package com.plusot.senselib.bluetooth;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.Arrays;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;

import com.plusot.common.Globals;
import com.plusot.common.share.LLog;

public class BTConnectedThread extends Thread {
	private static final String CLASSTAG = BTConnectedThread.class.getSimpleName();
	//private static final int MESSAGE_STATECHANGE = 1;
	private static final int MESSAGE_READ = 2;
	private static final int MESSAGE_READBYTES = 3;
	private static final int MESSAGE_WRITE = 4;
	private static final int MESSAGE_DEVICENAME = 5;
	//private static final int MESSAGE_TOAST = 5;
	private static final int MESSAGE_TERMINATED = 6;
	private static final String DEVICE_NAME = "device_name";

	private final BluetoothSocket mmSocket;
	private final InputStream mmInStream;
	private final OutputStream mmOutStream;
	private boolean mayRun = true;
	private final Listener listener;
	private final Command[] commands;
	private MyHandler handler = new MyHandler(this);
	private final Type type;
	private final byte startByte;
	private final byte endByte;
	private final int lengthByteIndex;
	private final int extraBytes;

	public enum Type {
		WITH_LINEENDS,
		//		WITH_STARTBYTE,
		//		WITH_ENDBYTE,
		WITH_STARTENDBYTE,
		STRINGS,
		BYTES;
	}

	public interface Listener {
		public void onBluetoothWrite(BTConnectedThread sender, String msg);
		public void onBluetoothRead(BTConnectedThread sender, String msg);
		public void onBluetoothReadBytes(BTConnectedThread sender, byte[] msg);
		public void onBluetoothTerminate(BTConnectedThread sender);
		public void onBluetoothDeviceName(BTConnectedThread sender, String deviceName);
	};

	private static class MyHandler extends Handler {
		private WeakReference<BTConnectedThread> thread;

		private MyHandler(BTConnectedThread thread) {
			this.thread = new WeakReference<BTConnectedThread>(thread);
		}

		@Override
		public void handleMessage(Message msg) {
			BTConnectedThread t = thread.get();
			if (t == null || t.listener == null) return;
			switch (msg.what) {
			/*case MESSAGE_STATECHANGE:
				LLog.i(Globals.TAG, CLASSTAG + ".handleMessage: MESSAGE_STATE_CHANGE: " + msg.arg1);
				switch (BluetoothState.fromInt(msg.arg1)) {
				case CONNECTED:
					Toast.makeText(BlueWindActivity.this, "Connected: " + deviceName, Toast.LENGTH_SHORT);
					break;
				case CONNECTING:
					Toast.makeText(BlueWindActivity.this, "Connected: " + deviceName, Toast.LENGTH_SHORT);
					break;
				case DISCONNECTED:
					Toast.makeText(BlueWindActivity.this, "Disconnected: " + deviceName, Toast.LENGTH_SHORT);
					break;
				case SENDING:
					Toast.makeText(BlueWindActivity.this, "Sending: " + deviceName, Toast.LENGTH_SHORT);
					break;
				case NONE:
					break;
				}
				break;
			 */
			case MESSAGE_WRITE:
				/*String writeMessage = (String) msg.obj;
				TextView tv = (TextView) findViewById(R.id.info);
				tv.append("me: " + writeMessage + "\r");
				 */
				t.listener.onBluetoothWrite(t, (String) msg.obj);
				break;
			case MESSAGE_READ:
				t.listener.onBluetoothRead(t, (String) msg.obj);
				break;
			case MESSAGE_READBYTES:
				t.listener.onBluetoothReadBytes(t, (byte[]) msg.obj);
				break;
			case MESSAGE_DEVICENAME:
				// save the connected device's name
				String deviceName = msg.getData().getString(DEVICE_NAME);
				t.listener.onBluetoothDeviceName(t, deviceName);
				break;
			case MESSAGE_TERMINATED:
				t.listener.onBluetoothTerminate(t);
				break;
			}
		}
	};

	public BTConnectedThread(final Listener listener, final BluetoothSocket socket, final Command[] commands, final Type type) {
		this(listener, socket, commands, type, null, -1, 0);
	}	

	public BTConnectedThread(final Listener listener, final BluetoothSocket socket, final Command[] commands, final Type type, final byte[] cutBytes) {
		this(listener, socket, commands, type, cutBytes, -1, 0);
	}	

	public BTConnectedThread(final Listener listener, final BluetoothSocket socket, final Command[] commands, final Type type, final byte[] cutBytes, final int lengthByteIndex, final int extraBytes) {
		LLog.d(Globals.TAG, CLASSTAG + ".create");
		this.listener = listener;
		this.commands = commands;
		this.type = type;
		this.lengthByteIndex = lengthByteIndex;
		this.extraBytes = extraBytes;
		mmSocket = socket;

		if (cutBytes == null || cutBytes.length < 1 || (cutBytes.length < 2 && type.equals(Type.WITH_STARTENDBYTE))) {
			startByte = (byte) 0; 
			endByte = (byte) 0; 
		} else switch (type) {
		//		case WITH_STARTBYTE:
		//			startByte = cutBytes[0]; endByte = (byte)0; break;
		//		case WITH_ENDBYTE:
		//			startByte = (byte)0; endByte = cutBytes[0]; break;
		case WITH_STARTENDBYTE:
			startByte = cutBytes[0]; endByte = cutBytes[1]; break;
		case WITH_LINEENDS: 
		default:
			startByte = (byte) 0; endByte = (byte) 0; break;
		}

		InputStream tmpIn = null;
		OutputStream tmpOut = null;

		// Get the BluetoothSocket input and output streams
		try {
			tmpIn = socket.getInputStream();
			tmpOut = socket.getOutputStream();
		} catch (IOException e) {
			LLog.e(Globals.TAG, CLASSTAG + ".create: Tempory sockets not created", e);
		}

		mmInStream = tmpIn;
		mmOutStream = tmpOut;
	}

	public void addCommand() {

	}

	public void run() {
		LLog.i(Globals.TAG, CLASSTAG + ".run");
		byte[] buffer = new byte[1024];
		int bytes;
		int pos = 0;
		if (commands != null && commands.length > 0) write(commands[0].toString());
		while (mayRun) {
			try {
				// Read from the InputStream
				if (pos >= buffer.length - 10) pos = 0;
				bytes = mmInStream.read(buffer, pos, buffer.length - pos);	
				switch (type) {
				case WITH_LINEENDS:
					pos += bytes;
					if	(pos > 0 && ((buffer[pos - 1] & 0xFF) == 0x0D || (buffer[pos - 1] & 0xFF) == 0x0A)) {
						if (pos > 1) {
							//LLog.d(Globals.TAG, CLASSTAG + ".run: " + Util.toReadableString(buffer, pos, true));
							String temp = new String(buffer, 0, pos);
							handler.obtainMessage(MESSAGE_READ, pos, -1, temp).sendToTarget();
						}
						//buffer = new byte[1024];
						pos = 0;
					}
					break;
					
				case WITH_STARTENDBYTE:
//					LLog.d(Globals.TAG, CLASSTAG + ".run (" + pos + "," + bytes + "): " + StringUtil.toHexString(buffer, pos + bytes)); //.toReadableString(buffer, bytes, true));
					int i = 0;
					int iEnd = bytes;
					while (i < iEnd) {
						if (buffer[0] == startByte && buffer[i + pos] == endByte && (lengthByteIndex < 1 || i + pos + 1 == (0xFF & buffer[lengthByteIndex]) + extraBytes)) {
							handler.obtainMessage(MESSAGE_READBYTES, -1, -1, Arrays.copyOf(buffer, pos + i + 1)).sendToTarget();
							if (iEnd + pos > i + pos + 1) {
								buffer = Arrays.copyOfRange(buffer, pos + i + 1, pos + i + 1 + 1024);
								iEnd = bytes - i - 1;
							} else {
								iEnd = 0;	
							}
							i = 0;
							pos = 0;
						} else
							i++;
					}
					pos += iEnd;
					break;
				case STRINGS:
					handler.obtainMessage(MESSAGE_READ, -1, -1, new String(buffer, 0, bytes)).sendToTarget();
					pos = 0;
					break;
				default:
					//LLog.d(Globals.TAG, CLASSTAG + ".run: " + StringUtil.toHexString(buffer, bytes)); //.toReadableString(buffer, bytes, true));
					handler.obtainMessage(MESSAGE_READBYTES, -1, -1, Arrays.copyOf(buffer, bytes)).sendToTarget();
					pos = 0;
					break;
				}
				if (commands != null) for (Command command: commands) {
					if (command.maySend()) synchronized(this) {
						write(command.toString());
						break;
					}
				}	

			} catch (IOException e) {
				LLog.e(Globals.TAG, CLASSTAG + ".run: Disconnected" + e.getMessage());
				//connectionLost();
				break;
			} 
		}
		try {
			synchronized(this) {
				mmSocket.close();
			}
		} catch (IOException e) {
			LLog.e(Globals.TAG, CLASSTAG + ".run: Close of socket failed", e);
		}
		handler.obtainMessage(MESSAGE_TERMINATED).sendToTarget();
	}

	public void write(byte[] buffer) {
		if (mayRun) try {
			mmOutStream.write(buffer);
		} catch (IOException e) {
			LLog.e(Globals.TAG, CLASSTAG + ".write: Exception during write", e);
		}
	}

	public void write(String message) {
		write(message.getBytes());	
	}

	public void close() {
		try {
			mayRun = false;
			synchronized(this) {
				mmSocket.close();
			}
			interrupt();
		} catch (IOException e) {
			LLog.e(Globals.TAG, CLASSTAG + ".cancel: Close of socket failed", e);
		}
	}
}


