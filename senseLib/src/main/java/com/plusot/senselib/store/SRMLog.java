package com.plusot.senselib.store;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Locale;

import com.plusot.common.Globals;
import com.plusot.common.share.LLog;
import com.plusot.javacommon.util.StringUtil;
import com.plusot.javacommon.util.TimeUtil;
import com.plusot.senselib.SenseGlobals;
import com.plusot.senselib.settings.PreferenceKey;
import com.plusot.senselib.values.Value;
import com.plusot.senselib.values.ValueItem;
import com.plusot.senselib.values.ValueType;

public class SRMLog {
	private static final String CLASSTAG = SRMLog.class.getSimpleName();
	private static SRMLog instance = null;

	private static final String SPEC = "srm";
	private static final String EXT = ".txt";

	private double pow = 0;
	private int hrm = 0;
	private double cad = 0;
	private double speed = 0;
	private double alt = 0;
	private double dist = 0;
	private double temp = 0;
	private double offset = 0;
	private double slope = 0;
	private int id = 0;

	private String serialnr = "00112244";

	private PrintWriter writer = null;
	private String logFilename = null;
	//private int recordsWritten = 0;
	private long refTime = 0;

	//	private SRMLog() {
	//		AppPreferences prefs = AppPreferences.getInstance();
	//		String ref = Globals.DATA_PATH + SPEC + Globals.DELIM_DETAIL + TimeUtil.formatTime(System.currentTimeMillis(), "yyyyMMdd");
	//		String prefFilename = prefs.getSrmFileName();
	//		if (prefFilename.startsWith(ref)) {
	//			id = prefs.getSrmLastId();
	//		}
	//	}


	public static void stopInstance(final String caller) {
		if (instance != null) synchronized(SRMLog.class) {
			instance.close(caller + ",stopInstance");
			instance = null;

		} else {
			LLog.d(Globals.TAG, CLASSTAG + ".stopInstance(): no SRM file to close!");
		}
	}

	public static SRMLog getInstance() {
		if (instance != null) return instance;
		if (!Globals.runMode.isRun()) return null;
		LLog.d(Globals.TAG, CLASSTAG + ".getInstance: Creating object");
		synchronized(SRMLog.class) {
			if (instance == null) instance = new SRMLog();
		}
		return instance;
	}

	public void setPow(long time, Double pow) {
		if (pow == null) return;
		checkTime(time);
		this.pow = pow;
	}

	public void setHrm(long time, Integer hrm) {
		if (hrm == null) return;
		checkTime(time);
		this.hrm = hrm;
	}

	public void setCad(long time, Double cad) {
		if (cad == null) return;
		checkTime(time);
		this.cad = cad;
	}

	public void setSpeed(long time, Double speed) {
		if (speed == null) return;
		checkTime(time);
		this.speed = speed * 3.6;
	}

	public void setAlt(long time, Double alt) {
		if (alt == null) return;
		checkTime(time);
		this.alt = alt;
	}

	public void setDist(long time, Double dist) {
		if (dist == null) return;
		checkTime(time);
		this.dist = dist;
	}

	public void setTemp(long time, Double temp) {
		if (temp == null) return;
		checkTime(time);
		this.temp = temp;
	}

	public void setOffset(long time, double offset) {
		checkTime(time);
		this.offset = offset;
	}

	public void setSlope(long time, double slope) {
		checkTime(time);
		this.slope = slope;
	}

	public double getOffset() {
		return offset;
	}

	public double getSlope() {
		return slope;
	}

	public void setSerialnr(String serialnr) {
		this.serialnr = serialnr;
	}

	public static String getFileName(String session) {
		return Globals.getDataPath() + SPEC + SenseGlobals.DELIM_DETAIL + session  + EXT;
	}

	private boolean open() {
		if (Globals.runMode.isFinished()) return false;
		boolean isNew = true;

		File file = new File(Globals.getDataPath()); 
		if (!file.isDirectory()) file.mkdirs();

		logFilename = getFileName(Value.getSessionString());
		file = new File(logFilename); 
		try {
			isNew = file.createNewFile();
			if (isNew) {
				LLog.d(Globals.TAG, CLASSTAG + ".open: Opening new file " + logFilename);
				id = 0;
			} else {
				LLog.d(Globals.TAG, CLASSTAG + ".open: Appending to " + logFilename);
				id = PreferenceKey.SRMLASTID.getInt();
			}

			writer = new PrintWriter(new BufferedWriter(new FileWriter(file, true)));
		} catch (IOException e) {
			LLog.e(Globals.TAG, CLASSTAG + ".open: Error creating file");
			writer = null;
		} 
		return isNew;
	}

	private void save(String logStr) {
		if (writer != null && writer.checkError()) {
			LLog.d(Globals.TAG, CLASSTAG + ".log: Closing & opening writer due to error.");
			writer.close();
			writer = null;
		}
		if (writer == null) {
			if (open() && writer != null) saveCaption();
		}
		if (writer == null)	{
			LLog.d(Globals.TAG, CLASSTAG + ".save: Could not write: " + logStr);
		} else {
			//recordsWritten++;
			writer.println(logStr);
			flush();
		}
	}
	
	private long timeFlushed = 0;

	private void flush() {
		long now = System.currentTimeMillis();
		if (now - timeFlushed < Globals.FLUSHTIME) return;
		if (writer != null) writer.flush();
		timeFlushed = System.currentTimeMillis();
	}

	private  void saveCaption() {
		if (writer != null) {
			Calendar cal = Calendar.getInstance();
			writer.println(String.format(Locale.US, "%10.0f%10.0f%10d%10s%10d", offset, slope, SenseGlobals.wheelCirc, this.serialnr, id++));
			writer.println(String.format(Locale.US, "%10d%10d%10d%10s", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), "EVV"));
			writer.println(String.format(Locale.US, "%10d%10.2f%10.2fkm 24˚C", 0, 1.0f, dist));
		}
	}


	private void save() {
		String line = String.format(Locale.US, "%5.0f%5d%5.0f%6.1f%6.0f%6.1f", pow, hrm, cad, speed, alt, temp) + "  " + TimeUtil.formatTime(this.refTime, "HH:mm:ss") + String.format(Locale.US, "%7d", id++);
		save(line);
	}

	private void checkTime(long time) {
		if (refTime == 0) refTime = time;
		if (time - refTime >= 1000) {
			save();
			refTime = time;
		}	
	}

	public void close(final String caller) {
		synchronized(this) {
			if (writer == null) return;
			PreferenceKey.SRMLASTID.set(id);

			LLog.d(Globals.TAG, CLASSTAG + ": File closed by " + caller); 

			writer.close();
			writer = null;
			logFilename = null;
		}
	}

	public void set(ValueType valueType, ValueItem item) {
		long timeStamp = item.getTimeStamp();
		Object value = item.getValue();
		try { 
			switch (valueType) {
			case POWER: setPow(timeStamp, (Double)value); break;
			case CADENCE: setCad(timeStamp, (Double)value); break;
			case DISTANCE: setDist(timeStamp, (Double)value); break;
			case SPEED: setSpeed(timeStamp, (Double)value); break;
			case TEMPERATURE: setTemp(timeStamp, (Double)value); break;
			case ALTITUDE: setAlt(timeStamp, (Double)value); break;
			case HEARTRATE: setHrm(timeStamp, (Integer)value); break;
			default: break;
			} 
		} catch (ClassCastException e) {
			LLog.e(Globals.TAG, CLASSTAG + ": Could not cast " + value.getClass().getName() + ", " + StringUtil.toStringXL(value, ",") + " to " + valueType.toString(), e);
		}
	}



}
