package com.plusot.senselib.values;

import com.plusot.senselib.db.DbData;

/**
 * Created by peet on 16-10-14.
 */
public class DataIndex {
    ValueType valueType;
    DeviceType deviceType;
    int number;
    String model;

    public DataIndex(ValueType valueType, DeviceType deviceType, int number, String model) {
        this.valueType = valueType;
        this.deviceType = deviceType;
        this.number = number;
        this.model = model;

    }


}
