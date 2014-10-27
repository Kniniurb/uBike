package com.plusot.bluelib.ble;

public enum GattAttributeType {
	SERVICE("Service"),
	CHARACTERISTIC("Characteristic"),
	DESCRIPTOR("Descriptor");
	
	private final String label;
	
	private GattAttributeType(final String label) {
		this.label = label;
	}
	
	@Override
	public String toString() {
		return label;
	}
}
