/*
 * Copyright 2010 Dynastream Innovations Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.plusot.senselib.ant;

import java.lang.reflect.Field;
import java.util.EnumMap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Settings;
import android.widget.Toast;

import com.dsi.ant.AntDefine;
import com.dsi.ant.AntInterface;
import com.dsi.ant.AntInterfaceIntent;
import com.dsi.ant.AntMesg;
import com.dsi.ant.exception.AntInterfaceException;
import com.dsi.ant.exception.AntServiceNotConnectedException;
import com.plusot.common.Globals;
import com.plusot.common.share.LLog;
import com.plusot.common.util.ToastHelper;
import com.plusot.javacommon.util.StringUtil;
import com.plusot.senselib.R;
import com.plusot.senselib.settings.PreferenceKey;
import com.plusot.senselib.values.DeviceType;
import com.plusot.senselib.values.ValueType;

/**
 * This class handles connecting to the AntRadio service, setting up the channels,
 * and processing Ant events.
 */
public class AntPlusManager {
	private static final String CLASSTAG = AntPlusManager.class.getSimpleName();
	public static final boolean DEBUG = false;
	public static final short WILDCARD = 0;
	private static final byte DEFAULT_BIN = 0;

	/**
	 * Defines the interface needed to work with all call backs this class makes
	 */
	public interface Callbacks {
		public void errorCallback(String method, String message, Throwable e);
		public void notifyAntStateChanged();
		public void notifyChannelStateChanged(AntProfile profile);
		public void notifyChannelDataChanged(DeviceType deviceType);
		public void onDecode(AntProfile profile, byte[] bytes);
	}

	/** The interface to the ANT radio. */
	private AntInterface mAntReceiver;

	/** Is the ANT background service connected. */
	private boolean mServiceConnected = false;

	/** Stores which ANT status Intents to receive. */
	private IntentFilter statusIntentFilter;

	/** Flag to know if an ANT Reset was triggered by this application. */
	private boolean mAntResetSent = false;

	/** Flag if waiting for ANT_ENABLED. Default is now false, We assume ANT is disabled until told otherwise.*/
	private boolean mEnabling = false;

	/** Flag if waiting for ANT_DISABLED. Default is false, will be set to true when a disable is attempted. */
	private boolean mDisabling = false;

	//This string will eventually be provided by the system or by AntLib
	/** String used to represent ant in the radios list. */
	private static final String RADIO_ANT = "ant";

	/** Description of ANT's current state */
	private String mAntStateText = "";

	/** Possible states of a device channel */
	public enum ChannelStates
	{
		/** Channel was explicitly closed or has not been opened */
		CLOSED,

		/** User has requested we open the channel, but we are waiting for a reset */
		PENDING_OPEN,

		/** Channel is opened, but we have not received any data yet */
		SEARCHING,

		/** Channel is opened and has received status data from the device most recently */
		TRACKING_STATUS,

		/** Channel is opened and has received measurement data most recently */
		TRACKING_DATA,

		/** Channel is closed as the result of a search timeout */
		OFFLINE
	}

	/** If this application has control of the ANT Interface. */
	private boolean mClaimedAntInterface;

	private static class AntSensor {
		int mDeviceNumber = 0;
		AntChannelConfiguration channelConfig = new AntChannelConfiguration();
		boolean mDeferredStart = false;
		ChannelStates mState = ChannelStates.CLOSED;
		final AntProfile profile;

		public AntSensor(AntProfile profile) {
			this.profile = profile;
		}

		@Override
		public String toString() {
			return "Sensor_" + profile.toString();
		}
	}
	public static abstract class Result {
		public abstract Number getValue();
	}

	public static class LongResult extends Result {
		private final long value;

		public LongResult(final long value) {
			this.value = value;
		}

		@Override
		public Number getValue() {
			return value;
		}		

	}

	public static class DoubleResult extends Result {
		private final double value;

		public DoubleResult(final double value) {
			this.value = value;
		}

		@Override
		public Number getValue() {
			return value;
		}		

	}
	public static class IntResult extends Result {
		private final int value;

		public IntResult(final int value) {
			this.value = value;
		}

		@Override
		public Number getValue() {
			return value;
		}		

	}


	private EnumMap<AntProfile, AntSensor> devices = new EnumMap<AntProfile, AntSensor>(AntProfile.class);
	private EnumMap<ValueType, Result> results = new EnumMap<ValueType, Result>(ValueType.class);

	/**
	 * The possible HRM page toggle bit states.
	 */
	public enum HRMStatePage
	{
		/** Toggle bit is 0. */
		TOGGLE0,

		/** Toggle bit is 1. */
		TOGGLE1,

		/** Initialising (bit value not checked). */
		INIT,

		/** Extended pages are valid. */
		EXT
	}

	private Context mContext;

	private Callbacks mCallbackSink;

	/**
	 * Default Constructor
	 */
	public AntPlusManager()
	{
		if (DEBUG)  LLog.d(Globals.TAG, CLASSTAG + " AntChannelManager: enter Constructor");

		for (AntProfile profile: AntProfile.values()) devices.put(profile, new AntSensor(profile));

		mClaimedAntInterface = false;

		// ANT intent broadcasts.
		statusIntentFilter = new IntentFilter();
		statusIntentFilter.addAction(AntInterfaceIntent.ANT_ENABLED_ACTION);
		statusIntentFilter.addAction(AntInterfaceIntent.ANT_ENABLING_ACTION);
		statusIntentFilter.addAction(AntInterfaceIntent.ANT_DISABLED_ACTION);
		statusIntentFilter.addAction(AntInterfaceIntent.ANT_DISABLING_ACTION);
		statusIntentFilter.addAction(AntInterfaceIntent.ANT_RESET_ACTION);
		statusIntentFilter.addAction(AntInterfaceIntent.ANT_INTERFACE_CLAIMED_ACTION);
		statusIntentFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);

		mAntReceiver = new AntInterface();
	}


	/**
	 * Creates the connection to the ANT service back-end.
	 */
	public boolean start(Context context)
	{
		boolean initialised = false;

		mContext = context;

		if(AntInterface.hasAntSupport(mContext)) {
			mContext.registerReceiver(mAntStatusReceiver, statusIntentFilter);

			if(!mAntReceiver.initService(mContext, mAntServiceListener))
			{
				// Need the ANT Radio Service installed.
				LLog.e(Globals.TAG, CLASSTAG + " AntChannelManager Constructor: No ANT Service.");
				requestServiceInstall();
			} else {
				mServiceConnected = mAntReceiver.isServiceConnected();

				if(mServiceConnected) {
					try {
						mClaimedAntInterface = mAntReceiver.hasClaimedInterface();
						if(mClaimedAntInterface) {
							receiveAntRxMessages(true);
						}
						if (DEBUG)  LLog.d(Globals.TAG, CLASSTAG + ".start: Claimed interface");

					} catch (AntInterfaceException e) {
						antError("start", "ANT+ error claiming interface ", e);
					}
				}

				initialised = true;
			}
		}

		return initialised;
	}

	/**
	 * Requests that the user install the needed service for ant
	 */
	private void requestServiceInstall()
	{
		Toast installNotification = Toast.makeText(mContext, mContext.getResources().getString(R.string.notify_install), Toast.LENGTH_LONG);
		installNotification.show();

		AntInterface.goToMarket(mContext);
	}

	public void setCallbacks(Callbacks callbacks)
	{
		mCallbackSink = callbacks;
	}

	//Getters and setters

	public boolean isServiceConnected()
	{
		return mServiceConnected;
	}

	public int getDeviceNumber(AntProfile profile) {
		AntSensor device = devices.get(profile);	
		return device.mDeviceNumber;
	}

	public void setDeviceNumber(AntProfile profile, int deviceNumber) {
		AntSensor device = devices.get(profile);	
		if (device.mDeviceNumber == 0) device.mDeviceNumber = deviceNumber;
	}
	
	
	public void resetDeviceNumber(AntProfile profile) {
		AntSensor device = devices.get(profile);
		if (device.mState.equals(ChannelStates.TRACKING_DATA)) return;
		device.mDeviceNumber = 0;
	}

	public Result getValue(ValueType type) {
		if (type == null) return null;
		return results.get(type);
	}

	public ChannelStates getState(AntProfile profile) {
		return devices.get(profile).mState;
	}

	public String getAntStateText()
	{
		return mAntStateText;
	}

	/**
	 * Checks if ANT can be used by this application
	 * Sets the AntState string to reflect current status.
	 * @return true if this application can use the ANT chip, false otherwise.
	 */
	public boolean checkAntState()
	{
		try
		{
			if(!AntInterface.hasAntSupport(mContext))
			{
				LLog.w(Globals.TAG, CLASSTAG + " updateDisplay: ANT not supported");

				mAntStateText = mContext.getString(R.string.ant_not_supported);
				return false;
			}
			else if(isAirPlaneModeOn())
			{
				mAntStateText = mContext.getString(R.string.ant_airplane_mode);
				return false;
			}
			else if(mEnabling)
			{
				mAntStateText = mContext.getString(R.string.ant_enabling);
				return false;
			}
			else if(mDisabling)
			{
				mAntStateText = mContext.getString(R.string.ant_disabling);
				return false;
			}
			else if(mServiceConnected)
			{
				if(!mAntReceiver.isEnabled())
				{
					mAntStateText = mContext.getString(R.string.ant_disabled);
					return false;
				}
				if(mAntReceiver.hasClaimedInterface() || mAntReceiver.claimInterface())
				{
					return true;
				}
				else
				{
					mAntStateText = mContext.getString(R.string.ant_in_use);
					return false;
				}
			}
			else
			{
				LLog.w(Globals.TAG, CLASSTAG + " updateDisplay: Service not connected");

				mAntStateText = mContext.getString(R.string.ant_disabled);
				return false;
			}
		}
		catch(AntInterfaceException e)
		{
			antError("checkAntState","Error while checking", e);
			return false;
		}
	}

	/**
	 * Attempts to claim the Ant interface
	 */
	public void tryClaimAnt()
	{
		try
		{
			mAntReceiver.requestForceClaimInterface(Globals.TAG);
		}
		catch(AntInterfaceException e)
		{
			antError("tryClaimAnt", "Error during requestForceClaimInterface", e);
		}
	}

	/**
	 * Unregisters all our receivers in preparation for application shutdown
	 */
	public void shutDown()
	{
		try
		{
			mContext.unregisterReceiver(mAntStatusReceiver);
		}
		catch(IllegalArgumentException e)
		{
			// Receiver wasn't registered, ignore as that's what we wanted anyway
		}

		receiveAntRxMessages(false);

		if(mServiceConnected)
		{
			try
			{
				if(mClaimedAntInterface)
				{
					if (DEBUG)  LLog.d(Globals.TAG, CLASSTAG + " AntChannelManager.shutDown: Releasing interface");

					mAntReceiver.releaseInterface();
				}

				mAntReceiver.stopRequestForceClaimInterface();
			}
			catch(AntServiceNotConnectedException e)
			{
				// Ignore as we are disconnecting the service/closing the app anyway
			}
			catch(AntInterfaceException e)
			{
				LLog.w(Globals.TAG, CLASSTAG + " Exception in AntChannelManager.shutDown", e);
			}

			mAntReceiver.releaseService();
		}
	}

	/**
	 * Class for receiving notifications about ANT service state.
	 */
	private AntInterface.ServiceListener mAntServiceListener = new AntInterface.ServiceListener()
	{
		public void onServiceConnected()
		{
			if (DEBUG)  LLog.d(Globals.TAG, CLASSTAG + " mAntServiceListener onServiceConnected()");

			mServiceConnected = true;

			try
			{

				mClaimedAntInterface = mAntReceiver.hasClaimedInterface();

				if (mClaimedAntInterface)
				{
					// mAntMessageReceiver should be registered any time we have
					// control of the ANT Interface
					receiveAntRxMessages(true);
				} else {
					// Need to claim the ANT Interface if it is available, now
					// the service is connected
					mClaimedAntInterface = mAntReceiver.claimInterface();
				}
			} catch (AntInterfaceException e) {
				antError("mAntServiceListener.onServiceConnected", "Error during interface claim",e);
			}

			if (DEBUG)  LLog.d(Globals.TAG, CLASSTAG + " mAntServiceListener Displaying icons only if radio enabled");
			if(mCallbackSink != null)
				mCallbackSink.notifyAntStateChanged();
		}

		public void onServiceDisconnected()
		{
			if (DEBUG)  LLog.d(Globals.TAG, CLASSTAG + " mAntServiceListener onServiceDisconnected()");

			mServiceConnected = false;
			mEnabling = false;
			mDisabling = false;

			if (mClaimedAntInterface)
			{
				receiveAntRxMessages(false);
			}

			if(mCallbackSink != null)
				mCallbackSink.notifyAntStateChanged();
		}
	};

	/**
	 * Configure the ANT radio to the user settings.
	 */
	public void setAntConfiguration()
	{
		try
		{
			if(mServiceConnected && mClaimedAntInterface && mAntReceiver.isEnabled())
			{
				try
				{
					// Event Buffering Configuration
//					if(DEFAULT_BUFFER_THRESHOLD > 0)
//					{
//						// No buffering by interval here.
//						mAntReceiver.ANTConfigEventBuffering((short)0xFFFF, DEFAULT_BUFFER_THRESHOLD, (short)0xFFFF, DEFAULT_BUFFER_THRESHOLD);
//					}
//					else
//					{
						mAntReceiver.ANTDisableEventBuffering();
//					}
				}
				catch(AntInterfaceException e)
				{
					LLog.e(Globals.TAG, CLASSTAG + " Could not configure event buffering", e);
				}
			}
			else
			{
				LLog.i(Globals.TAG, CLASSTAG + " Can't set event buffering right now.");
			}
		} catch (AntInterfaceException e)
		{
			LLog.e(Globals.TAG, CLASSTAG + " Problem checking enabled state.");
		}
	}

	/**
	 * Display to user that an error has occured communicating with ANT Radio.
	 */
	private void antError(String method, String message, Throwable e)
	{
		mAntStateText = mContext.getString(R.string.ant_error);
		if(mCallbackSink != null)
			mCallbackSink.errorCallback(method, message, e);
	}

	/**
	 * Opens a given channel using the proper configuration for the channel's sensor type.
	 * @param channel The channel to Open.
	 * @param deferToNextReset If true, channel will not open until the next reset.
	 */
//	public void openChannel(byte channel, boolean deferToNextReset) {
//		AntProfile profile = AntProfile.fromChannel(channel);
//		if (profile == null) return;
//		openChannel(profile, deferToNextReset);
//	}

	public void openChannel(AntProfile profile, boolean deferToNextReset) {
		if (!suuntoSet) setNetworkKeySuunto();
		AntSensor device = devices.get(profile);

		if (PreferenceKey.USEPAIRING.isTrue() && device.mDeviceNumber == 0) return;
			
		if (DEBUG)  LLog.i(Globals.TAG, CLASSTAG + ".openChannel: Starting service.");
		mContext.startService(new Intent(mContext, AntPlusService.class));
		if (Globals.testing.isVerbose()) ToastHelper.showToastShort(mContext.getString(R.string.ant_open_channel, profile));
		
		if (!deferToNextReset) {
			device.channelConfig.TransmissionType = 0; // Set to 0 for wild card search
			device.channelConfig.freq = profile.getFreq();
			device.channelConfig.proxSearch = DEFAULT_BIN;
			device.channelConfig.deviceNumber = device.mDeviceNumber;
			device.channelConfig.deviceType = profile.getAntDeviceType();
			device.channelConfig.period = profile.getPeriod();
			changeState(device, ChannelStates.PENDING_OPEN);
			// Configure and open channel
			antChannelSetup(
					profile.getNetwork(), // Network: 1 (ANT+)
					profile.getChannel() // channelConfig[channel] holds all the required info
					);
		} else {
			device.mDeferredStart = true;
			device.mState = ChannelStates.PENDING_OPEN;
		}
	}

	/**
	 * Attempts to cleanly close a specified channel 
	 * @param channel The channel to close.
	 */
	public void closeChannel(byte channel) {
		AntProfile profile = AntProfile.fromChannel(channel);
		if (profile == null) return;
		closeChannel(profile);
	}

	public void closeChannel(AntProfile profile) {
		AntSensor device = devices.get(profile);

		device.channelConfig.isInitializing = false;
		device.channelConfig.isDeinitializing = true;

		changeState(device, ChannelStates.CLOSED);

		try {
			mAntReceiver.ANTCloseChannel(profile.getChannel());
			// Unassign channel after getting channel closed event
		} catch (AntInterfaceException e) {
			LLog.w(Globals.TAG, CLASSTAG + " closeChannel: could not cleanly close channel " + profile.getChannel() + ".");
			antError("closeChannel", "Could not close channel", e);
		}
		if (isAllOff()) {
			if (DEBUG)  LLog.i(Globals.TAG, CLASSTAG + " Stopping service.");
			mContext.stopService(new Intent(mContext, AntPlusService.class));
		}
	}

	public boolean isAllOff() {
		for (AntSensor dev: devices.values()) {
			if (dev.mState != ChannelStates.CLOSED && dev.mState != ChannelStates.OFFLINE) return false;
		}
		return false;
	}

	/**
	 * Resets the channel state machines, used in error recovery.
	 */
	public void clearChannelStates()
	{
		if (DEBUG)  LLog.i(Globals.TAG, CLASSTAG + " Stopping service.");
		mContext.stopService(new Intent(mContext, AntPlusService.class));
		for (AntSensor device: devices.values()) {
			changeState(device, ChannelStates.CLOSED);
		}
	}

	/** check to see if a channel is open */
	public boolean isChannelOpen(AntProfile profile) {
		AntSensor device = devices.get(profile);
		if(device.mState == ChannelStates.CLOSED || device.mState == ChannelStates.OFFLINE) return false;
		return true;
	}

//	public boolean isChannelOpen(byte channel) {
//		AntProfile profile = AntProfile.fromChannel(channel);
//		if (profile == null) return false;
//		return isChannelOpen(profile);
//	}

	/** request an ANT reset */
	public void requestReset() {
		try
		{
			mAntResetSent = true;
			mAntReceiver.ANTResetSystem();
			setAntConfiguration();
			//blabla
			//setNetworkKeySuunto(); 
			//blabla
		} catch (AntInterfaceException e) {
			LLog.e(Globals.TAG, CLASSTAG + " requestReset: Could not reset ANT", e);
			mAntResetSent = false;
			//Cancel pending channel open requests
			for (AntSensor device: devices.values()) {
				if(device.mDeferredStart) {
					device.mDeferredStart = false;
					changeState(device, ChannelStates.CLOSED);
				}
			}
		}
	}

	/**
	 * Check if ANT is enabled
	 * @return True if ANT is enabled, false otherwise.
	 */
	public boolean isEnabled()
	{
		if(mAntReceiver == null || !mAntReceiver.isServiceConnected())
			return false;
		try
		{
			return mAntReceiver.isEnabled();
		} catch (AntInterfaceException e)
		{
			LLog.w(Globals.TAG, CLASSTAG + " Problem checking enabled state.");
			return false;
		}
	}

	/**
	 * Attempt to enable the ANT chip.
	 */
	public void doEnable()
	{
		if(mAntReceiver == null || mDisabling || isAirPlaneModeOn())
			return;
		try
		{
			mAntReceiver.enable();
		} catch (AntInterfaceException e)
		{
			//Not much error recovery possible.
			LLog.e(Globals.TAG, CLASSTAG + " Could not enable ANT.");
			return;
		}
	}

	/**
	 * Attempt to disable the ANT chip.
	 */
	public void doDisable()
	{
		if(mAntReceiver == null || mEnabling)
			return;
		try
		{
			mAntReceiver.disable();
		} catch (AntInterfaceException e)
		{
			//Not much error recovery possible.
			LLog.e(Globals.TAG, CLASSTAG + " Could not enable ANT.");
			return;
		}
	}

	/** Receives all of the ANT status intents. */
	private final BroadcastReceiver mAntStatusReceiver = new BroadcastReceiver() 
	{      
		public void onReceive(Context context, Intent intent) 
		{
			String ANTAction = intent.getAction();

			if (DEBUG)  LLog.d(Globals.TAG, CLASSTAG + ".mAntStatusReceiver.onReceive: " + ANTAction);
			if (ANTAction.equals(AntInterfaceIntent.ANT_ENABLING_ACTION))
			{
				if (DEBUG) LLog.i(Globals.TAG, CLASSTAG + " onReceive: ANT ENABLING");
				mEnabling = true;
				mDisabling = false;
				mAntStateText = mContext.getString(R.string.ant_enabling);
				if(mCallbackSink != null)
					mCallbackSink.notifyAntStateChanged();
			}
			else if (ANTAction.equals(AntInterfaceIntent.ANT_ENABLED_ACTION)) 
			{
				if (DEBUG) LLog.i(Globals.TAG, CLASSTAG + " onReceive: ANT ENABLED");
				mEnabling = false;
				mDisabling = false;
				//setNetworkKeySuunto();
				if(mCallbackSink != null) mCallbackSink.notifyAntStateChanged();

			}
			else if (ANTAction.equals(AntInterfaceIntent.ANT_DISABLING_ACTION))
			{
				if (DEBUG) LLog.i(Globals.TAG, CLASSTAG + " onReceive: ANT DISABLING");
				mEnabling = false;
				mDisabling = true;
				mAntStateText = mContext.getString(R.string.ant_disabling);
				if(mCallbackSink != null)
					mCallbackSink.notifyAntStateChanged();
			}
			else if (ANTAction.equals(AntInterfaceIntent.ANT_DISABLED_ACTION)) 
			{
				if (DEBUG) LLog.i(Globals.TAG, CLASSTAG + " onReceive: ANT DISABLED");
				for (AntSensor device: devices.values()) {
					changeState(device, ChannelStates.CLOSED);
				}
				mAntStateText = mContext.getString(R.string.ant_disabled);

				mEnabling = false;
				mDisabling = false;

				if(mCallbackSink != null) mCallbackSink.notifyAntStateChanged();
				LLog.i(Globals.TAG, CLASSTAG + " Stopping service.");
				mContext.stopService(new Intent(mContext, AntPlusService.class));
			}
			else if (ANTAction.equals(AntInterfaceIntent.ANT_RESET_ACTION))
			{
				if (DEBUG)  LLog.d(Globals.TAG, CLASSTAG + " onReceive: ANT RESET");

				LLog.i(Globals.TAG, CLASSTAG + " Stopping service.");
				mContext.stopService(new Intent(mContext, AntPlusService.class));

				if(false == mAntResetSent)
				{
					if (DEBUG) LLog.d(Globals.TAG, CLASSTAG + " onReceive: ANT RESET: Resetting state");

					for (AntSensor device: devices.values()) {
						if(device.mState != ChannelStates.CLOSED) changeState(device, ChannelStates.CLOSED);
					}
					setNetworkKeySuunto();
				} else {
					mAntResetSent = false;
					//Reconfigure event buffering
					setAntConfiguration();
					
					setNetworkKeySuunto();
					
					//Check if opening a channel was deferred, if so open it now.
					for (AntSensor device: devices.values()) {
						if(device.mDeferredStart) {
							openChannel(device.profile, false);
							device.mDeferredStart = false;
						}
					}

				}
				//setNetworkKeySuunto();
			}
			else if (ANTAction.equals(AntInterfaceIntent.ANT_INTERFACE_CLAIMED_ACTION)) {
				if (DEBUG) LLog.i(Globals.TAG, CLASSTAG + " onReceive: ANT INTERFACE CLAIMED");

				boolean wasClaimed = mClaimedAntInterface;

				// Could also read ANT_INTERFACE_CLAIMED_PID from intent and see if it matches the current process PID.
				try
				{
					mClaimedAntInterface = mAntReceiver.hasClaimedInterface();

					if(mClaimedAntInterface)
					{
						if (DEBUG)  LLog.i(Globals.TAG, CLASSTAG + " onReceive: ANT Interface claimed");
						//setNetworkKeySuunto();
						receiveAntRxMessages(true);
					}
					else
					{
						// Another application claimed the ANT Interface...
						if(wasClaimed)
						{
							// ...and we had control before that.  
							if (DEBUG)  LLog.i(Globals.TAG, CLASSTAG + " onReceive: ANT Interface released");

							if (DEBUG)  LLog.i(Globals.TAG, CLASSTAG + " Stopping service.");
							mContext.stopService(new Intent(mContext, AntPlusService.class));

							receiveAntRxMessages(false);

							mAntStateText = mContext.getString(R.string.ant_in_use);
							if(mCallbackSink != null)
								mCallbackSink.notifyAntStateChanged();
						}
					}
				}
				catch(AntInterfaceException e)
				{
					antError("mAntStatusReceiver.onReceive", "Error during interface claim", e);
				}
			}
			else if (ANTAction.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED))
			{
				if (DEBUG)  LLog.i(Globals.TAG, CLASSTAG + " onReceive: AIR_PLANE_MODE_CHANGED");
				if(isAirPlaneModeOn())
				{
					for (AntSensor device: devices.values()) {
						changeState(device, ChannelStates.CLOSED);
					}
					mAntStateText = mContext.getString(R.string.ant_airplane_mode);

					if (DEBUG)  LLog.i(Globals.TAG, CLASSTAG + " Stopping service.");
					mContext.stopService(new Intent(mContext, AntPlusService.class));

					if(mCallbackSink != null) mCallbackSink.notifyAntStateChanged();
				}
				else
				{
					if(mCallbackSink != null)
						mCallbackSink.notifyAntStateChanged();
				}
			}
			if(mCallbackSink != null)
				mCallbackSink.notifyAntStateChanged();
		}
	};

	//	public static String getHexString(byte[] data)
	//	{
	//		if(null == data)
	//		{
	//			return "";
	//		}
	//
	//		StringBuffer hexString = new StringBuffer();
	//		for(int i = 0;i < data.length; i++)
	//		{
	//			hexString.append("[").append(String.format("%02X", data[i] & 0xFF)).append("]");
	//		}
	//
	//		return hexString.toString();
	//	}
	
	private boolean suuntoSet = false;

	public void setNetworkKeySuunto() {
		if (mAntReceiver == null) return;
		suuntoSet = true;
		try {
			//antInterface.
			mAntReceiver.ANTTxMessage(new byte[] {17, 0x76, AntProfile.SUUNTODUAL.getNetwork(), 
					(byte)0x18, (byte)0xC5, (byte)0x28, (byte)0x1F, (byte)0x96, (byte)0xAF, (byte)0xFC, (byte)0xE9, 
					(byte)0xE8, (byte)0x5C, (byte)0xF1, (byte)0x5D, (byte)0x89, (byte)0x2C, (byte)0x96, (byte)0xF5});
			mAntReceiver.ANTTxMessage(new byte[] { 9, 0x46, AntProfile.SUUNTODUAL.getNetwork(), 
					(byte)0xB9, (byte)0xAD, (byte)0x32, (byte)0x28, (byte)0x75, (byte)0x7E, (byte)0xC7, (byte)0x4D});

			//static uchar GARMIN_KEY[] = "B9A521FBBD72C345"; // Garmin
			//static uchar SUUNTO_KEY[] = "B9AD3228757EC74D"; // Suunto 
			if (DEBUG)  LLog.d(Globals.TAG, CLASSTAG + ".setNetworkKeySuunto: Suunto network key accepted");
		} catch (AntInterfaceException e) {
			antError("setNetworkKeySuunto", "ANT Error in setNetworkKeySuunto: ", e);
		}
	}

	/** Receives all of the ANT message intents and dispatches to the proper handler. */
	private final BroadcastReceiver mAntMessageReceiver = new BroadcastReceiver() 
	{      
		Context mContext;

		public void onReceive(Context context, Intent intent) 
		{
			mContext = context;
			String ANTAction = intent.getAction();

			if (DEBUG)  LLog.d(Globals.TAG, CLASSTAG + ".mAntMessageReceiver.onReceive: " + ANTAction);
			if (ANTAction.equals(AntInterfaceIntent.ANT_RX_MESSAGE_ACTION)) 
			{
				if (DEBUG)  LLog.d(Globals.TAG, CLASSTAG + " onReceive: ANT RX MESSAGE");

				byte[] ANTRxMessage = intent.getByteArrayExtra(AntInterfaceIntent.ANT_MESSAGE);

				if (DEBUG)  LLog.d(Globals.TAG, CLASSTAG + " Rx: "+ StringUtil.toHexString(ANTRxMessage));

				switch(ANTRxMessage[AntMesg.MESG_ID_OFFSET])
				{
				case AntMesg.MESG_STARTUP_MESG_ID:
					break;
				case AntMesg.MESG_BROADCAST_DATA_ID:
				case AntMesg.MESG_ACKNOWLEDGED_DATA_ID:
					byte channelNum = ANTRxMessage[AntMesg.MESG_DATA_OFFSET];
					AntProfile profile = AntProfile.fromChannel(channelNum);
					if (profile == null) break;
					antDecode(profile, ANTRxMessage);
					break;
				case AntMesg.MESG_BURST_DATA_ID:
					break;
				case AntMesg.MESG_RESPONSE_EVENT_ID:
					responseEventHandler(ANTRxMessage);
					break;
				case AntMesg.MESG_CHANNEL_STATUS_ID:
					break;
				case AntMesg.MESG_CHANNEL_ID_ID:
					int deviceNum = ((ANTRxMessage[AntMesg.MESG_DATA_OFFSET + 1]&0xFF | ((ANTRxMessage[AntMesg.MESG_DATA_OFFSET + 2]&0xFF) << 8)) & 0xFFFF);
					profile = AntProfile.fromChannel(ANTRxMessage[AntMesg.MESG_DATA_OFFSET]);
					if (profile == null) break;
					AntSensor device = devices.get(profile);
					if (DEBUG)  LLog.i(Globals.TAG, CLASSTAG + " onRecieve: Received " + profile + " device number");
					device.mDeviceNumber = deviceNum;
					break;
				case AntMesg.MESG_VERSION_ID:
					break;
				case AntMesg.MESG_CAPABILITIES_ID:
					break;
				case AntMesg.MESG_GET_SERIAL_NUM_ID:
					break;
				case AntMesg.MESG_EXT_ACKNOWLEDGED_DATA_ID:
					break;
				case AntMesg.MESG_EXT_BROADCAST_DATA_ID:
					break;
				case AntMesg.MESG_EXT_BURST_DATA_ID:
					break;
				}
			}
		}

		/**
		 * Handles response and channel event messages
		 * @param ANTRxMessage
		 */
		private void responseEventHandler(byte[] ANTRxMessage)
		{
			// For a list of possible message codes
			// see ANT Message Protocol and Usage section 9.5.6.1
			// available from thisisant.com
			byte channelNumber = ANTRxMessage[AntMesg.MESG_DATA_OFFSET];

			AntProfile profile = AntProfile.fromChannel(channelNumber);
			if (profile == null) return;
			AntSensor device = devices.get(profile);
			if ((ANTRxMessage[AntMesg.MESG_DATA_OFFSET + 1] == AntMesg.MESG_EVENT_ID) && (ANTRxMessage[AntMesg.MESG_DATA_OFFSET + 2] == AntDefine.EVENT_RX_SEARCH_TIMEOUT)) {
				// A channel timed out searching, unassign it
				device.channelConfig.isInitializing = false;
				device.channelConfig.isDeinitializing = false;

				try {
					if (DEBUG)  LLog.i(Globals.TAG, CLASSTAG + " responseEventHandler: Received search timeout on " + profile + " channel");

					changeState(device, ChannelStates.OFFLINE);
					mAntReceiver.ANTUnassignChannel(profile.getChannel());
				} catch(AntInterfaceException e) {
					antError("mAntMessageReceiver.responseEventHandler", "Could not unassign channel", e);
				}

				if(isAllOff()) {
					if (DEBUG)  LLog.i(Globals.TAG, CLASSTAG + " Stopping service.");
					mContext.stopService(new Intent(mContext, AntPlusService.class));
				}
			}

			if (device.channelConfig.isInitializing) {
				if (ANTRxMessage[AntMesg.MESG_DATA_OFFSET + 2] != 0) // Error response
				{
					LLog.e(Globals.TAG, CLASSTAG + String.format("Error code(%#02x) on message ID(%#02x) on channel %d", ANTRxMessage[AntMesg.MESG_DATA_OFFSET + 2], ANTRxMessage[AntMesg.MESG_DATA_OFFSET + 1], channelNumber));
				}
				else
				{
					switch (ANTRxMessage[AntMesg.MESG_DATA_OFFSET + 1]) // Switch on Message ID
					{
					case AntMesg.MESG_ASSIGN_CHANNEL_ID:
						try
						{
							mAntReceiver.ANTSetChannelId(channelNumber, (short) device.channelConfig.deviceNumber, device.channelConfig.deviceType, device.channelConfig.TransmissionType);
						}
						catch (AntInterfaceException e)
						{
							antError("mAntMessageReceiver.responseEventHandler", "Could not set channel id", e);
						}
						break;
					case AntMesg.MESG_CHANNEL_ID_ID:
						try
						{
							mAntReceiver.ANTSetChannelPeriod(channelNumber, device.channelConfig.period);
						}
						catch (AntInterfaceException e)
						{
							antError("mAntMessageReceiver.responseEventHandler", "Could not set channel period", e);
						}
						break;
					case AntMesg.MESG_CHANNEL_MESG_PERIOD_ID:
						try
						{
							mAntReceiver.ANTSetChannelRFFreq(channelNumber, device.channelConfig.freq);
						}
						catch (AntInterfaceException e)
						{
							antError("mAntMessageReceiver.responseEventHandler", "Could not set message period", e);
						}
						break;
					case AntMesg.MESG_CHANNEL_RADIO_FREQ_ID:
						try
						{
							//if (DEBUG)  LLog.d(Globals.TAG, CLASSTAG + " Setting channel search time out");
							mAntReceiver.ANTSetChannelSearchTimeout(channelNumber, (byte)0); // Disable high priority search
						}
						catch (AntInterfaceException e)
						{
							antError("mAntMessageReceiver.responseEventHandler", "Could not set search time out", e);
						}
						break;
					case AntMesg.MESG_CHANNEL_SEARCH_TIMEOUT_ID:
						try
						{
							//if (DEBUG)  LLog.d(Globals.TAG, CLASSTAG + " Setting low priority channel search time out");
							mAntReceiver.ANTSetLowPriorityChannelSearchTimeout(channelNumber,(byte) 12); // Set search timeout to 30 seconds (low priority search)
						}
						catch (AntInterfaceException e)
						{
							antError("mAntMessageReceiver.responseEventHandler", "Could not set low priority search", e);
						}
						break;
					case AntMesg.MESG_SET_LP_SEARCH_TIMEOUT_ID:
						if (device.channelConfig.deviceNumber == WILDCARD)
						{
							try
							{
								mAntReceiver.ANTSetProximitySearch(channelNumber, device.channelConfig.proxSearch);   // Configure proximity search, if using wild card search
							}
							catch (AntInterfaceException e)
							{
								antError("mAntMessageReceiver.responseEventHandler", "Could not set proximity search", e);
							}
						}
						else
						{
							try
							{
								mAntReceiver.ANTOpenChannel(channelNumber);
							}
							catch (AntInterfaceException e)
							{
								antError("mAntMessageReceiver.responseEventHandler", "Could not open channel", e);
							}
						}
						break;
					case AntMesg.MESG_PROX_SEARCH_CONFIG_ID:
						try
						{
							mAntReceiver.ANTOpenChannel(channelNumber);
						}
						catch (AntInterfaceException e)
						{
							antError("mAntMessageReceiver.responseEventHandler", "Could not open channel on MESG_PROX_SEARCH_CONFIG_ID", e);
						}
						break;
					case AntMesg.MESG_OPEN_CHANNEL_ID:
						device.channelConfig.isInitializing = false;
						changeState(device, ChannelStates.SEARCHING);
						break;
					}
				}
			}
			else if (device.channelConfig.isDeinitializing)
			{
				if ((ANTRxMessage[AntMesg.MESG_DATA_OFFSET + 1] == AntMesg.MESG_EVENT_ID) && (ANTRxMessage[AntMesg.MESG_DATA_OFFSET + 2] == AntDefine.EVENT_CHANNEL_CLOSED))
				{
					try
					{
						mAntReceiver.ANTUnassignChannel(channelNumber);
					}
					catch (AntInterfaceException e)
					{
						antError("mAntMessageReceiver.responseEventHandler", "Could not unassign channel", e);
					}
				}
				else if ((ANTRxMessage[AntMesg.MESG_DATA_OFFSET + 1] == AntMesg.MESG_UNASSIGN_CHANNEL_ID) && (ANTRxMessage[AntMesg.MESG_DATA_OFFSET + 2] == AntDefine.RESPONSE_NO_ERROR))
				{
					device.channelConfig.isDeinitializing = false;
				}
			}
		}


		
		private void antDecode(AntProfile profile, byte[] ANTRxMessage) {
			if (DEBUG)  LLog.d(Globals.TAG, CLASSTAG + " antDecode start");

			AntSensor device = devices.get(profile);
			if(device.mState != ChannelStates.CLOSED) {
				changeState(device, ChannelStates.TRACKING_DATA);
			}

			if(device.mDeviceNumber == WILDCARD) {
				try {
					if (DEBUG)  LLog.i(Globals.TAG, CLASSTAG + " antDecode: Requesting device number");

					mAntReceiver.ANTRequestMessage(device.profile.getChannel(), AntMesg.MESG_CHANNEL_ID_ID);
				} catch(AntInterfaceException e) {
					antError("antDecode", "Could not send request message", e);
				}
			}

			if(mCallbackSink != null) mCallbackSink.onDecode(profile, ANTRxMessage);			
			if (DEBUG)  LLog.d(Globals.TAG, CLASSTAG + " antDecode end");
		}


		
	};
	
	private void changeState(AntSensor device, ChannelStates state) {
		if (device.mState.equals(state)) return;
		if (DEBUG)  LLog.i(Globals.TAG, CLASSTAG + ".changeState: " + device.profile + " = " + state);
		device.mState = state;
		if(mCallbackSink != null) mCallbackSink.notifyChannelStateChanged(device.profile);
	}

	/**
	 * ANT Channel Configuration.
	 *
	 * @param networkNumber the network number
	 * @param channelNumber the channel number
	 * @param deviceNumber the device number
	 * @param deviceType the device type
	 * @param txType the tx type
	 * @param channelPeriod the channel period
	 * @param radioFreq the radio freq
	 * @param proxSearch the prox search
	 * @return true, if successfully configured and opened channel
	 */   
	private void antChannelSetup(byte networkNumber, byte channel)
	{
		try {
			AntProfile profile = AntProfile.fromChannel(channel);
			if (profile == null) return;
			AntSensor device = devices.get(profile);
			device.channelConfig.isInitializing = true;
			device.channelConfig.isDeinitializing = false;

			mAntReceiver.ANTAssignChannel(channel, AntDefine.PARAMETER_RX_NOT_TX, networkNumber);  // Assign as slave channel on selected network (0 = public, 1 = ANT+, 2 = ANTFS)
			// The rest of the channel configuration will occur after the response is received (in responseEventHandler)
		}
		catch(AntInterfaceException e)
		{
			antError("antChannelSetup", "Could not assign channel", e);
		}
	}

	/**
	 * Enable/disable receiving ANT Rx messages.
	 *
	 * @param register If want to register to receive the ANT Rx Messages
	 */
	private void receiveAntRxMessages(boolean register)
	{
		if(register) {
			//this.setNetworkKeySuunto();
			if (DEBUG)  LLog.i(Globals.TAG, CLASSTAG + " receiveAntRxMessages: START");
			mContext.registerReceiver(mAntMessageReceiver, new IntentFilter(AntInterfaceIntent.ANT_RX_MESSAGE_ACTION));
		} else {
			try {
				mContext.unregisterReceiver(mAntMessageReceiver);
			} catch(IllegalArgumentException e) {
				// Receiver wasn't registered, ignore as that's what we wanted anyway
			}

			if (DEBUG)  LLog.i(Globals.TAG, CLASSTAG + " receiveAntRxMessages: STOP");
		}
	}

	/**
	 * Checks if ANT is sensitive to airplane mode, if airplane mode is on and if ANT is not toggleable in airplane
	 * mode. Only returns true if all 3 criteria are met.
	 * @return True if airplane mode is stopping ANT from being enabled, false otherwise.
	 */
	private boolean isAirPlaneModeOn()
	{
		if(!Settings.System.getString(mContext.getContentResolver(),
				Settings.Global.AIRPLANE_MODE_RADIOS).contains(RADIO_ANT))
			return false;
		if(Settings.System.getInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) == 0)
			return false;

		try
		{
			Field field = Settings.System.class.getField("AIRPLANE_MODE_TOGGLEABLE_RADIOS");
			if(Settings.System.getString(mContext.getContentResolver(),
					(String) field.get(null)).contains(RADIO_ANT))
				return false;
			else
				return true;
		} catch(Exception e)
		{
			return true; //This is expected if the list does not yet exist so we just assume we would not be on it.
		}
	}

	public void sendMessage(AntProfile profile, byte[] txBuffer) {
		if (profile != null) try {
			mAntReceiver.ANTSendAcknowledgedData(profile.getChannel(), txBuffer);
		} catch (AntInterfaceException e) {
			antError("sendMessage", "ANT Error in sendMessage", e);
		}
	}
	
	
	
	
}
