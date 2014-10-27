package com.plusot.senselib.bluetooth;


import java.util.HashMap;
import java.util.Map;

import android.bluetooth.BluetoothSocket;

import com.plusot.common.Globals;
import com.plusot.common.share.LLog;
import com.plusot.javacommon.util.StringUtil;
import com.plusot.senselib.ant.DeviceManufacturer;
import com.plusot.senselib.values.DeviceType;
import com.plusot.senselib.values.Unit;
import com.plusot.senselib.values.ValueType;

public class KestrelDevice extends BTDevice {
	private final static String CLASSTAG = KestrelDevice.class.getSimpleName();
	private final String WIND = "WS";
	private final String TEMPERATURE = "TP";
	private final String HUMIDITY = "RH";
	private final String PRESSURE = "BP";

	private boolean infoRead = false;
	private boolean unitRead = false;
	private StringBuilder info = new StringBuilder();
	private StringBuilder units = new StringBuilder();
	
	private Map<String, String> unitMap = new HashMap<String, String>();
	private WindData windData = new WindData();
	private Double windSpeed = 0.0;
	

	private class WindData {
		String speed = null;
		String humidity = null;
		String pressure = null;
		String lastMsgId = null;
		String temperature = null;
		String units1 = null;
		String units2 = null;
		String bearing = null;
	}

	public KestrelDevice(final String deviceName) { //, SleepAndWake.Listener terminateListener) {
		super(DeviceType.WIND_SENSOR, deviceName);	
		this.manufacturer = DeviceManufacturer.KESTREL;
		this.productId = "4000BT";
	}

	private Unit getUnit(ValueType type, String id) {
		String unit = unitMap.get(id);
		if (unit != null) {
			if (unit.equalsIgnoreCase("kt"))
				return Unit.KNOT;
			else if (unit.equalsIgnoreCase("mph"))
				return Unit.MPH;
			else if (unit.equalsIgnoreCase("fpm"))
				return Unit.FPM;
			else if (unit.equalsIgnoreCase("Bft"))
				return Unit.BEAUFORT;
			else if (unit.equalsIgnoreCase("m/s"))
				return Unit.MPS;
			else if (unit.equalsIgnoreCase("km/h"))
				return Unit.KPH;
			else if (unit.equalsIgnoreCase("F"))
				return Unit.FAHRENHEIT;
			else if (unit.equalsIgnoreCase("C"))
				return Unit.CELCIUS;
			else if (unit.equalsIgnoreCase("inHg"))
				return Unit.INHG;
			else if (unit.equalsIgnoreCase("hPa"))
				return Unit.HPA;
			else if (unit.equalsIgnoreCase("psi"))
				return Unit.PSI;
			else if (unit.equalsIgnoreCase("mb"))
				return Unit.MBAR;
			else if (unit.equalsIgnoreCase("m"))
				return Unit.METER;
			else if (unit.equalsIgnoreCase("ft"))
				return Unit.FEET;
		}
		return type.defaultUnit();
	}

	public String toText(String delim) {
		StringBuilder tv = new StringBuilder();
		tv.append("Bluetooth id = " + deviceName + "\r"); // + ": \r" + StringUtil.trimAll(msg) + "\r");
		if (revision != null) tv.append("Model = " + revision + delim);
		if (serialNumber != null) tv.append("Serial nr = " + serialNumber + delim);
		if (softwareVersion != null) tv.append("Software = " + softwareVersion + delim);
		if (battery != null) tv.append("Battery = " + battery + delim);
		//if (windData.units1 != null) tv.append("Units = " + '\r' + windData.units1 + "\r");
		//if (windData.units2 != null) tv.append(Util.toReadableString(windData.units2, true) + "\r");
		tv.append("Wind = " + windData.speed + getUnit(ValueType.WINDSPEED, WIND).getLabel(Globals.appContext) + delim);
		tv.append("Temperature = " + windData.temperature + getUnit(ValueType.TEMPERATURE, TEMPERATURE).getLabel(Globals.appContext) + delim);
		tv.append("Humidity = " + windData.humidity + getUnit(ValueType.HUMIDITY, HUMIDITY).getLabel(Globals.appContext) + delim);
		tv.append("Pressure = " + windData.pressure + getUnit(ValueType.AIRPRESSURE, PRESSURE).getLabel(Globals.appContext) + delim);
		if (windData.bearing != null) tv.append("Winddirection = " + windData.bearing + Unit.DEGREE.getLabel(Globals.appContext) + delim);
		tv.append("MessageId = " + windData.lastMsgId);
		return tv.toString();
	}

	@Override
	public String getRawValue() {
		return windData.speed;
	}

	@Override
	public void onBluetoothRead(BTConnectedThread sender, String msg) {
		active = System.currentTimeMillis();
		//			LLog.d(Globals.TAG, CLASSTAG + ".onBluetoothRead: " + msg);
		for (String msgPart: msg.replaceAll("> ", "").split("\r\n")) {
			if (msgPart.contains("DT,")) {
				//LLog.d(Globals.TAG, CLASSTAG + ".onBluetoothRead: Start of unit message: "+ StringUtil.trimAll(msgPart));
				unitRead = true;
				units.setLength(0);
				units.append(msgPart + '\r');
				windData.units1 = StringUtil.trimAll(msgPart);
			} else if (msgPart.contains("Iss")) {
				//LLog.d(Globals.TAG, CLASSTAG + ".onBluetoothRead: Software message: "+ StringUtil.trimAll(msgPart));
				infoRead = true;
				info.setLength(0);
				info.append(msgPart + '\r');
				softwareVersion = StringUtil.trimAll(msgPart.replace("Iss", ""));
			} else if (msgPart.contains("S/N:")) {
				//LLog.d(Globals.TAG, CLASSTAG + ".onBluetoothRead: Serial number message: "+ StringUtil.trimAll(msgPart));
				info.append(msgPart + '\r');
				serialNumber = StringUtil.trimAll(msgPart.replace("S/N:", ""));
			} else if (msgPart.contains("Battery:")) {
				//LLog.d(Globals.TAG, CLASSTAG + ".onBluetoothRead: Battery level message: "+ StringUtil.trimAll(msgPart));
				info.append(msgPart + '\r');
				battery = StringUtil.trimAll(msgPart.replace("Battery:", ""));
				if (battery != null && battery.contains("-")) battery = battery.split("-")[0];
			} else if (msgPart.startsWith("K4")) {
				//LLog.d(Globals.TAG, CLASSTAG + ".onBluetoothRead: Device model message: "+ StringUtil.trimAll(msgPart));
				info.append(msgPart + '\r');
				revision = StringUtil.trimAll(msgPart);

			} else if (msgPart.contains("DCOCTL")) {
				//LLog.d(Globals.TAG, CLASSTAG + ".onBluetoothRead: End of units message: "+ StringUtil.trimAll(msgPart));
				infoRead = false;
				info.append(msgPart + '\r');
				//Toast.makeText(this, info.toString(), Toast.LENGTH_LONG).show();
				//Alert(info.toString());
			} else if (infoRead) {
				//LLog.d(Globals.TAG, CLASSTAG + ".onBluetoothRead: Info message part: "+ StringUtil.trimAll(msgPart));

				if (msgPart.length() > 3) info.append(msgPart + '\r');
			} else if (unitRead && msgPart.length() > 3) {
				//LLog.d(Globals.TAG, CLASSTAG + ".onBluetoothRead: Unit message part: "+ StringUtil.trimAll(msgPart));

				units.append(msgPart + '\r');
				windData.units2 = StringUtil.trimAll(msgPart);
				unitRead = false;
				//Toast.makeText(this, units.toString(), Toast.LENGTH_LONG).show();
				if (windData.units1 == null || windData.units2 == null) continue;
				String parts[] = windData.units2.split(",");
				String unitParts[] = windData.units1.split(",");
				for (int i = 0; i < Math.min(parts.length, unitParts.length); i++) {
					unitMap.put(unitParts[i], parts[i]);
				}
			} else {
				//LLog.d(Globals.TAG, CLASSTAG + ".onBluetoothRead: Data message: "+ StringUtil.trimAll(msgPart));

				String[] parts = msgPart.split(",");
				if (parts.length >= 5 && parts.length <= 6) try {
					windData.lastMsgId = StringUtil.trimAll(parts[0]);
					windData.speed = StringUtil.trimAll(parts[1]);
					windData.temperature = StringUtil.trimAll(parts[2]);
					windData.humidity = StringUtil.trimAll(parts[3]);
					windData.pressure = StringUtil.trimAll(parts[4]);
					if (parts.length >= 6) windData.bearing = StringUtil.trimAll(parts[5]); else windData.bearing = null;

//					LLog.d(Globals.TAG, CLASSTAG + ".onBluetoothRead: " + msg); //toText(", "));

					long now =  System.currentTimeMillis();
					//ValueItem<WindData> value = new ValueItem<WindData>(windData, now); 
					windSpeed = (Double) StringUtil.toObject(windData.speed, Double.class, getUnit(ValueType.WINDSPEED, WIND).getScaler());
					fireOnDeviceValueChanged(ValueType.WINDSPEED, windSpeed, now);
					fireOnDeviceValueChanged(ValueType.TEMPERATURE, StringUtil.toObject(windData.temperature, Double.class, getUnit(ValueType.TEMPERATURE, TEMPERATURE).getScaler()), now);
					fireOnDeviceValueChanged(ValueType.AIRPRESSURE, StringUtil.toObject(windData.pressure, Double.class, getUnit(ValueType.AIRPRESSURE, PRESSURE).getScaler()), now);
					fireOnDeviceValueChanged(ValueType.HUMIDITY, StringUtil.toObject(windData.humidity, Double.class, getUnit(ValueType.HUMIDITY, HUMIDITY).getScaler()), now);
					if (windData.bearing != null) {
						Double windDirection = (Double) StringUtil.toObject(windData.bearing, Double.class, null);
						if (windDirection >= 0 && windDirection <= 360) {
							ValueType.WINDDIRECTION.setRaw(windDirection);
							fireOnDeviceValueChanged(ValueType.WINDDIRECTION, windDirection, now);
						} else
							LLog.e(Globals.TAG, CLASSTAG + " Invalid winddirection " + windDirection + " (" + windData.bearing + " from " + msg + ")");
					}
				} catch (ClassCastException e){
					LLog.e(Globals.TAG, CLASSTAG + " Invalid cast in conversion from string to number", e);
				}
			}
		}
	}

	@Override
	public void fadeOut(ValueType type) {
		switch(type) {
		case WINDSPEED:
			if (windSpeed > 0) {
				LLog.d(Globals.TAG, CLASSTAG + ".fadeOut: Windspeed = " + windSpeed);
				if (windSpeed > 1.0) {
					windSpeed *= 0.9;
				} else { 
					windSpeed = 0.0;
				}
				fireOnDeviceValueChanged(ValueType.WINDSPEED, windSpeed, System.currentTimeMillis());
			}
			break;
		default:
			break;
		}
	}
	
	@Override
	public boolean isActive() {
		if (System.currentTimeMillis() - active < 15000) return true;
		return false;
	}

	@Override
	public void onBluetoothReadBytes(BTConnectedThread sender,
			byte[] msg) {
		active = System.currentTimeMillis();
		LLog.d(Globals.TAG, CLASSTAG + ".onBluetoothReadBytes: " + StringUtil.toHexString(msg)); 
	}

	@Override
	protected BTConnectedThread createConnectedThread(BluetoothSocket socket) {
		return new BTConnectedThread(this, socket, new Command[]{
				new Command("p\r", 10, 300000),
				new Command("i?\r", 5000, 30000),
				new Command("S\r", 2000, 20000)}, 
				BTConnectedThread.Type.WITH_LINEENDS);
	}
}