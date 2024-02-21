package top.eiyooooo.easycontrol.app.client;

import static android.content.ClipDescription.MIMETYPE_TEXT_PLAIN;

import android.content.ClipData;
import android.hardware.usb.UsbDevice;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Pair;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import android.view.View;
import android.view.WindowManager;
import top.eiyooooo.easycontrol.app.entity.AppData;
import top.eiyooooo.easycontrol.app.entity.Device;
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
  private Adb adb;
  private BufferStream bufferStream;
  private BufferStream shell;

  // 子服务
  private final Thread executeStreamInThread = new Thread(this::executeStreamIn);
  private HandlerThread handlerThread;
  private Handler handler;
  private VideoDecode videoDecode;
  private AudioDecode audioDecode;
  public final ControlPacket controlPacket = new ControlPacket(this::write);
  public final ClientView clientView;
  public final String uuid;
  public int mode; // 0为屏幕镜像模式，1为应用流转模式
  private Thread startThread;
  private Thread loadingTimeOutThread;
  private Thread keepAliveThread;
  private static final int timeoutDelay = 5 * 1000;
  private static long lastKeepAliveTime;

  private static final String serverName = "/data/local/tmp/easycontrol_for_car_server_" + BuildConfig.VERSION_CODE + ".jar";
  private static final boolean supportH265 = PublicTools.isDecoderSupport("hevc");
  private static final boolean supportOpus = PublicTools.isDecoderSupport("opus");

  public Client(Device device, UsbDevice usbDevice) {
    this(device, usbDevice, 0);
  }

  public Client(Device device, UsbDevice usbDevice, int mode) {
    allClient.add(this);
    // 初始化
    uuid = device.uuid;
    this.mode = mode;
    if (device.setResolution & mode == 1) PublicTools.logToast("应用流转模式下暂不支持自由缩放哦");
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      handlerThread = new HandlerThread("easycontrol_mediacodec");
      handlerThread.start();
      handler = new Handler(handlerThread.getLooper());
    }
    clientView = new ClientView(device, mode, controlPacket, () -> {
      status = 1;
      executeStreamInThread.start();
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
        startServer(device);
        connectServer();
        AppData.uiHandler.post(() -> {
          if (device.defaultFull) clientView.changeToFull();
          else clientView.changeToSmall();
        });
      } catch (Exception e) {
        release(Arrays.toString(e.getStackTrace()));
      } finally {
        if (loading.first.getParent() != null) AppData.windowManager.removeView(loading.first);
        if (loadingTimeOutThread != null) loadingTimeOutThread.interrupt();
        keepAliveThread.start();
      }
    });
    AppData.windowManager.addView(loading.first, loading.second);
    loadingTimeOutThread.start();
    startThread.start();
  }

  // 连接ADB
  private static Adb connectADB(Device device, UsbDevice usbDevice) throws Exception {
    if (usbDevice == null) {
      Pair<String, Integer> address = PublicTools.getIpAndPort(device.address);
      return new Adb(address.first, address.second, AppData.keyPair);
    } else return new Adb(usbDevice, AppData.keyPair);
  }

  // 启动Server
  private void startServer(Device device) throws Exception {
    if (BuildConfig.ENABLE_DEBUG_FEATURE || !adb.runAdbCmd("ls /data/local/tmp/easycontrol_*").contains(serverName)) {
      adb.runAdbCmd("rm /data/local/tmp/easycontrol_* ");
      adb.pushFile(AppData.main.getResources().openRawResource(R.raw.easycontrol_server), serverName);
    }
    shell = adb.getShell();
    int ScreenMode = (AppData.setting.getTurnOnScreenIfStart() ? 1 : 0) * 1000
            + (AppData.setting.getTurnOffScreenIfStart() ? 1 : 0) * 100
            + (AppData.setting.getTurnOffScreenIfStop() ? 1 : 0) * 10
            + (AppData.setting.getTurnOnScreenIfStop() ? 1 : 0);
    shell.write(ByteBuffer.wrap(("app_process -Djava.class.path=" + serverName + " / top.eiyooooo.easycontrol.server.Server"
      + " isAudio=" + (device.isAudio ? 1 : 0)
      + " maxSize=" + device.maxSize
      + " maxFps=" + device.maxFps
      + " maxVideoBit=" + device.maxVideoBit
      + " mode=" + (mode == 1 ? 1 : 0)
      + " keepAwake=" + (AppData.setting.getKeepAwake() ? 1 : 0)
      + " ScreenMode=" + ScreenMode
      + " useH265=" + ((device.useH265 && supportH265) ? 1 : 0)
      + " useOpus=" + ((device.useOpus && supportOpus) ? 1 : 0) + " \n").getBytes()));
  }

  // 连接Server
  private void connectServer() throws Exception {
    Thread.sleep(50);
    for (int i = 0; i < 60; i++) {
      try {
        bufferStream = adb.localSocketForward("easycontrol");
        return;
      } catch (Exception ignored) {
        Thread.sleep(50);
      }
    }
    throw new Exception(AppData.main.getString(R.string.error_connect_server));
  }

  // 服务分发
  private static final int VIDEO_EVENT = 1;
  private static final int AUDIO_EVENT = 2;
  private static final int CLIPBOARD_EVENT = 3;
  private static final int CHANGE_SIZE_EVENT = 4;
  private static final int KEEP_ALIVE_EVENT = 5;
  private static final int DISPLAY_ID_EVENT = 6;

  private void executeStreamIn() {
    try {
      Pair<byte[], Long> videoCsd = null;
      // 音视频流参数
      boolean useOpus = true;
      if (bufferStream.readByte() == 1) useOpus = bufferStream.readByte() == 1;
      boolean useH265 = bufferStream.readByte() == 1;
      // 循环处理报文
      while (!Thread.interrupted()) {
        switch (bufferStream.readByte()) {
          case VIDEO_EVENT:
            byte[] videoFrame = controlPacket.readFrame(bufferStream);
            if (videoDecode != null) videoDecode.decodeIn(videoFrame, bufferStream.readLong());
            else {
              if (useH265) videoDecode = new VideoDecode(clientView.getVideoSize(), clientView.getSurface(), new Pair<>(videoFrame, bufferStream.readLong()), null, handler);
              else {
                if (videoCsd == null) videoCsd = new Pair<>(videoFrame, bufferStream.readLong());
                else videoDecode = new VideoDecode(clientView.getVideoSize(), clientView.getSurface(), videoCsd, new Pair<>(videoFrame, bufferStream.readLong()), handler);
              }
            }
            break;
          case AUDIO_EVENT:
            byte[] audioFrame = controlPacket.readFrame(bufferStream);
            if (audioDecode != null) audioDecode.decodeIn(audioFrame);
            else audioDecode = new AudioDecode(useOpus, audioFrame, handler);
            break;
          case CLIPBOARD_EVENT:
            controlPacket.nowClipboardText = new String(bufferStream.readByteArray(bufferStream.readInt()).array());
            AppData.clipBoard.setPrimaryClip(ClipData.newPlainText(MIMETYPE_TEXT_PLAIN, controlPacket.nowClipboardText));
            break;
          case CHANGE_SIZE_EVENT:
            Pair<Integer, Integer> newVideoSize = new Pair<>(bufferStream.readInt(), bufferStream.readInt());
            AppData.uiHandler.post(() -> clientView.updateVideoSize(newVideoSize));
            break;
          case KEEP_ALIVE_EVENT:
            lastKeepAliveTime = System.currentTimeMillis();
            break;
          case DISPLAY_ID_EVENT:
            int displayId = bufferStream.readByte();
            if (mode == 1) {
              if (displayId == 0) {
                PublicTools.logToast(AppData.main.getString(R.string.error_create_display));
                changeMode(0);
              }
              else if (displayId < 0) PublicTools.logToast(AppData.main.getString(R.string.error_transferred_app_failed));
            }
            break;
        }
      }
    } catch (Exception ignored) {
      String serverError = "";
      try {
        serverError = new String(shell.readAllBytes().array());
      } catch (IOException | InterruptedException ignored1) {
      }
      release(AppData.main.getString(R.string.error_stream_closed) + serverError);
    }
  }

  private void executeOtherService() {
    if (status == 1) {
      controlPacket.checkClipBoard();
      controlPacket.sendKeepAlive();
      AppData.uiHandler.postDelayed(this::executeOtherService, 1500);
    }
  }

  private void write(ByteBuffer byteBuffer) {
    try {
      bufferStream.write(byteBuffer);
    } catch (Exception ignored) {
      String serverError = "";
      try {
        serverError = new String(shell.readAllBytes().array());
      } catch (IOException | InterruptedException ignored1) {
      }
      release(AppData.main.getString(R.string.error_stream_closed) + serverError);
    }
  }

  public void release(String error) {
    if (status == -1) return;
    status = -1;
    allClient.remove(this);
    if (error != null) PublicTools.logToast(error);
    for (int i = 0; i < 4; i++) {
      try {
        switch (i) {
          case 0:
            keepAliveThread.interrupt();
            executeStreamInThread.interrupt();
            if (handlerThread != null) handlerThread.quit();
            break;
          case 1:
            AppData.uiHandler.post(() -> clientView.hide(true));
            break;
          case 2:
            bufferStream.close();
            adb.close();
            break;
          case 3:
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
        adb.close();
        handle.run(true);
      } catch (Exception ignored) {
        handle.run(false);
      }
    }).start();
  }

  public static void restartOnTcpip(Device device, UsbDevice usbDevice, PublicTools.MyFunctionBoolean handle) {
    new Thread(() -> {
      try {
        Adb adb = connectADB(device, usbDevice);
        String output = adb.restartOnTcpip(5555);
        adb.close();
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

  private void changeMode(int mode) {
    this.mode = mode;
    clientView.changeMode(mode);
  }
}
