package com.plusot.bluelib.ble;

public enum Unit {
	CELSIUS("ºC"),
	BPM("bpm"),
	RPM("rpm"),
	VOLT("%"),
	NONE(""),
	;
	
	final String label;
	private Unit(final String label) {
		this.label = label;
	}
	
	@Override
	public String toString() {
		return label;
	}
}
