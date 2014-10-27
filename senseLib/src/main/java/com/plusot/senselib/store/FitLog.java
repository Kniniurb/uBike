package com.plusot.senselib.store;

import java.io.File;
import java.util.Date;

import com.garmin.fit.Activity;
import com.garmin.fit.ActivityMesg;
import com.garmin.fit.DateTime;
import com.garmin.fit.DeviceInfoMesg;
import com.garmin.fit.DeviceType;
import com.garmin.fit.Event;
import com.garmin.fit.EventMesg;
import com.garmin.fit.EventType;
import com.garmin.fit.FileCreatorMesg;
import com.garmin.fit.FileEncoder;
import com.garmin.fit.FileIdMesg;
import com.garmin.fit.Fit;
import com.garmin.fit.FitRuntimeException;
import com.garmin.fit.GarminProduct;
import com.garmin.fit.LapMesg;
import com.garmin.fit.LapTrigger;
import com.garmin.fit.LengthMesg;
import com.garmin.fit.LengthType;
import com.garmin.fit.Manufacturer;
import com.garmin.fit.RecordMesg;
import com.garmin.fit.SessionMesg;
import com.garmin.fit.SessionTrigger;
import com.garmin.fit.Sport;
import com.garmin.fit.SubSport;
import com.garmin.fit.TimerTrigger;
import com.plusot.common.Globals;
import com.plusot.common.share.LLog;
import com.plusot.javacommon.util.TimeUtil;
import com.plusot.senselib.SenseGlobals;
import com.plusot.senselib.settings.PreferenceKey;
import com.plusot.senselib.values.Value;
import com.plusot.senselib.values.ValueItem;
import com.plusot.senselib.values.ValueType;

public class FitLog {
	private static final String CLASSTAG = FitLog.class.getSimpleName();
	public static final String SPEC = "ub";
	public static final String EXT = ".fit";
	private static final long SEMICIRCLE_DEVIDER = 1L << 31;
	private static FitLog instance = null;

	private FileEncoder encoder = null;
	private RecordMesg recordMesg = null;
	private DateTime startTime = new DateTime(new Date());
	private int startLat = 0;
	private int startLong = 0;
	private int endLat = 0;
	private int endLong = 0;
	private int swcLat = Integer.MAX_VALUE; //South West Corner
	private int swcLong = Integer.MAX_VALUE;
	private int necLat = Integer.MIN_VALUE; // North East Corner
	private int necLong = Integer.MIN_VALUE;
	private float distance = 0;
	private long totalCycles = 0;
	private long lastCyclesTime = 0;
	private float maxSpeed = 0;
	private short maxHeartRate = 0;
	private double heartRateTime = 0;
	private long lastHeartRateTimestamp = 0;
	private long firstHeartRateTimestamp = 0;
	private short lastHeartRate = 0;
	private short maxCadence = 0;
	private double cadenceTime = 0;
	private long lastCadenceTimestamp = 0;
	private long firstCadenceTimestamp = 0;
	private short lastCadence = 0;
	private int localNum = -1;
	private int prevNumFields = 0;
	private String fileName = null;
	private int recordsWritten = 0;
	private long lastTimestamp = 0;
	private FitStyle style = FitStyle.DOCS;
	private float prevAlt = 0;
	private float altDescent = 0;
	private float altAscent = 0;

	private enum FitStyle {
		GARMIN,
		DOCS;
	}

	private FitLog() {
	}

	public static FitLog getInstance() {
		if (instance != null) return instance;
		if (!Globals.runMode.isRun()) return null;
		synchronized(FitLog.class) {
			if (instance == null) instance = new FitLog();
		}
		return instance;
	}

	public static void stopInstance() {
		if (instance != null) synchronized(FitLog.class) {
			if (instance != null) instance.close();
			instance = null;
		}
	}

	private void init() {
		recordMesg = null;
		startTime = new DateTime(new Date());
		startLat = 0;
		startLong = 0;
		endLat = 0;
		endLong = 0;
		swcLat = Integer.MAX_VALUE; //South West Corner
		swcLong = Integer.MAX_VALUE;
		necLat = Integer.MIN_VALUE; // North East Corner
		necLong = Integer.MIN_VALUE;
		distance = 0;
		totalCycles = 0;
		lastCyclesTime = 0;
		maxSpeed = 0;
		maxHeartRate = 0;
		heartRateTime = 0;
		lastHeartRateTimestamp = 0;
		firstHeartRateTimestamp = 0;
		lastHeartRate = 0;
		maxCadence = 0;
		cadenceTime = 0;
		lastCadenceTimestamp = 0;
		firstCadenceTimestamp = 0;
		lastCadence = 0;
		localNum = -1;
		prevNumFields = 0;
		fileName = null;
		recordsWritten = 0;
		lastTimestamp = 0;
		prevAlt = 0;
		altDescent = 0;
		altAscent = 0;

	}

	private boolean open(long timestamp) {
		if (encoder != null) return true;
		if (!Globals.runMode.isRun()) return false;
		init();
		File file = new File(Globals.getDataPath()); 
		if (!file.isDirectory()) file.mkdirs();
		long sessionTime = Value.getSessionTime();
		long now = System.currentTimeMillis();
		if (Math.abs(sessionTime - now) < 20000) {
			now = sessionTime;
		}
		fileName = Globals.getDataPath() + SPEC + SenseGlobals.DELIM_DETAIL + TimeUtil.formatTime(now, "yyyyMMdd-HHmmss") + EXT;
		synchronized(this) {
			file = new File(fileName); 
			try {
				encoder = new FileEncoder(file);
				prepare(timestamp);
			} catch (FitRuntimeException e) {
				LLog.e(Globals.TAG, CLASSTAG + ": Error opening file " + fileName);
				return false;
			}
		}
		return true;
	}

	private int getLocalNum(boolean inc) {
		if (inc) {
			localNum++;		
			localNum %= Fit.MAX_LOCAL_MESGS;
		}return localNum;
	}

	private int getLocalNum() {
		return getLocalNum(true);
	}

	private void prepare(long timestamp) {
		switch (style) {
		case GARMIN: prepareGarminStyle(timestamp);
		default: prepareDocStyle(timestamp);
		}
	}

	private void prepareGarminStyle(long timestamp) {
		//Mesg mesg;
		//MesgDefinition mesgDef = null;
		recordsWritten = 0;

		startTime = new DateTime(new Date(timestamp));
		long serial = 3826433144L;
		int product = 1169;
		int softwareVersion = 240;
		//short hardwareVersion = 1;

		FileIdMesg fileIdMesg = new FileIdMesg();

		fileIdMesg.setLocalNum(getLocalNum());
		fileIdMesg.setManufacturer(Manufacturer.GARMIN);
		//fileIdMesg.setProduct(product);
		fileIdMesg.setGarminProduct(GarminProduct.EDGE800);

		fileIdMesg.setSerialNumber(serial);
		fileIdMesg.setType(com.garmin.fit.File.ACTIVITY);
		fileIdMesg.setTimeCreated(startTime);

		encoder.write(fileIdMesg);

		FileCreatorMesg fileCreatorMesg = new FileCreatorMesg();

		fileCreatorMesg.setSoftwareVersion(softwareVersion);
		//fileCreatorMesg.setHardwareVersion(hardwareVersion);

		encoder.write(fileCreatorMesg);

		EventMesg msg = new EventMesg();

		msg.setLocalNum(getLocalNum());
		msg.setTimestamp(startTime);
		msg.setTimerTrigger(TimerTrigger.MANUAL);
		msg.setEvent(Event.TIMER);
		msg.setData((long) 0);
		msg.setEventGroup((short) 0);
		msg.setEventType(EventType.START);
		encoder.write(msg);

		DeviceInfoMesg dev = new DeviceInfoMesg();
		dev.setLocalNum(getLocalNum());
		dev.setTimestamp(startTime);
		dev.setSerialNumber(serial);
		//dev.setCumOperatingTime(cumOperatingTime);
		dev.setManufacturer(Manufacturer.DYNASTREAM_OEM);
		dev.setProduct(product);
		dev.setSoftwareVersion(0.01f * softwareVersion);
		short deviceIndex = 0;
		dev.setDeviceIndex(deviceIndex++);
		dev.setDeviceType(DeviceType.ANTFS);
		encoder.write(dev);
		dev = new DeviceInfoMesg();
		dev.setTimestamp(startTime);
		dev.setDeviceIndex(deviceIndex++);
		dev.setDeviceType(DeviceType.STRIDE_SPEED_DISTANCE); //.CONTROL); //.ENVIRONMENT_SENSOR_LEGACY);
		dev.setSerialNumber(24671867L);
		encoder.write(dev);
		dev = new DeviceInfoMesg();
		dev.setTimestamp(startTime);
		dev.setDeviceIndex(deviceIndex++);
		dev.setDeviceType(DeviceType.HEART_RATE);
		dev.setSerialNumber(24671867L);
		encoder.write(dev);
		dev = new DeviceInfoMesg();
		dev.setTimestamp(startTime);
		dev.setDeviceIndex(deviceIndex++);
		dev.setDeviceType(DeviceType.BIKE_SPEED_CADENCE);
		dev.setSerialNumber(24747362L);
		encoder.write(dev);
		dev = new DeviceInfoMesg();
		dev.setTimestamp(startTime);
		dev.setDeviceIndex(deviceIndex++);
		dev.setDeviceType(DeviceType.BIKE_POWER);
		dev.setSerialNumber(512L);
		encoder.write(dev);

		//mesg = Factory.createMesg(MesgNum.FILE_ID);

	}

	private void prepareDocStyle(long timestamp) {
		//Mesg mesg;
		//MesgDefinition mesgDef = null;
		recordsWritten = 0;

		startTime = new DateTime(new Date(timestamp));
		long serial = 3826433144L;
		int product = 1169;
		//int softwareVersion = 240;
		//short hardwareVersion = 1;

		FileIdMesg fileIdMesg = new FileIdMesg();

		fileIdMesg.setLocalNum(getLocalNum());
		fileIdMesg.setManufacturer(Manufacturer.DYNASTREAM);
		fileIdMesg.setProduct(product);
		fileIdMesg.setSerialNumber(serial);
		fileIdMesg.setType(com.garmin.fit.File.ACTIVITY);
		fileIdMesg.setTimeCreated(startTime);

		encoder.write(fileIdMesg);

	}

	private void trailer(long timestamp) {
		switch (style) {
		case GARMIN: trailerGarminStyle(timestamp);
		default: trailerDocStyle(timestamp);
		}
	}	

	private void trailerGarminStyle(long timestamp) {

		//Mesg mesg;
		//MesgDefinition mesgDef = null;
		if (encoder == null) return;

		EventMesg msg = new EventMesg();

		DateTime now = new DateTime(new Date(timestamp));
		float timeElapsed = now.getTimestamp() - startTime.getTimestamp();

		msg.setLocalNum(getLocalNum());
		msg.setTimestamp(now);
		msg.setTimerTrigger(TimerTrigger.MANUAL);
		msg.setEvent(Event.TIMER);
		msg.setEventGroup((short) 0);
		msg.setEventType(EventType.STOP_ALL);
		encoder.write(msg);

		/*
		msg.setTimestamp(new DateTime(new Date()));
		//msg.setTimerTrigger(TimerTrigger.MANUAL);
		msg.setEvent(Event.POWER_DOWN);
		msg.setEventGroup((short) 0);
		msg.setEventType(EventType.STOP_ALL);
		encoder.write(msg);

		msg.setTimestamp(new DateTime(new Date()));
		//msg.setTimerTrigger(TimerTrigger.MANUAL);
		msg.setEvent(Event.POWER_UP);
		msg.setEventGroup((short) 0);
		msg.setEventType(EventType.STOP_ALL);
		encoder.write(msg);
		 */
		msg = new EventMesg();
		msg.setLocalNum(getLocalNum());
		msg.setTimestamp(now);
		//msg.setTimerTrigger(TimerTrigger.MANUAL);
		msg.setEvent(Event.SESSION);
		msg.setEventGroup((short) 1);
		msg.setEventType(EventType.STOP_DISABLE_ALL);
		encoder.write(msg);

		LapMesg lap = new LapMesg();
		lap.setLocalNum(getLocalNum());
		lap.setTimestamp(now);
		lap.setStartTime(startTime);
		lap.setStartPositionLat(startLat);
		lap.setStartPositionLong(startLong);
		lap.setEndPositionLat(endLat);
		lap.setEndPositionLong(endLong);
		lap.setTotalElapsedTime(timeElapsed);
		lap.setTotalTimerTime(timeElapsed);
		lap.setTotalDistance(distance);
		lap.setTotalCycles(totalCycles);
		lap.setTotalCalories(0);
		lap.setTotalFatCalories(0);
		lap.setAvgSpeed(distance / timeElapsed);
		lap.setMaxSpeed(maxSpeed);
		lap.setMessageIndex(0);
		//session.setAvgPower(avgPower);
		//session.setMaxPower(maxPower);
		lap.setTotalAscent(100);
		lap.setTotalDescent(100);
		//lap.setNormalizedPower(normalizedPower);
		//lap.setLeftRightBalance(leftRightBalance);
		lap.setEvent(Event.LAP);
		lap.setEventType(EventType.STOP);
		lap.setAvgHeartRate((short) (heartRateTime / (lastHeartRateTimestamp - firstHeartRateTimestamp)));
		lap.setMaxHeartRate(maxHeartRate);
		lap.setAvgCadence((short) (cadenceTime / (lastCadenceTimestamp - firstCadenceTimestamp)));
		lap.setMaxCadence(maxCadence);
		//lap.setIntensity(Intensity.ACTIVE);
		lap.setLapTrigger(LapTrigger.SESSION_END);
		lap.setSport(Sport.CYCLING);
		//lap.setEventGroup((short)0);
		encoder.write(lap);

		SessionMesg session = new SessionMesg();
		session.setLocalNum(getLocalNum());
		session.setTimestamp(now);
		session.setStartTime(startTime);
		session.setStartPositionLat(startLat);
		session.setStartPositionLong(startLong);
		session.setTotalElapsedTime(timeElapsed);
		session.setTotalDistance(distance);
		session.setTotalCycles(totalCycles);
		session.setNecLat(necLat);
		session.setNecLong(necLong);
		session.setSwcLat(swcLat);
		session.setSwcLong(swcLong);
		session.setMessageIndex(0);
		session.setTotalCalories(0);
		session.setTotalFatCalories(0);
		session.setAvgSpeed(distance / timeElapsed);
		session.setMaxSpeed(maxSpeed);
		//session.setAvgPower(avgPower);
		//session.setMaxPower(maxPower);
		session.setTotalAscent(100);
		session.setTotalDescent(100);
		session.setFirstLapIndex(0);
		session.setNumLaps(1);
		//session.setNormalizedPower(normalizedPower);
		//session.setTrainingStressScore(trainingStressScore);
		//session.setTrainingStressScore(trainingStressScore);
		//session.setIntensityFactor(intensityFactor);
		//session.setLeftRightBalance(leftRightBalance);
		session.setEvent(Event.LAP);
		session.setEventType(EventType.STOP);
		session.setSport(Sport.CYCLING);
		session.setSubSport(SubSport.GENERIC);
		session.setAvgHeartRate((short) (heartRateTime / (lastHeartRateTimestamp - firstHeartRateTimestamp)));
		session.setMaxHeartRate(maxHeartRate);
		session.setAvgCadence((short) (cadenceTime / (lastCadenceTimestamp - firstCadenceTimestamp)));
		session.setMaxCadence(maxCadence);
		//session.setEventGroup(eventGroup);
		session.setTrigger(SessionTrigger.ACTIVITY_END);

		encoder.write(session);

		ActivityMesg activity = new ActivityMesg();
		activity.setLocalNum(getLocalNum());
		activity.setTimestamp(now);
		activity.setTotalTimerTime((float)(now.getTimestamp() - startTime.getTimestamp()));
		activity.setNumSessions(1);
		activity.setType(Activity.MANUAL);
		activity.setEvent(Event.ACTIVITY);
		activity.setEventType(EventType.STOP);
		encoder.write(activity);

	}

	private void trailerDocStyle(long timestamp) {

		//Mesg mesg;
		//MesgDefinition mesgDef = null;
		if (encoder == null) return;
		DateTime now = new DateTime(new Date(timestamp));
		float timeElapsed = now.getTimestamp() - startTime.getTimestamp();


		/*EventMesg msg = new EventMesg();


		msg.setLocalNum(getLocalNum());
		msg.setTimestamp(now);
		msg.setTimerTrigger(TimerTrigger.MANUAL);
		msg.setEvent(Event.TIMER);
		msg.setEventGroup((short) 0);
		msg.setEventType(EventType.STOP_ALL);
		encoder.write(msg);
		 */
		/*
		msg.setTimestamp(new DateTime(new Date()));
		//msg.setTimerTrigger(TimerTrigger.MANUAL);
		msg.setEvent(Event.POWER_DOWN);
		msg.setEventGroup((short) 0);
		msg.setEventType(EventType.STOP_ALL);
		encoder.write(msg);

		msg.setTimestamp(new DateTime(new Date()));
		//msg.setTimerTrigger(TimerTrigger.MANUAL);
		msg.setEvent(Event.POWER_UP);
		msg.setEventGroup((short) 0);
		msg.setEventType(EventType.STOP_ALL);
		encoder.write(msg);
		 */
		/*msg = new EventMesg();
		msg.setLocalNum(getLocalNum());
		msg.setTimestamp(now);
		//msg.setTimerTrigger(TimerTrigger.MANUAL);
		msg.setEvent(Event.SESSION);
		msg.setEventGroup((short) 1);
		msg.setEventType(EventType.STOP_DISABLE_ALL);
		encoder.write(msg);
		 */
		LengthMesg length = new LengthMesg();
		length.setTimestamp(now);
		length.setEvent(Event.LAP);
		length.setEventType(EventType.STOP);
		length.setLengthType(LengthType.ACTIVE);
		encoder.write(length);

		LapMesg lap = new LapMesg();
		lap.setLocalNum(getLocalNum());
		lap.setTimestamp(now);
		lap.setStartTime(startTime);
		lap.setStartPositionLat(startLat);
		lap.setStartPositionLong(startLong);
		lap.setEndPositionLat(endLat);
		lap.setEndPositionLong(endLong);
		lap.setTotalElapsedTime(timeElapsed);
		lap.setTotalTimerTime(timeElapsed);
		lap.setTotalDistance(distance);
		lap.setTotalCycles(totalCycles);
		lap.setTotalCalories(0);
		lap.setTotalFatCalories(0);
		lap.setAvgSpeed(distance / timeElapsed);
		lap.setMaxSpeed(maxSpeed);
		lap.setMessageIndex(0);
		//session.setAvgPower(avgPower);
		//session.setMaxPower(maxPower);
		lap.setTotalAscent((int)altAscent);
		lap.setTotalDescent((int)altDescent);
		//lap.setNormalizedPower(normalizedPower);
		//lap.setLeftRightBalance(leftRightBalance);
		lap.setEvent(Event.LAP);
		lap.setEventType(EventType.STOP);
		lap.setAvgHeartRate((short) (heartRateTime / (lastHeartRateTimestamp - firstHeartRateTimestamp)));
		lap.setMaxHeartRate(maxHeartRate);
		lap.setAvgCadence((short) (cadenceTime / (lastCadenceTimestamp - firstCadenceTimestamp)));
		lap.setMaxCadence(maxCadence);
		//lap.setIntensity(Intensity.ACTIVE);
		lap.setLapTrigger(LapTrigger.SESSION_END);
		lap.setSport(Sport.CYCLING);
		//lap.setEventGroup((short)0);
		encoder.write(lap);

		SessionMesg session = new SessionMesg();
		session.setLocalNum(getLocalNum());
		session.setTimestamp(now);
		session.setStartTime(startTime);
		session.setStartPositionLat(startLat);
		session.setStartPositionLong(startLong);
		session.setTotalElapsedTime(timeElapsed);
		session.setTotalDistance(distance);
		session.setTotalCycles(totalCycles);
		session.setNecLat(necLat);
		session.setNecLong(necLong);
		session.setSwcLat(swcLat);
		session.setSwcLong(swcLong);
		session.setMessageIndex(0);
		session.setTotalCalories(0);
		session.setTotalFatCalories(0);
		session.setAvgSpeed(distance / timeElapsed);
		session.setMaxSpeed(maxSpeed);
		//session.setAvgPower(avgPower);
		//session.setMaxPower(maxPower);
		session.setTotalAscent((int)altAscent);
		session.setTotalDescent((int)altDescent);
		session.setFirstLapIndex(0);
		session.setNumLaps(1);
		//session.setNormalizedPower(normalizedPower);
		//session.setTrainingStressScore(trainingStressScore);
		//session.setTrainingStressScore(trainingStressScore);
		//session.setIntensityFactor(intensityFactor);
		//session.setLeftRightBalance(leftRightBalance);
		session.setEvent(Event.LAP);
		session.setEventType(EventType.STOP);
		session.setSport(Sport.CYCLING);
		session.setSubSport(SubSport.GENERIC);
		session.setAvgHeartRate((short) (heartRateTime / (lastHeartRateTimestamp - firstHeartRateTimestamp)));
		session.setMaxHeartRate(maxHeartRate);
		session.setAvgCadence((short) (cadenceTime / (lastCadenceTimestamp - firstCadenceTimestamp)));
		session.setMaxCadence(maxCadence);
		//session.setEventGroup(eventGroup);
		session.setTrigger(SessionTrigger.ACTIVITY_END);

		encoder.write(session);

		ActivityMesg activity = new ActivityMesg();
		activity.setLocalNum(getLocalNum());
		activity.setTimestamp(now);
		activity.setTotalTimerTime((float)(now.getTimestamp() - startTime.getTimestamp()));
		activity.setNumSessions(1);
		activity.setType(Activity.MANUAL);
		activity.setEvent(Event.ACTIVITY);
		activity.setEventType(EventType.STOP);
		encoder.write(activity);

	}

	public void store(ValueType valueType, ValueItem item) {
		if (!open(item.getTimeStamp()) || !Globals.runMode.isRun()) {
			if (!Globals.runMode.isRun()) LLog.d(Globals.TAG, CLASSTAG + ": Could not write data as application is finishing (" + valueType.getLabel(Globals.appContext) + ", " + item.getStringValue() +")");
			return;
		}
		DateTime dateTime = new DateTime(new Date(item.getTimeStamp())); //new Date());
		lastTimestamp = item.getTimeStamp();
		if (recordMesg == null) {
			LLog.d(Globals.TAG, CLASSTAG + ": Creating new recordMsg");
			prevNumFields = -1;
			recordMesg = new RecordMesg();
			recordMesg.setTimestamp(dateTime);
		}
		//LLog.d(Globals.TAG, CLASSTAG + ".store: TimeStamp = " + dateTime.getTimestamp() + " " + recordMesg.getTimestamp().getTimestamp());
		if (dateTime.getTimestamp() - recordMesg.getTimestamp().getTimestamp() > 1) {
			if (recordMesg.getNumFields() < 4) {
				recordMesg.setTimestamp(dateTime);
			} else {
				if (recordMesg.getNumFields() != prevNumFields) {
					recordMesg.setLocalNum(getLocalNum());
					prevNumFields = recordMesg.getNumFields();
				} else
					recordMesg.setLocalNum(getLocalNum(false));
				try {
					synchronized(this) {
						if (encoder != null) encoder.write(recordMesg);
					}
				} catch (FitRuntimeException e) {
					if (encoder != null) try {
						encoder.close();
					} catch (Exception e2) {
						
					}
					encoder = null;
					LLog.e(Globals.TAG, CLASSTAG + ".store: Exception trying to write data", e);
				}
				recordsWritten++;
				//LLog.d(Globals.TAG, CLASSTAG + ": Writing data to encoder with " + recordMesg.getNumFields() + " fields and localNum " + recordMesg.getLocalNum());
				recordMesg.setTimestamp(dateTime);
			}
		}

		switch(valueType) {
		case LOCATION:
			double[] doubles = item.getDoubleValues(false);
			endLat = (int)(doubles[0] / 180.0 * SEMICIRCLE_DEVIDER);
			endLong = (int)(doubles[1] / 180.0 * SEMICIRCLE_DEVIDER);
			if (startLong == 0) startLong = endLong;
			if (startLat == 0) startLat = endLat;
			if (endLat > necLat) necLat = endLat;
			if (endLong > necLong) necLong = endLong;
			if (endLat < swcLat) swcLat = endLat;
			if (endLong < swcLong) swcLong = endLong;
			recordMesg.setPositionLat(endLat);
			recordMesg.setPositionLong(endLong);
			break;
		case ALTITUDE:
			float alt = item.getFloatValue();
			recordMesg.setAltitude(alt);
			float div =  Math.abs(alt - prevAlt);
			if (div > 3.0f) {
				if (alt < prevAlt) altDescent +=  div; else altAscent += div;
				prevAlt = alt;
			}
			break;
		case HEARTRATE:
			short heartRate = item.getShortValue();
			if (heartRate > maxHeartRate) maxHeartRate = heartRate;
			if (firstHeartRateTimestamp == 0) firstHeartRateTimestamp = item.getTimeStamp() / 1000;
			if (lastHeartRateTimestamp > 0 ) {
				heartRateTime += 0.5 * (heartRate + lastHeartRate) * (item.getTimeStamp() / 1000 - lastHeartRateTimestamp);
			}
			recordMesg.setHeartRate(heartRate);

			lastHeartRateTimestamp = item.getTimeStamp() / 1000;
			lastHeartRate = heartRate;
			break;
//		case PULSEWIDTH:
//			short pulseWidth = item.getShortValue();
//			recordMesg.setCycles(pulseWidth);
//			break;
		case SPEED:
			recordMesg.setSpeed(item.getFloatValue());
			break;
		case CADENCE:
			short cadence = item.getShortValue();
			if (cadence > maxCadence) maxCadence = cadence;
			if (firstCadenceTimestamp == 0) firstHeartRateTimestamp = item.getTimeStamp() / 1000;
			if (lastCadenceTimestamp > 0 ) {
				cadenceTime += 0.5 * (cadence + lastCadence) * (item.getTimeStamp() / 1000 - lastCadenceTimestamp);
			}
			recordMesg.setCadence(cadence);
			lastCadenceTimestamp = item.getTimeStamp() / 1000;
			lastCadence = cadence;
			break;
		case DISTANCE:
			distance = item.getFloatValue();
			recordMesg.setDistance(distance);
			break;
		case POWER:
			recordMesg.setPower(item.getIntValue());
			break;
		case SLOPE:
			//recordMesg.setGrade(100f * item.getFloatValue());
			break;
		case POWER_PEDAL:
			recordMesg.setLeftRightBalance(Double.valueOf(item.getDoubleValue() * 100.0).shortValue());
			break;
		case WHEEL_REVS:
			if (lastCyclesTime > 0) {
				totalCycles += (long)(item.getDoubleValue() * (item.getTimeStamp() / 1000 - lastCyclesTime) / 60.0);
			}	
			short wheelCadence = item.getShortValue();
			recordMesg.setCycles(wheelCadence);
			recordMesg.setTotalCycles(totalCycles);
			lastCyclesTime = item.getTimeStamp() / 1000;

			break;
		default: 
			//LLog.d(Globals.TAG, CLASSTAG + ": Field " + valueType.getLabel(Globals.appContext) + " not used in Fit file yet");
			break;
		}
	}

	public void open() {
		//LLog.d(Globals.TAG, CLASSTAG + ".allowOpen");
		if (encoder != null) close();
		init();
	}

//	public String getFileName() {
//		return fileName;
//	}
//	
//	public static String getFileName(String sessionTime) {
//		return Globals.DATA_PATH + SPEC + Globals.DELIM_DETAIL + sessionTime + EXT;
//	}

	public void close() {
		if (encoder == null) return;
		LLog.d(Globals.TAG, CLASSTAG + ".close called");
		if (lastTimestamp == 0) {
			if (encoder != null) encoder.close();
			encoder = null;
			new File(fileName).delete();
			fileName = null;
			return;
		}
		PreferenceKey.addFitFileList(fileName);
		try {
			synchronized(this) {
				trailer(lastTimestamp + 1);
				if (encoder != null) encoder.close();
				encoder = null;
			}
			
		} catch(FitRuntimeException e) {
			LLog.e(Globals.TAG, CLASSTAG + ": Error closing encoder.", e);
			return;
		} finally {
			encoder = null;
		}
		return;
	}

	public int getRecordsWriten() {
		return recordsWritten;
	}
}
