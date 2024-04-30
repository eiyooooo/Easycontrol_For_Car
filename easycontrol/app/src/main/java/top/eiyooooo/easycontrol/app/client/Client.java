package top.eiyooooo.easycontrol.app.client;

import static android.content.ClipDescription.MIMETYPE_TEXT_PLAIN;

import android.content.ClipData;
import android.hardware.usb.UsbDevice;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Pair;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import top.eiyooooo.easycontrol.app.entity.AppData;
import top.eiyooooo.easycontrol.app.entity.Device;
import top.eiyooooo.easycontrol.app.helper.L;
import top.eiyooooo.easycontrol.app.helper.PublicTools;
import top.eiyooooo.easycontrol.app.BuildConfig;
import top.eiyooooo.easycontrol.app.R;
import top.eiyooooo.easycontrol.app.adb.Adb;
import top.eiyooooo.easycontrol.app.buffer.BufferStream;
import top.eiyooooo.easycontrol.app.client.view.ClientView;

public class Client {
  // 状态，0为初始，1为连接，-1为关闭
  private int status = 0;
  public static final ArrayList<Client> allClient = new ArrayList<>();

  // 连接
  public Adb adb;
  private BufferStream bufferStream;
  private BufferStream videoStream;
  private BufferStream shell;

  // 子服务
  private final Thread executeStreamInThread = new Thread(this::executeStreamIn);
  private final Thread executeStreamVideoThread = new Thread(this::executeStreamVideo);
  private HandlerThread handlerThread;
  private Handler handler;
  private VideoDecode videoDecode;
  private AudioDecode audioDecode;
  public final ControlPacket controlPacket = new ControlPacket(this::write);
  public final ClientView clientView;
  public final String uuid;
  public int mode = 0; // 0为屏幕镜像模式，1为应用流转模式
  public int displayId = 0;
  private Thread startThread;
  private final Thread loadingTimeOutThread;
  private final Thread keepAliveThread;
  private static final int timeoutDelay = 5 * 1000;
  private long lastKeepAliveTime;
  public int multiLink = 0; // 0为单连接，1为多连接主，2为多连接从

  private static final String serverName = "/data/local/tmp/easycontrol_for_car_server_" + BuildConfig.VERSION_CODE + ".jar";
  private static final boolean supportH265 = PublicTools.isDecoderSupport("hevc");
  private static final boolean supportOpus = PublicTools.isDecoderSupport("opus");

  public Client(Device device, UsbDevice usbDevice, int mode) {
    for (Client client : allClient) {
      if (client.uuid.equals(device.uuid)) {
        if (client.multiLink == 0) client.changeMultiLinkMode(1);
        this.multiLink = 2;
        break;
      }
    }
    allClient.add(this);
    // 初始化
    uuid = device.uuid;
    if (mode == 0) specifiedTransferred = true;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      handlerThread = new HandlerThread("easycontrol_mediacodec");
      handlerThread.start();
      handler = new Handler(handlerThread.getLooper());
    }
    clientView = new ClientView(device, controlPacket, this::changeMode, () -> {
      status = 1;
      executeStreamInThread.start();
      executeStreamVideoThread.start();
      AppData.uiHandler.post(this::executeOtherService);
    }, () -> release(null));
    Pair<View, WindowManager.LayoutParams> loading = PublicTools.createLoading(AppData.main);
    // 连接
    loadingTimeOutThread = new Thread(() -> {
      try {
        Thread.sleep(timeoutDelay);
        if (startThread != null) startThread.interrupt();
        if (loading.first.getParent() != null) AppData.windowManager.removeView(loading.first);
        release(null);
      } catch (InterruptedException ignored) {
      }
    });
    keepAliveThread = new Thread(() -> {
      lastKeepAliveTime = System.currentTimeMillis();
      while (status != -1) {
        if (System.currentTimeMillis() - lastKeepAliveTime > timeoutDelay)
          release(AppData.main.getString(R.string.error_stream_closed));
        try {
          Thread.sleep(1500);
        } catch (InterruptedException ignored) {
        }
      }
    });
    startThread = new Thread(() -> {
      try {
        adb = connectADB(device, usbDevice);
        changeMode(mode);
        changeMultiLinkMode(multiLink);
        startServer(device);
        connectServer();
        AppData.uiHandler.post(() -> {
          controlPacket.sendNightModeEvent(AppData.nightMode);
          if (AppData.setting.getAlwaysFullMode() || device.defaultFull) clientView.changeToFull();
          else clientView.changeToSmall();
        });
      } catch (Exception e) {
        L.log(device.uuid, e);
        release(AppData.main.getString(R.string.log_notify));
      } finally {
        if (!AppData.setting.getAlwaysFullMode() && loading.first.getParent() != null) AppData.windowManager.removeView(loading.first);
        loadingTimeOutThread.interrupt();
        keepAliveThread.start();
      }
    });
    if (AppData.setting.getAlwaysFullMode()) PublicTools.logToast(AppData.main.getString(R.string.loading_text));
    else AppData.windowManager.addView(loading.first, loading.second);
    loadingTimeOutThread.start();
    startThread.start();
  }

  // 连接ADB
  private static Adb connectADB(Device device, UsbDevice usbDevice) throws Exception {
    if (Adb.adbMap.containsKey(device.uuid)) return Adb.adbMap.get(device.uuid);
    Adb adb;
    if (usbDevice == null) adb = new Adb(device.uuid, device.address, AppData.keyPair);
    else adb = new Adb(device.uuid, usbDevice, AppData.keyPair);
    Adb.adbMap.put(device.uuid, adb);
    return adb;
  }

  // 启动Server
  private void startServer(Device device) throws Exception {
    if (adb.serverShell == null || adb.serverShell.isClosed()) adb.startServer();
    shell = adb.getShell();
    int ScreenMode = (AppData.setting.getTurnOnScreenIfStart() ? 1 : 0) * 1000
            + (AppData.setting.getTurnOffScreenIfStart() ? 1 : 0) * 100
            + (AppData.setting.getTurnOffScreenIfStop() ? 1 : 0) * 10
            + (AppData.setting.getTurnOnScreenIfStop() ? 1 : 0);
    StringBuilder cmd = new StringBuilder();
    cmd.append("app_process -Djava.class.path=").append(serverName).append(" / top.eiyooooo.easycontrol.server.Scrcpy");
    if (!device.isAudio) cmd.append(" isAudio=0");
    if (device.maxSize != 1600) cmd.append(" maxSize=").append(device.maxSize);
    if (device.maxFps != 60) cmd.append(" maxFps=").append(device.maxFps);
    if (device.maxVideoBit != 4) cmd.append(" maxVideoBit=").append(device.maxVideoBit);
    if (displayId != 0) cmd.append(" displayId=").append(displayId);
    if (AppData.setting.getMirrorMode()) cmd.append(" mirrorMode=1");
    if (!AppData.setting.getKeepAwake()) cmd.append(" keepAwake=0");
    if (ScreenMode != 1001) cmd.append(" ScreenMode=").append(ScreenMode);
    if (!(device.useH265 && supportH265)) cmd.append(" useH265=0");
    if (!(device.useOpus && supportOpus)) cmd.append(" useOpus=0");
    cmd.append(" \n");
    shell.write(ByteBuffer.wrap(cmd.toString().getBytes()));
    logger();
  }

  private Thread loggerThread;
  private void logger() {
    loggerThread = new Thread(() -> {
      try {
        while (!Thread.interrupted()) {
          String log = new String(shell.readAllBytes().array(), StandardCharsets.UTF_8);
          if (!log.isEmpty()) L.logWithoutTime(uuid, log);
          Thread.sleep(1000);
        }
      } catch (Exception ignored) {
      }
    });
    loggerThread.start();
  }

  private void tryCreateDisplay(Device device) {
    try {
      if (AppData.setting.getForceDesktopMode()) adb.runAdbCmd("settings put global force_desktop_mode_on_external_displays 1");
      else adb.runAdbCmd("settings put global force_desktop_mode_on_external_displays 0");

      String output = Adb.getStringResponseFromServer(device, "createVirtualDisplay");
      if (output.contains("success")) {
        displayId = Integer.parseInt(output.substring(output.lastIndexOf(" -> ") + 4));
        clientView.displayId = displayId;
        changeMode(1);
        PublicTools.logToast(AppData.main.getString(R.string.tip_application_transfer));
      } else throw new Exception("");
    } catch (Exception ignored) {
      changeMode(0);
      PublicTools.logToast(AppData.main.getString(R.string.error_create_display));
    }
  }

  boolean specifiedTransferred = false;
  private void appTransfer(Device device) {
    try {
      JSONArray tasksArray = null;
      try {
        JSONObject tasks = new JSONObject(Adb.getStringResponseFromServer(device, "getRecentTasks"));
        tasksArray = tasks.getJSONArray("data");
        for (int i = 0; i < tasksArray.length(); i++) {
          int taskId = tasksArray.getJSONObject(i).getInt("taskId");
          String topPackage = tasksArray.getJSONObject(i).getString("topPackage");
          if (taskId <= 0 || topPackage.isEmpty()) {
            tasksArray.remove(i);
            i--;
          }
        }
      } catch (Exception ignored) {
      }
      if (!specifiedTransferred && !device.specified_app.isEmpty()) {
        String checkApp = Adb.getStringResponseFromServer(device, "getAppMainActivity", "package=" + device.specified_app);
        if (checkApp.isEmpty()) {
          PublicTools.logToast(AppData.main.getString(R.string.error_app_not_found));
          throw new Exception("");
        } else {
          int appTaskId = 0;
          if (tasksArray != null) {
            for (int i = 0; i < tasksArray.length(); i++) {
              if (tasksArray.getJSONObject(i).getString("topPackage").equals(device.specified_app)) {
                try {
                  appTaskId = tasksArray.getJSONObject(i).getInt("taskId");
                } catch (JSONException ignored) {
                }
                break;
              }
            }
          }
          if (appTaskId == 0) {
            String output = Adb.getStringResponseFromServer(device, "openAppByPackage", "package=" + device.specified_app, "displayId=" + displayId);
            if (output.contains("failed")) throw new Exception("");
          } else {
            String output = adb.runAdbCmd("am display move-stack " + appTaskId + " " + displayId);
            if (output.contains("Exception")) throw new Exception("");
          }
          specifiedTransferred = true;
        }
      } else {
        if (tasksArray != null && tasksArray.length() > 0) {
          String output = adb.runAdbCmd("am display move-stack " + tasksArray.getJSONObject(0).getInt("taskId") + " " + displayId);
          if (output.contains("Exception")) throw new Exception("");
        } else throw new Exception("");
      }
    } catch (Exception ignored) {
      specifiedTransferred = true;
      changeMode(0);
      PublicTools.logToast(AppData.main.getString(R.string.error_transfer_app_failed));
    }
  }

  // 连接Server
  private void connectServer() throws Exception {
    Thread.sleep(50);
    for (int i = 0; i < 60; i++) {
      try {
        bufferStream = adb.localSocketForward("easycontrol_for_car_scrcpy");
        videoStream = adb.localSocketForward("easycontrol_for_car_scrcpy");
        return;
      } catch (Exception ignored) {
        Thread.sleep(50);
      }
    }
    throw new Exception(AppData.main.getString(R.string.error_connect_server));
  }

  // 服务分发
  private static final int AUDIO_EVENT = 2;
  private static final int CLIPBOARD_EVENT = 3;
  private static final int CHANGE_SIZE_EVENT = 4;
  private static final int KEEP_ALIVE_EVENT = 5;

  private void executeStreamVideo() {
    try {
      // 视频流参数
      boolean useH265 = videoStream.readByte() == 1;
      Pair<Integer, Integer> videoSize = new Pair<>(videoStream.readInt(), videoStream.readInt());
      Surface surface = clientView.getSurface();
      byte[] videoFrame = controlPacket.readFrame(videoStream);
      Pair<byte[], Long> csd0 = new Pair<>(videoFrame, videoStream.readLong());
      Pair<byte[], Long> csd1 = useH265 ? null : new Pair<>(videoFrame, videoStream.readLong());
      videoDecode = new VideoDecode(videoSize, surface, csd0, csd1, handler);
      // 循环处理报文
      while (!Thread.interrupted()) {
        videoDecode.decodeIn(controlPacket.readFrame(videoStream), videoStream.readLong());
      }
    } catch (Exception e) {
      L.log(uuid, e);
      release(AppData.main.getString(R.string.log_notify));
    }
  }

  private void executeStreamIn() {
    try {
      // 音频流参数
      boolean useOpus = true;
      if (bufferStream.readByte() == 1) useOpus = bufferStream.readByte() == 1;
      // 循环处理报文
      while (!Thread.interrupted()) {
        switch (bufferStream.readByte()) {
          case AUDIO_EVENT:
            byte[] audioFrame = controlPacket.readFrame(bufferStream);
            if (audioDecode != null) audioDecode.decodeIn(audioFrame);
            else {
              audioDecode = new AudioDecode(useOpus, audioFrame, handler);
              if (multiLink != 2) playAudio(true);
            }
            break;
          case CLIPBOARD_EVENT:
            controlPacket.nowClipboardText = new String(bufferStream.readByteArray(bufferStream.readInt()).array());
            if (clientView.device.clipboardSync) AppData.clipBoard.setPrimaryClip(ClipData.newPlainText(MIMETYPE_TEXT_PLAIN, controlPacket.nowClipboardText));
            break;
          case CHANGE_SIZE_EVENT:
            Pair<Integer, Integer> newVideoSize = new Pair<>(bufferStream.readInt(), bufferStream.readInt());
            AppData.uiHandler.post(() -> clientView.updateVideoSize(newVideoSize));
            break;
          case KEEP_ALIVE_EVENT:
            lastKeepAliveTime = System.currentTimeMillis();
            break;
        }
      }
    } catch (Exception e) {
      L.log(uuid, e);
      release(AppData.main.getString(R.string.log_notify));
    }
  }

  private void executeOtherService() {
    if (status == 1) {
      if (clientView.device.clipboardSync) controlPacket.checkClipBoard();
      controlPacket.sendKeepAlive();
      AppData.uiHandler.postDelayed(this::executeOtherService, 1500);
    }
  }

  private void write(ByteBuffer byteBuffer) {
    try {
      bufferStream.write(byteBuffer);
    } catch (Exception e) {
      L.log(uuid, e);
      release(AppData.main.getString(R.string.log_notify));
    }
  }

  public void release(String error) {
    if (status == -1) return;
    status = -1;
    allClient.remove(this);
    if (error != null) PublicTools.logToast(error);
    for (int i = 0; i < 7; i++) {
      try {
        switch (i) {
          case 0:
            try {
              Adb.getStringResponseFromServer(clientView.device, "releaseVirtualDisplay", "id=" + displayId);
            } catch (Exception ignored) {
            }
            break;
          case 1:
            if (multiLink == 1) {
              Client target = null;
              boolean multi = false;
              for (Client client : allClient) {
                if (client.uuid.equals(uuid) && client.multiLink == 2) {
                  if (target != null) {
                    multi = true;
                    break;
                  }
                  target = client;
                }
              }
              if (target != null) {
                if (multi) target.changeMultiLinkMode(1);
                else target.changeMultiLinkMode(0);
              }
            }
            break;
          case 2:
            if (loggerThread != null) loggerThread.interrupt();
            String log = new String(shell.readAllBytes().array(), StandardCharsets.UTF_8);
            if (!log.isEmpty()) L.logWithoutTime(uuid, log);
            break;
          case 3:
            keepAliveThread.interrupt();
            executeStreamInThread.interrupt();
            executeStreamVideoThread.interrupt();
            if (handlerThread != null) handlerThread.quit();
            break;
          case 4:
            AppData.uiHandler.post(() -> clientView.hide(true));
            break;
          case 5:
            bufferStream.close();
            break;
          case 6:
            videoDecode.release();
            if (audioDecode != null) audioDecode.release();
            break;
        }
      } catch (Exception ignored) {
      }
    }
  }

  public static void runOnceCmd(Device device, UsbDevice usbDevice, String cmd, PublicTools.MyFunctionBoolean handle) {
    new Thread(() -> {
      try {
        Adb adb = connectADB(device, usbDevice);
        adb.runAdbCmd(cmd);
        handle.run(true);
      } catch (Exception ignored) {
        handle.run(false);
      }
    }).start();
  }

  public static ArrayList<String> getAppList(Device device, UsbDevice usbDevice) {
    try {
      if (Adb.adbMap.get(device.uuid) == null) {
        if (device.isLinkDevice()) Adb.adbMap.put(device.uuid, new Adb(device.uuid, usbDevice, AppData.keyPair));
        else Adb.adbMap.put(device.uuid, new Adb(device.uuid, device.address, AppData.keyPair));
      }
      ArrayList<String> appList = new ArrayList<>();
      String output = Adb.getStringResponseFromServer(device, "getAllAppInfo", "app_type=1");
      String[] allAppInfo = output.split("<!@n@!>");
      for (String info : allAppInfo) {
        String[] appInfo = info.split("<!@r@!>");
        if (appInfo.length > 1) appList.add(appInfo[1] + "@" + appInfo[0]);
      }
      return appList;
    } catch (Exception e) {
      L.log(device.uuid, e);
      return new ArrayList<>();
    }
  }

  public static void restartOnTcpip(Device device, UsbDevice usbDevice, PublicTools.MyFunctionBoolean handle) {
    new Thread(() -> {
      try {
        Adb adb = connectADB(device, usbDevice);
        String output = adb.restartOnTcpip(5555);
        handle.run(output.contains("restarting"));
      } catch (Exception ignored) {
        handle.run(false);
      }
    }).start();
  }

  // 检查是否启动完成
  public boolean isStarted() {
    return status == 1 && clientView != null;
  }

  public void changeMode(int mode) {
    if (this.mode == mode) return;
    this.mode = mode;
    clientView.changeSizeLock.set(false);
    if (mode == 0) {
      try {
        Adb.getStringResponseFromServer(clientView.device, "releaseVirtualDisplay", "id=" + displayId);
      } catch (Exception ignored) {
      }
      displayId = 0;
      clientView.displayId = 0;
    } else if (mode == 1) {
      tryCreateDisplay(clientView.device);
      if (displayId == 0) return;
    }
    new Thread(() -> {
      try {
        while (!isStarted()) {
          Thread.sleep(1000);
        }
        controlPacket.sendConfigChangedEvent(-displayId);
        if (mode != 0) appTransfer(clientView.device);
        synchronized (clientView.changeSizeLock) {
          clientView.changeSizeLock.set(true);
          clientView.changeSizeLock.notifyAll();
        }
      } catch (Exception ignored) {
      }
    }).start();
    clientView.changeMode(mode);
  }

  public void changeMultiLinkMode(int multiLink) {
    playAudio(multiLink == 0 || multiLink == 1);
    if (multiLink == 2) {
      clientView.device.clipboardSync = false;
      clientView.device.nightModeSync = false;
    } else if (multiLink == 0 || multiLink == 1) {
      if (clientView.deviceOriginal.clipboardSync) clientView.device.clipboardSync = true;
      if (clientView.deviceOriginal.nightModeSync) clientView.device.nightModeSync = true;
    }
    this.multiLink = multiLink;
    clientView.multiLink = multiLink;
  }

  public void playAudio(boolean play) {
    if (audioDecode != null) audioDecode.playAudio(play);
  }
}
