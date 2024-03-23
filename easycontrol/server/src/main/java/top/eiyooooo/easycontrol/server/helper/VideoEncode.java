package top.eiyooooo.easycontrol.server.helper;

import android.graphics.Rect;
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
import top.eiyooooo.easycontrol.server.wrappers.SurfaceControl;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;

public final class VideoEncode {
    private static MediaCodec encoder;
    private static MediaFormat encoderFormat;
    public static boolean isHasChangeConfig = false;
    private static boolean useH265;

    private static IBinder display;

    public static void init() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException, ErrnoException {
        useH265 = Options.useH265 && Device.isEncoderSupport("hevc");
        Scrcpy.write(ByteBuffer.wrap(new byte[]{(byte) (useH265 ? 1 : 0)}));
        // 创建显示器
        display = SurfaceControl.createDisplay("easycontrol", Build.VERSION.SDK_INT < Build.VERSION_CODES.R || (Build.VERSION.SDK_INT == Build.VERSION_CODES.R && !"S".equals(Build.VERSION.CODENAME)));
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

    public static void startEncode() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException, ErrnoException {
        encoderFormat.setInteger(MediaFormat.KEY_WIDTH, Device.videoSize.first);
        encoderFormat.setInteger(MediaFormat.KEY_HEIGHT, Device.videoSize.second);
        encoder.configure(encoderFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        // 绑定Display和Surface
        surface = encoder.createInputSurface();
        setDisplaySurface(display, surface);
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
            ControlPacket.sendVideoEvent(bufferInfo.size, bufferInfo.presentationTimeUs, encoder.getOutputBuffer(outIndex));
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
        } catch (Exception e) {
            L.e("release error", e);
        }
    }

}
