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
package com.plusot.senselib.sensors;

import java.util.HashMap;
import java.util.Map;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.BatteryManager;

import com.plusot.common.Globals;
import com.plusot.common.share.LLog;
import com.plusot.common.share.SimpleLog;
import com.plusot.common.share.SimpleLogType;
import com.plusot.common.util.ToastHelper;
import com.plusot.javacommon.util.Format;
import com.plusot.javacommon.util.MathVector;
import com.plusot.javacommon.util.Matrix;
import com.plusot.javacommon.util.TimeUtil;
import com.plusot.senselib.SenseGlobals;
import com.plusot.senselib.settings.PreferenceKey;
import com.plusot.senselib.values.Device;
import com.plusot.senselib.values.DeviceType;
import com.plusot.senselib.values.Manager;
import com.plusot.senselib.values.ValueType;

public class AndroidSensorManager implements Manager{
	private static final String CLASSTAG = AndroidSensorManager.class.getSimpleName();
	private double slope = 0;
	private double bearing = 0;

	private SensorManager sensorManager = null;
	private Map<DeviceType, Device> devices = new HashMap<DeviceType, Device>();
	public static double batteryUsage = 0;



	public class AccelerometerDevice extends Device implements SensorEventListener {
		private final Sensor sensor;
		private static final double GRAVITY = 9.81f;
		private static final double FILTER = 0.99; //was oorspronkelijk 0.8
		private MathVector refGravity = new MathVector(0.0, 0.0, 1.0);
		private MathVector gravity = new MathVector(0.0, 0.0, GRAVITY);
		private MathVector acceleration = new MathVector(0.0, 0.0, 0.0);
		private long lastGravityFired = 0;
		//private long lastAccelerationFired = 0;
		private float[] lastEvent = null;

		public AccelerometerDevice(Context context) {
			super(DeviceType.ACCELEROMETER);
			sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		}

		@Override
		public boolean isActive() {
			return true;
		}

		@Override
		public void closeIt() {
			if (opened && sensorManager != null) sensorManager.unregisterListener(this);
			opened = false;
			//LLog.d(Globals.TAG, CLASSTAG + ": Unregistering listener for " + getName());
		}

		@Override
		public void openIt(boolean reOpen) {
			if (!opened && sensorManager != null) {
				sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL); //.SENSOR_DELAY_GAME); //.SENSOR_DELAY_FASTEST);
				opened = true;
			}
		}

		@Override
		public void onSensorChanged(SensorEvent event) {
			if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;
			lastEvent = event.values.clone();
			//LLog.d(Globals.TAG, CLASSTAG + ".onSensorChanged: acceleration: " + StringUtil.toString(lastEvent));

			calcAngles(true);
		}

		private void calcAngles(boolean updateLastUpdate) {
			if (lastEvent == null) return;
			long now = System.currentTimeMillis();
			acceleration.assign(lastEvent);
			//LLog.d(Globals.TAG, CLASSTAG + ".calcAngles: acceleration: " + StringUtil.toString(lastEvent));
			gravity.timestimes(FILTER);
			gravity.plusplus(acceleration.times(1 - FILTER));

			//acceleration.minmin(gravity);

			MathVector unitVector = gravity.unitVector();
			MathVector delta = unitVector.min(refGravity);

			double angle = 2 * Math.asin(delta.length() / 2);
			if (delta.getAbsValue(0) > delta.getAbsValue(1) && delta.getValue(0) > 0) angle *= -1;
			if (delta.getAbsValue(1) > delta.getAbsValue(0) && delta.getValue(1) < 0) angle *= -1;

			//if (acceleration.deltaLength() > 0.1 && now - lastAccelerationFired > 40) {
			fireOnDeviceValueChanged(ValueType.ACCELERATION, acceleration.length(), now, updateLastUpdate);
			//LLog.v(Globals.TAG, CLASSTAG + ".acceleration = " + acceleration.length());

			if (SenseGlobals.activity.equals(SenseGlobals.ActivityMode.RUN) && SenseGlobals.accelerometerTesting) {
				Map<String, String> map = new HashMap<String, String>();
				map.put("time", "\"" + TimeUtil.formatMilli(now) +  "\"");
				map.put("acc", Format.format2(acceleration.length()));
				map.put("acc3d", 
						"[" + 
								Format.format2(acceleration.getValue(0)) + "," +
								Format.format2(acceleration.getValue(1)) + "," +
								Format.format2(acceleration.getValue(2)) + "]"
						);

				SimpleLog.getInstance(SimpleLogType.JSON, "acc").log(map);
			}

			//	acceleration.setReference();
			//	lastAccelerationFired = now;

			//}
			if (gravity.deltaLength() > 0.1 && now - lastGravityFired > 40) {
				//fireOnDeviceValueChanged(ValueType.GRAVITY, gravity.getValues(), now);
				fireOnDeviceValueChanged(ValueType.SLOPE, Math.min(Math.tan(angle), 10), now);
				slope = angle;
				gravity.setReference();
				lastGravityFired = now;

			}
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {

		}

		public void setLevel() {
			refGravity = gravity.unitVector();
		}

		@Override
		public void fadeOut(ValueType type) {
			switch (type) {
			case SLOPE:
				calcAngles(false);
				break;
			default:
				break;
			}
		}
	}

	/*public class OrientationDevice extends Device implements SensorEventListener {
		private final Sensor sensor;

		public OrientationDevice(Context context) {
			super(DeviceType.ORIENTATION_SENSOR, EnumSet.of(ValueType.BEARING));
			sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
		}

		@Override
		public boolean isActive() {
			return true;
		}

		@Override
		public void close() {
			sensorManager.unregisterListener(this);
			LLog.d(Globals.TAG, CLASSTAG + ": Unregistering listener for " + getName());
		}

		@Override
		public void open(boolean reOpen) {
			sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
		}

		@Override
		public void onSensorChanged(SensorEvent event) {
			if (event.sensor.getType() != Sensor.TYPE_ORIENTATION) return;

			//fireOnDeviceValueChanged(ValueType.ORIENTATION, event.values.clone(), System.currentTimeMillis());
			fireOnDeviceValueChanged(ValueType.BEARING, event.values[0], System.currentTimeMillis());
			bearing = event.values[0] * Math.PI / 180.0;
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {

		}

		@Override
		public void fadeOut() {
			// TODO Auto-generated method stub
		}
	}*/

	public class ProximityDevice extends Device implements SensorEventListener {
		private final Sensor sensor;

		public ProximityDevice(Context context) {
			super(DeviceType.PROXIMITY_SENSOR);
			sensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
		}

		@Override
		public boolean isActive() {
			return true;
		}

		@Override
		public void closeIt() {
			if (opened && sensorManager != null) sensorManager.unregisterListener(this);
			opened = false;
			LLog.d(Globals.TAG, CLASSTAG + ": Unregistering listener for " + getName());
		}

		@Override
		public void openIt(boolean reOpen) {
			if (!opened && sensorManager != null) {
				sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
				opened = true;
			}
		}

		@Override
		public void onSensorChanged(SensorEvent event) {
			if (event.sensor.getType() != Sensor.TYPE_PROXIMITY) return;

			//LLog.d(Globals.TAG, CLASSTAG + ".ProximityDevice.onSensorChanged: " + event.values[0]);
			fireOnDeviceValueChanged(ValueType.PROXIMITY, Double.valueOf(0.01 * event.values[0]), System.currentTimeMillis());
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {

		}

		@Override
		public void fadeOut(ValueType type) {
		}
	}

	public class AirPressureDevice extends Device implements SensorEventListener {
		private final Sensor sensor;
		private double altitude = Double.MAX_VALUE;
		private double prevDistance = Double.MAX_VALUE;
		private long timestamp = 0; 

		public AirPressureDevice(Context context) {
			super(DeviceType.AIRPRESSURE_SENSOR);
			sensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
			LLog.d(Globals.TAG, CLASSTAG + ".AirPressureDevice.constructor");

		}

		@Override
		public boolean isActive() {
			return true;
		}

		@Override
		public void closeIt() {
			if (opened && sensorManager != null) sensorManager.unregisterListener(this);
			opened = false;
			LLog.d(Globals.TAG, CLASSTAG + ": Unregistering listener for " + getName());
		}

		@Override
		public void openIt(boolean reOpen) {
			LLog.d(Globals.TAG, CLASSTAG + ".AirPressureDevice.open with " + sensor);
			if (!opened && sensor != null && sensorManager != null) {
				sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
				opened = true;
			}
		}

		@Override
		public void onSensorChanged(SensorEvent event) {
			if (event.sensor.getType() != Sensor.TYPE_PRESSURE) return;

			long now = System.currentTimeMillis();
			double pressure = 100.0 * event.values[0];
			fireOnDeviceValueChanged(ValueType.AIRPRESSURE, pressure, now);

			//P = Actual pressure (Pascal)
			//Po = 101325	sea level standard pressure, Pa
			//To = 288.15   sea level standard temperature, deg K
			//g = 9.80665   gravitational constant, m/sec2
			//L =  6.5    	temperature lapse rate, deg K/km
			//R = 8.31432	gas constant, J/ mol*deg K 
			//M = 28.9644	molecular weight of dry air, gm/mol
			//H = To / L * (1 - Power{ (P / Po) , ((L * R ) / (g * M)) } )

			double newAltitude = 1000.0 * (44.3308 - 4.94654 * Math.pow(pressure, 0.190263));
			SenseGlobals.lastAltitude = newAltitude;
			SenseGlobals.lastAltitudeTime = now;
			//LLog.d(Globals.TAG, CLASSTAG + "AirPressureSensor.onSensorChanged: Pressure = " + pressure + ", Alt = " + newAltitude);
			if (PreferenceKey.ALTOFFSET.getInt() == Integer.MIN_VALUE && SenseGlobals.lastLocation != null && now - SenseGlobals.lastLocationTime < 60000) {
				LLog.d(Globals.TAG, CLASSTAG + ".AirPressureDevice.onSensorChanged: Set height offset " + (int)(PreferenceKey.GPSALTOFFSET.getInt() + SenseGlobals.lastLocation.getAltitude() - newAltitude));
				PreferenceKey.ALTOFFSET.set((int)(PreferenceKey.GPSALTOFFSET.getInt() + SenseGlobals.lastLocation.getAltitude() - newAltitude));
			} else 
				PreferenceKey.checkAltOffsets(CLASSTAG);
			if (PreferenceKey.ALTOFFSET.getInt() != Integer.MIN_VALUE && PreferenceKey.BARO_ADJUST.isTrue()) 
				fireOnDeviceValueChanged(ValueType.ALTITUDE, (double)(newAltitude + PreferenceKey.ALTOFFSET.getInt()), now);

			if ( timestamp > 0 && altitude != Double.MAX_VALUE && now - timestamp > 10000) {
				fireOnDeviceValueChanged(ValueType.VERTSPEED, 1000 * (newAltitude - altitude) / (now - timestamp), now);
				timestamp = now;
				altitude = newAltitude;
				double distance = ValueType.DISTANCE.getRaw();
				if (distance - prevDistance > 0 ) {
					fireOnDeviceValueChanged(ValueType.SLOPE, (newAltitude - altitude) / (distance - prevDistance), now);
				}
				prevDistance = ValueType.DISTANCE.getRaw();
			} else if (timestamp == 0 || altitude == Double.MAX_VALUE) {
				timestamp = now;
				altitude = newAltitude;
				prevDistance = ValueType.DISTANCE.getRaw();
			}
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
		}

		@Override
		public void fadeOut(ValueType type)  {
			// TODO Auto-generated method stub
		}
	}

	public class BatteryDevice extends Device  {
		private double batteryPercentage = 0;
		//private int reference = 0;
		private Context context = null;
		private boolean saved = false;
		public final static int BATTERYLOW = -1;
		public final static int BATTERYNORMAL = 0;
		private double startPercentage = -1;
		private long startTime;

		private BroadcastReceiver batteryReceiver = new BroadcastReceiver(){
			@Override
			public void onReceive(Context context, Intent intent) {
				int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
				int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 0);
				//if (intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0) return;
				//LLog.d(Globals.TAG, CLASSTAG + ".BatteryDevice.onReceive: " + level);	


				//				Bundle bundle = intent.getExtras();
				//				for (String key : bundle.keySet()) {
				//					LLog.d(Globals.TAG, CLASSTAG + "Battery info for " + key + " = " + bundle.get(key));
				//				}
				if (batteryPercentage != 100.0 * level / scale) {
					batteryPercentage = 100.0 * level / scale;
					if (startPercentage == -1) {
						startPercentage = batteryPercentage;
						startTime = System.currentTimeMillis();
					} else {
						batteryUsage = 3600000 * (startPercentage - batteryPercentage) / (System.currentTimeMillis() - startTime); // %/hr
					}

					fireOnDeviceValueChanged(ValueType.VOLTAGE, (Double)(0.01 * batteryPercentage), System.currentTimeMillis());
					
					//if ((int)(batteryPercentage / 10) != reference) {
					//	reference = (int)(batteryPercentage / 10);
					if (Globals.testing.isTest()) {
						LLog.d(Globals.TAG, CLASSTAG + ".BatteryDevice.onReceive: " + batteryPercentage + "%, usage: " + Format.format(batteryUsage, 2) + " %/hr");	
						//ToastHelper.showToastLong("Battery " + batteryPercentage + "%\nusage: " + Format.format(batteryUsage, 2) + " %/hr");


						fireOnDeviceValueChanged(ValueType.VOLTAGE, batteryPercentage, System.currentTimeMillis());
					}
					if (SenseGlobals.batteryTesting) {
						Map<String, String> map = new HashMap<String, String>();
						map.put("time", "\"" + TimeUtil.formatMilli(System.currentTimeMillis()) +  "\"");
						map.put("batt", Format.format2(batteryPercentage));
						map.put("usage", Format.format2(batteryUsage));

						SimpleLog.getInstance(SimpleLogType.JSON, "batt").log(map);

					}

					//}
					if (batteryPercentage < 4 && !saved) {
						saved = true;
						fireOnDeviceValueChanged(ValueType.VOLTAGE, (Double)(0.01 * batteryPercentage), System.currentTimeMillis());
						//new ZipFiles().zip(false);
						//SenseApp.zipFile(null, false);
						ToastHelper.showToastLong("Battery " + batteryPercentage + "%. Saving log files.");
					} 
					//fireOnDeviceValueChanged(ValueType.VOLTAGE, (Double)batteryPercentage, System.currentTimeMillis());
				}
			}
		};

		public BatteryDevice(Context context) {
			super(DeviceType.BATTERY_SENSOR);
			this.context = context;	
		}

		@Override
		public boolean isActive() {
			return true;
		}

		@Override
		public void closeIt() {
			if (opened) {
				context.unregisterReceiver(batteryReceiver);
				opened = false;
			}
			LLog.d(Globals.TAG, CLASSTAG + ": Unregistering listener for " + getName());
		}

		@Override
		public void openIt(boolean reOpen) {
			IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
			if (!opened) {
				context.registerReceiver(batteryReceiver, filter);
				opened = true;
			}
		}

		@Override
		public void fadeOut(ValueType type) {
			// TODO Auto-generated method stub
		}
	}

	public class MagneticDevice extends Device implements SensorEventListener {
		private Sensor sensor;
		private double slopeM = 0;
		private double slopeRef = 0;

		public MagneticDevice(Context context) {
			super(DeviceType.MAGNETICFIELD_SENSOR);
			if (sensorManager != null) sensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		}

		@Override
		public boolean isActive() {
			return true;
		}

		@Override
		public void closeIt() {
			if (opened && sensorManager != null) sensorManager.unregisterListener(this);
			opened = false;
			sensor = null;
			LLog.d(Globals.TAG, CLASSTAG + ": Unregistering listener for " + getName());
		}

		@Override
		public void openIt(boolean reOpen) {
			if (sensor == null) {
				sensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
			}
			if (!opened && sensorManager != null)  {
				sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
				opened = true;
			}
		}

		@Override
		public void onSensorChanged(SensorEvent event) {
			if (event.sensor.getType() != Sensor.TYPE_MAGNETIC_FIELD) return;

			if (SenseGlobals.lastLocation != null) {
				long now =  System.currentTimeMillis();
				/* 
				 * Geomagnetic field:
				 * x = north
				 * y = east
				 * z = to earth center
				 *
				 * Measured sensor event
				 * x = to right of device
				 * y = to top of device
				 * z = point upward from screen
				 */

				GeomagneticField field = new GeomagneticField((float)SenseGlobals.lastLocation.getLatitude(), (float) SenseGlobals.lastLocation.getLongitude(), (float) SenseGlobals.lastLocation.getAltitude(), now);

				MathVector earth3D = new MathVector(field.getY(), field.getX(), -field.getZ()).unitVector();
				MathVector measured3D = new MathVector(event.values, 3).unitVector();

				Matrix m = new Matrix(3);
				m.setRotationXY(-bearing);
				MathVector v =  m.product(measured3D);
				//Vector normal = v.crossProduct(earth3D);

				slopeM = Math.acos(v.dotProduct(earth3D));
				//LLog.d(Globals.TAG, CLASSTAG + ".MagneticDevice.onSensorChanged: ---------------- Start -----------------");
				//LLog.d(Globals.TAG, CLASSTAG + ".MagneticDevice.onSensorChanged: earth3D = " + earth3D);
				//LLog.d(Globals.TAG, CLASSTAG + ".MagneticDevice.onSensorChanged: measured3D = " + measured3D);
				//LLog.d(Globals.TAG, CLASSTAG + ".MagneticDevice.onSensorChanged: rotated = " + v + " by " + -bearing * 180.0 / Math.PI);
				//LLog.d(Globals.TAG, CLASSTAG + ".MagneticDevice.onSensorChanged: normal = " + normal);

				if (Math.abs(slope - slopeM) > 3)
					LLog.d(Globals.TAG, CLASSTAG + ".MagneticDevice.onSensorChanged: magnetic vs gravity slope = " + Format.format(slopeM * 180 / Math.PI) + " - " + Format.format(slope * 180.0 / Math.PI));

				fireOnDeviceValueChanged(ValueType.SLOPE, Math.min(Math.tan(slopeM - slopeRef), 2), now);
			}
			//fireOnDeviceValueChanged(ValueType.MAGNETICFIELD, event.values.clone(), System.currentTimeMillis());
		}

		public void setLevel() {
			slopeRef = slopeM;
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {

		}

		@Override
		public void fadeOut(ValueType type) {
			// TODO Auto-generated method stub	
		}
	}

	public AndroidSensorManager() {
		sensorManager = (SensorManager)Globals.appContext.getSystemService(Context.SENSOR_SERVICE);
		LLog.d(Globals.TAG, CLASSTAG + ".constructor");

		//Device device = null;
		//if ((device = 
		getDevice(Globals.appContext, DeviceType.AIRPRESSURE_SENSOR); //) != null) Device.addDevice(device);
		//if((device = 
		getDevice(Globals.appContext, DeviceType.BATTERY_SENSOR); //) != null) Device.addDevice(device);
		//if ((device = 
		getDevice(Globals.appContext, DeviceType.PROXIMITY_SENSOR); //) != null) Device.addDevice(device);
		if (PreferenceKey.XTRAVALUES.isTrue() && Globals.testing.isTest()) {
			//if ((device = 
			getDevice(Globals.appContext, DeviceType.ACCELEROMETER); //) != null) Device.addDevice(device);
			//if ((device = 
			getDevice(Globals.appContext, DeviceType.MAGNETICFIELD_SENSOR); //) != null) Device.addDevice(device);
		}
	}

	@Override
	public boolean init() {
		if (sensorManager == null) return false;
		LLog.d(Globals.TAG, CLASSTAG + ".start");
		for (Device device : devices.values()) device.openIt(true);
		return true;
	}

	@Override
	public void destroy() {
		LLog.d(Globals.TAG, CLASSTAG + ": Closing AndroidSensorManager");
		for (Device device : devices.values()) device.destroy();
		devices.clear();
		sensorManager = null;
	}

	public void setLevel() {
		AccelerometerDevice acc = ((AccelerometerDevice)devices.get(DeviceType.ACCELEROMETER));
		if (acc != null) acc.setLevel();
		MagneticDevice mag = ((MagneticDevice) devices.get(DeviceType.MAGNETICFIELD_SENSOR));
		if (mag != null) mag.setLevel();
	}

	private Device getDevice(Context context, DeviceType deviceType) {
		Device device = devices.get(deviceType);
		if (device == null) {
			switch (deviceType) {
			case ACCELEROMETER:	device = new AccelerometerDevice(context); break;
			//case ORIENTATION_SENSOR: device = new OrientationDevice(context); break;
			case MAGNETICFIELD_SENSOR: device = new MagneticDevice(context); break;
			case AIRPRESSURE_SENSOR: device = new AirPressureDevice(context); break;
			case BATTERY_SENSOR: device = new BatteryDevice(context); break;
			case PROXIMITY_SENSOR: device = new ProximityDevice(context); break;
			default: break;
			}
			devices.put(deviceType, device);
		}
		return device;
	}

//	@Override
//	public EnumSet<DeviceType> supportedDevices() {
//		if (PreferenceKey.XTRAVALUES.isTrue() && Globals.testing.isTest())
//			return EnumSet.of(DeviceType.ACCELEROMETER, DeviceType.MAGNETICFIELD_SENSOR, DeviceType.AIRPRESSURE_SENSOR, DeviceType.BATTERY_SENSOR, DeviceType.PROXIMITY_SENSOR);
//		return EnumSet.of(DeviceType.AIRPRESSURE_SENSOR, DeviceType.BATTERY_SENSOR, DeviceType.PROXIMITY_SENSOR);
//	}

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
		// TODO Auto-generated method stub

	}

}
