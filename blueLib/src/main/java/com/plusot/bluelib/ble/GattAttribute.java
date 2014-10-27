package com.plusot.bluelib.ble;

import java.util.UUID;

public enum GattAttribute {
	GENERIC_ACCESS_SERVICE(GattAttributeType.SERVICE, "00001800-0000-1000-8000-00805f9b34fb", "Generic Access Service"),
	GENERIC_ATTRIBUTE_SERVICE(GattAttributeType.SERVICE, "00001801-0000-1000-8000-00805f9b34fb", "Generic Attribute Service"),
	HEART_RATE_SERVICE(GattAttributeType.SERVICE, "0000180d-0000-1000-8000-00805f9b34fb", "Heart Rate Service"),
	DEVICE_INFORMATION_SERVICE(GattAttributeType.SERVICE, "0000180a-0000-1000-8000-00805f9b34fb", "Device Information Service"),
	BATTERY_SERVICE(GattAttributeType.SERVICE, "0000180f-0000-1000-8000-00805f9b34fb", "Battery Service"),
	CYCLIINGSPEEDANDCADENCE_SERVICE(GattAttributeType.SERVICE, "00001816-0000-1000-8000-00805f9b34fb", "Cycling Speed and Cadence Service"),
	HEARTRATE_MEASUREMENT(GattAttributeType.CHARACTERISTIC, "00002a37-0000-1000-8000-00805f9b34fb", "Heart Rate Measurement"),
	DEVICE_NAME_STRING(GattAttributeType.CHARACTERISTIC, "00002a00-0000-1000-8000-00805f9b34fb", "Device Name String"),
	APPEARANCE(GattAttributeType.CHARACTERISTIC, "00002a01-0000-1000-8000-00805f9b34fb", "Appearance"),
	PERIPHERAL_PRIVACY_FLAG(GattAttributeType.CHARACTERISTIC, "00002a02-0000-1000-8000-00805f9b34fb", "Peripheral Privacy Flag"),
	RECONNECTION_ADDRESS(GattAttributeType.CHARACTERISTIC, "00002a03-0000-1000-8000-00805f9b34fb", "Reconnection address"),
	PERIPHERAL_PREFERRED_CONNECTION_PARAMETERS(GattAttributeType.CHARACTERISTIC, "00002a04-0000-1000-8000-00805f9b34fb", "Peripheral Preferred Connection Parameters"),
	SERVICE_CHANGED(GattAttributeType.CHARACTERISTIC, "00002a05-0000-1000-8000-00805f9b34fb", "Service changed"),
	BATTERY_LEVEL(GattAttributeType.CHARACTERISTIC, "00002a19-0000-1000-8000-00805f9b34fb", "Battery level"),
	TEMPERATURE_MEASUREMENT(GattAttributeType.CHARACTERISTIC, "00002a1c-0000-1000-8000-00805f9b34fb", "Temperature measurement"),
	SYSTEM_ID(GattAttributeType.CHARACTERISTIC, "00002a23-0000-1000-8000-00805f9b34fb", "System ID"),
	MODEL_NUMBER(GattAttributeType.CHARACTERISTIC, "00002a24-0000-1000-8000-00805f9b34fb", "Model number"),
	SERIAL_NUMBER(GattAttributeType.CHARACTERISTIC, "00002a25-0000-1000-8000-00805f9b34fb", "Serial number"),
	FIRMWARE_REVISION(GattAttributeType.CHARACTERISTIC, "00002a26-0000-1000-8000-00805f9b34fb", "Firmware revision"),
	HARDWARE_REVISION(GattAttributeType.CHARACTERISTIC, "00002a27-0000-1000-8000-00805f9b34fb", "Hardware revision"),
	SOFTWARE_REVISION(GattAttributeType.CHARACTERISTIC, "00002a28-0000-1000-8000-00805f9b34fb", "Software revision"),
	MANUFACTURER_NAME_STRING(GattAttributeType.CHARACTERISTIC, "00002a29-0000-1000-8000-00805f9b34fb", "Manufacturer Name string"),
	SC_CONTROLPOINT(GattAttributeType.CHARACTERISTIC, "00002a55-0000-1000-8000-00805f9b34fb", "SC Control Point"),
	BODY_SENSOR_LOCATION(GattAttributeType.CHARACTERISTIC, "00002a38-0000-1000-8000-00805f9b34fb", "Body sensor location"),
	CSC_MEASUREMENT(GattAttributeType.CHARACTERISTIC, "00002a5b-0000-1000-8000-00805f9b34fb", "CSC Measurement"),
	CSC_FEATURE(GattAttributeType.CHARACTERISTIC, "00002a5c-0000-1000-8000-00805f9b34fb", "CSC Feature"),
	SENSOR_LOCATION(GattAttributeType.CHARACTERISTIC, "00002a5d-0000-1000-8000-00805f9b34fb", "Sensor location"),
	UNKNOWN(GattAttributeType.CHARACTERISTIC, "00000000-0000-0000-0000-000000000000", "Unknown"),
	CLIENT_CHARACTERISTIC_CONFIG(GattAttributeType.DESCRIPTOR, "00002902-0000-1000-8000-00805f9b34fb", "Client Charateristic config");
	;

	//public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
	//private final String sUUID;
	private final UUID uuid;
	private final String label;
	private final GattAttributeType type;

	private GattAttribute(final GattAttributeType type, final String sUUID, final String label) {
		uuid = UUID.fromString(sUUID);
		//	this.sUUID = sUUID;
		this.label = label;
		this.type = type;

	}

	//    public static String strinFromUUID(String uuid, GattAttributeType type) {
	//    	for (GattAttribute attr : GattAttribute.values()) if (attr.sUUID.equals(uuid)) return attr.label;	 
	//        return UNKNOWN_CHARACTERISTIC.label + " " + type + " " + uuid;
	//    }

	public static GattAttribute fromUUID(UUID uuid) {
		for (GattAttribute attr : GattAttribute.values()) if (attr.uuid.equals(uuid) ) return attr;	 
		return UNKNOWN;
	}

	public static String stringFromUUID(UUID uuid, GattAttributeType type) {
		for (GattAttribute attr : GattAttribute.values()) if (attr.uuid.equals(uuid)) return attr.label;	 
		return UNKNOWN.label + " " + type.toString(); // + " " + uuid;
	}

	public UUID getUuid() { 
		return uuid;
	}

	@Override
	public String toString() {
		return label;
	}

	public GattAttributeType getType() {
		return type;
	}

	public GattAttribute getDescriptor() {
		switch (this) {
		case HEARTRATE_MEASUREMENT:
		case CSC_MEASUREMENT:
		case TEMPERATURE_MEASUREMENT:
			return GattAttribute.CLIENT_CHARACTERISTIC_CONFIG;
		default:
			return null;
		}
	}

	public boolean isReadable() {
		switch (this) {
		case HEARTRATE_MEASUREMENT:
		case CSC_MEASUREMENT:
		case TEMPERATURE_MEASUREMENT:
			return true;
		default:
			return false;
		}
	}

	public boolean isPollable() {
		switch (this) {
		case BATTERY_LEVEL:
			return true;
		default:
			return false;
		}
	}
}
