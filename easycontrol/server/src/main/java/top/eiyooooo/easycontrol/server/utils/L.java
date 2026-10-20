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
        // stdout 是 App 端的请求/响应协议流（二进制帧），日志文本行会挤错帧解析；
        // logMode=1（Scrcpy 进程）的 stdout 连着 App 的 shell 协议流，同样不能输出
        if (logMode != 1) {
            System.out.println(sb);
            System.out.flush();
        }
        writeFile(sb);
    }

    // 只写文件与 logcat，不进 stdout。用于协议处理路径上的运行期日志（INPUT/RESPONSE 等）
    public static void logToFile(String message) {
        StringBuilder sb = new StringBuilder("DEBUG: <");
        sb.append(DateFormat.format("HH:mm:ss", new java.util.Date())).append("> ");
        sb.append(message);
        Log.d("easycontrol_for_car", message);
        writeFile(sb);
    }

    private static void writeFile(StringBuilder sb) {
        // Scrcpy(logMode=1) 的日志也写文件，否则切换显示器失败等关键错误无法排查；
        // append 模式，避免多进程（Server+Scrcpy、多会话）互相截断日志
        try {
            if (fileOut == null) {
                File logFile = new File("/data/local/tmp/easycontrol_for_car_log");
                if (logFile.length() > 2 * 1024 * 1024) logFile.delete(); // 防止 append 模式下文件无限增长
                fileOut = new PrintStream(new FileOutputStream("/data/local/tmp/easycontrol_for_car_log", true));
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
                // 只 tail 本进程启动后新增的日志；stdout 是 App 的请求/响应协议流，
                // 全量输出历史日志会把响应挤错位导致 App 解析失败
                long lastLength = file.length();
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