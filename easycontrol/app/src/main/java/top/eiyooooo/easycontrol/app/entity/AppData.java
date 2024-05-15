package top.eiyooooo.easycontrol.app.entity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.UiModeManager;
import android.content.ClipboardManager;
import android.content.Context;
import android.hardware.SensorManager;
import android.hardware.usb.UsbManager;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import java.io.File;

import top.eiyooooo.easycontrol.app.adb.AdbBase64;
import top.eiyooooo.easycontrol.app.adb.AdbKeyPair;
import top.eiyooooo.easycontrol.app.helper.DbHelper;
import top.eiyooooo.easycontrol.app.helper.MyBroadcastReceiver;

public class AppData {
  @SuppressLint("StaticFieldLeak")
  public static Context main;
  @SuppressLint("StaticFieldLeak")
  public static Context activity;
  public static Handler uiHandler;

  // 数据库工具库
  public static DbHelper dbHelper;

  // 密钥文件
  public static AdbKeyPair keyPair;

  // 系统服务
  public static ClipboardManager clipBoard;
  public static WifiManager wifiManager;
  public static UsbManager usbManager;
  public static WindowManager windowManager;
  public static SensorManager sensorManager;
  public static UiModeManager uiModeManager;

  // 设置值
  public static Setting setting;

  // 系统分辨率
  public static final DisplayMetrics realScreenSize = new DisplayMetrics();

  // 当前黑暗模式
  public static int nightMode;

  // 广播
  public static final MyBroadcastReceiver myBroadcastReceiver = new MyBroadcastReceiver();

  public static void init(Activity m) {
    activity = m;
    uiHandler = new Handler(m.getMainLooper());
    if (main != null) return;
    main = m.getApplicationContext();
    dbHelper = new DbHelper(main);
    clipBoard = (ClipboardManager) main.getSystemService(Context.CLIPBOARD_SERVICE);
    wifiManager = (WifiManager) main.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    usbManager = (UsbManager) main.getSystemService(Context.USB_SERVICE);
    windowManager = (WindowManager) main.getSystemService(Context.WINDOW_SERVICE);
    sensorManager = (SensorManager) main.getSystemService(Context.SENSOR_SERVICE);
    uiModeManager = (UiModeManager) main.getSystemService(Context.UI_MODE_SERVICE);
    nightMode = uiModeManager.getNightMode();
    myBroadcastReceiver.register(main);
    myBroadcastReceiver.checkConnectedUsb(main);
    getRealScreenSize(m);
    setting = new Setting(main.getSharedPreferences("setting", Context.MODE_PRIVATE));
    // 读取密钥文件
    try {
      AdbKeyPair.setAdbBase64(new AdbBase64() {
        @Override
        public String encodeToString(byte[] data) {
          return Base64.encodeToString(data, Base64.DEFAULT);
        }

        @Override
        public byte[] decode(byte[] data) {
          return Base64.decode(data, Base64.DEFAULT);
        }
      });
      File privateKey = new File(main.getApplicationContext().getFilesDir(), "private.key");
      File publicKey = new File(main.getApplicationContext().getFilesDir(), "public.key");
      if (!privateKey.isFile() || !publicKey.isFile()) AdbKeyPair.generate(privateKey, publicKey);
      keyPair = AdbKeyPair.read(privateKey, publicKey);
    } catch (Exception ignored) {
      reGenerateAdbKeyPair(main);
    }
  }

  public static void release() {
    myBroadcastReceiver.unRegister(main);
    dbHelper.close();
    activity = null;
    main = null;
    System.exit(0);
  }

  // 生成密钥
  public static void reGenerateAdbKeyPair(Context context) {
    try {
      // 读取密钥文件
      File privateKey = new File(context.getApplicationContext().getFilesDir(), "private.key");
      File publicKey = new File(context.getApplicationContext().getFilesDir(), "public.key");
      AdbKeyPair.generate(privateKey, publicKey);
      AppData.keyPair = AdbKeyPair.read(privateKey, publicKey);
    } catch (Exception ignored) {
    }
  }

  // 获取设备真实分辨率
  private static void getRealScreenSize(Activity m) {
    Display display = m.getWindowManager().getDefaultDisplay();
    display.getRealMetrics(realScreenSize);
    int rotation = display.getRotation();
    if (rotation == 1 || rotation == 3) {
      int tmp = realScreenSize.heightPixels;
      realScreenSize.heightPixels = realScreenSize.widthPixels;
      realScreenSize.widthPixels = tmp;
    }
  }
}
