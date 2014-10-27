package com.plusot.senselib.ant;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.plusot.common.Globals;
import com.plusot.common.share.LLog;
import com.plusot.common.util.SleepAndWake;
import com.plusot.common.util.Watchdog;
import com.plusot.javacommon.util.StringUtil;
import com.plusot.senselib.ant.AntPlusManager.ChannelStates;
import com.plusot.senselib.settings.PreferenceKey;
import com.plusot.senselib.values.DeviceType;
import com.plusot.senselib.values.Manager;

import java.util.EnumMap;

public class AntDeviceManager implements AntPlusManager.Callbacks, AntMesgCallback, Manager, Watchdog.Watchable {
	private final static String CLASSTAG = AntDeviceManager.class.getSimpleName();
//	private final static String FILLTAG = "          ";
	private static final boolean DEBUG = false;
	private AntPlusManager mAntManager = null;
	private boolean mBound = false;
	private AntProfile current = null;
	private EnumMap<AntProfile, Long> lastTry = new EnumMap<AntProfile, Long>(AntProfile.class);
	private static final long TIME_BETWEEN_OPEN = 1000;

	private EnumMap<AntProfile, AntDevice> antDevices = new EnumMap<AntProfile, AntDevice>(AntProfile.class);

	public AntDeviceManager() {
		for (AntProfile profile: AntProfile.values()) {
			AntDevice device = null;
			switch(profile) {
			case BIKECADENCE:
				device = new AntCadenceDevice(this);
				break;
			case BIKEPOWER:
				device = new AntPowerDevice(this); 
				break;
			case BIKESPEED:
				device = new AntSpeedDevice(this); 
				break;
			case BIKESPEEDCADENCE:
				device = new AntSpeedCadenceDevice(this); 
				break;
			case HEARTRATE:
				device =  new AntHrmDevice(this); 
				break;
			case STRIDE:
				device = new AntStrideDevice(this); 
				break;
			case SUUNTODUAL:
				device = new SuuntoHrmDevice(this); 
				break;
			}
			if (device != null) {
				antDevices.put(profile, device); 
				//Device.addDevice(device);
			}

		}
		Watchdog.getInstance().add(this, TIME_BETWEEN_OPEN);
	}

	private final ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name)
		{
			//This is very unlikely to happen with a local service (ie. one in the same process)
			mAntManager.setCallbacks(null);
			mAntManager = null;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service)
		{
			mAntManager = ((AntPlusService.LocalBinder)service).getManager();
			mAntManager.setCallbacks(AntDeviceManager.this);
			loadAntSettings();
			notifyAntStateChanged();
		}
	};

	@Override
	public void onStart() {
		if (DEBUG) LLog.d(Globals.TAG, CLASSTAG + ".onStart");
		Context context = Globals.appContext;
		mBound = context.bindService(new Intent(context, AntPlusService.class), mConnection, Context.BIND_AUTO_CREATE);
	}
	
	@Override
	public void onStop() {
		destroy();
	}

	@Override
	public void destroy() {
		Context context = Globals.appContext;

		if (mAntManager != null) {
			saveAntSettings();
			mAntManager.setCallbacks(null);
		}
		if (mBound) {
			context.unbindService(mConnection);
		}
		this.disable();
	}

	private void saveAntSettings() {
		for (AntProfile profile: AntProfile.values()) {
			PreferenceKey.setDeviceNumber(profile.ordinal(), mAntManager.getDeviceNumber(profile));
		}
	}

	private void loadAntSettings() {
		if (PreferenceKey.USEPAIRING.isTrue()) {
			for (AntProfile profile : AntProfile.values()) {
				int nr = PreferenceKey.getDeviceNumber(profile.ordinal());
				if (DEBUG)  LLog.d(Globals.TAG, CLASSTAG + "Devicenumber for: " + profile + " = " + nr);
				mAntManager.setDeviceNumber(profile, (short) nr);
			}
		}
	}

	@Override
	public void errorCallback(String method, String description, Throwable e) {
		LLog.e(Globals.TAG,  CLASSTAG + " Error in AntPlusManager." + method + ": " + description, e);	
		mAntManager.clearChannelStates();
	}

	@Override
	public void notifyAntStateChanged() {
		if (mAntManager.checkAntState()) {
			if (DEBUG)  LLog.d(Globals.TAG,  CLASSTAG + " AntState: OK");
		} else {
			if (DEBUG)  LLog.d(Globals.TAG,  CLASSTAG + " AntState: " + mAntManager.getAntStateText());
		}
		if (mAntManager.isEnabled()) {
			if (DEBUG)  LLog.d(Globals.TAG,  CLASSTAG + " AntState: Enabled");
		} else {
			if (DEBUG)  LLog.d(Globals.TAG,  CLASSTAG + " AntState: Disabled");
		}
	}

	@Override
	public void notifyChannelStateChanged(final AntProfile profile) {
		ChannelStates state = mAntManager.getState(profile);
		if (DEBUG)  LLog.d(Globals.TAG,  CLASSTAG + " State of " + profile + " = " + state);
		if (state.equals(ChannelStates.OFFLINE)) {
			new SleepAndWake(new SleepAndWake.Listener() {

				@Override
				public void onWake() {
					if (DEBUG)  LLog.d(Globals.TAG,  CLASSTAG + " Opening " + profile + " after state: " + ChannelStates.OFFLINE);
					open(profile);

				}
			}, 1000);
		}
	}


	@Override
	public void notifyChannelDataChanged(DeviceType deviceType) {
		if (DEBUG)  LLog.d(Globals.TAG,  CLASSTAG + " Data for " + deviceType);
//		for (ValueType type: deviceType.getValueTypes()) {
//			//if (DEBUG)  LLog.d(Globals.TAG,  FILLTAG + " Type = " + type);
//			Result result = mAntManager.getValue(type);
//			if (result != null) if (DEBUG)  LLog.d(Globals.TAG,  FILLTAG + " " + type + " " + mAntManager.getValue(type).getValue());	
//		}		
	}


	@Override
	public void onDecode(AntProfile profile, byte[] bytes) {
		if (DEBUG)  LLog.d(Globals.TAG,  CLASSTAG + " Decode data for " + profile + " = " + StringUtil.toHexString(bytes));
		AntDevice device = null;
		if ((device = antDevices.get(profile)) == null) {
			switch(profile) {
			case BIKECADENCE:
				antDevices.put(profile, new AntCadenceDevice(this)); 
				break;
			case BIKEPOWER:
				antDevices.put(profile, new AntPowerDevice(this)); 
				break;
			case BIKESPEED:
				antDevices.put(profile, new AntSpeedDevice(this)); 
				break;
			case BIKESPEEDCADENCE:
				antDevices.put(profile, new AntSpeedCadenceDevice(this)); 
				break;
			case HEARTRATE:
				antDevices.put(profile, new AntHrmDevice(this)); 
				break;
			case STRIDE:
				antDevices.put(profile, new AntStrideDevice(this)); 
				break;
			case SUUNTODUAL:
				antDevices.put(profile, new SuuntoHrmDevice(this)); 
				break;
				//			case WEIGHT:
				//				break;
			default:
				break;

			}
			device = antDevices.get(profile);
			//			device.addListener(antListener);
		}
		if (device != null) device.decode(bytes);

	}

	public void enable() {
		if(mAntManager == null) return;
		if(!mAntManager.isEnabled()) mAntManager.doEnable();
	}

	public void disable() {
		if(mAntManager == null) return;
		if(mAntManager.isEnabled()) mAntManager.doDisable();
	}

	public boolean isEnabled() {
		if(mAntManager == null) return false;
		return mAntManager.isEnabled();
	}

	public void open(AntProfile profile) {
		// If no channels are open, reset ANT
		lastTry.put(profile, System.currentTimeMillis());
		if(mAntManager == null) return;
		if (mAntManager.isAllOff()) {

			if (DEBUG)  LLog.d(Globals.TAG, CLASSTAG + ".open: No channels open, reseting ANT");
			mAntManager.openChannel(profile, true);
			mAntManager.requestReset();
		} else {
			if (!mAntManager.isChannelOpen(profile)) {
				// Configure and open channel
				if (DEBUG) LLog.d(Globals.TAG, CLASSTAG + ".open (" + profile + "): Open channel");
				mAntManager.openChannel(profile, false);
			} else {
				if (DEBUG) LLog.d(Globals.TAG, CLASSTAG + ".open (" + profile + "): Already open");
			}
		}
	}

	public void close(AntProfile profile) {
		// If no channels are open, reset ANT
		if(mAntManager == null) return;

		if (mAntManager.isChannelOpen(profile)) {
			// Close channel
			if (DEBUG) LLog.d(Globals.TAG, CLASSTAG + ".close (" + profile + "): Close channel");
			mAntManager.closeChannel(profile);
		}
	}

	@Override
	public void sendMessage(AntProfile profile, byte[] bytes) {
		if (mAntManager != null) mAntManager.sendMessage(profile, bytes);

	}

	public void reset() {
		if(mAntManager == null) return;
		mAntManager.requestReset();
	}

	@Override
	public int getDeviceNumber(AntProfile profile) {
		if(mAntManager == null) return -1;
		return mAntManager.getDeviceNumber(profile);
	}

	@Override
	public boolean init() {
		return true;
	}

	public boolean isActive(AntProfile profile) {
		if(mAntManager == null) return false;
		if (profile == null) return false;
		return mAntManager.getState(profile).equals(AntPlusManager.ChannelStates.TRACKING_DATA);

	}

	public void calibratePower() {
		//for (int i = 0; i < SenseGlobals.maxPowerDevices; i++) {
		AntPowerDevice device = (AntPowerDevice) antDevices.get(AntProfile.BIKEPOWER);
		if (device == null) {
			LLog.e(Globals.TAG, CLASSTAG + ".calibratePower: No Power device to calibrate");
			return;
		}
		device.calibrate();
		//}
	}

	@Override
	public void onWatchdogCheck(long count) {
		if (DEBUG) LLog.d(Globals.TAG, CLASSTAG + ".onWatchdogCheck current profile = " + current);
		if (mAntManager == null) {
			//if (DEBUG)  LLog.d(Globals.TAG, CLASSTAG + ".onWatchdogCheck: AntManager = null");
			return;
		}
		if (!mAntManager.isEnabled()) {
			//if (DEBUG)  LLog.d(Globals.TAG, CLASSTAG + ".onWatchdogCheck: AntManager is not enabled yet.");
			return;
		}
		if (current == null) 
			current = AntProfile.firstProfile();
		else
			current = AntProfile.nextProfile(current);
		if (
				current != null && 
				(lastTry.get(current) == null || System.currentTimeMillis() - lastTry.get(current) > 60000) && 
				!mAntManager.isChannelOpen(current)) {
			open(current);

		}
	}

	@Override
	public void onWatchdogClose() {
	}

	public void resetPairing() {
		if (mAntManager == null) return;
		if (DEBUG)  LLog.d(Globals.TAG, CLASSTAG + ".resetPairing");

		for (AntProfile profile: AntProfile.values()) {
			PreferenceKey.setDeviceNumber(profile.ordinal(), 0);
			mAntManager.resetDeviceNumber(profile);
		}
	}

	public void retreivePairing() {
		loadAntSettings();
	}

	@Override
	public void onResume() {
	}

}
