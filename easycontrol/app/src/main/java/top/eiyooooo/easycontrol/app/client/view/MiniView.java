package top.eiyooooo.easycontrol.app.client.view;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.graphics.PixelFormat;
import android.os.Build;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import java.util.concurrent.atomic.AtomicInteger;

import top.eiyooooo.easycontrol.app.entity.AppData;
import top.eiyooooo.easycontrol.app.helper.PublicTools;
import top.eiyooooo.easycontrol.app.R;
import top.eiyooooo.easycontrol.app.databinding.ModuleMiniViewBinding;

public class MiniView {

  private final ClientView clientView;
  private Thread timeoutListenerThread;
  private long lastTouchOutsideTime = 0;

  // 迷你悬浮窗
  private final ModuleMiniViewBinding miniView = ModuleMiniViewBinding.inflate(LayoutInflater.from(AppData.main));
  private final WindowManager.LayoutParams miniViewParams = new WindowManager.LayoutParams(
    WindowManager.LayoutParams.WRAP_CONTENT,
    WindowManager.LayoutParams.WRAP_CONTENT,
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE,
    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
    PixelFormat.TRANSLUCENT
  );

  private static int num = 0;

  public MiniView(ClientView clientView) {
    this.clientView = clientView;
    miniViewParams.gravity = Gravity.START | Gravity.TOP;
    miniViewParams.x = 0;
    // Bar颜色
    int colorNum = num++ % 4;
    int barColor = R.color.bar1;
    if (colorNum == 1) barColor = R.color.bar2;
    else if (colorNum == 2) barColor = R.color.bar3;
    else if (colorNum == 3) barColor = R.color.bar4;
    miniView.bar.setBackgroundTintList(ColorStateList.valueOf(AppData.main.getResources().getColor(barColor)));
  }

  public void show(int mode) {
    miniViewParams.y = clientView.device.mini_y;
    // 设置监听控制
    setBarListener();
    // 显示
    clientView.viewAnim(miniView.getRoot(), true, PublicTools.dp2px(-40f), 0, (isStart -> {
      if (isStart) {
        miniView.getRoot().setVisibility(View.VISIBLE);
        AppData.windowManager.addView(miniView.getRoot(), miniViewParams);
      }
    }));
    // 超时检测
    if (mode != 0 && AppData.setting.getMiniRecoverOnTimeout()) {
      lastTouchOutsideTime = System.currentTimeMillis();
      if (timeoutListenerThread != null) timeoutListenerThread.interrupt();
      timeoutListenerThread = new Thread(() -> timeoutListener(mode));
      timeoutListenerThread.start();
    }
  }

  public void hide() {
    try {
      miniView.getRoot().setVisibility(View.GONE);
      AppData.windowManager.removeView(miniView.getRoot());
      AppData.dbHelper.update(clientView.device);
      if (timeoutListenerThread != null) timeoutListenerThread.interrupt();
    } catch (Exception ignored) {
    }
  }

  // 设置监听控制
  @SuppressLint("ClickableViewAccessibility")
  private void setBarListener() {
    AtomicInteger yy = new AtomicInteger();
    AtomicInteger oldYy = new AtomicInteger();
    miniView.getRoot().setOnTouchListener((v, event) -> {
      switch (event.getActionMasked()) {
        case MotionEvent.ACTION_OUTSIDE:
          clientView.lastTouchIsInside = false;
          lastTouchOutsideTime = System.currentTimeMillis();
          break;
        case MotionEvent.ACTION_DOWN: {
          clientView.lastTouchIsInside = true;
          yy.set((int) event.getRawY());
          oldYy.set(miniViewParams.y);
          break;
        }
        case MotionEvent.ACTION_MOVE: {
          clientView.lastTouchIsInside = true;
          miniViewParams.y = oldYy.get() + (int) event.getRawY() - yy.get();
          clientView.device.mini_y = miniViewParams.y;
          AppData.windowManager.updateViewLayout(miniView.getRoot(), miniViewParams);
          break;
        }
        case MotionEvent.ACTION_UP:
          clientView.lastTouchIsInside = true;
          int flipY = (int) (yy.get() - event.getRawY());
          if (flipY * flipY < 16) clientView.changeToSmall();
          break;
      }
      return true;
    });
  }

  // 超时监听
  private void timeoutListener(int mode) {
    try {
      long now;
      while (!Thread.interrupted()) {
        Thread.sleep(1);
        now = System.currentTimeMillis();
        if (now - lastTouchOutsideTime > 5000) {
          if (mode == 1) AppData.uiHandler.post(clientView::changeToSmall);
          else if (mode == 2) AppData.uiHandler.post(clientView::changeToFull);
          return;
        }
      }
    } catch (Exception ignored) {}
  }
}
