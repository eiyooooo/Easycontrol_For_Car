package top.eiyooooo.easycontrol.app.helper;

import android.app.usage.UsageEvents;
import android.os.SystemClock;

import java.util.ArrayList;
import java.util.Objects;

import top.eiyooooo.easycontrol.app.client.Client;
import top.eiyooooo.easycontrol.app.entity.AppData;
import top.eiyooooo.easycontrol.app.entity.MonitorEvent;

public class EventMonitor {
    public static ArrayList<MonitorEvent> monitorEventsList = new ArrayList<>();
    public static boolean monitorRunning = false;
    private static Thread monitorThread;

    public static void startMonitor() {
        if (PublicTools.checkUsageStatsPermission(AppData.main)) {
            if (monitorThread != null) monitorThread.interrupt();
            monitorThread = new Thread(() -> {
                int latency = AppData.setting.getMonitorLatency();
                long responseDelay = 0;
                while (!Thread.interrupted()) {
                    SystemClock.sleep(latency);
                    if (Client.allClient.isEmpty()) {
                        monitorRunning = false;
                        monitorThread.interrupt();
                        return;
                    }
                    long ts = System.currentTimeMillis();
                    UsageEvents usageEvents = AppData.usageStatsManager.queryEvents(ts - latency - responseDelay, ts);
                    while (usageEvents.hasNextEvent()) {
                        UsageEvents.Event event = new UsageEvents.Event();
                        if (usageEvents.getNextEvent(event)) {
                            for (MonitorEvent monitorEvent : monitorEventsList) {
                                if (Objects.equals(monitorEvent.packageName, event.getPackageName()) && Objects.equals(monitorEvent.className, event.getClassName()) && monitorEvent.eventType == event.getEventType()) {
                                    switch (monitorEvent.responseType) {
                                        case 1:
                                            for (Client client : Client.allClient) {
                                                if (client.clientView.viewMode == 2) AppData.uiHandler.post(() -> {
                                                    client.clientView.needResumeToSmall = true;
                                                    client.clientView.changeToMini(0);
                                                });
                                            }
                                            break;
                                        case 2:
                                            for (Client client : Client.allClient) {
                                                if (client.clientView.needResumeToSmall && client.clientView.viewMode == 1) {
                                                    AppData.uiHandler.post(client.clientView::changeToSmall);
                                                }
                                            }
                                            break;
                                    }
                                }
                            }
                        }
                    }
                    responseDelay = System.currentTimeMillis() - ts;
                }
            });
            monitorThread.start();
            monitorRunning = true;
        } else {
            AppData.setting.setMonitorState(false);
            monitorRunning = false;
        }
    }

    public static void stopMonitor() {
        if (monitorThread != null) monitorThread.interrupt();
        monitorRunning = false;
    }
}
