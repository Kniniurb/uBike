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

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.util.SparseArray;

import com.plusot.common.Globals;
import com.plusot.common.settings.PreferenceHelper;
import com.plusot.common.share.LLog;
import com.plusot.common.share.SimpleLog;
import com.plusot.javacommon.util.Format;
import com.plusot.javacommon.util.MathVector;
import com.plusot.javacommon.util.NumberUtil;
import com.plusot.javacommon.util.StringUtil;
import com.plusot.javacommon.util.TimeUtil;
import com.plusot.senselib.R;
import com.plusot.senselib.SenseGlobals;
import com.plusot.senselib.settings.FileType;
import com.plusot.senselib.settings.PreferenceDefaults;
import com.plusot.senselib.settings.PreferenceKey;
import com.plusot.senselib.store.FitLog;
import com.plusot.senselib.store.GoldenCheetahLog;
import com.plusot.senselib.store.GpxLog;
import com.plusot.senselib.store.PwxLog;
import com.plusot.senselib.store.SRMLog;
import com.plusot.senselib.util.AverageDoubleItem;
import com.plusot.senselib.widget.XY;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

public class Value {
	private static final String CLASSTAG = Value.class.getSimpleName();
	private static final String EXT = ".csv";
	private final ValueType valueType;
	private static final long MAX_DELTA = 180000;
	private static final long TIMEOFFSET = 30 * 365 * 86400000;
	private static Map<ValueType, ValueItemTag> lastValues = new HashMap<ValueType, ValueItemTag>();
	private Map<ValueType, ValueItemTag> refValues = new HashMap<ValueType, ValueItemTag>();
	private SparseArray<ValueItem> currents = new SparseArray<ValueItem>();
	private static SparseArray <EnumMap<ValueType, ValueTag>> history = new SparseArray <EnumMap<ValueType, ValueTag>>();
	private ValueItem currentValue = null;
	private int currentTag = -1;
	private ValueItem lastValue = null;
	private ValueItem refValue = null;
	private ValueItem firstValue = null;
	private ValueItem maxValue = null;
	private ValueItem minValue = null;
	private List<XY> quickView = new ArrayList<XY>();
	private List<XY> tempQuickView = new ArrayList<XY>();
	//	private double variance = 0;
	private double recovery = 0;
	private double value4thTimeProduct = 0;
	private double valueTimeProduct = 0;
	private long valueTimeProductDelta = 0;
	private double deltaUp = 0;
	private double deltaDown = 0;
	private Presentation presentation = Presentation.VALUE;
	private Unit unit = null;
	private long lastUpdate = 0;
	private MathVector changeVector = null;
	private long lastNotified = 0;
	private final Vector<ValueListener> listeners = new Vector<ValueListener>();
	private static Vector<ValueListener> globalListeners = new Vector<ValueListener>();
	private static String FILE_VERSION = "Version_1.6";
	private static double totalEnergy = 0;
	private static double totalDistance = 0;
	private static double highestPeak = 0;
	private static double totalAscent = 0;
	private static int rides = -1;
	private static long totalTime = 0;
	private static String SUMMARY = "SUMMARY";
	private static String TOTAL = "TOTAL_";
	private static String PEAK = "PEAK";
	private static String RIDES = "RIDES";
	private static String SESSION = "SESSION"; 
	private static long sessionTime = 0;
	public static String summary = null;
	private AverageDoubleItem avgItem = null;
	public static double windDirection = -1;
	public static EnumSet<FileType> fileTypes = PreferenceDefaults.DEFAULT_FILETYPES;
	private float[] zones = null;
	private static EnumMap<ValueType, Value> values = new EnumMap<ValueType, Value>(ValueType.class); 
	private static List<Value> valueList = new ArrayList<Value>();

	private class ValueItemTag {
		private final int tag;
		private final ValueItem item;

		private ValueItemTag(final ValueItem item, final int tag) {
			this.item = item;
			this.tag = tag;
		}
	}

	private class ValueTag {
		final int tag;
		final Object obj;

		private ValueTag(final Object obj, final int tag) {
			this.obj = obj;
			this.tag = tag;
		}
	}

	private static void initTotals() {
		sessionTime = PreferenceHelper.get(SESSION, 0L);
		Log.d(Globals.TAG, CLASSTAG + ".initTotals = " + sessionTime);
		rides = PreferenceHelper.get(RIDES, 0);
		totalTime = PreferenceHelper.get(TOTAL + ValueType.TIME.getShortName(), 0L);
		totalDistance = PreferenceHelper.get(TOTAL + ValueType.DISTANCE.getShortName(), 0f);
		totalAscent = PreferenceHelper.get(TOTAL + ValueType.ALTITUDE.getShortName(), 0f);
		highestPeak = PreferenceHelper.get(PEAK + ValueType.ALTITUDE.getShortName(), 0f);
		totalEnergy = PreferenceHelper.get(TOTAL + ValueType.POWER.getShortName(), 0f);
		summary = PreferenceHelper.get(SUMMARY, (String)null);
		SimpleLog.setSession(getSessionString());
	}

	public static String saveTotals() {
		for (Value value : values.values()) value.checkLastValues();
		LLog.d(Globals.TAG, CLASSTAG + ".saveTotals = Session of " + TimeUtil.formatTime(sessionTime));
		PreferenceHelper.set(SESSION, sessionTime);
		PreferenceHelper.set(RIDES, rides);
		PreferenceHelper.set(TOTAL + ValueType.TIME.getShortName(), totalTime);
		PreferenceHelper.set(TOTAL + ValueType.DISTANCE.getShortName(), (float) totalDistance);
		PreferenceHelper.set(TOTAL + ValueType.ALTITUDE.getShortName(), (float) totalAscent);
		PreferenceHelper.set(PEAK + ValueType.ALTITUDE.getShortName(), (float) highestPeak);
		PreferenceHelper.set(TOTAL + ValueType.POWER.getShortName(), (float) totalEnergy);
		summary = getSummary(Globals.appContext);
		PreferenceHelper.set(SUMMARY, summary);
		return summary;
	}

	public static String getSummary() {
		if (rides == -1) initTotals();
		return summary;
	}

	public static void resetTotals() {
		rides = 0;
		if (SenseGlobals.activity.equals(SenseGlobals.ActivityMode.RUN)) {
			rides++;
		}
		totalTime = 0;
		totalDistance = 0;
		totalAscent = 0;
		highestPeak = 0;
		totalEnergy = 0;
		saveTotals();
	}

	public static int getRides() {
		if (rides == -1) initTotals();
		return rides;
	}

	public static long getSessionTime() {
		if (rides == -1) initTotals();
		return sessionTime;
	}

	public static String getSessionString() {
		if (rides == -1) initTotals();
		return TimeUtil.formatTime(sessionTime, "yyyyMMdd-HHmmss");
	}

	public static void newSession() {
		if (rides == -1) initTotals();
		rides++;
		sessionTime = System.currentTimeMillis();
		SimpleLog.setSession(getSessionString());
		saveTotals();
	}

	public static double getTotalEnergy() {
		if (rides == -1) initTotals();
		return totalEnergy;
	}

	public static double getTotalDistance() {
		if (rides == -1) initTotals();
		return totalDistance;
	}

	public static double getHighestPeak() {
		if (rides == -1) initTotals();
		return highestPeak;
	}

	public static double getTotalAscent() {
		if (rides == -1) initTotals();
		return totalAscent;
	}

	public static long getTotalTime() {
		if (rides == -1) initTotals();
		return totalTime;
	}

	public static void updateValues() {
		for (Value value : values.values()) value.update();
	}

	public static Value getNextValue(Value value, boolean showXtra) {
		int i = valueList.indexOf(value);
		int iCount = 0;
		Value newValue = null;
		do {
			if (i >= 0) { 
				i++;
				i %= valueList.size();
			} else
				i = 0;
			newValue = valueList.get(i);
			//LLog.d(Globals.TAG, CLASSTAG + ".getNextValue: Testing " + newValue.getValueType() + " = " + newValue.getValueType().mayShow(showXtra));
		} while ((!newValue.hasRegistrations() || !newValue.getValueType().mayShow(showXtra)) && iCount++ < valueList.size());
		//LLog.d(Globals.TAG, CLASSTAG + ".getNextValue: " + i + " of " + valueList.size());
		return valueList.get(i);
	}

	public static Value getPreviousValue(Value value, boolean showXtra) {
		int i = valueList.indexOf(value);
		int iCount = 0;
		do {
			if (i > 0) { 
				i--;
			} else
				i = valueList.size() - 1;
		} while ((!valueList.get(i).hasRegistrations() || !valueList.get(i).getValueType().mayShow(showXtra)) && iCount++ < valueList.size());
		//LLog.d(Globals.TAG, CLASSTAG + ".getPreviousValue: " + i + " of " + valueList.size());
		return valueList.get(i);
	}

	public static void clearAllValues() {
		ValueType.clear();
		Laps.clear();
		lastValues.clear();
		history.clear();
		for (Value value : values.values()) value.clearAll();
	}

	public double getValueTimeProduct() {
		return this.valueTimeProduct;
	}

	private Value(final ValueType valueType) { //, final Device device) {
		this.valueType = valueType;
		presentation = valueType.presentByDefault();
		//addDevice(valueType, device);
		unit = PreferenceKey.getUnit(valueType);
		if (rides == -1) {
			initTotals();
		}
	}

	public static String[] getValuePopupList(Context context, boolean showXtra) {
		List<String> popupList = new ArrayList<String>();
		for (ValueType valueType : ValueType.values()) if ((valueType.isXtraValue() || showXtra) && valueType.isVisible()) popupList.add(valueType.getLabel(context));	
		String[] array = popupList.toArray(new String[0]);
		if (array == null) return null;
		Arrays.sort(array);
		return array;
	}

	public static Map<String, Boolean> getValueSelectedPopupList(Context context, EnumSet<ValueType> valuesSelected, boolean showXtra) {
		Map<String, Boolean> map = new TreeMap<String, Boolean>(); 
		for (ValueType valueType : ValueType.values()) {
			if ((valueType.isXtraValue() && !showXtra) || !valueType.isVisible()) continue;
			if (valuesSelected != null && valuesSelected.contains(valueType)) 
				map.put(valueType.getLabel(context), true);
			else 
				map.put(valueType.getLabel(context), false);
		}
		return map;
	}

	public static void initDevices(boolean deserialize) {
		DeviceType.clearDeviceTypes();
		/*valueTypes = EnumSet.noneOf(ValueType.class);
		for (Manager manager : Manager.managers.values()) {
			EnumSet<DeviceType> deviceTypes = manager.supportedDevices();
			DeviceType.addDeviceTypes(deviceTypes);
			for (DeviceType type: deviceTypes) valueTypes.addAll(type.getValueTypes());
		}*/
		// Dit weghalen !!!!
		/*for (Device device: Device.getDevices()) {
			for (ValueType valueType: device.getDeviceType().getValueTypes()) getValue(valueType, true);
		}*/
		clearAllValues();
		if (deserialize) new DeserializerTask().execute(TimeUtil.formatTime(System.currentTimeMillis(), "yyyyMMdd"));
	}

	public static Value getValue(final ValueType valueType, boolean mayCreate) {
		Value value;
		if ((value = values.get(valueType)) != null) return value;
		if (!mayCreate) return null;
		value = new Value(valueType);
		values.put(valueType, value);
		if (valueList.indexOf(value) < 0) {
			valueList.add(value);
			Collections.sort(valueList, new Comparator<Value>() {
				public int compare(Value o1, Value o2) {
					return o1.getValueType(Globals.appContext).compareTo(o2.getValueType(Globals.appContext));
				}
			});
		}
		return value;
	}

	public static Value getValueByString(String valueTypeShortName) {
		if (valueTypeShortName == null) return null;
		for (ValueType valueType : ValueType.values()) if (valueType.getShortName().equals(valueTypeShortName)) {
			return getValue(valueType, true);
		}
		return null;
	}

	public static Value getValueByString(Context context, String valueTypeName) {
		if (valueTypeName == null) return null;
		for (ValueType valueType : ValueType.values()) {
			if (valueType.getLabel(context).equals(valueTypeName)) return getValue(valueType, true);
		}
		return null;
	}


	public static void stopInstance() {
		for (Value value: values.values()) if (value != null) value.finish();
		lastValues.clear();
		values.clear();
		valueList.clear();
		//valueTypes.clear();
		history.clear();
	}


	public boolean greaterThan(double minValue) {
		if (currentValue == null) return false;
		return NumberUtil.greaterThan(currentValue.getValue(), minValue);
	}

	public void finish() {
		//devices.clear();
		refValues.clear();
		quickView.clear();
		tempQuickView.clear();
	}

	private MathVector getVector() {
		if (currentValue == null) return null;
		return new MathVector(currentValue.getDoubleValues(true));
	}

	public boolean isChangeToNotify(long notifyInterval) {
		if (changeVector == null) 
			changeVector = getVector();
		else
			changeVector.assign(getVector());
		if (changeVector == null) return false;
		long now = System.currentTimeMillis();


		if (now - lastNotified  > notifyInterval || (changeVector.maxDelta() > valueType.deltaToNotify() && PreferenceKey.VOICEONCHANGE.isTrue())) {
			changeVector.setReference();
			lastNotified = now;
			return true;
		}
		return false;
	}

	public void serialize() {
		//LLog.d(Globals.TAG, CLASSTAG + ".onSerialize for: " + valueType.getLabel(Globals.appContext));
		if (firstValue == null) {
			//LLog.d(Globals.TAG, CLASSTAG + ".serialize: Nothing to serialize as firstValue = null");
			return;
		}
		if (currentValue == null ) {
			//LLog.d(Globals.TAG, CLASSTAG + ".serialize: Nothing to serialize as currentValue = null");
			return;
		}
		/*if (valueTimeProduct == null) {
			LLog.d(Globals.TAG, CLASSTAG + ".serialize: Nothing to serialize as valueTimeProduct = null");
			return;
		}*/
		if (maxValue == null) {
			//LLog.d(Globals.TAG, CLASSTAG + ".serialize: Nothing to serialize as maxValue = null");
			return;
		}
		if (minValue == null) {
			//LLog.d(Globals.TAG, CLASSTAG + ".serialize: Nothing to serialize as maxValue = null");
			return;
		}
		File file = new File(SenseGlobals.getRestorePath()); 
		if (!file.isDirectory()) file.mkdirs();

		String filename = SenseGlobals.getRestorePath() + valueType.getShortName() + SenseGlobals.DELIM_DETAIL + 
				//StringUtil.toString(devices.values(), Globals.DELIM_DETAIL) + Globals.DELIM_DETAIL + 
				TimeUtil.formatTime(System.currentTimeMillis(), "yyyyMMdd") + EXT;
		file = new File(filename); 
		PrintWriter writer = null;
		try {
			//file.createNewFile();
			file.delete();
			writer = new PrintWriter(new FileWriter(file));

			writer.println(FILE_VERSION);
			writer.println(valueTimeProduct);
			writer.println(valueType.getVariance());
			writer.println(recovery);
			writer.println(value4thTimeProduct);
			//LLog.d(Globals.TAG, CLASSTAG + ".serialize: valueTimeProduct for " + valueType + " = " +valueTimeProduct);

			writer.println(valueTimeProductDelta);
			writer.println(deltaUp);
			writer.println(deltaDown);
			writer.println("" + lastUpdate);
			writer.println(firstValue.toString());

			if (valueType.isZeroAtPause()) currentValue = ValueItem.fromZero(valueType.getUnitType().getUnitClass(), valueType.getDimensions());
			writer.println(currentValue.toString());
			LLog.d(Globals.TAG, CLASSTAG + ".serialize: currentValue for " + valueType + " = " + currentValue.toHumanString());

			writer.println(maxValue.toString());
			writer.println(minValue.toString());
			//writer.println(quickView.size());
			//LLog.d(Globals.TAG, CLASSTAG + ".serialize: Saving quickView for " + filename);
			for (int i = 0; i < quickView.size(); i++) {
				synchronized(this) {
					if (i < quickView.size()) writer.println(quickView.get(i).toString());	
				}		
			}
			//LLog.d(Globals.TAG, CLASSTAG + ".serialize: Completed saving " + filename);

		} catch (IOException e) {
			LLog.e(Globals.TAG, CLASSTAG + ".serialize: Error creating file " + filename);
			//writer = null;
			return;
		} finally {
			if (writer != null) writer.close();
		}
	}

	public void deserialize(String dateSpec) {

		String filename = SenseGlobals.getRestorePath() + valueType.getShortName() + SenseGlobals.DELIM_DETAIL + 
				//StringUtil.toString(devices.values(), Globals.DELIM_DETAIL) + Globals.DELIM_DETAIL + 
				dateSpec + EXT;
		File file = new File(filename); 
		if (!file.exists()) {
			//LLog.d(Globals.TAG, CLASSTAG + ".deserialize: File " + filename + " does not exist. Skipping 'deserialize'");
			return;
		}
		clear();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			String line;
			if ((line = reader.readLine()) != null && !line.equals(FILE_VERSION)) return; 

			if ((line = reader.readLine()) != null) valueTimeProduct = StringUtil.toDouble(line, null);
			//LLog.d(Globals.TAG, CLASSTAG + ".deserialize: valueTimeProduct for " + valueType + " = " + valueTimeProduct);
			if ((line = reader.readLine()) != null) valueType.setVariance(StringUtil.toDouble(line, null));
			if ((line = reader.readLine()) != null) recovery = StringUtil.toDouble(line, null);
			if ((line = reader.readLine()) != null) value4thTimeProduct = StringUtil.toDouble(line, null);
			if ((line = reader.readLine()) != null) valueTimeProductDelta = StringUtil.toLong(line, null);
			if ((line = reader.readLine()) != null) deltaUp = StringUtil.toDouble(line, null);
			if ((line = reader.readLine()) != null) deltaDown = StringUtil.toDouble(line, null);
			if ((line = reader.readLine()) != null) lastUpdate = StringUtil.toLong(line, null);

			if ((line = reader.readLine()) != null) firstValue = ValueItem.fromString(line, valueType.getUnitType().getUnitClass());
			if ((line = reader.readLine()) != null) {
				if (valueType.isZeroAtPause())
					currentValue = ValueItem.fromZero(valueType.getUnitType().getUnitClass(), valueType.getDimensions());
				else
					currentValue = ValueItem.fromString(line, valueType.getUnitType().getUnitClass());
			}
			if (currentValue != null) LLog.d(Globals.TAG, CLASSTAG + ".deserialize: Current value for " + valueType + " = " + currentValue.toHumanString());

			lastValue = null;
			refValue = null;
			if ((line = reader.readLine()) != null) maxValue = ValueItem.fromString(line, valueType.getUnitType().getUnitClass());
			if ((line = reader.readLine()) != null) minValue = ValueItem.fromString(line, valueType.getUnitType().getUnitClass());
			while ((line = reader.readLine()) != null) {
				XY xy = XY.fromString(line);
				if (xy != null) quickView.add(xy);
				//if (valueType.equals(ValueType.CADENCE)) LLog.d(Globals.TAG, CLASSTAG + ".deserialize: xy = " + xy.toHRString());
			}
			//LLog.d(Globals.TAG, CLASSTAG + ".deserialize: Completed reading " + filename + " with " + quickView.size() + " values. ");

		} catch (IOException e) {
			LLog.e(Globals.TAG, CLASSTAG + ".deSerialize: Error opening file " + filename);
			return;
		} finally {
			if (reader != null) try {
				reader.close();
			} catch (IOException e) {

			}
		}
	}

	public void setPresentation(Presentation presentation) {
		this.presentation = presentation;
		fireOnValueChanged(true, false); //, ChangeFlag.PRESENTATION_CHANGED);
	}

	public void resetPresentation() {
		this.presentation = valueType.presentByDefault();
		fireOnValueChanged(true, false); //, ChangeFlag.PRESENTATION_CHANGED);
	}

	public Presentation getPresentation() {
		return presentation;
	}

	public interface ValueListener {
		public void onValueChanged(Value value, boolean isLive); //, ChangeFlag tag);
	}


	public boolean hasRegistrations() {
		return currentValue != null;
	}

	public boolean hasRecentRegistrations() {
		if (currentValue == null) return false;
		if (System.currentTimeMillis() - valueChanged > 10000) return false;
		return true;
	}

	public static void addGlobalListener(ValueListener listener) {
		if (globalListeners.contains(listener)) return;
		globalListeners.add(listener);	
	}

	public void addListener(ValueListener listener) {
		if (listeners.contains(listener)) return;
		listeners.add(listener);	
		listener.onValueChanged(this, false); //, ChangeFlag.LISTENER_ADD);
	}

	public static void removeGlobalsListener(ValueListener listener) {
		if (!globalListeners.contains(listener)) return;
		globalListeners.remove(listener);	
	}

	public void removeListener(ValueListener listener) {
		if (!listeners.contains(listener)) return;
		listeners.remove(listener);	
	}

	private void fireOnValueChanged(boolean immediate, boolean isLive) { //, ChangeFlag flag) {
		long now = System.currentTimeMillis();
		for (ValueListener listener : globalListeners) {
			listener.onValueChanged(this, isLive); //, flag);
		}
		if (Math.abs(now - lastUpdate) < 250 && !immediate) return;
		if (!immediate) lastUpdate = now;
		for (ValueListener listener : listeners) {
			listener.onValueChanged(this, isLive); //, flag);
		}
	}

	private boolean quickViewRecycled = false;
	private long valueChanged = 0;

	private void setCurrentValue(ValueItem item, int tag) {
		currentValue = item;
		currentTag = tag;
		int seconds = (int)((item.getTimeStamp() - TIMEOFFSET) / 1000);
		EnumMap<ValueType, ValueTag> valueRow = history.get(seconds);
		if (valueRow == null) {
			valueRow = new EnumMap<ValueType, ValueTag>(ValueType.class);
			history.put(seconds, valueRow);
		}
		valueRow.put(valueType, new ValueTag(item.getValue(), tag));
		lastValues.put(valueType, new ValueItemTag(item, tag));
	}

	private void setValue(ValueItem valueItem, int tag, boolean isNew) { //, boolean replace) {
		if (valueItem == null) return;
		if (
				currentValue != null && 
				currentTag != -1 && 
				tag > currentTag && 
				valueItem.getTimeStamp() - currentValue.getTimeStamp() < 180000 &&
				!(EnumSet.of(ValueType.POWER, ValueType.HEARTRATE, ValueType.PULSEWIDTH).contains(valueType))) return;
		valueChanged  = System.currentTimeMillis();

		if (SenseGlobals.activity.equals(SenseGlobals.ActivityMode.STOP)) {
			currentValue = valueItem;
			currentTag = tag;
			lastValue = null;
		} else {
			//			if (valueType.equals(ValueType.SPEED)) LLog.d(Globals.TAG, CLASSTAG + ".setValue: " + valueType + " = " + valueItem.getDoubleValue());
			synchronized(this) {
				if (refValue != null) {
					long delta = valueItem.getTimeStamp() - refValue.getTimeStamp();
					if (currentValue == null || valueItem.getTimeStamp() - currentValue.getTimeStamp() > MAX_DELTA)
						refValue = valueItem;
					else {
						double mean = valueItem.getLengthOrAvg(valueType.getDimensions());
						double meanOld = refValue.getLengthOrAvg(valueType.getDimensions());
						if ((Math.abs(mean) > valueType.minimalRelevantValue() || Math.abs(meanOld) > valueType.minimalRelevantValue()) && Math.abs(mean - meanOld) > valueType.getMinimalChange()) {
							double avg = 0.5 * (mean + meanOld);
							valueTimeProduct += avg * delta;
							if (valueType.isCalcVariablility() || valueType.isCalcSlope()) {
								if (avgItem == null) avgItem = new AverageDoubleItem(valueType.getShortName(), 120000, valueType.getMinimalAnomaly(), valueType.isCalcVariablility(), valueType.isCalcSlope(), 60000);
								avgItem.addObject(valueType.getShortName(), (Double) valueItem.getDoubleValue(), valueItem.getTimeStamp(), 0); 
								if (valueType.isCalcVariablility()) valueType.setVariance(avgItem.getVariance());
								if (valueType.isCalcSlope()) recovery = avgItem.getSlope();
							} 
							value4thTimeProduct += Math.pow(avg, 4) * delta;
							valueTimeProductDelta += delta;
							Laps.addAggregate(valueType, Laps.ValueAggregate.VALUETIME, avg * delta);
							Laps.addAggregate(valueType, Laps.ValueAggregate.DELTA, delta);
							Laps.addAggregate(valueType, Laps.ValueAggregate.MAX, mean);
							if (mean < meanOld) deltaDown += meanOld - mean;
							if (mean > meanOld) deltaUp += mean - meanOld;

							if (SenseGlobals.activity.equals(SenseGlobals.ActivityMode.RUN)) switch (valueType) {
							case TIME:
								totalTime += delta;
								break;
							case DISTANCE:
								totalDistance += Math.abs(mean - meanOld);
								break;
							case POWER:
								totalEnergy += avg * delta / 1000.0;
								break;
							case ALTITUDE:
								if (mean > highestPeak) highestPeak = mean;
								if (mean > meanOld) totalAscent += mean - meanOld;
								break;
							default:
								break;
							}
							refValue = valueItem;
						}
					}
				}
				/*if (lastValue != null && valueType.getWeightFactor() < 1) {
					MathVector v1 = new MathVector(lastValue.getDoubleValues(true));
					MathVector v2 = new MathVector(valueItem.getDoubleValues(true));
					v1.timestimes(1.0 - valueType.getWeightFactor());
					v2.timestimes(valueType.getWeightFactor());
					valueItem = new ValueItem(v2.plus(v1).getValues(), valueItem.getTimeStamp());
				} */
				if (Globals.testing.isTest() && EnumSet.of(ValueType.POWER, ValueType.HEARTRATE).contains(valueType)) {
					int i = 0;
					while (currents.size() > 0 && i < currents.size()) {
						if (currents.keyAt(i) != tag && (valueItem.getTimeStamp() - currents.valueAt(i).getTimeStamp() > 5000))
							currents.delete(currents.keyAt(i));
						else
							i++;
					}
					currents.put(tag, valueItem);
					if (currents.size() > 1 && valueType.getDimensions() == 1) {
						double doubles[] = new double[currents.size()];
						int tags[] = new int[currents.size()];
						for (i = 0; i < currents.size(); i++) {
							doubles[i] = currents.valueAt(i).getDoubleValue();
							tags[i] = currents.keyAt(i);
						}
						setCurrentValue(new ValueItem(doubles, valueItem.getTimeStamp()), tag);
						//LLog.d(Globals.TAG, CLASSTAG + ".setValue tag = " + StringUtil.toString(tags, " - "));
					} else
						setCurrentValue( valueItem, tag);
					//if (valueType.equals(ValueType.POWER)) LLog.d(Globals.TAG, CLASSTAG + ".power = " + currentValue.toString());
				} else
					setCurrentValue(valueItem, tag);

				lastValue = valueItem;
				valueType.setRaw(lastValue.getDoubleValue());
				if (refValue == null) refValue = valueItem;

				if (maxValue == null) {
					maxValue =  valueItem.clone();
				} else
					maxValue.maxOf(valueItem);
				if (minValue == null) {
					minValue =  valueItem.clone();
				} else
					minValue.minOf(valueItem);
				if (firstValue == null) 
					firstValue = valueItem;
				else {
					long elapsed = firstValue.getTimeStamp(); 
					if (elapsed > 0) elapsed = valueItem.getTimeStamp() - elapsed;

					if (valueType.showLastValuesOnly()) {
						if (quickView.size() >= SenseGlobals.MAX_QUICKVIEW) {
							quickView.remove(0);
						}
						quickView.add(valueItem.getXY(tag));
					} else {
						float deltaTime = 1.0f * elapsed / SenseGlobals.MAX_QUICKVIEW;
						if (quickView.size() >= SenseGlobals.MAX_QUICKVIEW) {
							int reducer = 0;
							synchronized(this) {
								while (++reducer < quickView.size() - 1) {

									XY val1 = quickView.get(reducer);
									XY val2 = quickView.get(reducer + 1);
									if (val2.getX() - val1.getX() > 1.5f * deltaTime) {
										reducer++;
										continue;
									}
									val1.mean(val2);
                                    if (val1.getY()[0] == Float.NaN) {
                                        LLog.d(Globals.TAG, CLASSTAG + " val1.y =  NaN");
                                    }
									quickView.remove(reducer + 1);
                                    if (val1.toString().contains("NaN")) {
                                        LLog.d(Globals.TAG, CLASSTAG + " val1.y =  NaN");
                                    }
									quickView.set(reducer, val1);
								}	
							}
							quickViewRecycled = true;
						}
						XY xy = valueItem.getXY(tag); //valueItem.getTag());
						if (xy != null) {
							if (quickView.size() == 0 || !quickViewRecycled || xy.getX() - quickView.get(quickView.size() - 1).getX() > deltaTime) {
								if (tempQuickView.size() > 0) {
									XY avg = XY.mean(tempQuickView);
									synchronized(this) {
                                        if (avg.toString().contains("NaN")) {
                                            LLog.d(Globals.TAG, CLASSTAG + " avg.y =  NaN");
                                        }
										quickView.add(avg);
									}
									tempQuickView.clear();
                                    if (xy.toString().contains("NaN")) {
                                        LLog.d(Globals.TAG, CLASSTAG + " tempQuickView.add(xy.y =  NaN)");
                                    }
                                    tempQuickView.add(xy);
								} else synchronized(this) {
                                    if (xy.toString().contains("NaN")) {
                                        LLog.d(Globals.TAG, CLASSTAG + " xy.y =  NaN");
                                    }
                                    quickView.add(xy);
								}
							} else {
                                if (xy.toString().contains("NaN")) {
                                    LLog.d(Globals.TAG, CLASSTAG + " tempQuickView.add(xy.y =  NaN) 2");
                                }
                                tempQuickView.add(xy);
							}
						}
					}
				}
			}
			if (SenseGlobals.isBikeApp && isNew && valueType.isStorable() && SenseGlobals.activity.equals(SenseGlobals.ActivityMode.RUN) && (!valueType.isXtraValue() || ValueType.isShowExtraValues)) {
				for (FileType type: fileTypes) switch (type) {
				case FIT: if (FitLog.getInstance() != null) FitLog.getInstance().store(valueType, valueItem); break;
				case SRM: if (SRMLog.getInstance() != null) SRMLog.getInstance().set(valueType, valueItem); break;
				case GPX: if (GpxLog.getInstance() != null) GpxLog.getInstance().set(valueType, valueItem); break;
				case PWX: 
					if (PwxLog.getInstance() != null) if (valueType.equals(ValueType.TIME))
						PwxLog.getInstance().setDuration(valueItem.getTimeStamp());
					else
						PwxLog.getInstance().set(valueType, valueItem);
					break;
				case GCSV: GoldenCheetahLog.getInstance().save(valueType, valueItem); break;
				case CSV:
				case SCR:
				default:
					break;
				}
			}
		}
		fireOnValueChanged(valueType.equals(ValueType.ACCELERATION), isNew); //, ChangeFlag.VALUE_CHANGED);
	}

	public List<XY> getQuickView() {
		return quickView;
	}

	public float getValueAsFloat() {
		if (currentValue != null)
			return currentValue.getFloatValue();
		return 0f;
	}

	private long getValueTimeStamp() {
		if (currentValue != null) {
			return currentValue.getTimeStamp();
		}
		return 0;
	}

	private ValueItem getDelta() {
		if (firstValue == null) return currentValue;
		if(currentValue == null) return null;
		if (valueType.getUnitType().getClass().equals(String.class)) return currentValue;
		if (valueType.equals(ValueType.TIME)) return new ValueItem(valueTimeProductDelta, currentValue.getTimeStamp());
		ValueItem item1 = firstValue; //getFirstValue(valueItems);
		ValueItem item2 = currentValue;
		if (item1 != null && item2 != null)
			return new ValueItem(item2.getDoubleValue() - item1.getDoubleValue(), item2.getTimeStamp()); //, 0);
		return new ValueItem(Double.valueOf(0), item2.getTimeStamp()); //, 0);
	}

	private double getAverage() {
		if (valueTimeProductDelta == 0) return 0;
		return valueTimeProduct / valueTimeProductDelta;
	}

	private double getNormalized() {
		if (valueTimeProductDelta == 0) return 0;
		return Math.pow(value4thTimeProduct / valueTimeProductDelta, 0.25);
	}

	public String print(Presentation presentation) { 
		return print(presentation, false, false, false, false);
	}

	public String printSummary(Presentation presentation, boolean metric) {
		return print(presentation, true, false, true, metric);
	}

	private String print(Presentation presentation, boolean def, boolean speech, boolean summary, boolean metric) { 
		ValueItem valueItem = null; 
		//		AppPreferences PreferenceHelper = AppPreferences.getInstance();
		if (summary || currentValue != null) switch (presentation) {
		case VALUE: valueItem = currentValue; break;
		case AVERAGE: valueItem = new ValueItem(getAverage(), System.currentTimeMillis()); break; 
		case DELTA: valueItem = getDelta(); break;
		case MAX: valueItem = maxValue; break;
		case MIN: valueItem = minValue; break;
		case DELTAUP: 
			//			if (valueType.equals(ValueType.HRZONE))
			//				valueItem = new ValueItem(PreferenceHelper.getMaxHeartrate(), System.currentTimeMillis()); 
			//			else
			valueItem = new ValueItem(deltaUp, System.currentTimeMillis()); 
			break;
		case DELTADOWN: 
			//			if (valueType.equals(ValueType.HRZONE))
			//				valueItem = new ValueItem(PreferenceHelper.getMinHeartrate(), System.currentTimeMillis()); 
			//			else
			valueItem = new ValueItem(-1.0 * deltaDown, System.currentTimeMillis()); 
			break;
		case VALUETIME: valueItem = new ValueItem(valueTimeProduct / 1000.0, System.currentTimeMillis()); break;
		case NORMALIZED: valueItem = new ValueItem(getNormalized(), System.currentTimeMillis()); break;	
		case VARIANCE: 
			if (valueType.equals(ValueType.HEARTRATE))
				valueItem = new ValueItem(ValueType.PULSEWIDTH.getVariance(), System.currentTimeMillis()); 
			else
				valueItem = new ValueItem(valueType.getVariance(), System.currentTimeMillis()); 
			break;	
		case CHANGE: if (Math.abs(recovery) < 5 * valueType.getMinimalChange()) return ""; else valueItem = new ValueItem(recovery, System.currentTimeMillis()); break;	
		default: valueItem = currentValue; break;
		}
		if (summary && valueItem == null) valueItem = new ValueItem(0, System.currentTimeMillis());
		if (valueItem == null) {

			if (def) return Globals.appContext.getString(R.string.value);
			switch (presentation) {
			case VALUE: return Globals.appContext.getString(R.string.value);
			case AVERAGE: return Globals.appContext.getString(R.string.average);
			case DELTA: return Globals.appContext.getString(R.string.delta);
			case MAX: return Globals.appContext.getString(R.string.max);
			case MIN: return Globals.appContext.getString(R.string.min);
			case DELTAUP: return Globals.appContext.getString(R.string.up);
			case DELTADOWN: return Globals.appContext.getString(R.string.down);
			case VALUETIME: return valueType.getValueTimeText(Globals.appContext);
			case NORMALIZED: return Globals.appContext.getString(R.string.normalized);
			case VARIANCE: return Globals.appContext.getString(R.string.variance);
			case CHANGE: return Globals.appContext.getString(R.string.change);
			default: return "";
			}
		}

		if (valueType.isSingleValue(presentation))  {
			valueItem = valueItem.cloneToSingleDimension();
		}
		return print(presentation, valueItem, speech, metric);
	}

	public String printValue(Object value, boolean metric) {
		return print(null, new ValueItem(value, System.currentTimeMillis()), false, metric);
	}

	private String print(Presentation presentation, ValueItem item, boolean speech, boolean metric) {
		Unit tempUnit = unit;
		String unitStr = "";
		if (presentation != null && presentation.equals(Presentation.VALUETIME)) {
			tempUnit = valueType.getValueTimeUnit();
			unitStr = " " + tempUnit.getLabel(Globals.appContext);
		}
		if (metric) tempUnit = valueType.metricUnit();
		if (tempUnit == null) tempUnit = valueType.defaultUnit();
		return tempUnit.getFormated(item, valueType.getDecimals(), speech) + unitStr;
	}

	public String speak() {
		return print(this.presentation, true, true, false, false); //, false);
	}

	public String print() {
		return print(this.presentation, true, false, false, false); 
	}

	public String getUnitLabel(Context context) {
		if (unit != null) return unit.getLabel(context);
		return valueType.defaultUnit().getLabel(context);
	}

	public String getUnitSpeechLabel(Context context) {
		if (unit != null) return unit.getSpeechLabel(context);
		return valueType.defaultUnit().getSpeechLabel(context);
	}

	public void setUnit(Unit unit) {
		this.unit = unit;
		PreferenceKey.setUnit(valueType, unit);
		fireOnValueChanged(true, false); //, ChangeFlag.UNIT_CHANGED);
	}

	public Unit getUnit() {
		if (unit == null) return valueType.defaultUnit();
		return unit;
	}

	public void nextUnit() {
		if (unit == null) unit = valueType.defaultUnit();
		unit = Unit.getNextUnit(unit, valueType.getUnitType(), Globals.appContext);
		PreferenceKey.setUnit(valueType, unit);
		fireOnValueChanged(true, false); //, ChangeFlag.UNIT_CHANGED);
	}

	public String getValueType(Context context) {
		return valueType.getLabel(context);
	}

	public ValueType getValueType() {
		return valueType;
	}

	public void onDeviceValueChanged(DeviceStub device, /*ValueType valueType, */ ValueItem valueItem, boolean isNew) {
		onDeviceValueChanged(/*valueType, */ valueItem, device.getDeviceType().getPriority(), isNew);
	}



	private void onDeviceValueChanged(/*ValueType valueType,*/ ValueItem valueItem, int tag, boolean isNew) {
		if (valueItem == null) return;
		//ValueItemTag itemTag;
		//if ((itemTag = rawValues.get(valueType)) == null || itemTag.tag <= tag || valueItem.getTimeStamp() - itemTag.item.getTimeStamp() > 3000) 
		//	rawValues.put(valueType, new ValueItemTag(valueItem, tag));

		switch (this.valueType) {
		case SPEED: 
			setValue(valueItem, tag, isNew);
			getValue(ValueType.DISTANCE, true).onDeviceValueChanged(valueItem, tag, isNew);
			break;
		case CADENCE:
			setValue(valueItem, tag, isNew);
			getValue(ValueType.GAINRATIO, true).onDeviceValueChanged(valueItem, tag, isNew);
			break;
		case STEPS: 
			if (currentValue == null)
				setValue(valueItem, tag, isNew);
			else {
				valueItem.setValue(valueItem.getIntValue() + currentValue.getIntValue());
				setValue(valueItem, tag, isNew);
			}	
			break;

		case DISTANCE: 
			if (maxValue == null) {
				setValue(new ValueItem((Double)0.0, valueItem.getTimeStamp()), tag, isNew);
			} else {
				long delta = valueItem.getTimeStamp() - getValueTimeStamp();
				if (delta > MAX_DELTA) 
					setValue(new ValueItem(maxValue.getDoubleValue(), valueItem.getTimeStamp()), tag, isNew);
				else if (delta > 0) 
					setValue(new ValueItem((Double)0.001 * delta * valueItem.getDoubleValue()  + maxValue.getDoubleValue(), valueItem.getTimeStamp()), tag, isNew);
			}
			break;
		case GAINRATIO: 
			ValueItemTag cadence = lastValues.get(ValueType.CADENCE);
			ValueItemTag wheel = lastValues.get(ValueType.WHEEL_REVS);
			if (
					cadence != null && 
					wheel != null &&
					wheel.item.getDoubleValue() > 0 &&
					wheel.item.getDoubleValue() > 0 &&
					Math.abs(wheel.item.getTimeStamp() - cadence.item.getTimeStamp()) < 3000
					) {
				ValueItemTag power = lastValues.get(ValueType.POWER);
				double ratio = wheel.item.getDoubleValue() / cadence.item.getDoubleValue();
				if (power == null) { // && !devices.containsKey(ValueType.POWER)) {
					if (ratio < ValueType.GAINRATIO.maxValue()) setValue(new ValueItem(ratio, valueItem.getTimeStamp()), wheel.tag | cadence.tag, isNew);
				} else if (power != null && power.item.getDoubleValue() > 10.0 && Math.abs(valueItem.getTimeStamp() - power.item.getTimeStamp()) < 3000) {
					if (ratio < ValueType.GAINRATIO.maxValue()) setValue(new ValueItem(ratio, valueItem.getTimeStamp()), wheel.tag | cadence.tag, isNew);
				}				
			}
			break;
		case EFFICIENCY:
			ValueItemTag power = lastValues.get(ValueType.CADENCE);
			ValueItemTag hr = lastValues.get(ValueType.WHEEL_REVS);

			if (
					power != null && 
					hr != null &&
					power.item.getDoubleValue() > 0 &&
					hr.item.getDoubleValue() > 0 &&
					Math.abs(hr.item.getTimeStamp() - power.item.getTimeStamp()) < 3000

					) {
				if (power.item.getDoubleValue() > 10.0) {
					double efficiency = power.item.getDoubleValue() / hr.item.getDoubleValue();
					setValue(new ValueItem(efficiency, valueItem.getTimeStamp() ), power.tag | hr.tag, isNew);
				}
			}
			break;
		case ENERGY:
			if (currentValue == null)
				setValue(valueItem, tag, isNew);
			else {
				valueItem.setValue(valueItem.getDoubleValue() + currentValue.getDoubleValue());
				setValue(valueItem, tag, isNew);
			}
			break;
		case POWER:
			//LLog.d(Globals.TAG, CLASSTAG + ".atDeviceValueChanged for POWER = " + valueItem.getDoubleValue());
			setValue(valueItem, tag, isNew);
			getValue(ValueType.EFFICIENCY, true).onDeviceValueChanged(valueItem, tag, isNew);
			break;
		case AIRPRESSURE:
			//P = Actual pressure (Pascal)
			//Po = 101325	sea level standard pressure, Pa
			//To = 288.15   sea level standard temperature, deg K
			//g = 9.80665   gravitational constant, m/sec2
			//L =  6.5    	temperature lapse rate, deg K/km
			//R = 8.31432	gas constant, J/ mol*deg K 
			//M = 28.9644	molecular weight of dry air, gm/mol
			//H = To / L * (1 - Power{ (P / Po) , ((L * R ) / (g * M)) } )
			if (valueType.equals(this.valueType)) setValue(valueItem, tag, isNew);
			if (lastValues.get(ValueType.AIRPRESSURE) != null) {
				double pressure = valueItem.getDoubleValue();			
				double altitude = 1000.0 * (44.3308 - 4.94654 * Math.pow(pressure, 0.190263));
				//LLog.d(Globals.TAG, CLASSTAG + ".atDeviceValueChanged:" +
				//		" Pressure = " + pressure + 
				//		", Alt = " + altitude +
				//		", Time = " + Util.formatTime(valueItem.getTimeStamp()));
				ValueItemTag altitudeItem = refValues.get(ValueType.ALTITUDE);
				ValueItemTag distanceItem = refValues.get(ValueType.DISTANCE);

				Value distance = getValue(ValueType.DISTANCE, false);					
				if (distance == null || distance.currentValue == null) break;

				if (altitudeItem == null || distanceItem == null) {
					refValues.put(ValueType.ALTITUDE, new ValueItemTag(new ValueItem(altitude, valueItem.getTimeStamp()), tag));
					refValues.put(ValueType.DISTANCE, new ValueItemTag(distance.currentValue, tag));
				} else {
					double deltaHeight = (altitude - altitudeItem.item.getDoubleValue());
					double deltaDistance = (distance.currentValue.getDoubleValue() - distanceItem.item.getDoubleValue());
					long deltaT = valueItem.getTimeStamp() - altitudeItem.item.getTimeStamp();
					Double slope = 0.0;
					if (deltaT > 0 && Math.abs(deltaHeight) > ValueType.ALTITUDE.getMinimalChange() && deltaDistance > 0) {
						//						LLog.d(Globals.TAG, CLASSTAG + ".atDeviceValueChanged:" +
						//								" DeltaAlt = " + deltaHeight + 
						//								", DeltaTime = " + Util.formatElapsedMilli(deltaT, Globals.appContext) + " used!!!");
						slope = deltaHeight/ deltaDistance;
						refValues.put(ValueType.ALTITUDE, new ValueItemTag(new ValueItem(altitude, valueItem.getTimeStamp()), tag));
						refValues.put(ValueType.DISTANCE, new ValueItemTag(distance.currentValue, tag));
					} 
					getValue(ValueType.SLOPE, true).setValue(new ValueItem(slope, valueItem.getTimeStamp()), tag, isNew);
				}
			}
			//fireOnDeviceValueChanged(ValueType.ALTITUDE, newAltitude, now);
			//if ( timestamp > 0 && altitude != Double.MAX_VALUE)
			//	fireOnDeviceValueChanged(ValueType.VERTSPEED, 1000 * (newAltitude - altitude) / (now - timestamp), now);
			break;
		case SLOPE:
			setValue(valueItem, tag, isNew);
			break;
		case HEARTRATE:
			if (!valueType.equals(this.valueType)) break;
			int min = PreferenceKey.MIN_HEARTRATE.getInt();
			int max = PreferenceKey.MAX_HEARTRATE.getInt();
			if (zones == null || zones[0] != max || zones[zones.length - 1] != min) {
				zones = new float[] {
						max, 
						min + 0.8f * (max - min), 
						min + 0.6f * (max - min), 
						min + 0.4f * (max - min), 
						min + 0.2f * (max - min), 
						min}; 
			}
			setValue(valueItem, tag, isNew);
			getValue(ValueType.EFFICIENCY, true).onDeviceValueChanged(valueItem, tag, isNew);
			getValue(ValueType.HRZONE, true).onDeviceValueChanged(valueItem, tag, isNew);
			break;
		case HRZONE:
			int hrm = valueItem.getIntValue();
			min = PreferenceKey.MIN_HEARTRATE.getInt();
			max = PreferenceKey.MAX_HEARTRATE.getInt();
			double zone = 0;
			if (max != min) zone = 1 + 5.0 * (hrm - min) / (max - min);
			if (zone < 1) zone = 1;
			if (zone >= 6) zone = 5;
			if (zones == null) {
				zones = new float[] {
						5.5f, 
						4.5f, 
						3.5f, 
						2.5f, 
						1.5f, 
						0}; 
			}
			setValue(new ValueItem((int)zone, valueItem.getTimeStamp()), tag, isNew);
			if (Globals.testing.isVerbose()) LLog.d(Globals.TAG, CLASSTAG + " HR (" + hrm + ") Zone = " + Format.format(zone, 1) + " (" + min + ", " + max + ")");

			break;
		case WHEEL_REVS: 
			setValue(valueItem, tag, isNew);
			getValue(ValueType.SPEED, true).setValue(new ValueItem(valueItem.getDoubleValue() * 0.001 * SenseGlobals.wheelCirc / 60.0, valueItem.getTimeStamp()), tag, isNew); 
			getValue(ValueType.GAINRATIO, true).onDeviceValueChanged(valueItem, tag, isNew);
			break;
		default:
			setValue(valueItem, tag, isNew);
			break;
		}
	}


	//	@Override
	//	public boolean supportsValueType(ValueType valueType) {
	//		if (this.valueType.equals(valueType)) return true;
	//		if (this.valueType.getDependentOn() == null) return false;
	//		if (this.valueType.getDependentOn().contains(valueType)) return true;
	//		return false;
	//	}

	@Override 
	public int hashCode() {
		return valueType.getShortName().hashCode();
	}

	@Override 
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (!(obj instanceof Value)) return false;
		if (obj == this) return true;
		Value value = (Value) obj;
		if (value.valueType.equals(valueType)) return true;
		return false;
	}

	//	@Override
	//	public void onDataLogHasData(ValueType valueType, ValueItem item, int tag) {
	//		Device.globalListener.onDeviceValueChanged(null, valueType, item, tag);
	//		onDeviceValueChanged(valueType, item, tag, false);	
	//	}

	private void clear() {
		lastValues.clear();
		refValues.clear();
		currentValue = null;
		lastValue = null;
		refValue = null;
		firstValue = null;
		quickView.clear();
		tempQuickView.clear();
		valueTimeProduct = 0;
		valueTimeProductDelta = 0;
		avgItem = null;
		value4thTimeProduct = 0;
		maxValue = null;
		minValue = null;
		deltaDown = 0;
		deltaUp = 0;
		presentation = valueType.presentByDefault();
		lastUpdate = 0;
	}

	public void update() {
		fireOnValueChanged(true, false); 
	}

	public void clearAll() {	
		if (Globals.testing.isVerbose()) LLog.d(Globals.TAG, CLASSTAG + ".clearAll for " + valueType.getLabel(Globals.appContext)); // + " by " + StringUtil.toString(devices.values(), ", "));
		clear();
		String filename = SenseGlobals.getRestorePath() + valueType.getShortName() + SenseGlobals.DELIM_DETAIL + 
				//		StringUtil.toString(devices.values(), Globals.DELIM_DETAIL) + Globals.DELIM_DETAIL + 
				TimeUtil.formatTime(System.currentTimeMillis(), "yyyyMMdd") + EXT;
		new File(filename).delete(); 
		//LLog.d(Globals.TAG, CLASSTAG + ".onReadStart: Deleting " + filename);
		fireOnValueChanged(true, false); //, ChangeFlag.READ_START);
	}

	public void checkLastValues() {
		if (lastValue == null || !SenseGlobals.activity.equals(SenseGlobals.ActivityMode.RUN)) return;

		long delta = lastValue.getTimeStamp() - refValue.getTimeStamp();
		double mean = lastValue.getLengthOrAvg(valueType.getDimensions());
		double meanOld = refValue.getLengthOrAvg(valueType.getDimensions());
		if ((Math.abs(mean) > valueType.minimalRelevantValue() || Math.abs(meanOld) > valueType.minimalRelevantValue())) {
			double avg = 0.5 * (mean + meanOld);
			valueTimeProduct += avg * delta;
			valueTimeProductDelta += delta;
			value4thTimeProduct += Math.pow(avg, 4) * delta;

			if (mean < meanOld) deltaDown += meanOld - mean;
			if (mean > meanOld) deltaUp += mean - meanOld;
			switch (valueType) {
			case TIME:
				totalTime += delta;
				break;
			case DISTANCE:
				totalDistance += Math.abs(mean - meanOld);
				break;
			case POWER:
				totalEnergy += avg * delta / 1000.0;
				break;
			case ALTITUDE:
				if (mean > highestPeak) highestPeak = mean;
				totalAscent += deltaUp;
				break;
			default:
				break;
			}

		}
		refValue = null;
		lastValue = null;
	}

	public float[] getZones() {
		if (Globals.testing.isTest() || PreferenceKey.XTRAVALUES.isTrue()) return zones;
		return null;
	}

	public static void serializeValues() {
		LLog.d(Globals.TAG, CLASSTAG + ".serialize with " + values.size() + " listeners.");
		new SerializerTask().execute();
	}

	private static class SerializerTask extends AsyncTask<Void, Void, Boolean> {

		@Override
		protected Boolean doInBackground(Void... params) {
			for (Value value : values.values()) {
				value.serialize();
			}
			return true;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			//ToastHelper.showToastShort("Serialization complete");
			LLog.d(Globals.TAG, CLASSTAG + ".SerializerTask.onPostExecute: Serialization complete");

		}
	}

	private static class DeserializerTask extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... params) {
			if (params.length > 0) {
				for (Value value : values.values()) value.deserialize(params[0]);
				return params[0];
			}
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			//ToastHelper.showToastShort("Deserialization complete");
			LLog.d(Globals.TAG, CLASSTAG + ".DeserializerTask.onPostExecute: Deserialization complete");
			//Globals.mayRead = result == null || result.equals(Util.formatTime(System.currentTimeMillis(), "yyyyMMdd"));
			for (Value value : values.values()) {
				value.update();
			}
		}

	}

	public static String getSummary(Context context) {

		StringBuffer summary = new StringBuffer();
		summary.
		append(context.getString(R.string.overview_alltime)).append("\r\n");

		Value value;
		if ((value = getValue(ValueType.TIME, false))!= null) {
			summary.
			append(context.getString(R.string.overview_rides)).append(": ").
			append(getRides()).append(" ");
			summary.append("\r\n");
			summary.
			append(context.getString(R.string.overview_total_time)).append(": ").
			append(value.printValue((Long)getTotalTime(), false)).append(" ");
			summary.append("\r\n");
		}

		if (SenseGlobals.isBikeApp && (value = getValue(ValueType.DISTANCE, false))!= null) {
			summary.
			append(context.getString(R.string.overview_total_distance)).append(": ").
			append(value.printValue((Double)getTotalDistance(), false)).append(" ");
			summary.append(value.getUnitLabel(context));
			summary.append("\r\n");
		}

		if ((value = getValue(ValueType.ALTITUDE, false))!= null) {
			summary.
			append(context.getString(R.string.overview_total_altitude)).append(": ").
			append(value.printValue((Double)getTotalAscent(), false)).append(" ");
			summary.append(value.getUnitLabel(context));
			summary.append("\r\n");
		}
		if (SenseGlobals.isBikeApp && (value = getValue(ValueType.POWER, false))!= null && getTotalEnergy() > 0) {
			summary.
			append(context.getString(R.string.overview_total_energy)).append(": ").
			append(Unit.KILOJOULE.getFormated((Double)getTotalEnergy(),0)).append(" ");
			summary.append(Unit.KILOJOULE.getLabel(context));
			summary.append("\r\n\r\n");
		}

		summary.
		append(context.getString(R.string.overview_activity)).append("\r\n");
		if ((value = getValue(ValueType.TIME, false)) != null && value.hasRegistrations()) {
			summary.
			append(context.getString(R.string.overview_starttime)).append(": ").
			append(Unit.DATETIME.getFormated((Long) getSessionTime(), 0)).append(" ").
			append(value.getUnitLabel(context)).append("\r\n");;
			summary.
			append(context.getString(R.string.overview_time)).append(": ").
			append(value.printSummary(Presentation.DELTA, false)).append(" ").
			append(value.getUnitLabel(context)).append("\r\n");;
		}
		if ((value = getValue(ValueType.DISTANCE, false)) != null && value.hasRegistrations()) {
			summary.
			append(context.getString(R.string.overview_distance)).append(": ").
			append(value.printSummary(Presentation.MAX, false)).append(" ").
			append(value.getUnitLabel(context)).append("\r\n");;
		}
		if ((value = getValue(ValueType.SPEED, false)) != null && value.hasRegistrations()) {
			summary.
			append(context.getString(R.string.overview_avg_speed)).append(": ").
			append(value.printSummary(Presentation.AVERAGE, false)).append(" ").
			append(value.getUnitLabel(context)).append("\r\n");;
			summary.
			append(context.getString(R.string.overview_max_speed)).append(": ").
			append(value.printSummary(Presentation.MAX, false)).append(" ").
			append(value.getUnitLabel(context)).append("\r\n");
		}
		if ((value = getValue(ValueType.WINDSPEED, false)) != null && value.hasRegistrations()) {
			summary.
			append(context.getString(R.string.overview_avg_windspeed)).append(": ").
			append(value.printSummary(Presentation.AVERAGE, false)).append(" ").
			append(value.getUnitLabel(context)).append("\r\n");;
			summary.
			append(context.getString(R.string.overview_max_windspeed)).append(": ").
			append(value.printSummary(Presentation.MAX, false)).append(" ").
			append(value.getUnitLabel(context)).append("\r\n");
		}
		if (SenseGlobals.isBikeApp && (value = getValue(ValueType.HEARTRATE, false)) != null && value.hasRegistrations()) {
			summary.
			append(context.getString(R.string.overview_avg_heartrate)).append(": ").
			append(value.printSummary(Presentation.AVERAGE, false)).append(" ").
			append(value.getUnitLabel(context)).append("\r\n");;
			summary.
			append(context.getString(R.string.overview_max_heartrate)).append(": ").
			append(value.printSummary(Presentation.MAX, false)).append(" ").
			append(value.getUnitLabel(context)).append("\r\n");
		}
		if (SenseGlobals.isBikeApp && (value = getValue(ValueType.CADENCE, false)) != null && value.hasRegistrations()) {
			summary.
			append(context.getString(R.string.overview_avg_cadence)).append(": ").
			append(value.printSummary(Presentation.AVERAGE, false)).append(" ").
			append(value.getUnitLabel(context)).append("\r\n");;
		}
		if (SenseGlobals.isBikeApp && (value = values.get(ValueType.POWER)) != null && value.hasRegistrations()) {
			summary.
			append(context.getString(R.string.overview_avg_power)).append(": ").
			append(value.printSummary(Presentation.AVERAGE, false)).append(" ").
			append(value.getUnitLabel(context)).append("\r\n");;
			summary.
			append(context.getString(R.string.overview_normalized_power)).append(": ").
			append(value.printSummary(Presentation.NORMALIZED, false)).append(" ").
			append(value.getUnitLabel(context)).append("\r\n");;
			summary.
			append(context.getString(R.string.overview_max_power)).append(": ").
			append(value.printSummary(Presentation.MAX, false)).append(" ").
			append(value.getUnitLabel(context)).append("\r\n");
			summary.
			append(context.getString(R.string.overview_energy)).append(": ").
			append(Unit.KILOJOULE.getFormated((Double)(value.getValueTimeProduct() / 1000.0), 0)).append(" ").
			//append(value.printSummary(Presentation.VALUETIME)).append(" ").
			append(Unit.KILOJOULE.getLabel(context)).append("\r\n");
		}
		if (SenseGlobals.isBikeApp && (value = getValue(ValueType.EFFICIENCY, false)) != null && value.hasRegistrations()) {
			summary.
			append(context.getString(R.string.overview_avg_efficiency)).append(": ").
			append(value.printSummary(Presentation.AVERAGE, false)).append(" ").
			append(value.getUnitLabel(context)).append("\r\n");;
		}
		if ((value = getValue(ValueType.ALTITUDE, false)) != null && value.hasRegistrations()) {
			summary.
			append(context.getString(R.string.overview_ascent)).append(": ").
			append(value.printSummary(Presentation.DELTAUP, false)).append(" ").
			append(value.getUnitLabel(context)).append("\r\n");;
			summary.
			append(context.getString(R.string.overview_descent)).append(": ").
			append(value.printSummary(Presentation.DELTADOWN, false)).append(" ").
			append(value.getUnitLabel(context)).append("\r\n");;
			summary.
			append(context.getString(R.string.overview_highest_altitude)).append(": ").
			append(value.printSummary(Presentation.MAX, false)).append(" ").
			append(value.getUnitLabel(context)).append("\r\n");;
			summary.
			append(context.getString(R.string.overview_lowest_altitude)).append(": ").
			append(value.printSummary(Presentation.MIN, false)).append(" ").
			append(value.getUnitLabel(context)).append("\r\n");;
		}
		return summary.toString();
	}


	private static void getMinMax(StringBuffer summary, final String id, final Value value) {
		summary.
		append("<").
		append(id).
		append(" max=\"").
		append(value.printSummary(Presentation.MAX, true)).
		append("\" min=\"").
		append(value.printSummary(Presentation.MIN, true)).
		append("\" avg=\"").
		append(value.printSummary(Presentation.AVERAGE, true)).
		append("\" />").
		append(SenseGlobals.ln); 
	}

	public static String getPwxSummary() {
		StringBuffer summary = new StringBuffer();
		Value value;
		if ((value = getValue(ValueType.POWER, false))!= null && value.hasRegistrations()) {
			summary.
			append("<normalizedPower>").
			append(value.printSummary(Presentation.NORMALIZED, true)).
			append("</normalizedPower>").
			append(SenseGlobals.ln); 
		}
		if ((value = getValue(ValueType.HEARTRATE, false))!= null && value.hasRegistrations()) {
			getMinMax(summary, "hr", value);
		}
		if ((value = getValue(ValueType.SPEED, false))!= null && value.hasRegistrations()) {
			getMinMax(summary, "spd", value);
		}
		if ((value = getValue(ValueType.POWER, false))!= null && value.hasRegistrations()) {
			getMinMax(summary, "pwr", value);
		}
		if ((value = getValue(ValueType.CADENCE, false))!= null && value.hasRegistrations()) {
			getMinMax(summary, "cad", value);
		}
		if ((value = getValue(ValueType.DISTANCE, false))!= null && value.hasRegistrations()) {
			summary.
			append("<dist>").
			append(value.printSummary(Presentation.MAX, true)).
			append("</dist>").
			append(SenseGlobals.ln);
		}
		if ((value = getValue(ValueType.ALTITUDE, false))!= null && value.hasRegistrations()) {
			getMinMax(summary, "alt", value);
		}
		if ((value = getValue(ValueType.TEMPERATURE, false))!= null && value.hasRegistrations()) {
			getMinMax(summary, "temp", value);
		}

		if ((value = getValue(ValueType.ALTITUDE, false))!= null && value.hasRegistrations()) {
			summary.
			append("<climbingelevation>").
			append(value.printSummary(Presentation.DELTAUP, true)).
			append("</climbingelevation>").
			append(SenseGlobals.ln); 
			summary.
			append("<descendingelevation>").
			append(value.printSummary(Presentation.DELTADOWN, true)).
			append("</descendingelevation>").
			append(SenseGlobals.ln); 
		}
		return summary.toString();
	}



}
