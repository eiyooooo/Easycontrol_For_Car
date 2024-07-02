package top.eiyooooo.easycontrol.app.client.view;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import top.eiyooooo.easycontrol.app.adb.Adb;
import top.eiyooooo.easycontrol.app.client.Client;
import top.eiyooooo.easycontrol.app.client.ControlPacket;
import top.eiyooooo.easycontrol.app.entity.AppData;
import top.eiyooooo.easycontrol.app.entity.Device;
import top.eiyooooo.easycontrol.app.helper.PublicTools;

import java.util.concurrent.atomic.AtomicBoolean;

public class ClientView implements TextureView.SurfaceTextureListener {
  public final Device device;
  public final Device deviceOriginal;
  public int mode = 0;
  public int displayId = 0;
  public final ControlPacket controlPacket;
  final PublicTools.MyFunctionInt changeMode;
  private final PublicTools.MyFunction onReady;
  public final PublicTools.MyFunction onClose;
  public final TextureView textureView;
  private SurfaceTexture surfaceTexture;

  private FullActivity fullView;

  private Pair<Integer, Integer> realDeviceSize;
  private Pair<Integer, Integer> videoSize;
  private Pair<Integer, Integer> maxSize;
  private Pair<Integer, Integer> surfaceSize;
  boolean lightState;
  public int multiLink = 0;

  public ClientView(Device device, ControlPacket controlPacket, PublicTools.MyFunctionInt changeMode, PublicTools.MyFunction onReady, PublicTools.MyFunction onClose) {
    lightState = !AppData.setting.getTurnOffScreenIfStart();
    this.deviceOriginal = device;
    this.device = new Device(device.uuid, device.type);
    Device.copyDevice(device, this.device);
    textureView = new TextureView(AppData.main);
    this.controlPacket = controlPacket;
    this.changeMode = changeMode;
    this.onReady = onReady;
    this.onClose = onClose;
    setTouchListener();
    textureView.setSurfaceTextureListener(this);
  }

  public final AtomicBoolean changeSizeLock = new AtomicBoolean(false);
  public void changeSize(float ratio) {
    new Thread(() -> {
      if (!changeSizeLock.get()) {
        try {
          synchronized (changeSizeLock) {
            changeSizeLock.wait(5000);
          }
        } catch (InterruptedException ignored) {
        }
      }
      if (!changeSizeLock.get() || mode == 0 || displayId == 0) return;
      try {
        float targetRatio = ratio;
        if (targetRatio > 4 || targetRatio < 0.25) return;

        if (realDeviceSize == null) {
          String displayInfo = Adb.getStringResponseFromServer(device, "getDisplayInfo");
          JSONArray jsonArray = new JSONArray(displayInfo);
          for (int i = 0; i < jsonArray.length(); i++) {
            if (jsonArray.getJSONObject(i).getInt("id") == 0) {
              int width = jsonArray.getJSONObject(i).getInt("width");
              int height = jsonArray.getJSONObject(i).getInt("height");
              int rotation = jsonArray.getJSONObject(i).getInt("rotation");
              if (rotation == 1 || rotation == 3) realDeviceSize = new Pair<>(height, width);
              else realDeviceSize = new Pair<>(width, height);
              break;
            }
          }
        }
        if (realDeviceSize == null) return;

        Pair<Integer, Integer> deviceSize = null;
        String displayInfo = Adb.getStringResponseFromServer(device, "getDisplayInfo");
        JSONArray jsonArray = new JSONArray(displayInfo);
        for (int i = 0; i < jsonArray.length(); i++) {
          if (jsonArray.getJSONObject(i).getInt("id") == displayId) {
            int width = jsonArray.getJSONObject(i).getInt("width");
            int height = jsonArray.getJSONObject(i).getInt("height");
            deviceSize = new Pair<>(width, height);
            break;
          }
        }
        if (deviceSize == null) return;

        int rotation = targetRatio > 1 ? 1 : 0;

        if (targetRatio > 1) targetRatio = 1 / targetRatio;

        float ratioChange = targetRatio / ((float) realDeviceSize.first / realDeviceSize.second);

        int newWidth, newHeight;
        if (realDeviceSize.first < realDeviceSize.second) {
          newWidth = (int) (realDeviceSize.first * ratioChange);
          newHeight = realDeviceSize.second;
        } else {
          newWidth = realDeviceSize.first;
          newHeight = (int) (realDeviceSize.second * ratioChange);
        }
        newWidth = newWidth + 8 & ~15;
        newHeight = newHeight + 8 & ~15;
        if (newWidth == newHeight) newWidth -= 16;

        if (mode == 0 || displayId == 0) return;
        String output = Adb.getStringResponseFromServer(device, "resizeDisplay", "width=" + newWidth, "height=" + newHeight, "id=" + displayId);

        if (output.contains("success")) {
          Thread.sleep(500);
          if (displayId == 0) controlPacket.sendConfigChangedEvent(1);
          else controlPacket.sendConfigChangedEvent(2);
          Thread.sleep(300);
          controlPacket.sendRotateEvent(rotation);
        }
      } catch (Exception ignored) {
      }
    }).start();
  }

  public void changeMode(int mode) {
    this.mode = mode;
    if (fullView != null) fullView.changeMode(mode);
  }

  public synchronized void changeToFull() {
    hide(false);
    Intent intent = new Intent(AppData.activity, FullActivity.class);
    int i = 0;
    for (Client client : Client.allClient) {
      if (client.clientView == this) break;
      i++;
    }
    intent.putExtra("index", i);
    AppData.activity.startActivity(intent);
  }

  public synchronized void hide(boolean isRelease) {
    if (fullView != null) fullView.hide();
    if (isRelease && surfaceTexture != null) surfaceTexture.release();
  }

  public void setFullView(FullActivity fullView) {
    this.fullView = fullView;
  }

  public void updateMaxSize(Pair<Integer, Integer> maxSize) {
    if (maxSize == null || maxSize.first == 0 || maxSize.second == 0) return;
    this.maxSize = maxSize;
    if (fullView != null && fullView.fullMaxSize != null && (AppData.setting.getFillFull() || (mode == 1 && device.setResolution)))
      reCalculateTextureViewSize(fullView.fullMaxSize.first, fullView.fullMaxSize.second);
    else reCalculateTextureViewSize();
  }

  public void updateVideoSize(Pair<Integer, Integer> videoSize) {
    if (videoSize == null || videoSize.first == 0 || videoSize.second == 0) return;
    this.videoSize = videoSize;
    if (fullView != null && fullView.fullMaxSize != null && (AppData.setting.getFillFull() || (mode == 1 && device.setResolution)))
      reCalculateTextureViewSize(fullView.fullMaxSize.first, fullView.fullMaxSize.second);
    else reCalculateTextureViewSize();
  }

  public Pair<Integer, Integer> getVideoSize() {
    return videoSize;
  }

  public Surface getSurface() {
    return new Surface(surfaceTexture);
  }

  // 重新计算TextureView大小
  private void reCalculateTextureViewSize() {
    if (maxSize == null || videoSize == null) return;
    // 根据原画面大小videoSize计算在maxSize空间内的最大缩放大小
    int tmp1 = videoSize.second * maxSize.first / videoSize.first;
    // 横向最大不会超出
    if (maxSize.second > tmp1) surfaceSize = new Pair<>(maxSize.first, tmp1);
      // 竖向最大不会超出
    else surfaceSize = new Pair<>(videoSize.first * maxSize.second / videoSize.second, maxSize.second);
    // 更新大小
    ViewGroup.LayoutParams layoutParams = textureView.getLayoutParams();
    layoutParams.width = surfaceSize.first;
    layoutParams.height = surfaceSize.second;
    textureView.setLayoutParams(layoutParams);
  }
  public void reCalculateTextureViewSize(int width, int height) {
    surfaceSize = new Pair<>(width, height);
    ViewGroup.LayoutParams layoutParams = textureView.getLayoutParams();
    layoutParams.width = width;
    layoutParams.height = height;
    textureView.setLayoutParams(layoutParams);
  }

  // 设置视频区域触摸监听
  @SuppressLint("ClickableViewAccessibility")
  private void setTouchListener() {
    textureView.setOnTouchListener((view, event) -> {
      if (surfaceSize == null) return true;
      int action = event.getActionMasked();
      if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
        int i = event.getActionIndex();
        pointerDownTime[i] = event.getEventTime();
        createTouchPacket(event, MotionEvent.ACTION_DOWN, i);
      } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) createTouchPacket(event, MotionEvent.ACTION_UP, event.getActionIndex());
      else for (int i = 0; i < event.getPointerCount(); i++) createTouchPacket(event, MotionEvent.ACTION_MOVE, i);
      return true;
    });
  }

  private final int[] pointerList = new int[20];
  private final long[] pointerDownTime = new long[10];

  private void createTouchPacket(MotionEvent event, int action, int i) {
    int offsetTime = (int) (event.getEventTime() - pointerDownTime[i]);
    int x = (int) event.getX(i);
    int y = (int) event.getY(i);
    int p = event.getPointerId(i);
    if (action == MotionEvent.ACTION_MOVE) {
      // 减少发送小范围移动(小于4的圆内不做处理)
      int flipX = pointerList[p] - x;
      if (flipX > -4 && flipX < 4) {
        int flipY = pointerList[10 + p] - y;
        if (flipY > -4 && flipY < 4) return;
      }
    }
    pointerList[p] = x;
    pointerList[10 + p] = y;
    controlPacket.sendTouchEvent(action, p, (float) x / surfaceSize.first, (float) y / surfaceSize.second, offsetTime);
  }

  // 更改View的形态
  public void viewAnim(View view, boolean toShowView, int translationX, int translationY, PublicTools.MyFunctionBoolean action) {
    // 创建平移动画
    view.setTranslationX(toShowView ? translationX : 0);
    float endX = toShowView ? 0 : translationX;
    view.setTranslationY(toShowView ? translationY : 0);
    float endY = toShowView ? 0 : translationY;
    // 创建透明度动画
    view.setAlpha(toShowView ? 0f : 1f);
    float endAlpha = toShowView ? 1f : 0f;

    // 设置动画时长和插值器
    ViewPropertyAnimator animator = view.animate()
      .translationX(endX)
      .translationY(endY)
      .alpha(endAlpha)
      .setDuration(toShowView ? 300 : 200)
      .setInterpolator(toShowView ? new OvershootInterpolator() : new DecelerateInterpolator());
    animator.withStartAction(() -> {
      if (action != null) action.run(true);
    });
    animator.withEndAction(() -> {
      if (action != null) action.run(false);
    });

    // 启动动画
    animator.start();
  }

  @Override
  public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
    // 初始化
    if (this.surfaceTexture == null) {
      this.surfaceTexture = surfaceTexture;
      onReady.run();
    } else textureView.setSurfaceTexture(this.surfaceTexture);
  }

  @Override
  public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
  }

  @Override
  public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
    return false;
  }

  @Override
  public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {
  }

}
