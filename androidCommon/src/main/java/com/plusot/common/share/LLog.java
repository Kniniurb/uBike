package com.plusot.common.share;

import android.util.Log;

import com.plusot.javacommon.util.TimeUtil;

public class LLog {
	
	private static final String SPEC = "applog";

	private static void save(char level, String tag, String msg, Throwable tw) {
		SimpleLog log;
		if ((log = SimpleLog.getInstance(SimpleLogType.LOG, SPEC)) == null) return;
		if (tw == null)
			log.log(TimeUtil.formatMilli() + ';' + level + ';' + tag + ';' + msg);
		else
			log.log(TimeUtil.formatMilli() + ';' + level + ';' + tag + ';' + msg, tw); //tw.getMessage());
	}

	public static void v(String tag, String msg, Throwable tw) {
		save('v', tag, msg, tw);
		Log.v(tag, msg, tw);
	}

	public static void v(String tag, String msg) {
		v(tag, msg, null);
	}

	public static void w(String tag, String msg, Throwable tw) {
		save('w', tag, msg, tw);
		Log.w(tag, msg, tw);
	}

	public static void w(String tag, String msg) {
		w(tag, msg, null);
	}

	public static void d(String tag, String msg, Throwable tw) {
		save('d', tag, msg, tw);
		Log.d(tag, msg, tw);
	}

	public static void d(String tag, String msg) {
		d(tag, msg, null);
	}

	public static void i(String tag, String msg, Throwable tw) {
		save('i', tag, msg, tw);
		Log.i(tag, msg, tw);
	}

	public static void i(String tag, String msg) {
		i(tag, msg, null);
	}

	public static void e(String tag, String msg, Throwable tw) {
		save('e', tag, msg, tw);
		Log.e(tag, msg, tw);
	}

	public static void e(String tag, String msg) {
		e(tag, msg, null);
	}

}
