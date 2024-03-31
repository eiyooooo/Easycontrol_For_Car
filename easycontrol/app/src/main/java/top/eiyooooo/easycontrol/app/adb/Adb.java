package top.eiyooooo.easycontrol.app.adb;

import android.graphics.drawable.Drawable;
import android.hardware.usb.UsbDevice;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import android.util.Pair;
import top.eiyooooo.easycontrol.app.BuildConfig;
import top.eiyooooo.easycontrol.app.R;
import top.eiyooooo.easycontrol.app.buffer.Buffer;
import top.eiyooooo.easycontrol.app.buffer.BufferStream;
import top.eiyooooo.easycontrol.app.entity.AppData;
import top.eiyooooo.easycontrol.app.entity.Device;
import top.eiyooooo.easycontrol.app.helper.L;
import top.eiyooooo.easycontrol.app.helper.PublicTools;

public class Adb {
  public static final HashMap<String, Adb> adbMap = new HashMap<>();

  private final AdbChannel channel;
  private int localIdPool = 1;
  private int MAX_DATA = AdbProtocol.CONNECT_MAXDATA;
  private final ConcurrentHashMap<Integer, BufferStream> connectionStreams = new ConcurrentHashMap<>(10);
  private final ConcurrentHashMap<Integer, BufferStream> openStreams = new ConcurrentHashMap<>(5);
  private final Buffer sendBuffer = new Buffer();

  private final Thread handleInThread = new Thread(this::handleIn);
  private final Thread handleOutThread = new Thread(this::handleOut);

  private final String uuid;
  private static final String serverName = "/data/local/tmp/easycontrol_for_car_server_" + BuildConfig.VERSION_CODE + ".jar";
  public Thread startServerThread = new Thread(this::startServer);
  public BufferStream serverShell;

  public Adb(String uuid, String address, AdbKeyPair keyPair) throws Exception {
    this.uuid = uuid;
    Pair<String, Integer> addressPair = PublicTools.getIpAndPort(address);
    channel = new TcpChannel(addressPair.first, addressPair.second, false);
    connect(keyPair);
    startServerThread.start();
  }

  public Adb(String address, AdbKeyPair keyPair) throws Exception {
    this.uuid = null;
    Pair<String, Integer> addressPair = PublicTools.getIpAndPort(address);
    channel = new TcpChannel(addressPair.first, addressPair.second, true);
    connect(keyPair);
  }

  public Adb(String uuid, UsbDevice usbDevice, AdbKeyPair keyPair) throws Exception {
    this.uuid = uuid;
    channel = new UsbChannel(usbDevice);
    connect(keyPair);
    startServerThread.start();
  }

  private void connect(AdbKeyPair keyPair) throws Exception {
    // 连接ADB并认证
    channel.write(AdbProtocol.generateConnect());
    AdbProtocol.AdbMessage message = AdbProtocol.AdbMessage.parseAdbMessage(channel);
    if (message.command == AdbProtocol.CMD_AUTH) {
      channel.write(AdbProtocol.generateAuth(AdbProtocol.AUTH_TYPE_SIGNATURE, keyPair.signPayload(message.payload)));
      message = AdbProtocol.AdbMessage.parseAdbMessage(channel);
      if (message.command == AdbProtocol.CMD_AUTH) {
        channel.write(AdbProtocol.generateAuth(AdbProtocol.AUTH_TYPE_RSA_PUBLIC, keyPair.publicKeyBytes));
        message = AdbProtocol.AdbMessage.parseAdbMessage(channel);
      }
    }
    if (message.command != AdbProtocol.CMD_CNXN) {
      channel.close();
      throw new Exception("ADB connect error");
    }
    MAX_DATA = message.arg1;
    if (uuid == null) {
      channel.close();
      return;
    }
    // 启动后台进程
    handleInThread.setPriority(Thread.MAX_PRIORITY);
    handleInThread.start();
    handleOutThread.start();
  }

  public void startServer() {
    try {
      if (BuildConfig.ENABLE_DEBUG_FEATURE || !runAdbCmd("ls /data/local/tmp/easycontrol_*").contains(serverName)) {
        runAdbCmd("rm /data/local/tmp/easycontrol_* ");
        pushFile(AppData.main.getResources().openRawResource(R.raw.easycontrol_server), serverName);
      }
      if (serverShell != null) serverShell.close();
      String cmd = "CLASSPATH=" + serverName + " app_process / top.eiyooooo.easycontrol.server.Server\n";
      serverShell = getShell();
      serverShell.write(ByteBuffer.wrap(cmd.getBytes()));
      waitingData(0);
    } catch (Exception e) {
      L.log(uuid, e);
      PublicTools.logToast(AppData.main.getString(R.string.log_notify));
    }
  }

  public static String getStringResponseFromServer(Device device, String request, String... args) throws Exception {
    Adb adb = getAdb(device);
    return adb.getStringResponse(request, args);
  }

  public static Drawable getDrawableResponseFromServer(Device device, String request, String... args) throws Exception {
    Adb adb = getAdb(device);
    return adb.getDrawableResponse(request, args);
  }

  private static Adb getAdb(Device device) throws Exception {
    String uuid = device.uuid;
    Adb adb = adbMap.get(uuid);
    if (adb == null) throw new Exception("adb not start");
    if (!adb.startServerThread.isAlive() && (adb.serverShell == null || adb.serverShell.isClosed())) {
      adb.startServerThread = new Thread(adb::startServer);
      adb.startServerThread.start();
    }
    if (adb.startServerThread.isAlive())
      adb.startServerThread.join();
    return adb;
  }

  private String getStringResponse(String request, String... args) throws Exception {
    return new String(getResponse(request, args), StandardCharsets.UTF_8);
  }

  private Drawable getDrawableResponse(String request, String... args) throws Exception {
    byte[] bitmap = getResponse(request, args);
    return PublicTools.byteArrayBitmapToDrawable(bitmap);
  }

  private byte[] getResponse(String request, String[] args) throws Exception {
    StringBuilder sb = new StringBuilder();
    sb.append("/").append(request).append("?");
    for (String arg : args) {
      sb.append(arg).append("&");
    }
    sb.deleteCharAt(sb.length() - 1).append("\n");
    String requestCmd = sb.toString();

    serverShell.readAllBytes();
    serverShell.write(ByteBuffer.wrap(requestCmd.getBytes()));
    serverShell.readByteArray(requestCmd.length() + 1);
    waitingData(4);
    int len = serverShell.readInt();
    return serverShell.readByteArray(len).array();
  }

  ArrayList<Integer> dataReceivingList = new ArrayList<>();
  private void waitingData(int byteNum) throws InterruptedException {
    dataReceivingList.clear();
    while (true) {
      int newSize = serverShell.getSize();
      if (newSize == 0) continue;
      if (byteNum > 0 && newSize > byteNum)
        break;
      dataReceivingList.add(newSize);
      if (dataReceivingList.size() > 1) {
        int oldSize = dataReceivingList.get(dataReceivingList.size() - 2);
        if (oldSize == newSize) break;
      }
      Thread.sleep(400);
    }
  }

  private BufferStream open(String destination, boolean canMultipleSend) throws InterruptedException {
    int localId = localIdPool++ * (canMultipleSend ? 1 : -1);
    sendBuffer.write(AdbProtocol.generateOpen(localId, destination));
    BufferStream bufferStream;
    do {
      synchronized (this) {
        wait();
      }
      bufferStream = openStreams.get(localId);
    } while (bufferStream == null);
    openStreams.remove(localId);
    return bufferStream;
  }

  public String restartOnTcpip(int port) throws InterruptedException {
    BufferStream bufferStream = open("tcpip:" + port, false);
    do {
      synchronized (this) {
        wait();
      }
    } while (!bufferStream.isClosed());
    return new String(bufferStream.readByteArrayBeforeClose().array());
  }

  public void pushFile(InputStream file, String remotePath) throws Exception {
    // 打开链接
    BufferStream bufferStream = open("sync:", false);
    // 发送信令，建立push通道
    String sendString = remotePath + ",33206";
    byte[] bytes = sendString.getBytes();
    bufferStream.write(AdbProtocol.generateSyncHeader("SEND", sendString.length()));
    bufferStream.write(ByteBuffer.wrap(bytes));
    // 发送文件
    byte[] byteArray = new byte[10240 - 8];
    int len = file.read(byteArray, 0, byteArray.length);
    do {
      bufferStream.write(AdbProtocol.generateSyncHeader("DATA", len));
      bufferStream.write(ByteBuffer.wrap(byteArray, 0, len));
      len = file.read(byteArray, 0, byteArray.length);
    } while (len > 0);
    file.close();
    // 传输完成，为了方便，文件日期定为2024.1.1 0:0
    bufferStream.write(AdbProtocol.generateSyncHeader("DONE", 1704038400));
    bufferStream.write(AdbProtocol.generateSyncHeader("QUIT", 0));
    do {
      synchronized (this) {
        wait();
      }
    } while (!bufferStream.isClosed());
  }

  public String runAdbCmd(String cmd) throws InterruptedException {
    BufferStream bufferStream = open("shell:" + cmd, true);
    do {
      synchronized (this) {
        wait();
      }
    } while (!bufferStream.isClosed());
    return new String(bufferStream.readByteArrayBeforeClose().array());
  }

  public BufferStream getShell() throws InterruptedException {
    return open("shell:", true);
  }

  public BufferStream tcpForward(int port) throws IOException, InterruptedException {
    BufferStream bufferStream = open("tcp:" + port, true);
    if (bufferStream.isClosed()) throw new IOException("error forward");
    return bufferStream;
  }

  public BufferStream localSocketForward(String socketName) throws IOException, InterruptedException {
    BufferStream bufferStream = open("localabstract:" + socketName, true);
    if (bufferStream.isClosed()) throw new IOException("error forward");
    return bufferStream;
  }

  private void handleIn() {
    try {
      while (!Thread.interrupted()) {
        AdbProtocol.AdbMessage message = AdbProtocol.AdbMessage.parseAdbMessage(channel);
        BufferStream bufferStream = connectionStreams.get(message.arg1);
        boolean isNeedNotify = bufferStream == null;
        // 新连接
        if (isNeedNotify) bufferStream = createNewStream(message.arg1, message.arg0, message.arg1 > 0);
        switch (message.command) {
          case AdbProtocol.CMD_OKAY:
            bufferStream.setCanWrite(true);
            break;
          case AdbProtocol.CMD_WRTE:
            bufferStream.pushSource(message.payload);
            sendBuffer.write(AdbProtocol.generateOkay(message.arg1, message.arg0));
            break;
          case AdbProtocol.CMD_CLSE:
            bufferStream.close();
            isNeedNotify = true;
            break;
        }
        if (isNeedNotify) {
          synchronized (this) {
            notifyAll();
          }
        }
      }
    } catch (Exception e) {
      L.log(uuid, e);
      PublicTools.logToast(AppData.main.getString(R.string.log_notify));
      close();
    }
  }

  private void handleOut() {
    try {
      while (!Thread.interrupted()) {
        channel.write(sendBuffer.readNext());
        if (!sendBuffer.isEmpty()) channel.write(sendBuffer.read(sendBuffer.getSize()));
        channel.flush();
      }
    } catch (Exception e) {
      L.log(uuid, e);
      PublicTools.logToast(AppData.main.getString(R.string.log_notify));
      close();
    }
  }

  private BufferStream createNewStream(int localId, int remoteId, boolean canMultipleSend) throws Exception {
    return new BufferStream(false, canMultipleSend, new BufferStream.UnderlySocketFunction() {
      @Override
      public void connect(BufferStream bufferStream) {
        connectionStreams.put(localId, bufferStream);
        openStreams.put(localId, bufferStream);
      }

      @Override
      public void write(BufferStream bufferStream, ByteBuffer buffer) {
        while (buffer.hasRemaining()) {
          byte[] byteArray = new byte[Math.min(MAX_DATA - 128, buffer.remaining())];
          buffer.get(byteArray);
          sendBuffer.write(AdbProtocol.generateWrite(localId, remoteId, byteArray));
        }
      }

      @Override
      public void flush(BufferStream bufferStream) {
        sendBuffer.write(AdbProtocol.generateClose(localId, remoteId));
      }

      @Override
      public void close(BufferStream bufferStream) {
        connectionStreams.remove(localId);
        sendBuffer.write(AdbProtocol.generateClose(localId, remoteId));
      }
    });
  }

  public void close() {
    adbMap.remove(uuid);
    for (Object bufferStream : connectionStreams.values().toArray()) ((BufferStream) bufferStream).close();
    handleInThread.interrupt();
    handleOutThread.interrupt();
    channel.close();
  }

}
