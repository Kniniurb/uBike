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
package com.plusot.senselib.time;

import java.lang.ref.WeakReference;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.plusot.common.Globals;
import com.plusot.senselib.values.Device;
import com.plusot.senselib.values.DeviceType;
import com.plusot.senselib.values.Manager;
import com.plusot.senselib.values.ValueType;

public class TimeManager implements Manager {
	private final static String NEW_TIME = "New_Time";

	//private static final String CLASSTAG = TimeManager.class.getSimpleName();
	private TimeDevice timeDevice;


	public static class TimeDevice extends Device {
		private Clock clock = null;
		private MyHandler timeHandler = new MyHandler(this);

		public TimeDevice(Context context) {
			super(DeviceType.TIMER);
		}

		private static class MyHandler extends Handler {
			private WeakReference<TimeDevice> device;

			public MyHandler(final TimeDevice device) {
				this.device = new WeakReference<TimeDevice>(device);
			}

			public void handleMessage (Message msg) {	
				Bundle bundle = msg.getData();
				for (String key: bundle.keySet()) {
					if (key.equals(NEW_TIME)) {
						long newTime = msg.getData().getLong(NEW_TIME);
						TimeDevice dev = device.get();
						if (dev != null) dev.fireOnDeviceValueChanged(ValueType.TIME, newTime, newTime);
					}
				}
			}
		};


		@Override
		public boolean isActive() {
			return true;
		}

		@Override
		public void closeIt() {
			if (!opened || clock == null) return;
			clock.stopIt();
			clock = null;
		}

		@Override
		public void openIt(boolean reOpen) {
			if (clock == null) {
				clock = new Clock();
				clock.start();
			}
			opened = true;
		}

		private class Clock extends Thread {
			private boolean mayRun = true;

			@Override
			public void run() {
				while (mayRun) {
					try {
						Thread.sleep(200);
					} catch (InterruptedException e) {

					}
					Message msg = new Message();
					Bundle bundle = new Bundle();
					bundle.putLong(NEW_TIME, System.currentTimeMillis());
					msg.setData(bundle);
					timeHandler.sendMessage(msg);
				}
			}

			public void stopIt() {
				mayRun = false;
				interrupt();
			}
		}

		@Override
		public void fadeOut(ValueType type) {
			// TODO Auto-generated method stub

		}

	}

	public TimeManager() { 
		timeDevice = new TimeDevice(Globals.appContext);
		//Device.addDevice(timeDevice);
	}

	@Override
	public boolean init() {
		timeDevice.openIt(true);
		return true;
	}


	@Override
	public void destroy() {
		if (timeDevice != null) timeDevice.destroy();

	}


	@Override
	public void onStart() {
		// TODO Auto-generated method stub

	}
	
	@Override
	public void onStop() {
		// TODO Auto-generated method stub

	}
	
	@Override
	public void onResume() {
		// TODO Auto-generated method stub

	}

//	@Override
//	public EnumSet<DeviceType> supportedDevices() {
//		return EnumSet.of(DeviceType.TIMER);
//	}

}
