package top.eiyooooo.easycontrol.server.helper;

import android.graphics.Rect;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.os.IBinder;
import android.system.ErrnoException;
import android.view.Surface;
import top.eiyooooo.easycontrol.server.Scrcpy;
import top.eiyooooo.easycontrol.server.entity.Device;
import top.eiyooooo.easycontrol.server.entity.Options;
import top.eiyooooo.easycontrol.server.utils.L;
import top.eiyooooo.easycontrol.server.wrappers.DisplayManager;
import top.eiyooooo.easycontrol.server.wrappers.SurfaceControl;
import top.eiyooooo.easycontrol.server.wrappers.WindowManager;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.HashMap;

public final class VideoEncode {
    private static MediaCodec encoder;
    private static MediaFormat encoderFormat;
    public static boolean isHasChangeConfig = false;
    private static boolean useH265;

    private static IBinder display;
    private static final HashMap<Integer, VirtualDisplay> virtualDisplays = new HashMap<>();

    public static void init() throws Exception {
        useH265 = Options.useH265 && Device.isEncoderSupport("hevc");
        ByteBuffer byteBuffer = ByteBuffer.allocate(9);
        byteBuffer.put((byte) (useH265 ? 1 : 0));
        byteBuffer.putInt(Device.videoSize.first);
        byteBuffer.putInt(Device.videoSize.second);
        byteBuffer.flip();
        Scrcpy.writeVideo(byteBuffer);
        // 创建显示器
        try {
            display = SurfaceControl.createDisplay("easycontrol_for_car", Build.VERSION.SDK_INT < Build.VERSION_CODES.R || (Build.VERSION.SDK_INT == Build.VERSION_CODES.R && !"S".equals(Build.VERSION.CODENAME)));
        } catch (Exception e) {
            L.w("createDisplay by SurfaceControl error", e);
            Options.mirrorMode = 1;
        }
        // 创建Codec
        createEncoderFormat();
        startEncode();
    }

    private static void createEncoderFormat() throws IOException {
        String codecMime = useH265 ? MediaFormat.MIMETYPE_VIDEO_HEVC : MediaFormat.MIMETYPE_VIDEO_AVC;
        encoder = MediaCodec.createEncoderByType(codecMime);
        encoderFormat = new MediaFormat();

        encoderFormat.setString(MediaFormat.KEY_MIME, codecMime);

        encoderFormat.setInteger(MediaFormat.KEY_BIT_RATE, Options.maxVideoBit);
        encoderFormat.setInteger(MediaFormat.KEY_FRAME_RATE, Options.maxFps);
        encoderFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            encoderFormat.setInteger(MediaFormat.KEY_INTRA_REFRESH_PERIOD, Options.maxFps * 3);
        encoderFormat.setFloat("max-fps-to-encoder", Options.maxFps);

        encoderFormat.setLong(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, 50_000);
        encoderFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
    }

    // 初始化编码器
    private static Surface surface;

    public static void startEncode() throws Exception {
        encoderFormat.setInteger(MediaFormat.KEY_WIDTH, Device.videoSize.first);
        encoderFormat.setInteger(MediaFormat.KEY_HEIGHT, Device.videoSize.second);
        encoder.configure(encoderFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        // 绑定Display和Surface
        surface = encoder.createInputSurface();
        if (Device.displayId != 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            Options.mirrorMode = 1;
        if (Options.mirrorMode == 1) {
            try {
                VirtualDisplay virtualDisplay = virtualDisplays.get(Device.displayId);
                if (virtualDisplay == null) {
                    virtualDisplay = DisplayManager.createVirtualDisplay("easycontrol_for_car",
                            Device.videoSize.first, Device.videoSize.second, Device.displayId, surface);
                    virtualDisplays.put(Device.displayId, virtualDisplay);
                    int displayId = virtualDisplay.getDisplay().getDisplayId();
                    WindowManager.freezeRotation(displayId, 0);
                    Device.display2virtualDisplay.put(Device.displayId, displayId);
                    L.d("mirroring display " + Device.displayId + " to " + displayId + " with size " + Device.videoSize.first + "x" + Device.videoSize.second);
                }
                else {
                    virtualDisplay.setSurface(surface);
                    virtualDisplay.resize(Device.videoSize.first, Device.videoSize.second, 1);
                    int displayId = virtualDisplay.getDisplay().getDisplayId();
                    WindowManager.freezeRotation(displayId, 0);
                    L.d("resize virtual display " + displayId + " to " + Device.videoSize.first + "x" + Device.videoSize.second);
                }
            } catch (Exception e) {
                L.e("createVirtualDisplay by DisplayManager error", e);
                throw e;
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                SurfaceControl.destroyDisplay(display);
                display = SurfaceControl.createDisplay("easycontrol_for_car", false);
            }
            setDisplaySurface(display, surface);
        }
        // 启动编码
        encoder.start();
        ControlPacket.sendVideoSizeEvent();
    }

    public static void stopEncode() {
        encoder.stop();
        encoder.reset();
        surface.release();
    }

    private static void setDisplaySurface(IBinder display, Surface surface) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        SurfaceControl.openTransaction();
        try {
            SurfaceControl.setDisplaySurface(display, surface);
            SurfaceControl.setDisplayProjection(display, 0, new Rect(0, 0, Device.deviceSize.first, Device.deviceSize.second), new Rect(0, 0, Device.videoSize.first, Device.videoSize.second));
            SurfaceControl.setDisplayLayerStack(display, Device.layerStack);
        } finally {
            SurfaceControl.closeTransaction();
        }
    }

    private static final MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

    public static void encodeOut() throws IOException, ErrnoException {
        try {
            // 找到已完成的输出缓冲区
            int outIndex;
            do outIndex = encoder.dequeueOutputBuffer(bufferInfo, -1); while (outIndex < 0);
            ByteBuffer buffer = encoder.getOutputBuffer(outIndex);
            if (buffer == null) return;
            ControlPacket.sendVideoEvent(bufferInfo.presentationTimeUs, buffer);
            encoder.releaseOutputBuffer(outIndex, false);
        } catch (IllegalStateException e) {
            L.e("encodeOut error", e);
        }
    }

    public static void release() {
        try {
            stopEncode();
            encoder.release();
            SurfaceControl.destroyDisplay(display);
            for (VirtualDisplay virtualDisplay : virtualDisplays.values()) {
                virtualDisplay.release();
            }
        } catch (Exception e) {
            L.e("release error", e);
        }
    }

}
