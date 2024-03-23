package top.eiyooooo.easycontrol.server.utils;

import android.text.format.DateFormat;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

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

    public static int logMode = 0;

    private static PrintStream fileOut;

    public static void flush(StringBuilder sb) {
        if (logMode != 2) {
            System.out.println(sb);
            System.out.flush();
        }

        if (logMode == 1) return;
        try {
            if (fileOut == null) {
                fileOut = new PrintStream(new FileOutputStream("/data/local/tmp/easycontrol_for_car_log", false));
            }
            fileOut.println(sb);
        } catch (Exception ignored) {
        }
    }

    public static void postLog() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) return;
        new Thread(() -> {
            try {
                File file = new File("/data/local/tmp/easycontrol_for_car_log");
                long lastLength = 0;
                while (true) {
                    long fileLength = file.length();
                    if (fileLength > lastLength) {
                        RandomAccessFile raf = new RandomAccessFile(file, "r");
                        raf.seek(lastLength);
                        String line;
                        while ((line = raf.readLine()) != null) {
                            System.out.println(line);
                        }
                        raf.close();
                        lastLength = fileLength;
                    }
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                L.e("postLog error", e);
            }
        }).start();
    }
}