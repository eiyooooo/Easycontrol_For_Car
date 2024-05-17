package top.eiyooooo.easycontrol.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.usage.UsageEvents;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import androidx.annotation.RequiresApi;

import top.eiyooooo.easycontrol.app.client.Client;
import top.eiyooooo.easycontrol.app.databinding.ActivityMonitorBinding;
import top.eiyooooo.easycontrol.app.databinding.ItemAddEventBinding;
import top.eiyooooo.easycontrol.app.databinding.ItemTextBinding;
import top.eiyooooo.easycontrol.app.entity.AppData;
import top.eiyooooo.easycontrol.app.entity.MonitorEvent;
import top.eiyooooo.easycontrol.app.helper.EventMonitor;
import top.eiyooooo.easycontrol.app.helper.PublicTools;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.UUID;

public class MonitorActivity extends Activity {
  /**
   * 0:English
   * <p>
   * 1:Chinese Simplified
   */
  private int language = 1;
  private ActivityMonitorBinding monitorActivity;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    PublicTools.setStatusAndNavBar(this);
    PublicTools.setLocale(this);
    monitorActivity = ActivityMonitorBinding.inflate(this.getLayoutInflater());
    setContentView(monitorActivity.getRoot());
    if (monitorActivity.textEnable.getText().toString().contains("Enable monitor")) language = 0;
    setListener();
    checkPermission();
    addAllEventToList();
  }

  @Override
  public void onResume() {
    super.onResume();
    checkPermission();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (captureThread != null) captureThread.interrupt();
  }

  private void setListener() {
    monitorActivity.backButton.setOnClickListener(v -> finish());
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) monitorActivity.permissionChecker.setText(R.string.monitor_android_version_not_support);
    else monitorActivity.permissionChecker.setOnClickListener(v -> startActivity(PublicTools.getPackagePermissionIntent(this)));

    monitorActivity.switchEnable.setChecked(AppData.setting.getMonitorState());
    monitorActivity.switchEnable.setOnCheckedChangeListener((buttonView, checked) -> {
      if (checked && EventMonitor.monitorEventsList.isEmpty()) {
        PublicTools.logToast(getString(R.string.monitor_no_event));
        monitorActivity.switchEnable.setChecked(false);
        return;
      }
      AppData.setting.setMonitorState(checked);
      if (checked && !Client.allClient.isEmpty()) EventMonitor.startMonitor();
      if (!checked) EventMonitor.stopMonitor();
    });

    ArrayAdapter<Integer> adapter = new ArrayAdapter<>(this, R.layout.item_spinner_item, new Integer[]{500, 1000, 1500, 2000, 2500});
    monitorActivity.spinnerLatency.setAdapter(adapter);
    monitorActivity.spinnerLatency.setSelection(adapter.getPosition(AppData.setting.getMonitorLatency()));
    monitorActivity.spinnerLatency.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (EventMonitor.monitorRunning) EventMonitor.startMonitor();
        AppData.setting.setMonitorLatency((Integer) monitorActivity.spinnerLatency.getSelectedItem());
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {
      }
    });

    setCaptureEventOnClickListener();
  }

  private void setCaptureEventOnClickListener() {
    monitorActivity.captureEvent.setOnClickListener(v -> {
      if (!PublicTools.checkUsageStatsPermission(this)) {
        PublicTools.logToast(getString(R.string.monitor_no_permission));
      } else {
        monitorActivity.captureEvent.setText(R.string.monitor_capture_event_stop);
        startCapture();
        monitorActivity.captureEvent.setOnClickListener(vv -> {
          monitorActivity.captureEvent.setText(R.string.monitor_capture_event_start);
          if (captureThread != null) captureThread.interrupt();
          captureThread = null;
          setCaptureEventOnClickListener();
        });
      }
    });
  }

  private void checkPermission() {
    if (PublicTools.checkUsageStatsPermission(this)) {
      monitorActivity.permissionChecker.setVisibility(View.GONE);
      monitorActivity.containerEnable.setVisibility(View.VISIBLE);
      monitorActivity.containerLatency.setVisibility(View.VISIBLE);
      monitorActivity.textLatencyDetail.setVisibility(View.VISIBLE);
    } else {
      monitorActivity.permissionChecker.setVisibility(View.VISIBLE);
      monitorActivity.containerEnable.setVisibility(View.GONE);
      monitorActivity.containerLatency.setVisibility(View.GONE);
      monitorActivity.textLatencyDetail.setVisibility(View.GONE);
    }
  }

  private Thread captureThread;

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
  @SuppressLint("SimpleDateFormat")
  private void startCapture() {
    if (captureThread != null) captureThread.interrupt();
    captureThread = new Thread(() -> {
      int latency = AppData.setting.getMonitorLatency();
      try {
        while (!Thread.interrupted()) {
          Thread.sleep(latency);
          ArrayList<UsageEvents.Event> events = getUsageEvents(latency + 10);
          if (events == null) continue;
          for (UsageEvents.Event event : events) {
            SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm:ss");
            String show = timeFormatter.format(event.getTimeStamp()) + " | " + event.getPackageName() + " | " + event.getClassName() + " | " + explainEvent(event.getEventType());
            ItemTextBinding text = PublicTools.createTextCard(this, show, () -> {
              ItemAddEventBinding addEventView = ItemAddEventBinding.inflate(LayoutInflater.from(this));
              Dialog addEventDialog = PublicTools.createDialog(this, true, addEventView.getRoot());
              addEventView.buttonChangeToMini.setOnClickListener(v -> {
                MonitorEvent monitorEvent = new MonitorEvent(UUID.randomUUID().toString(), event.getPackageName(), event.getClassName(), event.getEventType(), 1);
                EventMonitor.monitorEventsList.add(monitorEvent);
                AppData.dbHelper.insert(monitorEvent);
                addEventToList(monitorEvent);
                addEventDialog.cancel();
              });
              addEventView.buttonChangeToSmall.setOnClickListener(v -> {
                MonitorEvent monitorEvent = new MonitorEvent(UUID.randomUUID().toString(), event.getPackageName(), event.getClassName(), event.getEventType(), 2);
                EventMonitor.monitorEventsList.add(monitorEvent);
                AppData.dbHelper.insert(monitorEvent);
                addEventToList(monitorEvent);
                addEventDialog.cancel();
              });
              addEventDialog.show();
            });
            monitorActivity.capturedEvents.post(() -> monitorActivity.capturedEvents.addView(text.getRoot()));
          }
        }
      } catch (InterruptedException ignored) {
      }
    });
    captureThread.start();
  }

  private void addAllEventToList() {
    for (MonitorEvent event : EventMonitor.monitorEventsList) addEventToList(event);
  }

  private void addEventToList(MonitorEvent event) {
    String show = event.packageName + " | " + event.className + " | " + explainEvent(event.eventType);
    ItemTextBinding textView = ItemTextBinding.inflate(LayoutInflater.from(this));
    textView.getRoot().setText(show);
    textView.getRoot().setOnClickListener(v -> {
      EventMonitor.monitorEventsList.remove(event);
      AppData.dbHelper.delete(event);
      if (event.responseType == 1) monitorActivity.changeToMiniEvents.post(() -> monitorActivity.changeToMiniEvents.removeView(textView.getRoot()));
      else if (event.responseType == 2) monitorActivity.changeToSmallEvents.post(() -> monitorActivity.changeToSmallEvents.removeView(textView.getRoot()));
    });
    if (event.responseType == 1) monitorActivity.changeToMiniEvents.post(() -> monitorActivity.changeToMiniEvents.addView(textView.getRoot()));
    else if (event.responseType == 2) monitorActivity.changeToSmallEvents.post(() -> monitorActivity.changeToSmallEvents.addView(textView.getRoot()));
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
  private static ArrayList<UsageEvents.Event> getUsageEvents(long interval) {
    long ts = System.currentTimeMillis();
    UsageEvents usageEvents = AppData.usageStatsManager.queryEvents(ts - interval, ts);
    if (usageEvents == null) return null;
    else return UnpackUsageEvents(usageEvents);
  }

  private static ArrayList<UsageEvents.Event> UnpackUsageEvents(UsageEvents usageEvents) {
    if (usageEvents == null) return new ArrayList<>();
    ArrayList<UsageEvents.Event> arrayList = new ArrayList<>();
    while (usageEvents.hasNextEvent()) {
      UsageEvents.Event event = new UsageEvents.Event();
      if (usageEvents.getNextEvent(event)) arrayList.add(event);
    }
    return arrayList;
  }

  private String explainEvent(int eventType) {
      switch (eventType) {
      case 2:
        return (language == 1) ? "活动已暂停" : "Activity Paused";
      case 1:
        return (language == 1) ? "活动已恢复" : "Activity Resumed";
      case 5:
        return (language == 1) ? "配置变更" : "Configuration Change";
      case 7:
        return (language == 1) ? "用户交互" : "User Interaction";
      case 11:
        return (language == 1) ? "待机模式变更" : "Standby Bucket Changed";
      case 19:
        return (language == 1) ? "前台服务已启动" : "Foreground Service Started";
      case 20:
        return (language == 1) ? "前台服务已停止" : "Foreground Service Stopped";
      case 23:
        return (language == 1) ? "活动已停止" : "Activity Stopped";
      default:
        return (language == 1) ? "未知事件类型" : "Unknown type";
    }
  }
}
