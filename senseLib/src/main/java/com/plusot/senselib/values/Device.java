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
package com.plusot.senselib.values;

import android.annotation.SuppressLint;

import com.plusot.common.Globals;
import com.plusot.common.share.LLog;
import com.plusot.common.util.Watchdog;
import com.plusot.javacommon.util.NumberUtil;
import com.plusot.senselib.SenseGlobals;
import com.plusot.senselib.ant.BatteryStatus;
import com.plusot.senselib.ant.DeviceManufacturer;
import com.plusot.senselib.store.DataLog;
import com.plusot.senselib.util.AverageItem;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@SuppressLint("DefaultLocale")
public abstract class Device implements Watchdog.Watchable, DeviceStub {
    private static final String CLASSTAG = Device.class.getSimpleName();
    private static final boolean DEBUG = false;
    protected Set<DeviceListener> listeners = new HashSet<DeviceListener>();
	private static DeviceListener globalListener = null;
	private static Set<Device> devices = new HashSet<Device>();
	
	protected final static double FADEOUT_FACTOR = 0.6;
	protected long fadeOutTime = 10000;
	private Map<ValueType, Long> lastUpdate = new HashMap<ValueType, Long>();
	private Map<ValueType, AverageItem<?>> avgValues = new HashMap<ValueType, AverageItem<?>>();
	protected final DeviceType deviceType;
	protected DeviceManufacturer manufacturer = DeviceManufacturer.UNKNOWN;
	protected String productId = "";
	protected String revision = "";
	protected String softwareVersion = "";
	protected String serialNumber = "";
	protected String battery = "";
	protected long operatingTime = 0;
	protected BatteryStatus batteryStatus = BatteryStatus.UNKNOWN;
	protected String address = "";
	protected String rawValue = null;
	protected EnumMap<ValueType, Boolean> zeroPending = new EnumMap<ValueType, Boolean>(ValueType.class);
	protected boolean opened = false;
    private static EnumMap<DeviceType, Integer> deviceCount = new EnumMap<DeviceType, Integer>(DeviceType.class);
    private int count = 0;

	public static Device[] getDevices() {
		return devices.toArray(new Device[0]);
	}

	public static void devicesToZero() {
		for (Device device : devices) device.fireOnDeviceZeroValue();
	}

	public static void clearDevices() {
		Set<Device> tempSet = new HashSet<Device>();
		tempSet.addAll(devices);
		devices.clear();
		for (Device device : tempSet) device.destroy();
		//valueTypeDevices.clear();
		//stringDevices.clear();
	}

	public Device(final DeviceType deviceType) {
		this.deviceType = deviceType;

		Watchdog watchdog = Watchdog.getInstance();
		if (watchdog != null) watchdog.add(this, 1000);
		devices.add(this);
        Integer count = deviceCount.get(deviceType);
        if (count == null)
            count = 1;
        else
            count++;
        this.count = count;
        deviceCount.put(deviceType, count);
	}

    public int getCount() {
        return count;
    }

	public DeviceType getDeviceType() {
		return deviceType;
	}

	public static void setListener(DeviceListener listener) {
		globalListener = listener;
	}
	
	public synchronized void addListener(DeviceListener listener) {
		listeners.add(listener);
	}

	public synchronized void removeListener(DeviceListener listener) {
		listeners.remove(listener);
	}

	public abstract void fadeOut(ValueType type);

	public void openIt(boolean reOpen) {
		opened = true;
	}

	public void closeIt() {
		opened = false;
	}

	public void destroy() {
		if (opened) closeIt();
		listeners.clear();
		Watchdog.getInstance().remove(this);
		devices.remove(this);
	}

	//	protected void fireGlobally(ValueType valueType, Object value, long timeStamp) {
	//		if (!Globals.runMode.isRun()) return;
	//		if (listener != null) listener.onDeviceValueChanged(deviceType, valueType, new ValueItem(value, timeStamp), true);
	//	}

	protected void fireOnDeviceValueChanged(ValueType valueType, Object value, long timeStamp, boolean checkLastUpdate) {
		if (!Globals.runMode.isRun()) return;

		if (checkLastUpdate) lastUpdate.put(valueType, System.currentTimeMillis());

		if (SenseGlobals.activity.equals(SenseGlobals.ActivityMode.REPLAY)) return;

		//		if (!deviceType.getValueTypes().contains(valueType)) {
		//			LLog.e(Globals.TAG, CLASSTAG + ".fireOnDeviceValueChanged: ValueType " + valueType + " not supported by device " + this.getName());
		//		}

		if (zeroPending.get(valueType) != null) {
			this.fireOnDeviceZeroValue(valueType);
			zeroPending.put(valueType, null);
		}

		if (checkLastUpdate && valueType.getUnitType().isNumeric() && valueType.getDimensions() == 1 && valueType.getSampleTime() > 0) {
			if (!NumberUtil.inRange(value, valueType.minValue(), valueType.maxValue())) return;

			AverageItem<?> avgItem = avgValues.get(valueType);
			if (avgItem == null) { 
				avgItem = AverageItem.getInstance(valueType.getUnitType().getUnitClass(), valueType.getShortName(), valueType.getSampleTime(), valueType.getMinimalAnomaly(), false, false, 0);
				avgValues.put(valueType, avgItem);
			}
			if (!avgItem.addObject(valueType.toString(), value, timeStamp, 1000)) return;
			value = avgItem.getAverage(valueType.getSampleTime());
		}

		ValueItem item = new ValueItem(value, timeStamp); 
		if (valueType.isStorable() && valueType.getUnitType().isNumeric() && SenseGlobals.activity.equals(SenseGlobals.ActivityMode.RUN)) {
			DataLog dataLog =  DataLog.getInstance();
			if (dataLog != null) 
				dataLog.log(deviceType, valueType, 
						valueType.defaultUnit().getFormated(item, valueType.getDecimals(), DataLog.DELIM, false), timeStamp);
		}
		for (DeviceListener listener: listeners) listener.onDeviceValueChanged(this, valueType, item, true);
		globalListener.onDeviceValueChanged(this, valueType, item, true);
	}

	protected void fireOnDeviceZeroValue(ValueType valueType) {

		if (!Globals.runMode.isRun()) return;
		//lastUpdate.put(valueType, System.currentTimeMillis());
		ValueItem item = ValueItem.fromZero(valueType.getUnitType().getUnitClass(), valueType.getDimensions());

		if (SenseGlobals.activity.equals(SenseGlobals.ActivityMode.REPLAY)) return;

		if (DEBUG) LLog.d(Globals.TAG, CLASSTAG + ".fireOnDeviceZeroValue: ValueType " + valueType + " by device " + this.getName());

		if (valueType.getUnitType().isNumeric() && valueType.getDimensions() == 1 && valueType.getSampleTime() > 0) {

			AverageItem<?> avgItem = avgValues.get(valueType);
			if (avgItem == null) { 
				avgItem = AverageItem.getInstance(valueType.getUnitType().getUnitClass(), valueType.getShortName(), valueType.getSampleTime(), valueType.getMinimalAnomaly(), false, false, 0);
				avgValues.put(valueType, avgItem);
			}
			avgItem.addObject(valueType.toString(), item.getValue(), item.getTimeStamp(), 1000);
		}

		if (valueType.isStorable() && valueType.getUnitType().isNumeric() && SenseGlobals.activity.equals(SenseGlobals.ActivityMode.RUN)) {
			DataLog dataLog =  DataLog.getInstance();
			if (dataLog != null) 
				dataLog.log(deviceType, valueType,
						valueType.defaultUnit().getFormated(item, valueType.getDecimals(), DataLog.DELIM, false), item.getTimeStamp());
		}
		for (DeviceListener listener : listeners) listener.onDeviceValueChanged(this, valueType, item, true);
		globalListener.onDeviceValueChanged(this, valueType, item, true);
	}

	protected void fireOnDeviceZeroValue() {
		for (ValueType type: lastUpdate.keySet()) {
			if  (lastUpdate.get(type) != null && System.currentTimeMillis() - lastUpdate.get(type) < 60000) 
				this.fireOnDeviceZeroValue(type);
			else
				zeroPending.put(type, true);
		}
	}

	protected void fireOnDeviceValueChanged(ValueType valueType, Object value, long timeStamp) {
		fireOnDeviceValueChanged(valueType, value, timeStamp, true);
	}

	public String getName() {
		return deviceType.getLabel(Globals.appContext);
	}

    public String getLongName() {
        String name = getName();
        if (count > 0) name += " " + count;
        if (getManufacturer() != null) name += " " + getManufacturer();
        if (getProductId() != null) name += " " + getProductId();
        return name;
    }

	public String getShortName() {
		return deviceType.getShortName();
	}

	public abstract boolean isActive();

	@Override
	public String toString() {
		return getShortName();
	}

	@Override
	public int hashCode() {
		return getShortName().toUpperCase(Locale.US).hashCode();
	}

	@Override
	public void onWatchdogCheck(long count) {
		long now = System.currentTimeMillis();
		for (ValueType type : lastUpdate.keySet()) if (lastUpdate.get(type) != null && now - lastUpdate.get(type) > fadeOutTime) {
			fadeOut(type);		
		}	
	}

	public DeviceManufacturer getManufacturer() {
		return manufacturer;
	}

	public String getProductId() {
		return productId;
	}

	public String getRevision() {
		return revision;
	}

	public String getSoftwareVersion() {
		return softwareVersion;
	}

	public String getSerialNumber() {
		return serialNumber;
	}

	public String getBattery() {
		return battery;
	}

	public long getOperatingTime() {
		return operatingTime;
	}

	public BatteryStatus getBatteryStatus() {
		return batteryStatus;
	}

	public String getAddress() {
		return address;
	}

	public String getRawValue() {
		return rawValue;
	}

	public int getDeviceNumber() {
		return -1;
	}

	public String getNumberOrAddress() {
		if (getDeviceNumber() == -1) return address; else return "ANT-" + getDeviceNumber();
	}

	@Override
	public void onWatchdogClose() {

	}


}
