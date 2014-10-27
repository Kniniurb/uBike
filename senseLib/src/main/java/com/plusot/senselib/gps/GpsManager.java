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
package com.plusot.senselib.gps;

import android.content.Context;
import android.location.GpsStatus;
import android.location.GpsStatus.NmeaListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;

import com.plusot.common.Globals;
import com.plusot.common.share.LLog;
import com.plusot.common.util.Calc;
import com.plusot.javacommon.util.Format;
import com.plusot.javacommon.util.TimeUtil;
import com.plusot.senselib.R;
import com.plusot.senselib.SenseGlobals;
import com.plusot.senselib.settings.PreferenceKey;
import com.plusot.senselib.util.Util;
import com.plusot.senselib.values.Device;
import com.plusot.senselib.values.DeviceType;
import com.plusot.senselib.values.Manager;
import com.plusot.senselib.values.ValueType;

public class GpsManager implements Manager {
	private static final String CLASSTAG = GpsManager.class.getSimpleName();

	private GpsDevice gpsDevice;

	public enum Nmea {
		GPGGA("$GPGGA"),
		GPGSV("$GPGSV");
		private final String label;

		private Nmea(final String label) {
			this.label = label;
		}

		public static Nmea fromString(String value) {
			for (Nmea nmea: Nmea.values()) if (nmea.label.equals(value)) return nmea;
			return null;
		}
	}

	public enum State {
		PROVIDER_DISABLED(R.string.provider_disabled),
		PROVIDER_ENABLED(R.string.provider_enabled),
		PROVIDER_AVAILABLE(R.string.provider_available),
		PROVIDER_OUT_OF_SERVICE(R.string.provider_out_of_service),
		PROVIDER_TEMPORARILY_UNAVAILABLE(R.string.provider_temporarily_unavailable),
		SATELLITES_IN_VIEW(R.string.satellites_in_view),
		LOCATION_FIRST_FIX(R.string.location_first_fix);

		private final int idLabel;

		private State(final int label) {
			this.idLabel = label;
		}
		public String getLabel(Context context) {
			return context.getString(idLabel);
		}

		public String getLabel(Context context, Object ... args) {
			if (context == null) {
				LLog.e(Globals.TAG, CLASSTAG + ": No context in Device.State.getLabel");
				return "unknown state";
			}
			return context.getString(idLabel, args);
		}
	}

	public static class GpsDevice extends Device {
		private LocationManager locationMgr;
		private boolean active = false;
		private double speed = Double.NaN;

		public GpsDevice(Context context) {
			super(DeviceType.GPS_SENSOR);
			fadeOutTime = (long) (3 * PreferenceKey.GPSINTERVAL.getInt());
			locationMgr = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
			if (locationMgr == null) {
				return;
			} /*else {
				//List<String> providerList = locationMgr.getAllProviders();
				Criteria criteria = new Criteria();
				criteria.setAccuracy(Criteria.ACCURACY_HIGH);
				List<String> providerList = locationMgr.getProviders(criteria, true); //getAllProviders();
				if (providerList != null) for (String provider: providerList){
					LLog.d(Globals.TAG, CLASSTAG + ".GpsDevice Provider: " + provider);
				}
			}*/
			SenseGlobals.lastLocation = getLastLocation();
		}

		@Override
		public boolean isActive() {
			return true;
		}

		private class GpsLocationListener implements LocationListener {
			private Location prevLocation = null;

			public void onLocationChanged(Location location) {
				//LLog.d(Globals.TAG, CLASSTAG + ".GpsDevice Location changed: " + location);
				long now = System.currentTimeMillis(); //location.getTime();
				if (location != null) {
					double[] loc = new double[]{location.getLatitude(), location.getLongitude()};
					fireOnDeviceValueChanged(ValueType.LOCATION, loc, now);
					if (prevLocation != null) {
						double deltaDistance = location.distanceTo(prevLocation);
						if (location.hasAltitude() && prevLocation.hasAltitude()) {
							double deltaAltitude = location.getAltitude() - prevLocation.getAltitude();
							fireOnDeviceValueChanged(ValueType.SLOPE, deltaAltitude / deltaDistance, now);
						}
						//distance += deltaDistance;
						//fireOnDeviceValueChanged(ValueType.DISTANCE, distance, location.getTime());
						speed = 1000 * deltaDistance / (location.getTime() - prevLocation.getTime());
						fireOnDeviceValueChanged(ValueType.SPEED, speed, now);
						double bearing = Calc.bearing(prevLocation, location);
						fireOnDeviceValueChanged(ValueType.BEARING, bearing, now);
					}

					if (location.getAltitude() != 0) fireOnDeviceValueChanged(ValueType.ALTITUDE, (double)(location.getAltitude() + PreferenceKey.GPSALTOFFSET.getInt()), now);
					//if (location.getSpeed() != 0) fireOnDeviceValueChanged(ValueType.SPEED, location.getSpeed(), location.getTime());
					//if (location.getBearing() != 0) fireOnDeviceValueChanged(ValueType.BEARING, location.getBearing(), location.getTime());
					Bundle extras = location.getExtras();
					if (extras != null && extras.containsKey("satellites")) {
						atStateChanged(State.SATELLITES_IN_VIEW, extras.getInt("satellites"));
						fireOnDeviceValueChanged(ValueType.SATELLITES, extras.getInt("satellites"), now);
					}

					prevLocation = location;
					SenseGlobals.lastLocation = location;
					SenseGlobals.lastLocationTime = now;
					PreferenceKey.checkAltOffsets(CLASSTAG);
				}
			}

			private void atStateChanged(State state, Object ... args) {
				//				LLog.d(
				//						Globals.TAG, 
				//						CLASSTAG + ".atStateChanged for " + getName() + 
				//						" = " +	state.getLabel(Globals.appContext, args));
			}


			public void onProviderDisabled(String provider) { 
				atStateChanged(State.PROVIDER_DISABLED, provider);
			}

			public void onProviderEnabled(String provider) { 
				atStateChanged(State.PROVIDER_ENABLED, provider);
			}

			public void onStatusChanged(String provider, int status, Bundle extras){
				switch (status) {
				case LocationProvider.AVAILABLE: 
					//					LLog.d(Globals.TAG, CLASSTAG + ": Status provider available");
					atStateChanged(State.PROVIDER_AVAILABLE, provider);
					break;
				case LocationProvider.OUT_OF_SERVICE: 
					LLog.d(Globals.TAG, CLASSTAG + ": Status provider out of service");
					atStateChanged(State.PROVIDER_OUT_OF_SERVICE, provider);
					break;
				case LocationProvider.TEMPORARILY_UNAVAILABLE: 
					LLog.d(Globals.TAG, CLASSTAG + ": Status provider temporarily unavailable");
					atStateChanged(State.PROVIDER_TEMPORARILY_UNAVAILABLE, provider); 
					break;
				}

				if (extras != null && extras.containsKey("satellites")) {
					//					LLog.d(Globals.TAG, CLASSTAG + ": Satellites: " + extras.getInt("satellites"));
					atStateChanged(State.SATELLITES_IN_VIEW, extras.getInt("satellites"));
					fireOnDeviceValueChanged(ValueType.SATELLITES, extras.getInt("satellites"), System.currentTimeMillis());
				}
			}
		};

		private int prevSatellites = 0;
		private boolean logNmea = false;

		private void parseNmea(String nmea) {
			String[] parts = nmea.split(",");
			if (parts != null && parts.length > 0) {
				Nmea nmeaType = Nmea.fromString(parts[0]);
				if (nmeaType != null) switch(nmeaType) {
				case GPGSV:
					if (parts.length > 4) {
						//LLog.d(Globals.TAG, CLASSTAG + "parseNmea: Satellites raw: " + parts[3]);
						try {
							int satellites = Util.parseInt(parts[3]);
							if (satellites > 0 && satellites != prevSatellites) {
								if (logNmea) LLog.d(Globals.TAG, CLASSTAG + "parseNmea: Satellites: " + satellites );
								prevSatellites = satellites;
								//allowFire = true;
								fireOnDeviceValueChanged(ValueType.SATELLITES, -satellites, System.currentTimeMillis());
							}
						} catch (NumberFormatException e) {

						}
					}
					break;
				case GPGGA:
					if (parts.length > 7 && parts[1] != null && parts[1].length() > 0) {
						String parse = TimeUtil.formatTimeUTC(System.currentTimeMillis(), "yyyy-MM-dd") + " " + parts[1] + "00";
						long time = TimeUtil.parseTimeUTC(parse, "yyyy-MM-dd HHmmss.S");
						double lat = 0;
						if (parts[2].length() > 3) lat = Util.parseInt(parts[2].substring(0, 2)) + Util.parseDouble(parts[2].substring(2)) / 60;
						if (parts[3].equals("S")) lat *= -1;
						double lng = 0;
						if (parts[4].length() > 3) lng = Util.parseInt(parts[4].substring(0, 3)) + Util.parseDouble(parts[4].substring(3)) / 60;
						if (parts[5].equals("W")) lng *= -1;
						int satellites = Util.parseInt(parts[7]);
						double hdop = 0;
						if (parts[8].length() > 0) hdop = + Util.parseDouble(parts[8]);
						double alt = 0;
						if (parts[9].length() > 0) alt = + Util.parseDouble(parts[9]);
						double geoid = 0;
						if (parts[11].length() > 0) geoid = + Util.parseDouble(parts[11]);

						if (lat != 0 && lng != 0) {
							if (logNmea) LLog.d(Globals.TAG, CLASSTAG + ".parseNmea: Time = " + TimeUtil.formatMilli(time, 3) + 
									", loc = " + Format.format(lat, 4) + ", "  + Format.format(lng, 4) + 
									", hdop = " + Format.format(hdop, 1) +
									", alt = " + Format.format(alt, 1) +
									", geoid = " + Format.format(geoid, 1) +
									", satellites = " + satellites);
							fireOnDeviceValueChanged(ValueType.SATELLITES, satellites, System.currentTimeMillis());
						}
						//if (allowFire) fireTime(time);
					}
					break;
				default:
					break;

				}
			}

		}

		private NmeaListener nmeaListener =  new NmeaListener() {

			@Override
			public void onNmeaReceived(long timestamp, String nmea) {
				if (logNmea) LLog.d(Globals.TAG,  CLASSTAG + ".nmea = " + nmea);
				parseNmea(nmea);
			}

		};



		private final GpsStatus.Listener gpsStatusListener = new GpsStatus.Listener () {
			@Override
			public void onGpsStatusChanged(int event) {
				GpsStatus status;
				switch (event) {
				case GpsStatus.GPS_EVENT_STARTED:
					//LLog.d(Globals.TAG, CLASSTAG + ": GPS Started");
					break;
				case GpsStatus.GPS_EVENT_STOPPED:
					//LLog.d(Globals.TAG, CLASSTAG + ": GPS Stopped");
					break;
				case GpsStatus.GPS_EVENT_FIRST_FIX: 
					status = locationMgr.getGpsStatus(null);
					LLog.d(Globals.TAG, CLASSTAG + ": First fix");
					ParcelGpsStatus parcel = new ParcelGpsStatus(event, status);
					//LLog.d(Globals.TAG, CLASSTAG + ": Satellite status " + parcel.toString());
					if (parcel != null && parcel.getSatellites() != null) fireOnDeviceValueChanged(ValueType.SATELLITES, parcel.getSatellites().length, System.currentTimeMillis());
					//broadcast(new ParcelGpsStatus(event, status));
					break;
				case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
					status = locationMgr.getGpsStatus(null);
					parcel = new ParcelGpsStatus(event, status);
					if (parcel == null || parcel.getSatellites() == null) return;
					//LLog.d(Globals.TAG, CLASSTAG + ": Satellite status " + parcel.toString());
					fireOnDeviceValueChanged(ValueType.SATELLITES, parcel.getSatellites().length, System.currentTimeMillis());
					//broadcast(new ParcelGpsStatus(event, status));
					break;
				}
			}
		};

		private GpsLocationListener listener = new GpsLocationListener();

		@Override
		public void closeIt() {
			if (opened && locationMgr != null) {

				locationMgr.removeGpsStatusListener(gpsStatusListener);
				locationMgr.removeUpdates(listener);
				locationMgr.removeNmeaListener(nmeaListener);

			}
			active = false;
		}

		@Override
		public void openIt(boolean reOpen) {
			if (active) {
				closeIt();
			}
			if (PreferenceKey.GPSON.isTrue()) {
				locationMgr.requestLocationUpdates( 
						LocationManager.GPS_PROVIDER, 
						PreferenceKey.GPSINTERVAL.getInt(),	// minTime in ms 
						PreferenceKey.GPSMINDISTANCE.getInt(),	// minDistance in meters 
						listener
						);
				locationMgr.addNmeaListener(nmeaListener);
				locationMgr.addGpsStatusListener(gpsStatusListener);
			} else
				locationMgr.requestLocationUpdates( 
						LocationManager.NETWORK_PROVIDER, 
						PreferenceKey.GPSINTERVAL.getInt(),	// minTime in ms 
						PreferenceKey.GPSMINDISTANCE.getInt(),	// minDistance in meters 
						listener
						);
			active = true;
			opened = true;
		}

		public Location getLastLocation() {
			if (listener != null && listener.prevLocation != null) return listener.prevLocation;
			Location loc = locationMgr.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			if (loc == null) 
				loc = locationMgr.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			if (loc == null) {
				loc = new Location(LocationManager.GPS_PROVIDER);
				loc.setLatitude(PreferenceKey.LASTLOCATION_LAT.getDouble());
				loc.setLongitude(PreferenceKey.LASTLOCATION_LONG.getDouble());
			}
			return loc;
			//return null;
		}

		@Override
		public void fadeOut(ValueType type) {
			switch(type) {
			case SPEED:
				if (speed != Double.NaN && speed > 0.2) {

					speed *= Device.FADEOUT_FACTOR;
					if (speed < 0.3) speed = 0;
					fireOnDeviceValueChanged(ValueType.SPEED, speed, System.currentTimeMillis(), false);
				}
				break;
			default:
				break;

			}
		}
	}

	public GpsManager() { //, Callbacks callbacks) {
		gpsDevice = new GpsDevice(Globals.appContext);
		//Device.addDevice(gpsDevice);
	}

	@Override 
	public boolean init() {
		gpsDevice.openIt(true);
		return true;
	}

	@Override
	public void destroy() {
		if (gpsDevice == null) return;
		Location loc = gpsDevice.getLastLocation();
		if (loc != null) {
			PreferenceKey.LASTLOCATION_LAT.set(loc.getLatitude());
			PreferenceKey.LASTLOCATION_LONG.set(loc.getLongitude());
		} 
		gpsDevice.destroy();
	}

	public Location getLastLocation() {
		return gpsDevice.getLastLocation();
	}

	@Override
	public void onStart() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onStop() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onResume() {
		gpsDevice.openIt(true);
	}

	
//	@Override
//	public EnumSet<DeviceType> supportedDevices() {
//		return EnumSet.of(DeviceType.GPS_SENSOR);
//	}

}
