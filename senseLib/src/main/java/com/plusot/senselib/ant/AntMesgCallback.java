package com.plusot.senselib.ant;

public interface AntMesgCallback {
	public void sendMessage(AntProfile profile, byte[] txBuffer);
	public int getDeviceNumber(AntProfile profile);

}
