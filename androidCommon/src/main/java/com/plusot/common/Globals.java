package com.plusot.common;

import java.io.File;
import java.util.Locale;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Environment;
import android.util.Log;


public class Globals {

	public enum RunMode {
		RUN,
		FINISHING,
		FINISHED;
		public boolean isRun() {
			return this.equals(RUN);
		}
		public boolean isFinishing() {
			return this.equals(FINISHING);
		}
		public boolean isFinished() {
			return this.equals(FINISHED);
		}

	}

	private static final String CLASSTAG = Globals.class.getSimpleName();
	public static long TIMEBIAS = 1400000000000L;
	public static String TAG = "PlusotApp";
	public static String appName = "PlusotApp";
	public static final String FILE_DETAIL_DELIM = "_";
	public static Context appContext = null;
	public static Application app = null;
	//	public static boolean appFinishing = false;
	public static RunMode runMode = RunMode.RUN;
	public static long FLUSHTIME = 20000;
	public static final String LOG_SPEC = "syslog";
	public static final String LOG_EXT = ".txt";

	public enum TestLevel {
		NONE("notest"),
		NORMAL("test"),
		VERBOSE("verbose");
		private final String label;

		private TestLevel(final String label) {
			this.label = label;
		}
		public boolean isVerbose() {
			return this.equals(VERBOSE);
		}

		public boolean isTest() {
			return !this.equals(NONE);
		}

		public boolean isNoTest() {
			return this.equals(NONE);
		}

		public String getLabel() {
			return label;
		}
		
		public static TestLevel fromString(final String label) {
			for (TestLevel level: TestLevel.values()) {
				if (level.label.equalsIgnoreCase(label)) return level;
			}
			return null;
		}

	}

	public static TestLevel testing = TestLevel.NONE;

	public static final String SD_PATH = Environment.getExternalStorageDirectory().getAbsolutePath();

	public static String getStorePath() { 
		return SD_PATH + "/" + TAG.toLowerCase(Locale.US) + '/';
	}
	
	public static String getDownloadPath() { 
		return SD_PATH + "/Download/";
	}

	public static String getDataPath() { 
		return SD_PATH + "/" + TAG.toLowerCase(Locale.US) + "/data/";
	}

	public static String getLogPath() { 
		return SD_PATH + "/" + TAG.toLowerCase(Locale.US) + "/log/";
	}

	public static void init(Context _appContext, Application _app) {

		appContext = _appContext;
		appName = appContext.getApplicationContext().getString(appContext.getApplicationContext().getApplicationInfo().labelRes);
		TAG = appName.replace("'", "").replace(" ", "");
		if (app != null)
			Log.e(Globals.TAG, CLASSTAG + ".init: Application already exists");
		else
			Log.d(Globals.TAG, CLASSTAG + ".init");
		app = _app;
		runMode = RunMode.RUN;

		String name = appName;
		try {
			PackageInfo info = appContext.getPackageManager().getPackageInfo(appContext.getPackageName(), 0);
			name += " " + info.versionName;
		} catch (NameNotFoundException e) {
		}

		String nameStr = "   *" + "                          ".substring(0, 26 - name.length() / 2) + 
				name + "                             ";
		nameStr = nameStr.substring(0, 56) + "*\n";

		Log.d(TAG, CLASSTAG + ".onCreate:\n" +
				"   ******************************************************\n" +
				"   *                                                    *\n" +
				"   *                                                    *\n" +
				nameStr +                                                  
				"   *                 Application Started                *\n" +
				"   *                                                    *\n" +
				"   *                                                    *\n" +
				"   ******************************************************");

		File file = new File(Globals.getStorePath()); 
		if (!file.isDirectory() && !file.mkdirs()) {
			Log.w(Globals.TAG, CLASSTAG + " Could not create: " + Globals.getStorePath());
		}
		file = new File(Globals.getLogPath()); 
		if (!file.isDirectory() && !file.mkdirs()) {
			Log.w(Globals.TAG, CLASSTAG + " Could not create: " + Globals.getLogPath());
		}
		file = new File(Globals.getDataPath()); 
		if (!file.isDirectory() && !file.mkdirs()) {
			Log.w(Globals.TAG, CLASSTAG + " Could not create: " + Globals.getDataPath());
		}
	}

}
