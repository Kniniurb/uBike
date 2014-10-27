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
package com.plusot.senselib.store;

//E4B5B1856EB34130

import android.os.AsyncTask;

import com.plusot.common.Globals;
import com.plusot.common.share.LLog;
import com.plusot.common.share.SimpleLog;
import com.plusot.common.share.SimpleLogType;
import com.plusot.javacommon.util.StringUtil;
import com.plusot.javacommon.util.TimeUtil;
import com.plusot.senselib.SenseGlobals;
import com.plusot.senselib.http.HttpSender;
import com.plusot.senselib.settings.PreferenceKey;
import com.plusot.senselib.values.DeviceListener;
import com.plusot.senselib.values.DeviceStub;
import com.plusot.senselib.values.DeviceType;
import com.plusot.senselib.values.Value;
import com.plusot.senselib.values.ValueItem;
import com.plusot.senselib.values.ValueType;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DataLog {
	private static final char DELIM_NUMBER = '#';
	public static final String EXT = ".csv";
	public static final String DELIM = ";";
	public static final String SPEC = "ub";

	private static final String CLASSTAG = DataLog.class.getSimpleName();
	private static long replayFactor = 100;
	private static DataLog instance = null;

	private PrintWriter writer = null;
	private Map<String, String> logItem = new LinkedHashMap<String, String>();
	private Map<String, String> lastItem = new LinkedHashMap<String, String>();
	private long timeStamp = 0;
	private boolean inProgress = false;
	private String logFilename = null;
	private int recordsWritten = 0;
	private ReadCsvTask readTask = null;
	private String lastString = null;

	public static DataLog getInstance() {
		if (instance != null) return instance;
		if (!Globals.runMode.isRun()) return null;
		synchronized(DataLog.class) {
			if (instance == null) instance = new DataLog();
		}
		return instance;
	}

	public static void stopInstance() {
		if (instance != null) synchronized(DataLog.class) {
			if (instance != null) instance.close();
			instance = null;
		}
	}

	public void readCsv(InputStream is, final DeviceListener listener) {
		close();
		replayFactor = PreferenceKey.REPLAYINTERVAL.getInt(); 
		readTask = new ReadCsvTask(listener);
		readTask.execute(new FileOrStreamItem(null, is));
	}

	public void readCsv(String[] fileList, final DeviceListener listener) {
		if (fileList == null || fileList.length == 0) {
			if (listener != null) listener.onStreamFinished();
			return;
		}

		close();

		Arrays.sort(fileList, java.text.Collator.getInstance());
		List<FileOrStreamItem> list = new ArrayList<FileOrStreamItem>();
		for (String file: fileList) {
			list.add(new FileOrStreamItem(file, null));
		}
		replayFactor = PreferenceKey.REPLAYINTERVAL.getInt();
		readTask = new ReadCsvTask(listener);
		readTask.execute(list.toArray(new FileOrStreamItem[0]));
	}

	public void cancelReadTask() {
		if (readTask != null) readTask.cancel(true);
		readTask = null;
	}

	private boolean hasValues(List<String> container) {
		for (String item: container) {
			if (item != null && !item.equals("")) return true;
		}
		return false;
	}

	private class ProgressItem {
		private final String device;
		private final String valueType;
		private final long timeStamp;
		private final String[] values;
		private final boolean lineEnd;
		public ProgressItem(final String device, final String valueType, final long timeStamp, final String[] values) {
			this.device = device;
			this.valueType = valueType;
			this.timeStamp = timeStamp;
			this.values = values;
			this.lineEnd = false;
		}
		public ProgressItem(final long timeStamp, final long sleep) {
			this.device = null;
			this.valueType = "" + sleep;
			this.timeStamp = timeStamp;
			this.values = null;
			this.lineEnd = true;
		}
	}

	private class FileOrStreamItem {
		private final String filename;
		private final InputStream is;

		public FileOrStreamItem(final String filename, final InputStream is) {
			this.is = is;
			this.filename = filename;
		}
	}

	private class ReadCsvTask extends AsyncTask<FileOrStreamItem, ProgressItem, Boolean> {
		private long timeStamp = 0;
		private EnumMap<ParamType, String> jsonItems = new EnumMap<ParamType, String>(ParamType.class);
		private StringBuilder result = new StringBuilder();
		private final DeviceListener listener;

		public ReadCsvTask(final DeviceListener listener) {
			super();
			this.listener = listener;
		}

		private void fire(String device, String item, long time, String[] values) {
			ValueType dataValueType = ValueType.fromShortName(item);
			ValueItem valueItem = null;
			final DeviceType deviceType = DeviceType.fromShortName(device);
			if (dataValueType.equals(ValueType.TIME) && dataValueType != null) {
				valueItem = new ValueItem(time, time);
			} else if (dataValueType != null) {
				valueItem = new ValueItem(StringUtil.toObject(values, dataValueType.getUnitType().getUnitClass(), dataValueType.defaultUnit().getScaler()), time);
			}

			if (listener != null) listener.onDeviceValueChanged(new DeviceStub() {

                @Override
                public int getCount() {
                    return 0;
                }

                @Override
                public String getLongName() {
                    return deviceType.getLabel(Globals.appContext);
                }

                @Override
                public DeviceType getDeviceType() {
                    return deviceType;
                }
            }, dataValueType, valueItem, false);
		}

		@Override
		protected void onProgressUpdate(ProgressItem... progress) {
			for (ProgressItem item: progress) {
				if (item.lineEnd) {
					//LLog.d(Globals.TAG, CLASSTAG + ".sleep " + item.valueType);
					if (SenseGlobals.jsonCopy && jsonItems.size() > 0) {
						result.append('{');
						//result.append("\"id\":" + item.iCounter).append(",");
						for (ParamType resultType : ParamType.values()) if (jsonItems.get(resultType) != null) {
							result.append("\"" + resultType.getLabel() + "\":" + jsonItems.get(resultType)).append(",");
						}
						result.append("\"time\":\"" + TimeUtil.formatTime(item.timeStamp) + "\"");
						result.append('}');
						SimpleLog.getInstance(SimpleLogType.JSON, "json").log(result.toString());
						LLog.i(Globals.TAG, CLASSTAG + ".onProgressUpdate: " + result.toString());
						result = new StringBuilder();
						jsonItems.clear();
					}
				} else {
					//LLog.d(Globals.TAG, CLASSTAG + ".onProgressUpdate " + item.device+ ", " + item.valueType + ", " + TimeUtil.formatTime(item.timeStamp) + ", (" + StringUtil.toString(item.values, ", ") + ')');
					fire(item.device, item.valueType, item.timeStamp, item.values);
					if (SenseGlobals.jsonCopy) {
						int i = 0;
						for (String strValue: item.values) {
							ParamType type = ParamType.fromValueType(ValueType.fromShortName(item.valueType), i++);
							if (type != null) try {
								jsonItems.put(type, strValue);
							} catch (NumberFormatException e ) {
							}
						}
					}
				}
			}
		}



		@Override
		protected void onPostExecute(Boolean result) {
			if (listener != null) listener.onStreamFinished();
			LLog.d(Globals.TAG, CLASSTAG + ".ReadCsvTask.onPostExecute = task done");
			if (SenseGlobals.jsonCopy) SimpleLog.getInstance(SimpleLogType.JSON, "json").close();
		}

		private void readFile(Reader in) {
			BufferedReader reader = null;
			SenseGlobals.activity = SenseGlobals.ActivityMode.REPLAY;
			try {
				reader = new BufferedReader(in);
				String caption = reader.readLine();
				if (caption == null) return;
				String captions[] = caption.split(DELIM);
				if (captions.length == 0) return;

				String line = reader.readLine();
				List<String> tempValues = new ArrayList<String>();
				List<ProgressItem> progressItems = new ArrayList<ProgressItem>();
				long prevTimeStamp = 0;
				long sleep = 10;
				long now = System.currentTimeMillis();
				while (line != null) {
					String[] lineParts = line.split(DELIM);
					if (lineParts.length > 1) {
						if (lineParts[0].equalsIgnoreCase("time")) {
							captions = lineParts;
						} else {
							prevTimeStamp = timeStamp;
							timeStamp = TimeUtil.parseTime(lineParts[0]);
							if (prevTimeStamp > timeStamp) timeStamp = prevTimeStamp;
							progressItems.add(new ProgressItem(DeviceType.TIMER.getLowerCaseShortName(), ValueType.TIME.getLowerCaseShortName(), timeStamp, new String[] {lineParts[0]}));
							String prevCaption = null;
							tempValues.clear();
							if (timeStamp > 0) for (int i = 1; i < Math.min(captions.length, lineParts.length); i++) {
								String[] captionParts = captions[i].split("" + DELIM_NUMBER);
								if (tempValues.size() > 0 && captionParts.length >= 1 && prevCaption != null && !prevCaption.equals(captionParts[0])) {
									String[] deviceItem = prevCaption.split(SenseGlobals.DELIM_DETAIL);
									if (deviceItem.length > 1 && hasValues(tempValues)) {
										String[] strings= tempValues.toArray(new String[0]);
										//LLog.d(Globals.TAG, CLASSTAG + ".doInBackground.publish " + deviceItem[0] + ", " + deviceItem[1] + ", " + TimeUtil.formatTime(timeStamp) + ", (" + StringUtil.toString(strings) + ')');
										progressItems.add(new ProgressItem(deviceItem[0], deviceItem[1], timeStamp, strings));
									}
									tempValues.clear();
								}
								if (captionParts.length >= 2) {
									int index = Integer.valueOf(captionParts[captionParts.length - 1]);
									if (index > 0) {
										tempValues.add(lineParts[i]);
									}
									prevCaption = captionParts[0];
								} else if (lineParts[i] != null && !lineParts[i].equals("")) {
									String[] deviceItem = captions[i].split(SenseGlobals.DELIM_DETAIL);
									if (deviceItem.length > 1) {
										//LLog.d(Globals.TAG, CLASSTAG + ".doInBackground.publish " + deviceItem[0] + ", " + deviceItem[1] + ", " + TimeUtil.formatTime(timeStamp) + ", (" + lineParts[i] + ')');			
										progressItems.add(new ProgressItem(deviceItem[0], deviceItem[1], timeStamp, new String[] {lineParts[i]}));
									}
									prevCaption = captions[i];
								} else
									prevCaption = captions[i];
							}
							long now2 = System.currentTimeMillis();
							sleep = 2;
							if (prevTimeStamp > 0 && now2 - now < (timeStamp - prevTimeStamp) / replayFactor) {
								sleep = Math.min((timeStamp - prevTimeStamp) / replayFactor - (now2 - now), 3000);
							} 
							now = now2;
							Thread.sleep(sleep);
						}
					}
					if (progressItems.size() > 0) {
						progressItems.add(new ProgressItem(timeStamp, sleep));
						this.publishProgress(progressItems.toArray(new ProgressItem[0]));
					}
					progressItems.clear();
					line = reader.readLine();
				}
			} catch (IOException e) {
				LLog.e(Globals.TAG, CLASSTAG + ".ReadCsvTask.readFile: IO Exception in stream.", e);
			} catch (InterruptedException e) {
				LLog.e(Globals.TAG, CLASSTAG + ".ReadCsvTask.readFile: Sleep interrupted.", e);
			} finally {
				if (reader != null) try {
					reader.close();
				} catch (IOException e) {
					LLog.e(Globals.TAG, CLASSTAG + ".ReadCsvTask.readFile: Could not close stream.");
				}
			}
		}

		@Override
		protected Boolean doInBackground(FileOrStreamItem... params) {
			if (params != null && params.length != 0) for (FileOrStreamItem item: params) {
				Reader in = null;
				if (item.filename != null) try {
					in = new FileReader(new File(Globals.getDataPath() + item.filename));
					readFile(in);
				} catch (FileNotFoundException e1) {
					LLog.e(Globals.TAG, CLASSTAG + ".ReadCsvTask.doInBackground: Could not open file for reading: " + item.filename);
					continue;
				} else {
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
					}
					in = new InputStreamReader(item.is);
					readFile(in);
				}
			}
			return true;
		}
	}

	public static String getFileName(String session) {
		return Globals.getDataPath() + SPEC + SenseGlobals.DELIM_DETAIL + session  + EXT;
	}

	private boolean open() {
		File file = new File(Globals.getDataPath()); 
		boolean isNew = true;
		if (!file.isDirectory()) file.mkdirs();

		logFilename = getFileName(Value.getSessionString());
		file = new File(logFilename); 
		try {
			isNew = file.createNewFile();
			if (isNew)
				LLog.d(Globals.TAG, CLASSTAG + ".open: Opening new file " + logFilename);
			else
				LLog.d(Globals.TAG, CLASSTAG + ".open: Appending to " + logFilename);
			writer = new PrintWriter(new BufferedWriter(new FileWriter(file, true)));
			recordsWritten = 0;
		} catch (IOException e) {
			LLog.e(Globals.TAG, CLASSTAG + ".open: Error creating file");
			writer = null;
		} 
		return isNew;
	}

	private long timeFlushed = 0;
	private boolean shouldWriteCaptions = false;

	private void flush() {
		long now = System.currentTimeMillis();
		if (now - timeFlushed < Globals.FLUSHTIME) return;
		if (writer != null) writer.flush();
		timeFlushed = System.currentTimeMillis();
	}

	private void write(String logStr) {
		if (writer != null && writer.checkError()) {
			LLog.d(Globals.TAG, CLASSTAG + ".log: Closing & opening writer due to error.");
			writer.close();
			writer = null;
		}
		if (writer == null) {

			if (open() && writer != null) logCaptions();
		}
		if (writer == null)	{
			LLog.d(Globals.TAG, CLASSTAG + ".log: Could not write: " + logStr);
		} else {
			recordsWritten++;
			writer.println(logStr);
			flush();
		}
	}

	private void logCaptions() {
		StringBuilder sb = new StringBuilder();

		for (String key : logItem.keySet()) {
			sb.append(DELIM).append(key);
		}
		write("time" + sb.toString().toLowerCase(Locale.US));
		shouldWriteCaptions = false;
	}

	private void save() {
		if (!inProgress) return;
		String time = TimeUtil.formatTime(this.timeStamp * 1000);

		if (HttpSender.isHttpPost) {
			HttpSender sender = HttpSender.getInstance();
			if (sender != null) sender.sendData(time, logItem);
		}
		if (shouldWriteCaptions) logCaptions();
		StringBuilder sb = new StringBuilder();		
		for (String key : logItem.keySet()) {
			sb.append(DELIM).append(logItem.get(key));
			logItem.put(key, "");
		}
		String str = sb.toString();
		if (lastString == null || !str.equals(lastString)) {
			write(time + sb.toString());
			lastString = str;
		}

		inProgress = false;
	}

//	private void prepareLog(String identifier, int items) {
//		String id = identifier.toLowerCase(Locale.US);
//		if (logItem.containsKey(id)) return;
//		if (logItem.containsKey(id + DELIM_NUMBER + 1)) return;
//		if (items > 1) {
//			for (int i = 1; i <= items; i++) {
//				logItem.put(id + DELIM_NUMBER + i, "");
//			} 
//		} else {
//			logItem.put(id, "");
//		}
//		shouldWriteCaptions  = true;
//	}


	public void log(final DeviceType deviceType, final ValueType valueType, String value, final long timeStamp) {
		if (!SenseGlobals.activity.equals(SenseGlobals.ActivityMode.RUN)) return;
		_log(deviceType.getShortName() + '_' + valueType.getShortName(), value, timeStamp);
	}

	private void _log(final String id, String value, final long timeStamp) {

		final String identifier = id.toLowerCase(Locale.US);
		long sec = timeStamp / 1000;
		if (this.timeStamp == 0) this.timeStamp = sec;
		if (inProgress) {
			if (sec < this.timeStamp && this.timeStamp - sec > 30) {
				//close();
				save();
				this.timeStamp = sec;
			} else if (sec >= this.timeStamp + SenseGlobals.logInterval) {
				save();
				this.timeStamp = sec;
			}
		}
		if (value.contains("\r") || value.contains("\n")) {
			value = value.replace("\r\n", " ");
			value = value.replace('\r', ' ');
			value = value.replace('\n', ' ');
		}
		boolean isNew = false;
		String result = null;
		if (value.contains(DELIM)) {
			String[] values = value.split(DELIM);
			int i = 0;
			for (String temp : values) {
				if (!temp.equals(lastItem.get(identifier + DELIM_NUMBER + ++i))) isNew = true;
			}
			if (!isNew) return;
			i = 0;
			for (String temp : values) {
				result = logItem.put(identifier + DELIM_NUMBER + ++i, temp);
				lastItem.put(identifier + DELIM_NUMBER + i, temp);
			}
		} else {
			if (value.equals(lastItem.get(identifier))) return;
			result = logItem.put(identifier, value);
			lastItem.put(identifier, value);
		}
		inProgress = true;
		if (result == null) {
			shouldWriteCaptions = true;
			//this allow app to open a new log file when a new unexpected valueType has been recorded.
			//LLog.w(Globals.TAG, CLASSTAG + ".log: A new valueType has been found = " + identifier); 
			//close();
		}

	}

	public void close() {
		synchronized(this) {
			if (writer == null) return;
			if (inProgress) save();
			LLog.d(Globals.TAG, CLASSTAG + ": Closing log file"); 

			writer.close();
			writer = null;
			logFilename = null;
			recordsWritten = 0;
		}
	}

	public int getRecordsWriten() {
		return recordsWritten;
	}

}
