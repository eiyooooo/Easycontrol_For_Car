package top.eiyooooo.easycontrol.app.helper;

import android.text.format.DateFormat;
import android.util.Log;
import top.eiyooooo.easycontrol.app.entity.Device;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class L {
    private static final Map<String, StringBuilder> logs = new HashMap<>();

    public static void log(String uuid, String log) {
        StringBuilder logBuilder = logs.get(uuid);
        if (logBuilder == null) {
            logBuilder = new StringBuilder();
            logs.put(uuid, logBuilder);
        }
        logBuilder.append("<").append(DateFormat.format("HH:mm:ss", new java.util.Date())).append("> ");
        logBuilder.append(log).append("\n");
    }

    public static void log(String uuid, Throwable throwable) {
        String log = Log.getStackTraceString(throwable);
        log(uuid, log);
    }

    public static void logWithoutTime(String uuid, String log) {
        StringBuilder logBuilder = logs.get(uuid);
        if (logBuilder == null) {
            logBuilder = new StringBuilder();
            logs.put(uuid, logBuilder);
        }
        logBuilder.append(log).append("\n");
    }

    public static void logWithoutTime(String uuid, Throwable throwable) {
        String log = Log.getStackTraceString(throwable);
        logWithoutTime(uuid, log);
    }

    public static String getLogs() {
        StringBuilder logBuilder = new StringBuilder();
        ArrayList<String> uuids = new ArrayList<>();
        for (Device device : DeviceListAdapter.devicesList) {
            uuids.add(device.uuid);
        }
        for (Map.Entry<String, StringBuilder> entry : logs.entrySet()) {
            if (!uuids.contains(entry.getKey())) {
                logBuilder.append(entry.getValue());
            }
        }
        if (logBuilder.length() > 0) {
            return logBuilder.toString();
        }
        return "no log found";
    }

    public static String getLogs(String uuid) {
        StringBuilder logBuilder = logs.get(uuid);
        if (logBuilder != null) {
            return logBuilder.toString();
        }
        return "no log found";
    }
}
