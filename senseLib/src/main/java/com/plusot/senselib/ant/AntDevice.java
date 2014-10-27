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
package com.plusot.senselib.ant;

import java.util.EnumSet;

import com.dsi.ant.AntMesg;
import com.plusot.common.Globals;
import com.plusot.common.share.LLog;
import com.plusot.common.util.Calc;
import com.plusot.common.util.ToastHelper;
import com.plusot.javacommon.util.Format;
import com.plusot.senselib.R;
import com.plusot.senselib.store.SRMLog;
import com.plusot.senselib.values.Device;
import com.plusot.senselib.values.ValueType;
import com.plusot.senselib.widget.BaseGraph;

public abstract class AntDevice extends Device{
	protected static final int PAGE_OFFSET = AntMesg.MESG_DATA_OFFSET + 1;
	private static final String CLASSTAG = AntDevice.class.getSimpleName();
//	private boolean deferredStart = false;
	protected final AntProfile profile;
//	protected final int sub;
	protected byte channel = 0;
	private static final int MANUFACTURER_PAGE = 0x50;
	private static final int PRODUCT_PAGE = 0x51;
	private static final int BATTERY_PAGE = 0x52;
	public static final int ANT_MSG_TIMEOUT = 10000;
	
	//private State channelState = State.CLOSED;
//	protected byte[] antMsg;
//	private int deviceNumber = 0;
	private boolean firstMsg = true;
	private double refVoltage = 0;
	protected long lastMsgTime = System.currentTimeMillis();
	private BatteryStatus refBatteryStatus = BatteryStatus.UNKNOWN;
	protected final AntMesgCallback callback;
	
	//private boolean disabled = false;
	
	/*public enum State {
		CLOSED(R.string.channel_closed),
		PENDING_OPEN(R.string.channel_pending),
		SEARCHING(R.string.channel_searching),
		
		TRACKING_STATUS(R.string.channel_tracking),
		TRACKING_DATA(R.string.channel_data),
		OFFLINE(R.string.channel_offline)
		;

		private final int idLabel;

		private State(final int label) {
			this.idLabel = label;
		}
		public String getLabel(Context context) {
			return context.getString(idLabel);
		}

		public String getLabel(Context context, Object ... args) {
			if (context == null) {
				LLog.e(Globals.TAG, CLASSTAG + ".getLabel: No context in Device.State.getLabel");
				return "unknown state";
			}
			return context.getString(idLabel, args);
		}
	}*/

	public AntDevice(final AntProfile profile, final AntMesgCallback callback) {
		super(profile.getDeviceType()); 
		this.callback = callback;
		this.profile = profile;
//		this.sub = sub;
		//prepareLog(ValueType.VOLTAGE);
		
//		AppPreferences prefs = AppPreferences.getInstance();
//		if (prefs != null) {
//			int nr = prefs.getDeviceNumber(deviceType.ordinal());
//			LLog.d(Globals.TAG, CLASSTAG + "Devicenumber for: " + deviceType.getShortName() + " = " + nr);
//			if (prefs.isUsePairing()) deviceNumber = nr;	
//		}
		/*
		 * for (AntDevice device : antManager.getDevices()) {
		 * device.setDeviceNumber(settings.getInt(device.getName() +
		 * "_DeviceNumber", AntManager.WILDCARD)); }
		 */
	}

	public AntProfile getProfile() {
		return this.profile;
	}
	
//	public State getState() {
//		return channelState;
//	}
//	
//	public int getSub() {
//		return sub;
//	}
	
	/*@Override
	public void onWake() {
		
	}*/
	
//	private void atStateChanged(State state, Object ... args) {
////		LLog.d(
////				Globals.TAG, 
////				CLASSTAG + ".atStateChanged for " + getName() + 
////				" = " +	state.getLabel(Globals.appContext, args));
//	}

	public boolean decode(byte[] antMsg) {
//		this.antMsg = antMsg;
		
//		if(channelState != State.CLOSED && channelState != State.TRACKING_DATA) {
//			LLog.d(Globals.TAG, CLASSTAG + ".decode: " + getName() +  " is tracking data");
//			setState(State.TRACKING_DATA);
//		}

//		if(deviceNumber == AntManager.WILDCARD) {
//			try {
//				mgr.getInterface().ANTRequestMessage(channel, AntMesg.MESG_CHANNEL_ID_ID);
//				return true;
//			} catch(AntInterfaceException e) {
//				LLog.w(Globals.TAG, CLASSTAG + ".decode: Could not decode " + StringUtil.toHexString(antMsg) + " for " + getName() + ".");
//			}
//		}
		if (firstMsg) {
			firstMsg = false;
			//Activity activity = mgr.getActivity();
			//if (activity != null) 
			ToastHelper.showToastLong(Globals.appContext.getString(R.string.first_msg, getName())); // + " (" + deviceNumber + ")");
			//new SleepAndWake(this, 4000);
		}
		
		lastMsgTime = System.currentTimeMillis();

		switch (antMsg[PAGE_OFFSET]) {
		case MANUFACTURER_PAGE: 
			//LLog.i(Globals.TAG, CLASSTAG + ".decode: " + getName() + " manufacturer page"); 
			int manufacturingId = (0xFF & antMsg[PAGE_OFFSET + 4]) + ((0xFF & antMsg[PAGE_OFFSET + 5]) << 8);
			int productId = (0xFF & antMsg[PAGE_OFFSET + 6]) + ((0xFF & antMsg[PAGE_OFFSET + 7]) << 8);
			manufacturer = DeviceManufacturer.getById(manufacturingId);
			if (this.productId.equals("" + productId)) {
				//ToastHelper.showToastShort(name + ": " + manufacturer.getLabel() + " " + productId);			
				LLog.d(Globals.TAG, CLASSTAG + ".decode : " +  getName() + " Manufacturer " + manufacturer.getLabel() + ", model " + productId);
				BaseGraph.labels[deviceType.getPriorityBit()] = manufacturer.getLabel();
				this.productId = "" + productId;
			}
			break;

		case PRODUCT_PAGE: 
			//LLog.i(Globals.TAG, CLASSTAG + ".decode: Product page for " + getName()); 
			int revisionId = (0xFF & antMsg[PAGE_OFFSET + 3]);
			revision = "" + revisionId;
			long serialNumber = (0xFFL & antMsg[PAGE_OFFSET + 4]) + 
					((0xFFL & antMsg[PAGE_OFFSET + 5]) << 8) +
					((0xFFL & antMsg[PAGE_OFFSET + 6]) << 16)+ 
					((0xFFL & antMsg[PAGE_OFFSET + 7]) << 24);
			if (serialNumber != 0xFFFFFFFF &&  !this.serialNumber.equals("" + serialNumber)) {
				//ToastHelper.showToastShort(name + ": Revision " + revision + " Serial number " + serialNumber);
				//LLog.d(Globals.TAG, CLASSTAG + ".decode: " + getName() + " Revision " + revision + " Serial number " + serialNumber);
				if (this instanceof AntPowerDevice) {
					if (SRMLog.getInstance() != null) SRMLog.getInstance().setSerialnr("" + String.format("%8X", serialNumber));
				}
				this.serialNumber = "" + serialNumber;
			}
			break;

		case BATTERY_PAGE: 
			//LLog.i(Globals.TAG, CLASSTAG + ".decode: Battery page"); 
			operatingTime = (0xFF & antMsg[PAGE_OFFSET + 3]) + 
					((0xFF & antMsg[PAGE_OFFSET + 4]) << 8) +
					((0xFF & antMsg[PAGE_OFFSET + 5]) << 16);
			int voltageInt = (0xFF & antMsg[PAGE_OFFSET + 6]);
			double voltage = 1.0 * voltageInt / 256.0;
			int bits = (0xFF & antMsg[PAGE_OFFSET + 7]);
			voltageInt = bits & 0xF;
			if (voltageInt == 0xF)
				voltage = 0;
			else
				voltage += 1.0 * voltageInt;
			int batteryStatusInt = (bits >> 4) & 0x7;
			BatteryStatus batteryStatus = BatteryStatus.getById(batteryStatusInt);
			if ((bits & 0x80) == 0x80) {
				operatingTime *= 2;
			} else
				operatingTime *= 16;
			if ((Calc.absChange(voltage, this.refVoltage) > 0.2 || !batteryStatus.equals(this.refBatteryStatus)) && EnumSet.of(BatteryStatus.CRITICAL, BatteryStatus.LOW).contains(batteryStatus)) {
				ToastHelper.showToastShort(
						getName() + ": Voltage " + Format.format(voltage, 1) + 
						"  " + batteryStatus.getLabel(Globals.appContext));
				fireOnDeviceValueChanged(ValueType.VOLTAGE, voltage, System.currentTimeMillis());
				LLog.d(Globals.TAG, 
						CLASSTAG + ".decode: " + getName() + " Voltage " + Format.format(voltage, 1) + 
						" " + batteryStatus.getLabel(Globals.appContext) ); 
				this.refVoltage = voltage;
				this.refBatteryStatus = batteryStatus;
			}
			this.battery = Format.format(voltage, 2);
			this.batteryStatus = batteryStatus;
			break;
		}

		return true;
	}

//	@Override
//	public void open(boolean reOpen) {
//		AppPreferences prefs = AppPreferences.getInstance();
//		if (prefs != null && prefs.isUsePairing()) {
//			deviceNumber = prefs.getDeviceNumber(deviceType.ordinal());
//			if (deviceNumber == 0) {
//				LLog.d(Globals.TAG, CLASSTAG + ".open: Will not open " + this.getName() + " as deviceNumber = 0 and pairing is ON");
//				return;
//			}
//		}
//		LLog.d(Globals.TAG, CLASSTAG + ".open: Opening " + this.getName() + " with deviceNumber " + deviceNumber);
//		//disabled = false;
//		mgr.open(this, reOpen);	
//	}

//	@Override
//	public void close() {
//		if(isActive()) {
//			LLog.d(Globals.TAG, CLASSTAG + ".close: Close channel for " + getName());
//			closeChannel();
//		} 
//	}

//	public int getChannel() {
//		return channel;
//	}
//
//	public State getChannelState() {
//		return channelState;
//	}
//
//	public void setState(State state) {
//		channelState = state;
//		atStateChanged(state); 
//	}
//
//	public boolean isDeferredStart() {
//		return deferredStart;
//	}

	@Override
	public int getDeviceNumber() {
		return callback.getDeviceNumber(profile);
	}
	
//	public void resetDeviceNumber() {
//		deviceNumber = 0;
//		AppPreferences prefs = AppPreferences.getInstance();
//		if (prefs != null) {
//			prefs.setDeviceNumber(deviceType.ordinal(), 0);
//		}
//		LLog.d(Globals.TAG, CLASSTAG + ".setDeviceNumber: ANT device " + getName() + " number changed: " + deviceNumber); 
//	}
//	
//	public void retrieveDeviceNumber() {
//		if (deviceNumber == 0) {
//			AppPreferences prefs = AppPreferences.getInstance();
//			if (prefs != null) {
//				deviceNumber = prefs.retrieveDeviceNumber(deviceType.ordinal());
//				LLog.d(Globals.TAG, CLASSTAG + ".retrieveDeviceNumber: ANT device " + getName() + " number: " + deviceNumber); 
//			}
//		}
//	}
//		
//	public void setDeviceNumber(int deviceNumber) {
//		this.deviceNumber = deviceNumber;
//		AppPreferences prefs = AppPreferences.getInstance();
//		if (prefs != null) {
//			prefs.setDeviceNumber(deviceType.ordinal(), deviceNumber);
//		}
//		LLog.d(Globals.TAG, CLASSTAG + ".setDeviceNumber: ANT device " + getName() + " number changed: " + deviceNumber); 
//	}

//	public AntProfile getDeviceProfile() {
//		return profile;
//	}


//	public byte[] getAntMsg() {
//		return antMsg;
//	}

//	@Override
//	public boolean isActive() {
//		if(channelState.equals(State.CLOSED) || channelState.equals(State.OFFLINE))
//			return false;				
//		return true;
//	}
//
//	@Override
//	public boolean isReading() {
//		if(channelState.equals(State.TRACKING_DATA))
//			return true;				
//		return false;
//	}
//	
//	public boolean isReadingRecent() {
//		if(channelState.equals(State.TRACKING_DATA) && System.currentTimeMillis() - lastMsgTime < 10000)
//			return true;				
//		return false;
//	}

//	public void openChannel(boolean deferToNextReset) {
//		if (!deferToNextReset) {
//			//byte freq = 57; // 2457Mhz (ANT+ frequency)
//			AppPreferences prefs = AppPreferences.getInstance();
//			if (prefs != null && prefs.isUsePairing() && deviceNumber == 0) {
//				LLog.w(Globals.TAG, CLASSTAG + ".openChannel: Not opening " + getName() + " on channel " + channel + " as pairing is ON and device number = 0.");
//				return;
//			}
//			
//			byte transmissionType = 0; // Set to 0 for wild card search
//			setState(State.SEARCHING);
//			//LLog.d(Globals.TAG, CLASSTAG + ".openChannel: Trying to open " + getName() + " on channel " + channel + " with number: " + deviceNumber);
//			if (!mgr.channelSetup(
//					profile.getNetwork(), // Network: 1 (ANT+)
//					channel, (short) deviceNumber, profile.getAntDeviceType(), transmissionType, profile.getPeriod(), profile.getFreq(),
//					mgr.getProximityThreshold()))
//			{
//				LLog.w(Globals.TAG, CLASSTAG + ".openChannel: failed to configure and open channel " + channel + ".");
//				setState(State.CLOSED);
//			}
//			deferredStart = false;
//		} else {
//			deferredStart = true;
//			setState(State.PENDING_OPEN);
//		}
//	}
	
//
//	public void closeChannel() {
//		setState(State.CLOSED);
//
//		try {
//			mgr.getInterface().ANTCloseChannel(channel);
//			mgr.getInterface().ANTUnassignChannel(channel);
//		} catch (AntInterfaceException e) {
//			LLog.w(Globals.TAG, CLASSTAG + ".closeChannel: could not cleanly close channel " + channel + " for " + getName() + ".");
//		}
//	}

//	public void setOffLine() {
//		try {
//			//LLog.i(Globals.TAG, CLASSTAG + ".responseEventHandler: Received search timeout for: " + name);
//			setState(State.OFFLINE);
//			mgr.getInterface().ANTUnassignChannel(channel);
//		} catch(AntInterfaceException e) {
//			LLog.w(Globals.TAG, CLASSTAG + ".closeChannel: could not cleanly close channel " + channel + " for " + getName() + ".");
//		}
//	}

//	public void cancelRequests() {
//		if (deferredStart) {
//			deferredStart = false;
//			setState(State.CLOSED);
//		}
//	}
	
	protected void sendMessage(byte[] txBuffer) {
		callback.sendMessage(profile, txBuffer);	
	}
	
	@Override
	public String getAddress() {
		return "" + profile.getChannel();
	}
	
//	@Override
//	public void open(boolean reOpen) {
//		// TODO Auto-generated method stub
//		
//	}
//
//	@Override
//	public void close() {
//		// TODO Auto-generated method stub
//		
//	}

	@Override
	public boolean isActive() {
		if (System.currentTimeMillis() - lastMsgTime < 5000) return true;
		return false;
	}

	

}