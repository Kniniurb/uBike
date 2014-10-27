package com.plusot.senselib.store;

import java.util.HashMap;
import java.util.Map;

import com.plusot.common.Globals;
import com.plusot.common.share.LLog;
import com.plusot.common.share.SimpleLog;
import com.plusot.common.share.SimpleLogType;
import com.plusot.common.util.UserInfo;
import com.plusot.javacommon.util.Format;
import com.plusot.senselib.values.Unit;
import com.plusot.senselib.values.Value;
import com.plusot.senselib.values.ValueItem;
import com.plusot.senselib.values.ValueType;

public class GoldenCheetahLog {
	private static String CLASSTAG = GoldenCheetahLog.class.getSimpleName();

	private static GoldenCheetahLog instance;
	private final String name;
	private Map<ValueType, Double> items = new HashMap<ValueType, Double>();
	private long lastUpdate;

	public GoldenCheetahLog() {
		name = UserInfo.getUserData().name;
		//		new Thread(new Runnable() {
		//
		//			@Override
		//			public void run() {
		//				Thread.sleep(1000);
		//				
		//			}
		//			
		//		}).start();
	}
	
	public static void stopInstance(final String caller, final boolean writeConcat) {
		if (instance != null) synchronized(GpxLog.class) {
			instance.close();
			instance = null;
		} else {
			LLog.d(Globals.TAG, CLASSTAG + ".stopInstance(): no Golden Cheetah CSV file to close!");
		}
	}

	public static GoldenCheetahLog getInstance() {
		if (instance != null) return instance;
		if (!Globals.runMode.isRun()) return null;
		LLog.d(Globals.TAG, CLASSTAG + ".getInstance: Creating object");
		synchronized(GpxLog.class) {
			if (instance == null) instance = new GoldenCheetahLog();
		}
		return instance;
	}
	
	private void close() {
		SimpleLog.getInstance(SimpleLogType.ARGOS, name).close();
	}

	
	private double get(ValueType type) {
		Double value = items.get(type);
		if (value == null) return 0;
		return value;
	}

	private void log() {
		StringBuffer line = new StringBuffer();
		line.append(Format.format(Math.max(1.0 * (lastUpdate - Value.getSessionTime()) / 60000, 0), 4)).append(SimpleLogType.ARGOS.sep);
		line.append(Format.format(get(ValueType.CRANK_TORQUE), 2)).append(SimpleLogType.ARGOS.sep);
		line.append(Format.format(Unit.KPH.scale(get(ValueType.SPEED)), 1)).append(SimpleLogType.ARGOS.sep);
		line.append(Format.format(get(ValueType.POWER), 1)).append(SimpleLogType.ARGOS.sep);
		line.append(Format.format(get(ValueType.DISTANCE) / 1000, 4)).append(SimpleLogType.ARGOS.sep);
		line.append(Format.format(get(ValueType.CADENCE), 0)).append(SimpleLogType.ARGOS.sep);
		line.append(Format.format(get(ValueType.HEARTRATE), 0)).append(SimpleLogType.ARGOS.sep);
		line.append(Format.format(get(ValueType.INTERVAL), 0)).append(SimpleLogType.ARGOS.sep);
		line.append(Format.format(get(ValueType.ALTITUDE), 0));
		SimpleLog.getInstance(SimpleLogType.ARGOS, name).log(line.toString());
	}

	public void save(ValueType type, ValueItem item) {
		items.put(type, item.getDoubleValue());
		if (item.getTimeStamp() / 1000 - lastUpdate / 1000 >= 1) {
			if (lastUpdate != 0) log();
			lastUpdate = item.getTimeStamp();
		}
	}
	
	public String getFilename() {
		return SimpleLog.getInstance(SimpleLogType.ARGOS, name).getFileName(Value.getSessionString());
	}

}
