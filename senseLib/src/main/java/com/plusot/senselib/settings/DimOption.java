package com.plusot.senselib.settings;


public enum DimOption {
	NODIM("nodim"),
	DIM("dim"),
	SCREENOFF_WAKE("screenoff_wake"),
	SCREENOFF_FULL("screenoff");
	final private String label;

	private DimOption(final String label) {
		this.label = label;
	}

	public static DimOption fromString(String label) {
		for (DimOption option : DimOption.values()) {
			if (option.label.equals(label)) return option;
		}
		return DimOption.DIM;
	}

	@Override
	public String toString () {
		return label;
	}

}
