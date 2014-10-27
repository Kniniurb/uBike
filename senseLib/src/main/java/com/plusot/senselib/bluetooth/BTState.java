package com.plusot.senselib.bluetooth;

public enum BTState {
	NONE,
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
    SENDING;

	public static BTState fromInt(int value) {
		for(BTState state: BTState.values()) {
			if (state.ordinal() == value) return state;
		}
		return null;
	}
}
