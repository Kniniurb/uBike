package com.plusot.senselib.bluetooth;

import android.os.Handler;
import android.os.Looper;

import com.plusot.senselib.values.Device;
import com.plusot.senselib.values.DeviceType;
import com.plusot.senselib.values.ValueType;

public class BleDevice extends Device {
	//private static final String CLASSTAG = BleDevice.class.getSimpleName();
	final Handler handler = new Handler(Looper.getMainLooper());
	
	
	public BleDevice() {
		super(DeviceType.BLE);	
	}
	
	public void onData(final ValueType type, final Object value, final long timeStamp) {
		
		//LLog.d(Globals.TAG, CLASSTAG + ".onData " + type + " = " + value);
		handler.post(new Runnable() {
			@Override
			public void run() {
				fireOnDeviceValueChanged(type, value, timeStamp);
				
			}
		});
	}


	@Override
	public void fadeOut(ValueType type) {
	}


	@Override
	public boolean isActive() {
		return true;
	}

	
	


}