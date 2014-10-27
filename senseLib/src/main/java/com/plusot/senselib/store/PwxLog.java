package com.plusot.senselib.store;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;

import com.plusot.common.Globals;
import com.plusot.common.share.LLog;
import com.plusot.common.util.FileUtil;
import com.plusot.common.util.UserInfo;
import com.plusot.javacommon.util.Format;
import com.plusot.javacommon.util.StringUtil;
import com.plusot.javacommon.util.TimeUtil;
import com.plusot.senselib.SenseGlobals;
import com.plusot.senselib.settings.PreferenceKey;
import com.plusot.senselib.values.Laps;
import com.plusot.senselib.values.Value;
import com.plusot.senselib.values.ValueItem;
import com.plusot.senselib.values.ValueType;

public class PwxLog {
	private static final String CLASSTAG = PwxLog.class.getSimpleName();
	private static PwxLog instance = null;

	private static final String SPEC = "ub";
	private static final String EXT = ".pw";
	private static final String ZIPEXT = "x.zip";

	private boolean hasLatLng = false;
	private double lat = 0;
	private double lng = 0;
	private Integer hrm = null;
	private long[] duration = new long[1];
	private Double pow = null;
	private Double cad = null;
	private Double speed = null;
	private Double alt = null;
	private Double dist = null;
	private Double temp = null;
	private long timestamp = 0;

	private PrintWriter writer = null;
	private String logFilename = null;
	private long refTime = 0;
	public String pwxSummary = null;
	public boolean invalidateSummary = false;
	private boolean closeing = false;
	private final String ln;

	private void reset() {
		hasLatLng = false;
		lat = 0;
		lng = 0;
		hrm = null;
		pow = null;
		cad = null;
		speed = null;
		alt = null;
		dist = null;
		temp = null;
		timestamp = 0;
		refTime = 0;
	}

	private PwxLog() {
		ln = SenseGlobals.ln;
	}

	public static void stopInstance(final String caller, boolean writeZip) {
		if (instance != null) synchronized(PwxLog.class) {
			instance.close(caller + ",stopInstance", false, writeZip);
			instance = null;
		} else {
			LLog.d(Globals.TAG, CLASSTAG + ".stopInstance(): no PWX file to close!");
		}
	}

	public static PwxLog getInstance() {
		if (instance != null) return instance;
		if (!Globals.runMode.isRun()) return null;
		LLog.d(Globals.TAG, CLASSTAG + ".getInstance: Creating object");
		synchronized(PwxLog.class) {
			if (instance == null) instance = new PwxLog();
		}
		return instance;
	}

	private void setPow(long time, Double pow) {
		if (pow == null) return;
		checkTime(time);
		this.pow = pow;
	}

	public void setDuration(long time) {
		int lap = Laps.getCurrentLap();
		if (lap >= duration.length) duration = Arrays.copyOf(duration, lap + 1);
		this.duration[lap] = Math.max(duration[lap], time - Value.getSessionTime());
	}

	private void setHrm(long time, Integer hrm) {
		if (hrm == null) return;
		checkTime(time);
		this.hrm = hrm;
	}

	private void setCad(long time, Double cad) {
		if (cad == null) return;
		checkTime(time);
		this.cad = cad;
	}

	private void setLatLng(long time, double[] latlng) {
		if (latlng == null || latlng.length < 2) return;
		this.lat = latlng[0];
		this.lng = latlng[1];
		hasLatLng = true;
		checkTime(time);
	}

	private void setSpeed(long time, Double speed) {
		if (speed == null) return;
		checkTime(time);
		this.speed = speed;
	}

	private void setAlt(long time, Double alt) {
		if (alt == null) return;
		checkTime(time);
		this.alt = alt;
	}

	public void setDist(long time, Double dist) {
		if (dist == null) return;
		checkTime(time);
		this.dist = dist;
	}

	private void setTemp(long time, Double temp) {
		if (temp == null) return;
		checkTime(time);
		this.temp = temp;
	}

	public static String getFileName(String session) {
		return Globals.getDataPath() + SPEC + SenseGlobals.DELIM_DETAIL + session + EXT + ZIPEXT;
	}

	public static File getFile(String session) {
		if (session == null) {
			LLog.d(Globals.TAG, CLASSTAG + ".getFile: No Session time");
			return null;
		}
		String pwxFile = getFileName(session);
		LLog.d(Globals.TAG, CLASSTAG + ".getFile: " + pwxFile);
		File file = new File(pwxFile);
		if (file != null && file.exists()) return file;
		return null;
	}

	private boolean open() {
		if (Globals.runMode.isFinished()) return false;
		boolean isNew = true;

		File file = new File(Globals.getDataPath()); 
		if (!file.isDirectory()) file.mkdirs();

		logFilename = Globals.getDataPath() + SPEC + SenseGlobals.DELIM_DETAIL + Value.getSessionString() + EXT;
		file = new File(logFilename); 
		try {
			isNew = file.createNewFile();
			if (isNew)
				LLog.d(Globals.TAG, CLASSTAG + ".open: Opening new file " + logFilename);
			else
				LLog.d(Globals.TAG, CLASSTAG + ".open: Appending to " + logFilename);
			writer = new PrintWriter(new BufferedWriter(new FileWriter(file, true)));
		} catch (IOException e) {
			LLog.e(Globals.TAG, CLASSTAG + ".open: Error creating file");
			writer = null;
		} 
		return isNew;
	}

	private void _save(String logStr) {
		if (writer != null && writer.checkError()) {
			LLog.d(Globals.TAG, CLASSTAG + ".log: Closing & opening writer due to error.");
			writer.close();
			writer = null;
		}
		if (writer == null) {
			if (open() && writer != null) {

			}
		}
		if (writer == null)	{
			LLog.d(Globals.TAG, CLASSTAG + ".save: Could not write: " + logStr);
		} else {
			//recordsWritten++;
			writer.println(logStr);
			flush(false);
		}
	}
	
	private long timeFlushed = 0;

	private void flush(boolean force) {
		long now = System.currentTimeMillis();
		if (!force && now - timeFlushed < Globals.FLUSHTIME) return;
		if (writer != null) writer.flush();
		timeFlushed = System.currentTimeMillis();
	}

	private String pwxOpen() {
		return "<?xml version=\"1.0\"?>" + ln +
				"<pwx xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " + ln +
				"xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " + ln +
				"xsi:schemaLocation=\"http://www.peaksware.com/PWX/1/0 http://www.peaksware.com/PWX/1/0/pwx.xsd\" " + ln +
				"version=\"1.0\" xmlns=\"http://www.peaksware.com/PWX/1/0\"" + ln +
				"creator=\"" + UserInfo.appNameVersion() + "\">" + ln;
	}


	private String pwxClose() {
		return "</pwx>";
	}

	private String workoutOpen() {

		String tpUser = "ubike";
		tpUser = PreferenceKey.TPUSER.getString();
		return "<workout>" + ln + "<athlete><name>" + tpUser  +"</name></athlete><sportType>Bike</sportType>";
	}

	private String device() {
		PackageInfo info;
		String versionName = "";
		try {
			info = Globals.appContext.getPackageManager().getPackageInfo(Globals.appContext.getPackageName(), 0);
			versionName = " " + info.versionName;
		} catch (NameNotFoundException e) {
		}
		return 
				"<device id=\"" + Globals.TAG + versionName + "\">" + ln +
				"<make>Broxtech</make>" + ln +
				"<model>" + Globals.TAG + versionName + "</model>" + ln +
				"</device>" + ln;

	}

	private String summary() {
		String temp = null;
		if (pwxSummary == null || invalidateSummary) temp = Value.getPwxSummary();
		if (temp != null) pwxSummary = temp;
		StringBuffer sb = new StringBuffer().
				append("<time>").append(TimeUtil.formatTime(Value.getSessionTime(), "yyyy-MM-dd'T'HH:mm:ss")).append("</time>").append(ln).
				append("<summarydata>").append(ln).
				append("<beginning>0</beginning>").append(ln).
				append("<duration>").append(Format.format(0.001 * duration[duration.length - 1], 1)).append("</duration>").append(ln);
		if (pwxSummary != null) sb.append(pwxSummary);
		sb.append("</summarydata>").append(ln);
		for (int i = 0; i < duration.length; i++) {
			sb.append("<segment>").append(ln).
			append("<name>Lap " + (i + 1) + "</name>").append(ln).
			append("<summarydata>").append(ln);
			if (i == 0) {
				sb.append("<beginning>0</beginning>").append(ln);
				sb.append("<duration>").append(Format.format(0.001 * duration[i], 1)).append("</duration>").append(ln);

			} else {
				sb.append("<beginning>").append(Format.format(0.001 * duration[i - 1], 1)).append("</beginning>").append(ln);
				sb.append("<duration>").append(Format.format(0.001 * (duration[i] - duration[i - 1]), 1)).append("</duration>").append(ln);
			}
			sb.append("</summarydata>").append(ln).
			append("</segment>").append(ln);
		}

		return sb.toString();
	}

	private String workoutClose() {
		return "</workout>" + ln;
	}

	private void writeSample() {
		synchronized(this) {
			_save("<sample>");
			_save("<timeoffset>" + Format.format(0.001 * (timestamp - Value.getSessionTime()), 1) + "</timeoffset>");		

			if (hrm != null) _save("<hr>" + hrm + "</hr>");
			if (speed != null) _save("<spd>" + Format.format(speed, 1) + "</spd>");
			if (pow != null) _save("<pwr>" + Format.format(pow, 0) + "</pwr>");
			//if (torq != null) _save("<torq>" + Format.format(torq, 0) + "</torq>");
			if (cad != null) _save("<cad>" + Format.format(cad, 0) + "</cad>");
			if (dist != null) _save("<dist>" + Format.format(dist, 0) + "</dist>");
			if (hasLatLng) {
				_save("<lat>" + Format.format(lat, 7) + "</lat>");			
				_save("<lon>" + Format.format(lng, 7) + "</lon>");
				//hasLatLng = false;
			}
			if (alt != null) _save("<alt>" + Format.format(alt, 1) + "</alt>");
			if (temp != null) _save("<temp>" + Format.format(temp, 0) + "</temp>");
			_save("<time>" + TimeUtil.formatTime(timestamp, "yyyy-MM-dd'T'HH:mm:ss'Z'") + "</time>");
			_save("</sample>");
			flush(true);
		}
	}

	private void checkTime(long time) {
		timestamp = time;
		if (refTime == 0) refTime = time;
		if (!closeing && time - refTime >= 1000) { // && hasLatLng) {
			writeSample();
			refTime = time;
		}	
	}

	public void close(final String caller,  boolean fullClose, boolean writeZip) {
		try {
			synchronized(this) {
				closeing = true;
				LLog.d(Globals.TAG, CLASSTAG + ": File closed by " + caller); 
				if (writer == null) return;
				writer.close();
				writer = null;

				if (writeZip) {
					StringBuffer start = new StringBuffer();
					start.append(pwxOpen());
					start.append(workoutOpen());
					start.append(device());
					start.append(summary());

					StringBuffer end = new StringBuffer();
					end.append(workoutClose());
					end.append(pwxClose());
					FileUtil.toGZip(start.toString(), end.toString(), new File(logFilename), new File(logFilename + ZIPEXT));
				}
				logFilename = null;
				reset();
			}
		} finally {
			if (fullClose) pwxSummary = null;
			closeing = false;
		}
	}

	public void set(ValueType valueType, ValueItem item) {
		if (!Globals.runMode.isRun()) return;
		invalidateSummary = true;
		long timeStamp = item.getTimeStamp();
		Object value = item.getValue();
		try{ 
			switch (valueType) {
			case LOCATION: setLatLng(timeStamp, (double[])value); break;
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
