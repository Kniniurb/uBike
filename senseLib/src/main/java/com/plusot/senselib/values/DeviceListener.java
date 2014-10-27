package com.plusot.senselib.values;

public interface DeviceListener {
	public void onDeviceValueChanged(DeviceStub device, ValueType valueType, ValueItem valueItem, boolean isNew);
	public void onStreamFinished();
	public void onDeviceLost();
}
