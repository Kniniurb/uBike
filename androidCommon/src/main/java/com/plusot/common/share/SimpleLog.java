package com.plusot.common.share;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.util.Log;

import com.plusot.common.Globals;
import com.plusot.javacommon.util.TimeUtil;

public class SimpleLog {
	private static final String CLASSTAG = SimpleLog.class.getSimpleName();
	private static Map<String, SimpleLog> instances = new HashMap<String, SimpleLog>();

	private final String spec;
	private final SimpleLogType type;

	private PrintWriter writer = null;
	private String logFilename = null;
	private boolean closeing = false;
	private static String session = null;

	public SimpleLog(final SimpleLogType type, final String spec) {
		this.type = type;
		this.spec = spec;
	}

	public static void setSession(String session) {
		SimpleLog.session = session;
	}

	//	public String getFileName() {
	//		return logFilename;
	//	}

	public static void stopInstances() {
		if (instances.size() > 0) for (SimpleLog instance : instances.values()) if (instance != null) synchronized(SimpleLog.class) {
			instance.close();
			instance = null;
		} else {
			Log.d(Globals.TAG, CLASSTAG + ".stopInstance(): no SimpleLog file to close!");
		}
		instances.clear();
	}

	public static boolean hasInstances() {
		return instances.size() > 0;
	}

	/*
	 * Used by LLog. Creating instance can not use LLog!!!
	 */
	public static SimpleLog getInstance(SimpleLogType type, String spec) {
		//if (instance != null) return instance;
		if (!Globals.runMode.isRun()) return null;
		SimpleLog instance = null;
		synchronized(SimpleLog.class) {
			//if (instance == null) 
			instance = instances.get(spec);
			if (instance == null) {
				instance = new SimpleLog(type, spec);
				instances.put(spec, instance);
				//LLog.d(Globals.TAG, CLASSTAG + ".getInstance: Creating object");

			}
		}
		return instance;
	}

	public static void closeInstances() {
		synchronized(SimpleLog.class) { 
			for (SimpleLog instance: instances.values()){ 
				instance.close();
			}
		}
	}

	public static List<String> getFileNames(String session) {
		List<String> list = new ArrayList<String>();
		synchronized(SimpleLog.class) { 
			for (SimpleLog instance: instances.values()){ 
				String name = instance.getFileName(session);
				if (new File(name).exists()) list.add(name);
			}
		}
		return list;
	}

	public String getFileName(String session) {
		if (session == null) return logFilename;
		return Globals.getDataPath() + spec + Globals.FILE_DETAIL_DELIM + session + type.getExt();
	}

	private boolean open() {
		if (Globals.runMode.isFinished()) return false;
		boolean isNew = true;

		File file = new File(Globals.getDataPath()); 
		if (!file.isDirectory()) file.mkdirs();
		String session;
		if (SimpleLog.session == null) {
			session = TimeUtil.formatFileTime();
		} else
			session = SimpleLog.session;
		logFilename = Globals.getDataPath() + spec + Globals.FILE_DETAIL_DELIM + session + type.getExt();
		file = new File(logFilename); 
		try {
			if (type.isAppend()) 
				isNew = file.createNewFile();
			if (isNew)
				Log.d(Globals.TAG, CLASSTAG + ".open: Opening new file " + logFilename);
			else
				Log.d(Globals.TAG, CLASSTAG + ".open: Appending to " + logFilename);
			writer = new PrintWriter(new BufferedWriter(new FileWriter(file, type.isAppend())));
			if (isNew && type.getBeforeLines() != null) {
				writer.println(type.getBeforeLines());
			}
		} catch (IOException e) {
			Log.e(Globals.TAG, CLASSTAG + ".open: Error creating file", e);
			writer = null;
		} 
		return isNew;
	}

	public void log(Map<String, String> map) {
		if (closeing) return;
		if (type.isWriteOnSessionOnly() && session == null) return;
		if (type.equals(SimpleLogType.JSON)) {
			StringBuilder sb = new StringBuilder();
			for (String key : map.keySet()) {
				if (sb.length() > 0) sb.append(",");
				sb.append("\"").append(key).append("\":").append(map.get(key));
			}
			log("{" + sb.toString() + "}");
		}
	}

	public void log(String logStr) {
		if (closeing) return;
		if (type.isWriteOnSessionOnly() && session == null) return;
		boolean isNew = false;
		synchronized(this) {
			if (writer != null && writer.checkError()) {
				Log.d(Globals.TAG, CLASSTAG + ".log: Closing & opening writer due to error.");
				writer.close();
				writer = null;
			}
			if (writer == null) {
				if (open() && writer != null) {
					isNew = true;
				}
			}
			if (writer == null)	{
				Log.d(Globals.TAG, CLASSTAG + ".save: Could not write: " + logStr);
			} else {
				//recordsWritten++;
				if (!isNew && type.getBetweenLines() != null) writer.println(type.getBetweenLines());
				writer.println(logStr);
				flush();
			}
		}
	}
	public void log(String logStr, Throwable e) {
		if (closeing) return;
		if (type.isWriteOnSessionOnly() && session == null) return;
		boolean isNew = false;
		synchronized(this) {
			if (writer != null && writer.checkError()) {
				Log.d(Globals.TAG, CLASSTAG + ".log: Closing & opening writer due to error.");
				writer.close();
				writer = null;
			}
			if (writer == null) {
				if (open() && writer != null) {
					isNew = true;
				}
			}
			if (writer == null)	{
				Log.d(Globals.TAG, CLASSTAG + ".save: Could not write: " + logStr);
			} else {
				//recordsWritten++;
				if (!isNew && type.getBetweenLines() != null) writer.println(type.getBetweenLines());
				writer.println(logStr);
				e.printStackTrace(writer);
				flush();
			}
		}
	}

	private long timeFlushed = 0;

	private void flush() {
		long now = System.currentTimeMillis();
		if (now - timeFlushed < Globals.FLUSHTIME) return;
		if (writer != null) writer.flush();
		timeFlushed = System.currentTimeMillis();
	}

	public void close() {

		synchronized(this) {
			try {
				closeing = true;
				Log.d(Globals.TAG, CLASSTAG + ": Closing log file"); 
				if (writer == null) return;
				if (type.getBehindLines() != null) writer.println(type.getBehindLines());
				writer.close();
				writer = null;

				logFilename = null;
			}
			finally {
				closeing = false;
			}
		}
	}

}
