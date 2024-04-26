package top.eiyooooo.easycontrol.server.helper;

import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.system.ErrnoException;
import top.eiyooooo.easycontrol.server.Scrcpy;
import top.eiyooooo.easycontrol.server.entity.Device;
import top.eiyooooo.easycontrol.server.entity.Options;
import top.eiyooooo.easycontrol.server.utils.L;

import java.io.IOException;
import java.nio.ByteBuffer;

public final class AudioEncode {
    private static MediaCodec encoder;
    private static AudioRecord audioCapture;
    private static boolean useOpus;

    public static boolean init() throws IOException, ErrnoException {
        useOpus = Options.useOpus && Device.isEncoderSupport("opus");
        byte[] bytes = new byte[]{0};
        try {
            // 从安卓12开始支持音频
            if (!Options.isAudio) throw new Exception("audio not enabled");
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) throw new Exception("audio not supported");
            setAudioEncoder();
            encoder.start();
            audioCapture = AudioCapture.init();
        } catch (Exception e) {
            L.w(e);
            Scrcpy.writeMain(ByteBuffer.wrap(bytes));
            return false;
        }
        bytes[0] = 1;
        Scrcpy.writeMain(ByteBuffer.wrap(bytes));
        bytes[0] = (byte) (useOpus ? 1 : 0);
        Scrcpy.writeMain(ByteBuffer.wrap(bytes));
        return true;
    }

    private static void setAudioEncoder() throws IOException {
        String codecMime = useOpus ? MediaFormat.MIMETYPE_AUDIO_OPUS : MediaFormat.MIMETYPE_AUDIO_AAC;
        encoder = MediaCodec.createEncoderByType(codecMime);
        MediaFormat encoderFormat = MediaFormat.createAudioFormat(codecMime, AudioCapture.SAMPLE_RATE, AudioCapture.CHANNELS);
        encoderFormat.setInteger(MediaFormat.KEY_BIT_RATE, 96000);
        encoderFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, frameSize);
        if (!useOpus)
            encoderFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        encoder.configure(encoderFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
    }

    private static final int frameSize = AudioCapture.millisToBytes(50);

    public static void encodeIn() {
        try {
            int inIndex;
            do inIndex = encoder.dequeueInputBuffer(-1); while (inIndex < 0);
            ByteBuffer buffer = encoder.getInputBuffer(inIndex);
            if (buffer == null) return;
            int size = Math.min(buffer.remaining(), frameSize);
            audioCapture.read(buffer, size);
            encoder.queueInputBuffer(inIndex, 0, size, 0, 0);
        } catch (IllegalStateException e) {
            L.e("AudioEncode encodeIn error", e);
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
            if (useOpus) {
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    buffer.getLong();
                    int size = (int) buffer.getLong();
                    buffer.limit(buffer.position() + size);
                }
                // 当无声音时不发送
                if (buffer.remaining() < 5) {
                    encoder.releaseOutputBuffer(outIndex, false);
                    return;
                }
            }
            ControlPacket.sendAudioEvent(buffer);
            encoder.releaseOutputBuffer(outIndex, false);
        } catch (IllegalStateException e) {
            L.e("AudioEncode encodeOut error", e);
        }
    }

    public static void release() {
        try {
            audioCapture.stop();
            audioCapture.release();
            encoder.stop();
            encoder.release();
        } catch (Exception e) {
            L.e("AudioEncode release error", e);
        }
    }
}

