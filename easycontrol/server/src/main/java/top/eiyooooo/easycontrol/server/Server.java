/*
 * 本项目大量借鉴学习了开源投屏软件：Scrcpy，在此对该项目表示感谢
 */
package top.eiyooooo.easycontrol.server;

import android.annotation.SuppressLint;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.os.IBinder;
import android.os.IInterface;
import android.system.ErrnoException;
import android.system.Os;

import java.io.DataInputStream;
import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import android.view.Display;
import top.eiyooooo.easycontrol.server.entity.Device;
import top.eiyooooo.easycontrol.server.entity.Options;
import top.eiyooooo.easycontrol.server.helper.AudioEncode;
import top.eiyooooo.easycontrol.server.helper.VideoEncode;
import top.eiyooooo.easycontrol.server.helper.ControlPacket;
import top.eiyooooo.easycontrol.server.helper.VirtualDisplay;
import top.eiyooooo.easycontrol.server.wrappers.ClipboardManager;
import top.eiyooooo.easycontrol.server.wrappers.DisplayManager;
import top.eiyooooo.easycontrol.server.wrappers.InputManager;
import top.eiyooooo.easycontrol.server.wrappers.SurfaceControl;
import top.eiyooooo.easycontrol.server.wrappers.WindowManager;

// 此部分代码摘抄借鉴了著名投屏软件Scrcpy的开源代码(https://github.com/Genymobile/scrcpy/tree/master/server)
public final class Server {
  private static LocalSocket socket;
  private static FileDescriptor fileDescriptor;
  public static DataInputStream inputStream;

  private static final Object object = new Object();

  private static final int timeoutDelay = 5 * 1000;

  public static void main(String... args) {
    try {
      Thread timeOutThread = new Thread(() -> {
        try {
          Thread.sleep(timeoutDelay);
          release();
        } catch (InterruptedException ignored) {
        }
      });
      timeOutThread.start();
      // 解析参数
      Options.parse(args);
      // 初始化
      setManagers();
      if (Options.mode == 1) {
        try {
            Device.displayId = VirtualDisplay.create();
            } catch (Exception e) {
            Options.mode = 0;
        }
      }
      Device.init();
      // 连接
      connectClient();
      // 初始化子服务
      boolean canAudio = AudioEncode.init();
      VideoEncode.init();
      // 启动
      ArrayList<Thread> threads = new ArrayList<>();
      threads.add(new Thread(Server::executeVideoOut));
      if (canAudio) {
        threads.add(new Thread(Server::executeAudioIn));
        threads.add(new Thread(Server::executeAudioOut));
      }
      threads.add(new Thread(Server::executeControlIn));
      for (Thread thread : threads) thread.setPriority(Thread.MAX_PRIORITY);
      for (Thread thread : threads) thread.start();
      // 程序运行
      timeOutThread.interrupt();
      if (Options.TurnOnScreenIfStart) {
        Device.keyEvent(224, 0);
        if (Options.TurnOffScreenIfStart)
          postDelayed(() -> Device.changeScreenPowerMode(Display.STATE_UNKNOWN), 2000);
      }
      if (Options.mode == 1) foregroundAppMonitor();
      synchronized (object) {
        object.wait();
      }
      // 终止子服务
      for (Thread thread : threads) thread.interrupt();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (Options.TurnOffScreenIfStop) Device.keyEvent(223, 0);
      else if (Options.TurnOnScreenIfStop) Device.changeScreenPowerMode(Display.STATE_ON);
      // 释放资源
      release();
    }
  }

  private static void foregroundAppMonitor() {
    try {
      if (Device.foregroundApp.length != 7)
        Device.foregroundApp = Device.execReadOutput("am stack list | grep -E 'RootTask id=[0-9]*' | grep -oE -m1 [0-9]*").split("\n");
      else
        Device.foregroundApp = Device.execReadOutput("am stack list | grep -E 'RootTask id=" + Device.foregroundApp[0] + "' | grep -oE -m1 [0-9]*").split("\n");

      if (Device.foregroundApp.length != 7) throw new Exception();

      if (Device.foregroundApp[5].equals("0")) {
        Device.execReadOutput("am display move-stack " + Device.foregroundApp[0] + " " + Device.displayId);
        Device.foregroundApp[5] = String.valueOf(Device.displayId);
        Device.transferredApp.add(Device.foregroundApp[0]);
        ControlPacket.sendDisplayId(Device.displayId);
      }

      postDelayed(Server::foregroundAppMonitor, 2000);
    } catch (Exception ignored0) {
      Device.foregroundApp = new String[0];

      try {
        ControlPacket.sendDisplayId(-Device.displayId);
      } catch (Exception ignored1) {}

      postDelayed(Server::foregroundAppMonitor, 2000);
    }
  }

  public static void postDelayed(Runnable runnable, long delayMillis) {
    new Thread(() -> {
      try {
        Thread.sleep(delayMillis);
        runnable.run();
      } catch (InterruptedException ignored) {
      }
    }).start();
  }

  private static Method GET_SERVICE_METHOD;

  @SuppressLint({"DiscouragedPrivateApi", "PrivateApi"})
  private static void setManagers() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    GET_SERVICE_METHOD = Class.forName("android.os.ServiceManager").getDeclaredMethod("getService", String.class);
    // 1
    WindowManager.init(getService("window", "android.view.IWindowManager"));
    // 2
    DisplayManager.init(Class.forName("android.hardware.display.DisplayManagerGlobal").getDeclaredMethod("getInstance").invoke(null));
    // 3
    Class<?> inputManagerClass;
    try {
      inputManagerClass = Class.forName("android.hardware.input.InputManagerGlobal");
    } catch (ClassNotFoundException e) {
      inputManagerClass = android.hardware.input.InputManager.class;
    }
    InputManager.init(inputManagerClass.getDeclaredMethod("getInstance").invoke(null));
    // 4
    ClipboardManager.init(getService("clipboard", "android.content.IClipboard"));
    // 5
    SurfaceControl.init();
  }

  private static IInterface getService(String service, String type) {
    try {
      IBinder binder = (IBinder) GET_SERVICE_METHOD.invoke(null, service);
      Method asInterfaceMethod = Class.forName(type + "$Stub").getMethod("asInterface", IBinder.class);
      return (IInterface) asInterfaceMethod.invoke(null, binder);
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  private static void connectClient() throws IOException {
    try (LocalServerSocket serverSocket = new LocalServerSocket("easycontrol")) {
      socket = serverSocket.accept();
      fileDescriptor = socket.getFileDescriptor();
      inputStream = new DataInputStream(socket.getInputStream());
    }
  }

  private static void executeVideoOut() {
    try {
      int frame = 0;
      while (!Thread.interrupted()) {
        if (VideoEncode.isHasChangeConfig) {
          VideoEncode.isHasChangeConfig = false;
          VideoEncode.stopEncode();
          VideoEncode.startEncode();
        }
        VideoEncode.encodeOut();
        frame++;
        if (frame > 120) {
          if (System.currentTimeMillis() - lastKeepAliveTime > timeoutDelay) {
            timeoutClose = true;
            throw new IOException("连接断开");
          }
          frame = 0;
        }
      }
    } catch (Exception e) {
      errorClose(e);
    }
  }

  private static void executeAudioIn() {
    while (!Thread.interrupted()) AudioEncode.encodeIn();
  }

  private static void executeAudioOut() {
    try {
      while (!Thread.interrupted()) AudioEncode.encodeOut();
    } catch (IOException | ErrnoException e) {
      errorClose(e);
    }
  }

  private static long lastKeepAliveTime = System.currentTimeMillis();

  private static void executeControlIn() {
    try {
      while (!Thread.interrupted()) {
        switch (Server.inputStream.readByte()) {
          case 1:
            ControlPacket.handleTouchEvent();
            break;
          case 2:
            ControlPacket.handleKeyEvent();
            break;
          case 3:
            ControlPacket.handleClipboardEvent();
            break;
          case 4:
            ControlPacket.sendKeepAlive();
            lastKeepAliveTime = System.currentTimeMillis();
            break;
          case 5:
            Device.changeDeviceSize(inputStream.readFloat());
            break;
          case 6:
            Device.rotateDevice();
            break;
          case 7:
            Device.changeScreenPowerMode(inputStream.readByte());
            break;
          case 8:
            Device.changePower();
            break;
        }
      }
    } catch (Exception e) {
      errorClose(e);
    }
  }

  public synchronized static void write(ByteBuffer byteBuffer) throws IOException, ErrnoException {
    while (byteBuffer.remaining() > 0) Os.write(fileDescriptor, byteBuffer);
  }

  public static void errorClose(Exception e) {
    e.printStackTrace();
    synchronized (object) {
      object.notify();
    }
  }

  private static boolean timeoutClose = false;

  // 释放资源
  private static void release() {
    for (int i = 0; i < 4; i++) {
      try {
        switch (i) {
          case 0:
            inputStream.close();
            socket.close();
            break;
          case 1:
            for (String transferredAppId : Device.transferredApp) {
              try {
                Device.execReadOutput("am display move-stack " + transferredAppId + " 0");
              } catch (Exception ignored) {}
            }
            if (Options.mode == 1)  VirtualDisplay.release();
            VideoEncode.release();
            AudioEncode.release();
            break;
          case 2:
            if (Device.needReset) {
              if (Device.resetInfo_Width != 0 && Device.resetInfo_Height != 0)
                Device.execReadOutput("wm size " + Device.resetInfo_Width + "x" + Device.resetInfo_Height);
              else
                Device.execReadOutput("wm size reset");

              try {
                Thread.sleep(500);
              } catch (InterruptedException ignored) {
              }

              if (Device.resetInfo_Density != 0)
                Device.execReadOutput("wm density " + Device.resetInfo_Density);
            }
            if (Options.keepAwake) Device.execReadOutput("settings put system screen_off_timeout " + Device.oldScreenOffTimeout);
          case 3:
            if (timeoutClose)
              Device.execReadOutput("ps -ef | grep easycontrol.server | grep -v grep | grep -E \"^[a-z]+ +[0-9]+\" -o | grep -E \"[0-9]+\" -o | xargs kill -9");
            break;
        }
      } catch (Exception ignored) {
      }
    }
  }

}
