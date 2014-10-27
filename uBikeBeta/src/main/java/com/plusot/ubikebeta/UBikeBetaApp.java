package com.plusot.ubikebeta;

import com.plusot.senselib.SenseApp;
import com.plusot.senselib.SenseGlobals;
import com.plusot.senselib.values.ManagerType;

import java.util.EnumSet;

public class UBikeBetaApp extends SenseApp {

	public UBikeBetaApp() {
//		Globals.TAG = "uBike";
		ManagerType.supportedManagers = EnumSet.of(
				ManagerType.ANT_MANAGER,
				ManagerType.GPS_MANAGER,
				ManagerType.TIME_MANAGER,
				ManagerType.SENSOR_MANAGER,
				ManagerType.BLUETOOTH_MANAGER,
				ManagerType.BLUETOOTHLE_MANAGER
				);
		//SenseGlobals.setFilePath("/" + Globals.TAG.toLowerCase(Locale.US) + "/");
		SenseGlobals.notifyIcon = R.drawable.ubike_24;
		SenseGlobals.isBikeApp = true;
		
		SenseGlobals.splashLayout = R.layout.splash;
		SenseGlobals.splash_rides_id = R.id.splash_rides;
		SenseGlobals.splash_time_id = R.id.splash_time;
		SenseGlobals.splash_ascent_id = R.id.splash_ascent;
		SenseGlobals.splash_altitude_id = R.id.splash_altitude;
		SenseGlobals.splash_distance_id = R.id.splash_distance;
		SenseGlobals.splash_energy_id = R.id.splash_energy;
		SenseGlobals.splash_ascent_unit_id = R.id.splash_ascent_unit;
		SenseGlobals.splash_altitude_unit_id = R.id.splash_altitude_unit;
		SenseGlobals.splash_distance_unit_id = R.id.splash_distance_unit;
		SenseGlobals.splash_energy_unit_id = R.id.splash_energy_unit;
		SenseGlobals.splash_release_id = R.id.splash_release;
		SenseGlobals.splash_lasttimesent_id = R.id.splash_lasttimesent;
		SenseGlobals.splashrow_lasttimesent_id = R.id.splashrow_lasttimesent;
	}


}
