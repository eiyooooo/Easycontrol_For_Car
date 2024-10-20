package top.eiyooooo.easycontrol.server.wrappers;

import android.util.Pair;
import android.view.Display;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import top.eiyooooo.easycontrol.server.entity.Device;

public class ButtonMonitor {
    private static Thread monitorThread;
    private static Pair<Boolean, Long> lastVolumeEvent;

    public static void start() {
        if (monitorThread != null) return;
        monitorThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Process process = null;
                BufferedReader reader = null;
                String line;
                Pattern pattern = Pattern.compile("(/dev/input/event\\d+): (\\d{4}) (\\d{4}) ([0-9a-fA-F]+)");
                try {
                    process = new ProcessBuilder("getevent").start();
                    reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                    while (!Thread.interrupted() && (line = reader.readLine()) != null) {
                        Matcher matcher = pattern.matcher(line);
                        if (matcher.find()) {
                            int code = Integer.parseInt(matcher.group(3), 16);
                            int value;
                            try {
                                value = (int) Long.parseLong(matcher.group(4), 16);
                            } catch (NumberFormatException e) {
                                value = -1;
                            }

                            if ((code == 114 || code == 115) && value == 0) {
                                long timeMillis = System.currentTimeMillis();
                                boolean volumeDown = code == 114;
                                if (lastVolumeEvent != null && volumeDown && !lastVolumeEvent.first) {
                                    if ((timeMillis - lastVolumeEvent.second < 2000)) {
                                        Device.changeScreenPowerMode(Display.STATE_ON);
                                    }
                                }
                                lastVolumeEvent = new Pair<>(volumeDown, timeMillis);
                            }
                        }
                    }
                } catch (Exception ignored) {
                } finally {
                    try {
                        if (process != null) {
                            process.destroy();
                        }
                        if (reader != null) {
                            reader.close();
                        }
                    } catch (Exception ignored) {
                    }
                    monitorThread = null;
                }
            }
        });
        monitorThread.start();
    }

    public static void stop() {
        if (monitorThread != null) monitorThread.interrupt();
        monitorThread = null;
    }
}