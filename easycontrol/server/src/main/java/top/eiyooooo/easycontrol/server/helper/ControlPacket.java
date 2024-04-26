package top.eiyooooo.easycontrol.server.helper;

import android.system.ErrnoException;
import top.eiyooooo.easycontrol.server.Scrcpy;
import top.eiyooooo.easycontrol.server.entity.Device;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public final class ControlPacket {

    public static void sendVideoEvent(long pts, ByteBuffer data) throws IOException, ErrnoException {
        int size = data.remaining();
        if (size < 0) return;
        ByteBuffer byteBuffer = ByteBuffer.allocate(12 + size);
        byteBuffer.putInt(size);
        byteBuffer.put(data);
        byteBuffer.putLong(pts);
        byteBuffer.flip();
        Scrcpy.writeVideo(byteBuffer);
    }

    public static void sendAudioEvent(ByteBuffer data) throws IOException, ErrnoException {
        int size = data.remaining();
        if (size < 0) return;
        ByteBuffer byteBuffer = ByteBuffer.allocate(5 + size);
        byteBuffer.put((byte) 2);
        byteBuffer.putInt(size);
        byteBuffer.put(data);
        byteBuffer.flip();
        Scrcpy.writeMain(byteBuffer);
    }

    public static void sendClipboardEvent(String newClipboardText) {
        byte[] tmpTextByte = newClipboardText.getBytes(StandardCharsets.UTF_8);
        if (tmpTextByte.length == 0 || tmpTextByte.length > 5000) return;
        ByteBuffer byteBuffer = ByteBuffer.allocate(5 + tmpTextByte.length);
        byteBuffer.put((byte) 3);
        byteBuffer.putInt(tmpTextByte.length);
        byteBuffer.put(tmpTextByte);
        byteBuffer.flip();
        try {
            Scrcpy.writeMain(byteBuffer);
        } catch (IOException | ErrnoException e) {
            Scrcpy.errorClose(e);
        }
    }

    public static void sendVideoSizeEvent() throws IOException, ErrnoException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(9);
        byteBuffer.put((byte) 4);
        byteBuffer.putInt(Device.videoSize.first);
        byteBuffer.putInt(Device.videoSize.second);
        byteBuffer.flip();
        Scrcpy.writeMain(byteBuffer);
    }

    public static void sendKeepAlive() throws IOException, ErrnoException {
        Scrcpy.writeMain(ByteBuffer.wrap(new byte[]{5}));
    }

    public static void handleTouchEvent() throws IOException {
        int action = Scrcpy.inputStream.readByte();
        int pointerId = Scrcpy.inputStream.readByte();
        float x = Scrcpy.inputStream.readFloat();
        float y = Scrcpy.inputStream.readFloat();
        int offsetTime = Scrcpy.inputStream.readInt();
        Device.touchEvent(action, x, y, pointerId, offsetTime);
    }

    public static void handleKeyEvent() throws IOException {
        int keyCode = Scrcpy.inputStream.readInt();
        int meta = Scrcpy.inputStream.readInt();
        int displayIdToInject = Scrcpy.inputStream.readInt();
        if (displayIdToInject == -1)
            Device.keyEvent(keyCode, meta, Device.displayId);
        else
            Device.keyEvent(keyCode, meta, displayIdToInject);
    }

    public static void handleClipboardEvent() throws IOException {
        int size = Scrcpy.inputStream.readInt();
        byte[] textBytes = new byte[size];
        Scrcpy.inputStream.readFully(textBytes);
        String text = new String(textBytes, StandardCharsets.UTF_8);
        Device.setClipboardText(text);
    }

}

