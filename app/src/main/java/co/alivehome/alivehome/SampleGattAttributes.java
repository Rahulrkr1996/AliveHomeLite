package co.alivehome.alivehome;

/**
 * Created by Aman on 6/30/2017.
 */

import java.util.HashMap;

public class SampleGattAttributes {
    private static HashMap<String, String> attributes = new HashMap();
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    public static String HM_10 = "0000ffe1-0000-1000-8000-00805f9b34fb";

    static {
        // Sample Services.
        attributes.put("0000ffe0-0000-1000-8000-00805f9b34fb", "HM-10 Service");
        attributes.put("0000180d-0000-1000-8000-00805f9b34fb", "Heart Rate Service");
        attributes.put("0000180a-0000-1000-8000-00805f9b34fb", "Device Information Service");
        // Sample Characteristics.
        attributes.put("00002a29-0000-1000-8000-00805f9b34fb", "Manufacturer Name String");
        attributes.put(HM_10, "HM-10 Module");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
