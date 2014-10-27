package com.plusot.senselib.values;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by peet on 16-10-14.
 */
public class JSONUtil {

    public static  JSONObject toJSON(Map<String, Double> map) throws JSONException {
        Map<String, Double> data = new HashMap<String, Double>();
        for (String dataItem: map.keySet()) {
            Double dataValue = map.get(dataItem);
            if (dataValue != null && !dataValue.equals("")) data.put(dataItem, dataValue);
        }
        JSONObject json = new JSONObject();
        for (String key: data.keySet()) {
            json.put(key, data.get(key));
        }
        return json;
    }

//    public static EnumMap<ValueType, Map<DeviceType, Double>> fromJSON(JSONObject json) {
//
//    }
}
