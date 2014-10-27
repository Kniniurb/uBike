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
package com.plusot.senselib;

import android.location.Location;

import com.plusot.common.Globals;

public class SenseGlobals {
	public enum ActivityMode {
		RUN,
		REPLAY,
		STOP;
		public boolean isRun() {
			return this.equals(RUN);
		}
		public boolean isReplay() {
			return this.equals(REPLAY);
		}
		public boolean isStop() {
			return this.equals(STOP);
		}
	}
	public enum StopState {
		PAUZE,
		STOP;
		public static StopState fromInt(int value) {
			for (StopState state: StopState.values()) {
				if (state.ordinal() == value) return state;
			}
			return STOP;
		}
	}
	public static String getAudioPath() { return Globals.getStorePath() + "audio/"; }
	public static String getGpxPath() { return Globals.getStorePath() + "gpx/"; }
	public static String getScreenshotPath() { return Globals.getStorePath() + "screenshots/"; }
	public static String getRestorePath() { return Globals.getStorePath() + "restore/"; }
	public static final String DELIM_DETAIL = "_";
	public static final int MAX_QUICKVIEW = 200;
	public static int wheelCirc = 2070;
	public static ActivityMode activity = ActivityMode.STOP;
	public static Location lastLocation = null;
	public static long lastLocationTime = 0;
	public static double lastAltitude = 0;
	public static long lastAltitudeTime = 0;
	public static boolean screenLock = false;
	public static Class<?> mainClass = null;
	public static int notifyIcon = 0;
	public static boolean isBikeApp = false;
	public static boolean loadSplash = true;
	public static int splashLayout = 0;
	public static String fileSpec = "ub";
	public static StopState stopState = StopState.STOP;
	
	public static boolean batteryTesting = false;
	public static boolean accelerometerTesting = false;
	public static boolean hxmTesting = false;
	public static boolean suuntoTesting = false;
	public static boolean jsonCopy = false;
	
	public static int splash_time_id = 0;
	public static int splash_rides_id = 0;
	public static int splash_distance_id = 0;
	public static int splash_energy_id = 0;
	public static int splash_ascent_id = 0;
	public static int splash_altitude_id = 0;
	public static int splash_distance_unit_id = 0;
	public static int splash_energy_unit_id = 0;
	public static int splash_ascent_unit_id = 0;
	public static int splash_altitude_unit_id = 0;
	public static int splash_release_id = 0;	
	public static int splash_lasttimesent_id = 0;
	public static int splashrow_lasttimesent_id = 0;
	public static int logInterval = 2;
	public static int maxPowerDevices = 1;
	public static int maxHeartrateBands = 1;
	public static boolean supportSuunto = false;
	public static String ln = "\r\n";
	public static String replaySession = null;
	public static boolean argos = false;

}
