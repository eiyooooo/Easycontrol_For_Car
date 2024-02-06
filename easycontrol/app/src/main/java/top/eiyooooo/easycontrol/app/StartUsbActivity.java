package top.eiyooooo.easycontrol.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.util.Pair;
import android.widget.Toast;
import top.eiyooooo.easycontrol.app.client.Client;
import top.eiyooooo.easycontrol.app.entity.AppData;
import top.eiyooooo.easycontrol.app.entity.Device;

import java.util.ArrayList;
import java.util.Objects;

public class StartUsbActivity extends Activity {
  @SuppressLint({"StaticFieldLeak", "MutableImplicitPendingIntent"})
  @Override
  public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      // 初始化
      AppData.init(this);

      // 检查USB
      ArrayList<Pair<String, UsbDevice>> UsbDevices = new ArrayList<>();
      for (String k : AppData.usbManager.getDeviceList().keySet()) {
          UsbDevice device = AppData.usbManager.getDeviceList().get(k);
          if (!AppData.usbManager.hasPermission(device)) {
              Toast.makeText(this, "请授权后重试", Toast.LENGTH_SHORT).show();
              AppData.usbManager.requestPermission(
                      device,
                      PendingIntent.getBroadcast(getApplicationContext(),
                              0,
                              new Intent("top.eiyooooo.easycontrol.app.USB_PERMISSION"),
                              PendingIntent.FLAG_MUTABLE));
              finish();
              return;
          }
          String uuid = device.getSerialNumber();
          Device d = AppData.dbHelper.getByUUID(uuid);
          if (d == null) {
              d = Device.getDefaultDevice(uuid, Device.TYPE_LINK);
              AppData.dbHelper.insert(d);
          }
          UsbDevices.add(new Pair<>(uuid, device));
      }

      // 检查USB设备
      if (UsbDevices.isEmpty()) {
          Toast.makeText(this, "未检测到USB设备", Toast.LENGTH_SHORT).show();
          finish();
          return;
      }

      // 要启动的USB设备
      Device device = null;
      String uuid = "";
      Pair<String, UsbDevice> UsbDevice = new Pair<>("", null);

      // 检查默认USB设备
      String defaultUsbDevice = AppData.setting.getDefaultUsbDevice();
      if (defaultUsbDevice.isEmpty()) {
          Toast.makeText(this, "未设置默认USB设备，启动第一个USB设备", Toast.LENGTH_SHORT).show();
          device = AppData.dbHelper.getByUUID(UsbDevices.get(0).first);
          uuid = device.uuid;
          UsbDevice = UsbDevices.get(0);
      }
      else {
          for (Pair<String, UsbDevice> pair : UsbDevices) {
              if (Objects.equals(pair.first, defaultUsbDevice)) {
                  device = AppData.dbHelper.getByUUID(defaultUsbDevice);
                  uuid = device.uuid;
                  UsbDevice = pair;
                  break;
              }
          }
          if (UsbDevice.second == null) {
              Toast.makeText(this, "默认USB设备不存在，启动第一个USB设备", Toast.LENGTH_SHORT).show();
              device = AppData.dbHelper.getByUUID(UsbDevices.get(0).first);
              uuid = device.uuid;
              UsbDevice = UsbDevices.get(0);
          }
      }

      // 判断是否已经启动
      for (Client client : Client.allClient) {
            if (Objects.equals(client.uuid, UsbDevice.first)) {
                Toast.makeText(this, "USB设备已启动", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
      }

      // 启动USB设备
      if (device != null) {
          new Client(device, UsbDevice.second);
      }
      else {
          Toast.makeText(this, "USB设备启动失败", Toast.LENGTH_SHORT).show();
          finish();
          return;
      }

      // 等待启动
      String finalUuid = uuid;
      new Thread(() -> {
          while (true) {
              try {
                  Thread.sleep(100);
              } catch (InterruptedException e) {
                  e.printStackTrace();
              }
              // 判断是否已经启动
              for (Client client : Client.allClient) {
                  if (Objects.equals(client.uuid, finalUuid)) {
                      if (client.isStarted()) {
                          try {
                              Thread.sleep(500);
                          } catch (InterruptedException e) {
                              e.printStackTrace();
                          }
                          finish();
                          return;
                      }
                  }
              }
          }
      }).start();
  }
}