package top.eiyooooo.easycontrol.server.entity;

import android.content.IOnPrimaryClipChangedListener;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Pair;
import android.view.*;
import top.eiyooooo.easycontrol.server.Channel;
import top.eiyooooo.easycontrol.server.helper.ControlPacket;
import top.eiyooooo.easycontrol.server.helper.VideoEncode;
import top.eiyooooo.easycontrol.server.utils.L;
import top.eiyooooo.easycontrol.server.wrappers.SurfaceControl;
import top.eiyooooo.easycontrol.server.wrappers.WindowManager;
import top.eiyooooo.easycontrol.server.wrappers.*;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Device {
    public static Pair<Integer, Integer> realDeviceSize;
    public static int realDeviceDensity;
    public static Pair<Integer, Integer> deviceSize;
    public static int deviceRotation;
    public static Pair<Integer, Integer> videoSize;
    public static boolean needReset = false;
    public static int oldScreenOffTimeout = 60000;

    public static int displayId;
    public static int layerStack;

    public static void init() throws IOException, InterruptedException {
        displayId = Options.displayId;
        getRealDeviceSize();
        getDeviceSize();
        // 旋转监听
        setRotationListener();
        // 剪切板监听
        setClipBoardListener();
        // 设置不息屏
        if (Options.keepAwake) setKeepScreenLight();
    }

    // 获取真实的设备大小
    private static void getRealDeviceSize() {
        DisplayInfo displayInfo = DisplayManager.getDisplayInfo(Display.DEFAULT_DISPLAY);
        realDeviceSize = displayInfo.size;
        realDeviceDensity = displayInfo.density;
        deviceRotation = displayInfo.rotation;
        if (deviceRotation == 1 || deviceRotation == 3)
            realDeviceSize = new Pair<>(realDeviceSize.second, realDeviceSize.first);
    }

    private static void getDeviceSize() {
        DisplayInfo displayInfo = DisplayManager.getDisplayInfo(displayId);
        deviceSize = displayInfo.size;
        deviceRotation = displayInfo.rotation;
        layerStack = displayInfo.layerStack;
        getVideoSize();
    }

    public static void handleConfigChanged(int mode) {
        if (mode <= 0) {
            try {
                DisplayInfo test = DisplayManager.getDisplayInfo(-mode);
                if (test == null) throw new Exception();
                displayId = -mode;
                WindowManager.removeRotationWatcher();
                setRotationListener();
            } catch (Throwable e) {
                L.w("failed to switch display");
            }
        }
        getDeviceSize();
        if (mode == 1) needReset = true;
        VideoEncode.isHasChangeConfig = true;
    }

    private static void getVideoSize() {
        if (Options.maxSize == 0) {
            videoSize = deviceSize;
            return;
        }
        boolean isPortrait = deviceSize.first < deviceSize.second;
        int major = isPortrait ? deviceSize.second : deviceSize.first;
        int minor = isPortrait ? deviceSize.first : deviceSize.second;
        if (major > Options.maxSize) {
            minor = minor * Options.maxSize / major;
            major = Options.maxSize;
        }
        // h264只接受8的倍数，所以需要缩放至最近参数
        minor = minor + 4 & ~7;
        major = major + 4 & ~7;
        videoSize = isPortrait ? new Pair<>(minor, major) : new Pair<>(major, minor);
    }

    private static String nowClipboardText = "";

    private static void setClipBoardListener() {
        ClipboardManager.addPrimaryClipChangedListener(new IOnPrimaryClipChangedListener.Stub() {
            public void dispatchPrimaryClipChanged() {
                String newClipboardText = ClipboardManager.getText();
                if (newClipboardText == null) return;
                if (!newClipboardText.equals(nowClipboardText)) {
                    nowClipboardText = newClipboardText;
                    // 发送报文
                    ControlPacket.sendClipboardEvent(nowClipboardText);
                }
            }
        });
    }

    public static void setClipboardText(String text) {
        nowClipboardText = text;
        ClipboardManager.setText(nowClipboardText);
    }

    private static void setRotationListener() {
        WindowManager.registerRotationWatcher(new IRotationWatcher.Stub() {
            public void onRotationChanged(int rotation) {
                if ((deviceRotation + rotation) % 2 != 0) {
                    deviceSize = new Pair<>(deviceSize.second, deviceSize.first);
                    videoSize = new Pair<>(videoSize.second, videoSize.first);
                }
                deviceRotation = rotation;
                VideoEncode.isHasChangeConfig = true;
            }
        }, displayId);
    }

    private static final PointersState pointersState = new PointersState();

    public static void touchEvent(int action, Float x, Float y, int pointerId, int offsetTime) {
        Pointer pointer = pointersState.get(pointerId);

        if (pointer == null) {
            if (action != MotionEvent.ACTION_DOWN) return;
            pointer = pointersState.newPointer(pointerId, SystemClock.uptimeMillis() - 50);
        }

        if (pointer == null) return;
        pointer.x = x * deviceSize.first;
        pointer.y = y * deviceSize.second;
        int pointerCount = pointersState.update();

        if (action == MotionEvent.ACTION_UP) {
            pointersState.remove(pointerId);
            if (pointerCount > 1)
                action = MotionEvent.ACTION_POINTER_UP | (pointer.id << MotionEvent.ACTION_POINTER_INDEX_SHIFT);
        } else if (action == MotionEvent.ACTION_DOWN) {
            if (pointerCount > 1)
                action = MotionEvent.ACTION_POINTER_DOWN | (pointer.id << MotionEvent.ACTION_POINTER_INDEX_SHIFT);
        }
        MotionEvent event = MotionEvent.obtain(pointer.downTime, pointer.downTime + offsetTime, action, pointerCount, pointersState.pointerProperties, pointersState.pointerCoords, 0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0);
        injectEvent(event);
    }

    public static void keyEvent(int keyCode, int meta) {
        long now = SystemClock.uptimeMillis();
        KeyEvent event1 = new KeyEvent(now, now, MotionEvent.ACTION_DOWN, keyCode, 0, meta, -1, 0, 0, InputDevice.SOURCE_KEYBOARD);
        KeyEvent event2 = new KeyEvent(now, now, MotionEvent.ACTION_UP, keyCode, 0, meta, -1, 0, 0, InputDevice.SOURCE_KEYBOARD);
        injectEvent(event1);
        injectEvent(event2);
    }

    private static void injectEvent(InputEvent inputEvent) {
        try {
            if (displayId != Display.DEFAULT_DISPLAY) InputManager.setDisplayId(inputEvent, displayId);
            InputManager.injectInputEvent(inputEvent, InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
        } catch (Exception e) {
            L.e("injectEvent error", e);
        }
    }


    public static void changeScreenPowerMode(int mode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                long[] physicalDisplayIds = SurfaceControl.getPhysicalDisplayIds();
                if (physicalDisplayIds == null) throw new Exception();
                for (long physicalDisplayId : physicalDisplayIds) {
                    IBinder token = SurfaceControl.getPhysicalDisplayToken(physicalDisplayId);
                    if (token != null) SurfaceControl.setDisplayPowerMode(token, mode);
                }
            } catch (Exception ignored) {
                L.w("change power mode for all screens error, try built-in display");
            }
        }
        try {
            IBinder d = SurfaceControl.getBuiltInDisplay();
            if (d != null) SurfaceControl.setDisplayPowerMode(d, mode);
        } catch (Exception e) {
            L.e("change screen power mode error", e);
        }
    }

    public static void changePower() {
        keyEvent(26, 0);
    }

    public static void rotateDevice() {
        boolean accelerometerRotation = !WindowManager.isRotationFrozen(displayId);
        int currentRotation = getCurrentRotation(displayId);
        if (currentRotation == -1) return;
        int newRotation = (currentRotation & 1) ^ 1; // 0->1, 1->0, 2->1, 3->0
        WindowManager.freezeRotation(displayId, newRotation);
        if (accelerometerRotation) WindowManager.thawRotation(displayId);
    }

    private static int getCurrentRotation(int displayId) {
        if (displayId == 0) return WindowManager.getRotation();
        DisplayInfo displayInfo = DisplayManager.getDisplayInfo(displayId);
        return displayInfo.rotation;
    }

    private static void setKeepScreenLight() {
        try {
            String output = Channel.execReadOutput("settings get system screen_off_timeout");
            // 使用正则表达式匹配数字
            Matcher matcher = Pattern.compile("\\d+").matcher(output);
            if (matcher.find()) {
                int timeout = Integer.parseInt(matcher.group());
                if (timeout >= 20 && timeout <= 60 * 30) oldScreenOffTimeout = timeout;
            }
            Channel.execReadOutput("settings put system screen_off_timeout 600000000");
        } catch (Exception ignored) {
        }
    }

    public static boolean isEncoderSupport(String mimeName) {
        MediaCodecList mediaCodecList = new MediaCodecList(MediaCodecList.REGULAR_CODECS);
        for (MediaCodecInfo mediaCodecInfo : mediaCodecList.getCodecInfos())
            if (mediaCodecInfo.isEncoder() && mediaCodecInfo.getName().contains(mimeName)) return true;
        return false;
    }
}
