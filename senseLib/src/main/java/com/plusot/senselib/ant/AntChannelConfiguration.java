package com.plusot.senselib.ant;

public class AntChannelConfiguration {
    public int deviceNumber;
    public byte deviceType;
    public byte TransmissionType;
    public short period;
    public byte freq;
    public byte proxSearch;

    public boolean isInitializing = false;
    public boolean isDeinitializing = false;
}
