package com.plusot.senselib.store;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import com.plusot.common.Globals;
import com.plusot.common.share.LLog;
import com.plusot.common.util.FileUtil;
import com.plusot.javacommon.util.Format;
import com.plusot.javacommon.util.StringUtil;
import com.plusot.javacommon.util.TimeUtil;
import com.plusot.senselib.SenseGlobals;
import com.plusot.senselib.values.Value;
import com.plusot.senselib.values.ValueItem;
import com.plusot.senselib.values.ValueType;

public class GpxLog {
	private static final String CLASSTAG = GpxLog.class.getSimpleName();
	private static GpxLog instance = null;

	private static final String SPEC = "ub";
	private static final String EXT = ".gp";
	private static final String FINALEXT = "x";

	private boolean hasLatLng = false;
	private double lat = 0;
	private double lng = 0;
	private Integer hrm = null;
	private Integer hbi = null;
	private Double pow = null;
	private Double cad = null;
	private Double speed = null;
	private Double alt = null;
	private Double dist = null;
	private Double temp = null;
	private long timestamp = 0;
	//	private Double offset = null;
	//	private Double slope = null;

	private PrintWriter writer = null;
	private String logFilename = null;
	private long refTime = 0;
	private boolean closeing = false;

	private void reset() {
		hasLatLng = false;
		lat = 0;
		lng = 0;
		hrm = null;
		hbi = null;
		pow = null;
		cad = null;
		speed = null;
		alt = null;
		dist = null;
		temp = null;
		timestamp = 0;
		//		offset = null;
		//		slope = null;
		refTime = 0;
	}

	public static void stopInstance(final String caller, final boolean writeConcat) {
		if (instance != null) synchronized(GpxLog.class) {
			instance.close(caller + ",stopInstance", writeConcat);
			instance = null;

		} else {
			LLog.d(Globals.TAG, CLASSTAG + ".stopInstance(): no GPX file to close!");
		}
	}

	public static GpxLog getInstance() {
		if (instance != null) return instance;
		if (!Globals.runMode.isRun()) return null;
		LLog.d(Globals.TAG, CLASSTAG + ".getInstance: Creating object");
		synchronized(GpxLog.class) {
			if (instance == null) instance = new GpxLog();
		}
		return instance;
	}

	private void setPow(long time, Double pow) {
		if (pow == null) return;
		checkTime(time);
		this.pow = pow;
	}

	private void setHrm(long time, Integer hrm) {
		if (hrm == null) return;
		checkTime(time);
		this.hrm = hrm;
	}

	private void setHbi(long time, Integer hbi) {
		if (hbi == null) return;
		checkTime(time);
		this.hbi = hbi;
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

	private void setDist(long time, Double dist) {
		if (dist == null) return;
		checkTime(time);
		this.dist = dist;
	}

	private void setTemp(long time, Double temp) {
		if (temp == null) return;
		checkTime(time);
		this.temp = temp;
	}

	//	private void setOffset(long time, Double offset) {
	//		checkTime(time);
	//		this.offset = offset;
	//	}

	//	private void setSlope(long time, Double slope) {
	//		checkTime(time);
	//		this.slope = slope;
	//	}
	//
	//	private double getOffset() {
	//		return offset;
	//	}
	//
	//	private double getSlope() {
	//		return slope;
	//	}

	public static String getFileName(String session) {
		return Globals.getDataPath() + SPEC + SenseGlobals.DELIM_DETAIL + session + EXT + FINALEXT;
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
				//				writeGpxOpen();
				//				writeTrackOpen();
			}
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

	private String getGpxOpen() {
		return "<?xml version=\"1.0\"?>" + SenseGlobals.ln +
				"<gpx version=\"1.1\" creator=\"BroxTech uBike - http://www.broxtech.com\" " + SenseGlobals.ln +
				"xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " + SenseGlobals.ln +
				"xmlns=\"http://www.topografix.com/GPX/1/1\" " + SenseGlobals.ln +
				"xmlns:gpxdata=\"http://www.bikesenses.com/xmlschemas/GpxExtension/v2\" " + SenseGlobals.ln + 
				"xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd http://www.bikesenses.com/xmlschemas/GpxExtension/v2 http://www.bikesenses.com/xmlschemas/GpxExtensionv2.xsd\"" + SenseGlobals.ln +
				//"xmlns:gpxdata=\"http://www.garmin.com/xmlschemas/TrackPointExtension/v1\"" + 
				//"xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd http://www.garmin.com/xmlschemas/TrackPointExtension/v1 http://www.garmin.com/xmlschemas/TrackPointExtensionv1.xsd\"" +
				">" + SenseGlobals.ln;
	}

	private String getGpxClose() {
		return "</gpx>";
	}

	private String getTrackOpen() {	
		return "<trk>" + SenseGlobals.ln +
				"<name>" + SPEC + SenseGlobals.DELIM_DETAIL + TimeUtil.formatTime(System.currentTimeMillis(), "yyyyMMdd-HHmmss") + "</name>" + SenseGlobals.ln +
				"<trkseg>" + SenseGlobals.ln;
	}

	private String getTrackClose() {
		return "</trkseg>" + SenseGlobals.ln +
				"</trk>" + SenseGlobals.ln;
	}

	private void writeTrackPoint() {
		synchronized(this) {
			_save("<trkpt lat=\"" + Format.format(lat, 7) + "\" lon=\"" + Format.format(lng, 7) + "\">");

			if (alt != null) _save("<ele>" + Format.format(alt, 1) + "</ele>");
			_save("<time>" + TimeUtil.formatTime(timestamp, "yyyy-MM-dd'T'HH:mm:ss'Z'") + "</time>");
			if (temp != null || cad != null || pow != null || speed != null || dist != null) {
				_save("<extensions>");
				if (hrm != null) _save("<gpxdata:hr>" + hrm + "</gpxdata:hr>");
				if (hbi != null) _save("<gpxdata:hbi>" + hbi + "</gpxdata:hbi>");
				if (cad != null) _save("<gpxdata:cadence>" + Format.format(cad, 0) + "</gpxdata:cadence>");
				if (temp != null) _save("<gpxdata:temp>" + Format.format(temp, 0) + "</gpxdata:temp>");
				if (pow != null) _save("<gpxdata:power>" + Format.format(pow, 0) + "</gpxdata:power>");
				if (speed != null) _save("<gpxdata:speed>" + Format.format(speed, 0) + "</gpxdata:speed>");
				if (dist != null) _save("<gpxdata:distance>" + Format.format(dist, 0) + "</gpxdata:distance>");
				_save("</extensions>");
			}
			_save("</trkpt>");
		}
	}

	private void checkTime(long time) {
		timestamp = time;
		if (refTime == 0) refTime = time;
		if (!closeing && time - refTime >= 1000 && hasLatLng) {
			writeTrackPoint();
			refTime = time;
			hasLatLng = false;
		}	
	}

	public void close(final String caller, final boolean writeConcat) {
		try {
			synchronized(this) {
				closeing = true;
				LLog.d(Globals.TAG, CLASSTAG + ": File closed by " + caller); 
				if (writer == null) return;
				writer.close();
				writer = null;

				if (writeConcat) {
					StringBuffer start = new StringBuffer();
					start.append(getGpxOpen());
					start.append(getTrackOpen());
					StringBuffer end = new StringBuffer();
					end.append(getTrackClose());
					end.append(getGpxClose());
					FileUtil.concat(start.toString(), end.toString(), new File(logFilename), new File(logFilename + FINALEXT));
				}
				logFilename = null;

				reset();
			}
		} finally {
			closeing = false;
		}
	}

	public void set(ValueType valueType, ValueItem item) {
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
			case PULSEWIDTH: setHbi(timeStamp, (Integer)value); break;
			default: break;
			} 
		} catch (ClassCastException e) {
			LLog.e(Globals.TAG, CLASSTAG + ": Could not cast " + value.getClass().getName() + ", " + StringUtil.toStringXL(value, ",") + " to " + valueType.toString(), e);
		}
	}
}
