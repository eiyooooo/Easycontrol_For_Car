package top.eiyooooo.easycontrol.app.helper;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Build;
import top.eiyooooo.easycontrol.app.adb.Adb;
import top.eiyooooo.easycontrol.app.entity.AppData;
import top.eiyooooo.easycontrol.app.entity.Device;
import top.eiyooooo.easycontrol.app.R;
import top.eiyooooo.easycontrol.app.client.Client;

import java.util.Map;
import java.util.Objects;

public class MyBroadcastReceiver extends BroadcastReceiver {

  private static final String ACTION_USB_PERMISSION = "top.eiyooooo.easycontrol.app.USB_PERMISSION";
  private static final String ACTION_CONTROL = "top.eiyooooo.easycontrol.app.CONTROL";
  private static final String ACTION_SCREEN_OFF = "android.intent.action.SCREEN_OFF";
  public static final String ACTION_CONFIGURATION_CHANGED = "android.intent.action.CONFIGURATION_CHANGED";

  private DeviceListAdapter deviceListAdapter;
  private ConnectHelper connectHelper;

  // 注册广播
  @SuppressLint("UnspecifiedRegisterReceiverFlag")
  public void register(Context context) {
    IntentFilter filter = new IntentFilter();
    filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
    filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
    filter.addAction(ACTION_USB_PERMISSION);
    filter.addAction(ACTION_CONTROL);
    filter.addAction(ACTION_SCREEN_OFF);
    filter.addAction(ACTION_CONFIGURATION_CHANGED);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) context.registerReceiver(this, filter, Context.RECEIVER_EXPORTED);
    else context.registerReceiver(this, filter);
  }

  public void unRegister(Context context) {
    context.unregisterReceiver(this);
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    String action = intent.getAction();
    if (ACTION_SCREEN_OFF.equals(action)) handleScreenOff();
    else if (ACTION_CONTROL.equals(action)) handleControl(intent);
    else if (ACTION_CONFIGURATION_CHANGED.equals(action)) handleConfigurationChanged();
    else handleUSB(context, intent);
  }

  public void handleConfigurationChanged() {
    int nightMode = AppData.uiModeManager.getNightMode();
    if (nightMode == AppData.nightMode) return;
    for (Client client : Client.allClient) {
      if (client.clientView.device.nightModeSync) {
        client.controlPacket.sendNightModeEvent(nightMode);
      }
    }
    AppData.nightMode = nightMode;
  }

  public void setDeviceListAdapter(DeviceListAdapter deviceListAdapter) {
    this.deviceListAdapter = deviceListAdapter;
  }

  public void setConnectHelper(ConnectHelper connectHelper) {
    this.connectHelper = connectHelper;
  }

  private void handleScreenOff() {
    for (Client client : Client.allClient) client.release(null);
  }

  private void handleControl(Intent intent) {
    String action = intent.getStringExtra("action");
    if (action == null) return;
    if (action.equals("startDefault")) {
      DeviceListAdapter.startDefault(intent.getIntExtra("mode", 0));
      return;
    }
    String uuid = intent.getStringExtra("uuid");
    if (uuid == null) return;
    if (action.equals("start")) DeviceListAdapter.startByUUID(uuid, intent.getIntExtra("mode", 0));
    else {
      for (Client client : Client.allClient) {
        if (Objects.equals(client.uuid, uuid)) {
            switch (action) {
                case "buttonPower":
                    client.controlPacket.sendPowerEvent();
                    break;
                case "buttonWake":
                    client.controlPacket.sendKeyEvent(224, 0, 0);
                    break;
                case "buttonLock":
                    client.controlPacket.sendKeyEvent(223, 0, 0);
                    break;
                case "buttonLight":
                    client.controlPacket.sendLightEvent(1);
                    break;
                case "buttonLightOff":
                    client.controlPacket.sendLightEvent(0);
                    break;
                case "buttonBack":
                    client.controlPacket.sendKeyEvent(4, 0, -1);
                    break;
                case "buttonHome":
                    client.controlPacket.sendKeyEvent(3, 0, -1);
                    break;
                case "buttonSwitch":
                    client.controlPacket.sendKeyEvent(187, 0, -1);
                    break;
                case "buttonRotate":
                    client.controlPacket.sendRotateEvent();
                    break;
                case "close":
                    client.release(null);
                    break;
                case "runShell":
                    String cmd = intent.getStringExtra("cmd");
                    if (cmd == null) return;
                    try {
                        client.adb.runAdbCmd(cmd);
                    } catch (Exception ignored) {
                    }
                    break;
            }
          return;
        }
      }
    }
  }

  public void handleReconnect(Device device, int mode) {
    if (device.isLinkDevice() && !DeviceListAdapter.devicesList.contains(device)) return;
    ConnectHelper.show(connectHelper, device.uuid, mode);
  }

  private void handleUSB(Context context, Intent intent) {
    UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
    String action = intent.getAction();
    if (usbDevice == null && action != null) return;
    if (Objects.equals(action, UsbManager.ACTION_USB_DEVICE_DETACHED)) onCutUsb(usbDevice);
    if (AppData.setting.getEnableUSB()) {
        if (Objects.equals(action, UsbManager.ACTION_USB_DEVICE_ATTACHED)) onConnectUsb(context, usbDevice);
        else if (Objects.equals(action, ACTION_USB_PERMISSION)) if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) onGetUsbPer(usbDevice);
    }
  }

  // 检查已连接设备
  public void checkConnectedUsb(Context context) {
    if (AppData.usbManager==null)return;
    for (Map.Entry<String, UsbDevice> entry : AppData.usbManager.getDeviceList().entrySet()) onConnectUsb(context, entry.getValue());
  }

  // 请求USB设备权限
  private void onConnectUsb(Context context, UsbDevice usbDevice) {
    if (AppData.usbManager==null)return;
    Intent usbPermissionIntent = new Intent(ACTION_USB_PERMISSION);
    usbPermissionIntent.setPackage(AppData.main.getPackageName());
    PendingIntent permissionIntent = PendingIntent.getBroadcast(context, 0, usbPermissionIntent, Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0);
    AppData.usbManager.requestPermission(usbDevice, permissionIntent);
  }

  // 当断开设备
  private void onCutUsb(UsbDevice usbDevice) {
    for (Map.Entry<String, UsbDevice> entry : DeviceListAdapter.linkDevices.entrySet()) {
      UsbDevice tmp = entry.getValue();
      if (tmp.getVendorId() == usbDevice.getVendorId() && tmp.getProductId() == usbDevice.getProductId()) {
        for (Client client : Client.allClient) if (client.uuid.equals(entry.getKey())) client.release(AppData.main.getString(R.string.error_stream_closed));
        DeviceListAdapter.linkDevices.remove(entry.getKey());
        Adb.adbMap.remove(entry.getKey());
        ConnectHelper.needStartDefaultUSB.remove(entry.getKey());
        break;
      }
    }
    if (deviceListAdapter != null) deviceListAdapter.update();
  }

  // 处理USB授权结果
  private void onGetUsbPer(UsbDevice usbDevice) {
    // 有线设备使用序列号作为唯一标识符
    String uuid = usbDevice.getSerialNumber();
    if (uuid == null) return;
    // 查找ADB的接口
    for (int i = 0; i < usbDevice.getInterfaceCount(); i++) {
      UsbInterface tmpUsbInterface = usbDevice.getInterface(i);
      if ((tmpUsbInterface.getInterfaceClass() == UsbConstants.USB_CLASS_VENDOR_SPEC) && (tmpUsbInterface.getInterfaceSubclass() == 66) && (tmpUsbInterface.getInterfaceProtocol() == 1)) {
        // 若没有该设备，则新建设备
        Device device = AppData.dbHelper.getByUUID(uuid);
        if (device == null) {
          device = Device.getDefaultDevice(uuid, Device.TYPE_LINK);
          AppData.dbHelper.insert(device);
        }
        DeviceListAdapter.linkDevices.put(uuid, usbDevice);
        if (device.connectOnStart && DeviceListAdapter.startedDefault) {
          ConnectHelper.needStartDefaultUSB.put(device.uuid, device);
          if (connectHelper != null) {
            AppData.uiHandler.removeCallbacks(connectHelper.showStartDefaultUSB);
            AppData.uiHandler.postDelayed(connectHelper.showStartDefaultUSB, 1000);
          }
        }
        if (deviceListAdapter != null) deviceListAdapter.update();
        break;
      }
    }
  }
}
