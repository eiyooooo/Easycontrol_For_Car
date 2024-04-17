package top.eiyooooo.easycontrol.server;

import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.system.ErrnoException;
import android.system.Os;
import android.view.Display;
import top.eiyooooo.easycontrol.server.entity.Device;
import top.eiyooooo.easycontrol.server.entity.Options;
import top.eiyooooo.easycontrol.server.helper.AudioEncode;
import top.eiyooooo.easycontrol.server.helper.ControlPacket;
import top.eiyooooo.easycontrol.server.helper.VideoEncode;
import top.eiyooooo.easycontrol.server.utils.L;
import top.eiyooooo.easycontrol.server.utils.Workarounds;
import top.eiyooooo.easycontrol.server.wrappers.ServiceManager;
import top.eiyooooo.easycontrol.server.wrappers.UiModeManager;

import java.io.DataInputStream;
import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public final class Scrcpy {
    private static LocalSocket socket;
    private static FileDescriptor fileDescriptor;
    public static DataInputStream inputStream;
    public static boolean started = false;
    private static final Object object = new Object();
    private static final int timeoutDelay = 5 * 1000;

    public static void main(String... args) {
        L.logMode = 1;
        L.postLog();
        started = true;
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
            Workarounds.apply(1);
            ServiceManager.setManagers();
            Device.init();
            // 连接
            connectClient();
            // 初始化子服务
            boolean canAudio = AudioEncode.init();
            VideoEncode.init();
            // 启动
            ArrayList<Thread> threads = new ArrayList<>();
            threads.add(new Thread(Scrcpy::executeVideoOut));
            if (canAudio) {
                threads.add(new Thread(Scrcpy::executeAudioIn));
                threads.add(new Thread(Scrcpy::executeAudioOut));
            }
            threads.add(new Thread(Scrcpy::executeControlIn));
            for (Thread thread : threads) thread.setPriority(Thread.MAX_PRIORITY);
            for (Thread thread : threads) thread.start();
            // 程序运行
            timeOutThread.interrupt();
            if (Options.TurnOnScreenIfStart) {
                Device.keyEvent(224, 0, 0);
                if (Options.TurnOffScreenIfStart)
                    postDelayed(() -> Device.changeScreenPowerMode(Display.STATE_UNKNOWN), 2000);
            }
            synchronized (object) {
                object.wait();
            }
            // 终止子服务
            for (Thread thread : threads) thread.interrupt();
        } catch (Exception e) {
            L.e("startScrcpy error", e);
        } finally {
            // 释放资源
            release();
            started = false;
        }
    }

    public static void postDelayed(Runnable runnable, long delayMillis) {
        new Thread(() -> {
            try {
                Thread.sleep(delayMillis);
                runnable.run();
            } catch (InterruptedException e) {
                L.e("postDelayed error", e);
            }
        }).start();
    }

    private static void connectClient() throws IOException {
        try (LocalServerSocket serverSocket = new LocalServerSocket("easycontrol_for_car_scrcpy")) {
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
                switch (inputStream.readByte()) {
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
                        Device.handleConfigChanged(inputStream.readInt());
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
                    case 9:
                        if (Device.oldNightMode == -1) Device.oldNightMode = UiModeManager.getNightMode();
                        UiModeManager.setNightMode(inputStream.readByte());
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
        L.e("errorClose: ", e);
        synchronized (object) {
            object.notify();
        }
    }

    private static boolean timeoutClose = false;

    // 释放资源
    private static void release() {
        boolean lastScrcpy = false;
        try {
            lastScrcpy = Integer.parseInt(Channel.execReadOutput("ps -ef | grep easycontrol.server.Scrcpy | grep -v grep | grep -c 'easycontrol.server.Scrcpy'").replace("<!@n@!>", "")) == 1;
        } catch (Exception e) {
            L.w("get lastScrcpy error", e);
        }

        // 1
        try {
            inputStream.close();
            socket.close();
        } catch (Exception e) {
            L.e("release error", e);
        }

        // 2
        VideoEncode.release();
        AudioEncode.release();

        // 3
        if (Device.needReset) {
            try {
                if (Device.realDeviceSize != null)
                    Channel.execReadOutput("wm size " + Device.realDeviceSize.first + "x" + Device.realDeviceSize.second);
                else
                    Channel.execReadOutput("wm size reset");
            } catch (Exception e) {
                L.e("release error", e);
            }

            try {
                if (Device.realDeviceDensity != 0)
                    Channel.execReadOutput("wm density " + Device.realDeviceDensity);
                else
                    Channel.execReadOutput("wm density reset");
            } catch (Exception e) {
                L.e("release error", e);
            }
        }
        if (lastScrcpy && Device.oldNightMode != -1 && UiModeManager.getNightMode() != Device.oldNightMode) {
            UiModeManager.setNightMode(Device.oldNightMode);
        }

        // 4
        if (Options.keepAwake) {
            try {
                Channel.execReadOutput("settings put system screen_off_timeout " + Device.oldScreenOffTimeout);
            } catch (Exception e) {
                L.e("release error", e);
            }
        }

        // 5
        try {
            if (timeoutClose || lastScrcpy) {
                if (Options.TurnOffScreenIfStop) Device.keyEvent(223, 0, 0);
                else if (Options.TurnOnScreenIfStop) Device.changeScreenPowerMode(Display.STATE_ON);
            }
        } catch (Exception e) {
            L.e("release error", e);
        }

        // 6
        if (timeoutClose) {
            try {
                Channel.execReadOutput("ps -ef | grep easycontrol.server.Scrcpy | grep -v grep | grep -E \"^[a-z]+ +[0-9]+\" -o | grep -E \"[0-9]+\" -o | xargs kill -9");
            } catch (Exception e) {
                L.e("release error", e);
            }
        }

        // 7
        L.d("scrcpy release success");
        System.exit(0);
    }

}
