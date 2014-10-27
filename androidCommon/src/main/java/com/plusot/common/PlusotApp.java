package com.plusot.common;

import android.app.Application;

import com.plusot.common.Globals.TestLevel;
import com.plusot.common.share.LLog;
import com.plusot.common.share.LogMail;
import com.plusot.common.share.SimpleLog;
import com.plusot.common.util.Watchdog;
import com.plusot.javacommon.util.TimeUtil;

public class PlusotApp extends Application implements Thread.UncaughtExceptionHandler, LogMail.Listener {
	private static final String CLASSTAG = PlusotApp.class.getSimpleName();

	@Override
	public void onCreate() {
		super.onCreate();
		Thread.setDefaultUncaughtExceptionHandler(this);
		Globals.init(getApplicationContext(), this);
		Globals.testing = TestLevel.NORMAL;
		Watchdog.killIt = true;
		SimpleLog.setSession(TimeUtil.formatFileTime());
	}

	@Override
	public void uncaughtException(Thread thread, Throwable ex) {
		LLog.e(Globals.TAG, CLASSTAG + ".uncaughtException", ex);
		new LogMail(null, "Exception in " + Globals.TAG + ": " + ex.getMessage(), "Sending", "No mail to send", this);
	}

	@Override
	public void onMailComplete() {
		Watchdog.killIt();
	}
}
