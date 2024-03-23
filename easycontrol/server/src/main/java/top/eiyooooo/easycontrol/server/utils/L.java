package top.eiyooooo.easycontrol.server.utils;

import android.text.format.DateFormat;
import android.util.Log;

public class L {

    public static void i(String message) {
        StringBuilder sb = new StringBuilder();
        sb.append("INFO: ");
        sb.append("<").append(DateFormat.format("HH:mm:ss", new java.util.Date())).append("> ");
        sb.append(message);
        Log.i("easycontrol_for_car", message);
        flush(sb);
    }

    public static void d(String message) {
        StringBuilder sb = new StringBuilder();
        sb.append("DEBUG: ");
        sb.append("<").append(DateFormat.format("HH:mm:ss", new java.util.Date())).append("> ");
        sb.append(message);
        Log.d("easycontrol_for_car", message);
        flush(sb);
    }

    public static void w(String message, Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        sb.append("WARN: ");
        sb.append("<").append(DateFormat.format("HH:mm:ss", new java.util.Date())).append("> ");
        sb.append(message);
        if (throwable != null) {
            sb.append(": ").append(Log.getStackTraceString(throwable));
        }
        Log.w("easycontrol_for_car", message, throwable);
        flush(sb);
    }

    public static void w(String message) {
        w(message, null);
    }

    public static void w(Throwable throwable) {
        w("", throwable);
    }

    public static void e(String message, Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        sb.append("ERROR: ");
        sb.append("<").append(DateFormat.format("HH:mm:ss", new java.util.Date())).append("> ");
        sb.append(message);
        if (throwable != null) {
            sb.append(": ").append(Log.getStackTraceString(throwable));
        }
        Log.e("easycontrol_for_car", message, throwable);
        flush(sb);
    }

    public static void e(String message) {
        e(message, null);
    }

    public static void e(Throwable throwable) {
        e("", throwable);
    }

    public static void flush(StringBuilder sb) {
        System.out.println(sb);
        System.out.flush();
    }
}