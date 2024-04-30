package top.eiyooooo.easycontrol.app.client.view;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.graphics.Outline;
import android.graphics.PixelFormat;
import android.os.Build;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.view.*;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import top.eiyooooo.easycontrol.app.client.Client;
import top.eiyooooo.easycontrol.app.client.ControlPacket;
import top.eiyooooo.easycontrol.app.entity.AppData;
import top.eiyooooo.easycontrol.app.helper.PublicTools;
import top.eiyooooo.easycontrol.app.R;
import top.eiyooooo.easycontrol.app.databinding.ModuleSmallViewBinding;

public class SmallView extends ViewOutlineProvider {
  private final ClientView clientView;
  private static int statusBarHeight = 0;
  private boolean LocalIsPortrait() {
    return AppData.main.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
  }
  private boolean LastLocalIsPortrait;
  private boolean RemoteIsPortrait = true;
  private int InitSize = 0;
  private boolean InitPos = false;
  private boolean needSetResolution = true;
  int longEdge;
  int shortEdge;

  // 悬浮窗
  private final ModuleSmallViewBinding smallView = ModuleSmallViewBinding.inflate(LayoutInflater.from(AppData.main));
  private final WindowManager.LayoutParams smallViewParams =
    new WindowManager.LayoutParams(
      WindowManager.LayoutParams.WRAP_CONTENT,
      WindowManager.LayoutParams.WRAP_CONTENT,
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE,
      LayoutParamsFlagFocus,
      PixelFormat.TRANSLUCENT
    );

  private static final int LayoutParamsFlagFocus = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
  private static final int LayoutParamsFlagNoFocus = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;

  public SmallView(ClientView clientView) {
    this.clientView = clientView;
    smallViewParams.gravity = Gravity.START | Gravity.TOP;
    // 设置默认导航栏状态
    setNavBarHide(AppData.setting.getDefaultShowNavBar());
    // 获取屏幕宽高
    DisplayMetrics displayMetrics = AppData.main.getResources().getDisplayMetrics();
    int screenWidth = displayMetrics.widthPixels;
    int screenHeight = displayMetrics.heightPixels + statusBarHeight;
    longEdge = Math.max(screenWidth, screenHeight);
    shortEdge = Math.min(screenWidth, screenHeight);
    // 设置默认大小
    if (clientView.device.small_p_p_width == 0 || clientView.device.small_p_p_height == 0
            || clientView.device.small_p_l_width == 0 || clientView.device.small_p_l_height == 0
            || clientView.device.small_l_p_width == 0 || clientView.device.small_l_p_height == 0
            || clientView.device.small_l_l_width == 0 || clientView.device.small_l_l_height == 0
            || clientView.device.small_free_width == 0 || clientView.device.small_free_height == 0) {
      clientView.device.small_p_p_width = shortEdge * 4 / 5;
      clientView.device.small_p_p_height = longEdge * 4 / 5;
      clientView.device.small_p_l_width = shortEdge * 4 / 5;
      clientView.device.small_p_l_height = longEdge * 4 / 5;
      clientView.device.small_l_p_width = longEdge * 4 / 5;
      clientView.device.small_l_p_height = shortEdge * 4 / 5;
      clientView.device.small_l_l_width = longEdge * 4 / 5;
      clientView.device.small_l_l_height = shortEdge * 4 / 5;
      clientView.device.small_free_width = shortEdge * 4 / 5;
      clientView.device.small_free_height = longEdge * 4 / 5;
    }
    // 设置监听控制
    setFloatVideoListener();
    setReSizeListener();
    setBarListener();
    // 设置窗口监听
    smallView.textureViewLayout.addOnLayoutChangeListener((view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
      if (right == 0 || bottom == 0) return;
      InitSize++;
      if (InitSize < 2) return;

      if (clientView.mode == 1 && clientView.device.setResolution) {
        if (clientView.device.small_free_x == 0 && clientView.device.small_free_y == 0) {
          clientView.updateMaxSize(new Pair<>(clientView.device.small_free_width, clientView.device.small_free_height));
          ViewGroup.LayoutParams layoutParams = clientView.textureView.getLayoutParams();
          smallViewParams.x = clientView.device.small_free_x = (shortEdge - layoutParams.width) / 2;
          smallViewParams.y = clientView.device.small_free_y = (longEdge - layoutParams.height) / 2;
          AppData.windowManager.updateViewLayout(smallView.getRoot(), smallViewParams);
          InitPos = true;
        }

        if (needSetResolution) {
          clientView.changeSize((float) clientView.device.small_free_width / (float) clientView.device.small_free_height);
          needSetResolution = false;
        }

        if (!InitPos) {
          smallViewParams.x = clientView.device.small_free_x;
          smallViewParams.y = clientView.device.small_free_y;
          AppData.windowManager.updateViewLayout(smallView.getRoot(), smallViewParams);
          clientView.updateMaxSize(new Pair<>(clientView.device.small_free_width, clientView.device.small_free_height));
          InitPos = true;
        }
        return;
      }

      if (clientView.device.small_p_p_x == 0 && clientView.device.small_p_p_y == 0 && right < bottom && LocalIsPortrait()) {
        clientView.updateMaxSize(new Pair<>(clientView.device.small_p_p_width, clientView.device.small_p_p_height));
        ViewGroup.LayoutParams layoutParams = clientView.textureView.getLayoutParams();
        smallViewParams.x = clientView.device.small_p_p_x = (shortEdge - layoutParams.width) / 2;
        smallViewParams.y = clientView.device.small_p_p_y = (longEdge - layoutParams.height) / 2;
        AppData.windowManager.updateViewLayout(smallView.getRoot(), smallViewParams);
        InitPos = true;
        return;
      }
      if (clientView.device.small_p_l_x == 0 && clientView.device.small_p_l_y == 0 && right > bottom && LocalIsPortrait()) {
        clientView.updateMaxSize(new Pair<>(clientView.device.small_p_l_width, clientView.device.small_p_l_height));
        ViewGroup.LayoutParams layoutParams = clientView.textureView.getLayoutParams();
        smallViewParams.x = clientView.device.small_p_l_x = (shortEdge - layoutParams.width) / 2;
        smallViewParams.y = clientView.device.small_p_l_y = (longEdge - layoutParams.height) / 2;
        AppData.windowManager.updateViewLayout(smallView.getRoot(), smallViewParams);
        InitPos = true;
        return;
      }
      if (clientView.device.small_l_p_x == 0 && clientView.device.small_l_p_y == 0 && right < bottom && !LocalIsPortrait()) {
        clientView.updateMaxSize(new Pair<>(clientView.device.small_l_p_width, clientView.device.small_l_p_height));
        ViewGroup.LayoutParams layoutParams = clientView.textureView.getLayoutParams();
        smallViewParams.x = clientView.device.small_l_p_x = (longEdge - layoutParams.width) / 2;
        smallViewParams.y = clientView.device.small_l_p_y = (shortEdge - layoutParams.height) / 2;
        AppData.windowManager.updateViewLayout(smallView.getRoot(), smallViewParams);
        InitPos = true;
        return;
      }
      if (clientView.device.small_l_l_x == 0 && clientView.device.small_l_l_y == 0 && right > bottom && !LocalIsPortrait()) {
        clientView.updateMaxSize(new Pair<>(clientView.device.small_l_l_width, clientView.device.small_l_l_height));
        ViewGroup.LayoutParams layoutParams = clientView.textureView.getLayoutParams();
        smallViewParams.x = clientView.device.small_l_l_x = (longEdge - layoutParams.width) / 2;
        smallViewParams.y = clientView.device.small_l_l_y = (shortEdge - layoutParams.height) / 2;
        AppData.windowManager.updateViewLayout(smallView.getRoot(), smallViewParams);
        InitPos = true;
        return;
      }

      boolean LocalIsPortrait = LocalIsPortrait();
      if (!InitPos || right < bottom != RemoteIsPortrait || LocalIsPortrait != LastLocalIsPortrait) {
        InitPos = true;
        LastLocalIsPortrait = LocalIsPortrait;
        if (right < bottom) {
          if (LocalIsPortrait) {
            smallViewParams.x = clientView.device.small_p_p_x;
            smallViewParams.y = clientView.device.small_p_p_y;
            clientView.updateMaxSize(new Pair<>(clientView.device.small_p_p_width, clientView.device.small_p_p_height));
          } else {
            smallViewParams.x = clientView.device.small_l_p_x;
            smallViewParams.y = clientView.device.small_l_p_y;
            clientView.updateMaxSize(new Pair<>(clientView.device.small_l_p_width, clientView.device.small_l_p_height));
          }
        } else {
          if (LocalIsPortrait) {
            smallViewParams.x = clientView.device.small_p_l_x;
            smallViewParams.y = clientView.device.small_p_l_y;
            clientView.updateMaxSize(new Pair<>(clientView.device.small_p_l_width, clientView.device.small_p_l_height));
          } else {
            smallViewParams.x = clientView.device.small_l_l_x;
            smallViewParams.y = clientView.device.small_l_l_y;
            clientView.updateMaxSize(new Pair<>(clientView.device.small_l_l_width, clientView.device.small_l_l_height));
          }
        }
        AppData.windowManager.updateViewLayout(smallView.getRoot(), smallViewParams);
      }
      RemoteIsPortrait = right < bottom;
    });
    // 设置窗口大小
    clientView.updateMaxSize(new Pair<>(shortEdge * 4 / 5, shortEdge * 4 / 5));
    // 设置圆角
    smallView.getRoot().setOutlineProvider(this);
    smallView.getRoot().setClipToOutline(true);
  }

  public void show() {
    // 初始化
    InitPos = false;
    needSetResolution = true;
    smallView.barView.setVisibility(View.GONE);
    smallView.bar.setVisibility(View.VISIBLE);
    barTimer();
    // 设置监听
    setButtonListener(clientView.controlPacket);
    setKeyEvent(clientView.controlPacket);
    // 显示
    AppData.windowManager.addView(smallView.getRoot(), smallViewParams);
    smallView.textureViewLayout.addView(clientView.textureView, 0);
    clientView.viewAnim(smallView.getRoot(), true, 0, PublicTools.dp2px(40f), null);
  }

  public void hide() {
    try {
      if (barTimerThread != null) barTimerThread.interrupt();
      if (barViewTimerThread != null) barViewTimerThread.interrupt();
      smallView.textureViewLayout.removeView(clientView.textureView);
      AppData.windowManager.removeView(smallView.getRoot());
      clientView.updateDevice();
    } catch (Exception ignored) {
    }
  }

  public void changeMode(int mode) {
    smallView.buttonSwitch.setVisibility(mode == 0 ? View.VISIBLE : View.INVISIBLE);
    smallView.buttonHome.setVisibility(mode == 0 ? View.VISIBLE : View.INVISIBLE);
    if (mode == 0) smallView.buttonTransfer.setImageResource(R.drawable.share_out);
    else smallView.buttonTransfer.setImageResource(R.drawable.share_in);
    InitPos = false;
    if (clientView.mode == 1 && clientView.device.setResolution) {
      needSetResolution = true;
      clientView.updateMaxSize(new Pair<>(clientView.textureView.getWidth(), clientView.textureView.getHeight()));
    }
  }

  // 设置焦点监听
  @SuppressLint("ClickableViewAccessibility")
  private void setFloatVideoListener() {
    smallView.getRoot().setOnTouchHandle(event -> {
      if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
        clientView.lastTouchIsInside = false;
        if (AppData.setting.getDefaultMiniOnOutside()) {
          if (Client.allClient.size() > 1) {
            new Thread(() -> {
              try {
                Thread.sleep(100);
                for (Client client : Client.allClient)
                  if (client.clientView.lastTouchIsInside) return;
                AppData.uiHandler.post(() -> clientView.changeToMini(1));
              } catch (InterruptedException ignored) {}
            }).start();
          }
          else clientView.changeToMini(1);
        }
        else if (smallViewParams.flags != LayoutParamsFlagNoFocus) {
          smallView.editText.clearFocus();
          smallViewParams.flags = LayoutParamsFlagNoFocus;
          AppData.windowManager.updateViewLayout(smallView.getRoot(), smallViewParams);
        }
        if (smallView.barView.getVisibility() == View.VISIBLE) {
          clientView.viewAnim(smallView.barView, false, 0, PublicTools.dp2px(-40f), (isStart -> {
            if (!isStart) smallView.barView.setVisibility(View.GONE);
          }));
        }
      } else {
        clientView.lastTouchIsInside = true;
        changeBar(1);
        barTimer();
        if (smallViewParams.flags != LayoutParamsFlagFocus) {
          smallView.editText.requestFocus();
          smallViewParams.flags = LayoutParamsFlagFocus;
          AppData.windowManager.updateViewLayout(smallView.getRoot(), smallViewParams);
        }
      }
    });
  }

  // 设置上横条监听控制
  @SuppressLint("ClickableViewAccessibility")
  private void setBarListener() {
    AtomicBoolean isFilp = new AtomicBoolean(false);
    AtomicInteger xx = new AtomicInteger();
    AtomicInteger yy = new AtomicInteger();
    AtomicInteger paramsX = new AtomicInteger();
    AtomicInteger paramsY = new AtomicInteger();
    smallView.bar.setOnTouchListener((v, event) -> {
      switch (event.getActionMasked()) {
        case MotionEvent.ACTION_DOWN: {
          xx.set((int) event.getRawX());
          yy.set((int) event.getRawY());
          paramsX.set(smallViewParams.x);
          paramsY.set(smallViewParams.y);
          isFilp.set(false);
          break;
        }
        case MotionEvent.ACTION_MOVE: {
          int x = (int) event.getRawX();
          int y = (int) event.getRawY();
          int flipX = x - xx.get();
          int flipY = y - yy.get();
          // 适配一些机器将点击视作小范围移动(小于50的圆内不做处理)
          if (!isFilp.get()) {
            if (flipX * flipX + flipY * flipY < 2500) return true;
            isFilp.set(true);
          }
          // 拖动限制，避免拖到状态栏
          if (y < statusBarHeight + 10) return true;
          // 更新
          smallViewParams.x = paramsX.get() + flipX;
          smallViewParams.y = paramsY.get() + flipY;

          if (clientView.mode == 1 && clientView.device.setResolution) {
            clientView.device.small_free_x = smallViewParams.x;
            clientView.device.small_free_y = smallViewParams.y;
          }
          else {
            if (RemoteIsPortrait) {
              if (LocalIsPortrait()) {
                clientView.device.small_p_p_x = smallViewParams.x;
                clientView.device.small_p_p_y = smallViewParams.y;
              } else {
                clientView.device.small_l_p_x = smallViewParams.x;
                clientView.device.small_l_p_y = smallViewParams.y;
              }
            } else {
              if (LocalIsPortrait()) {
                clientView.device.small_p_l_x = smallViewParams.x;
                clientView.device.small_p_l_y = smallViewParams.y;
              } else {
                clientView.device.small_l_l_x = smallViewParams.x;
                clientView.device.small_l_l_y = smallViewParams.y;
              }
            }
          }
          AppData.windowManager.updateViewLayout(smallView.getRoot(), smallViewParams);
          break;
        }
        case MotionEvent.ACTION_UP:
          if (!isFilp.get()) {
            changeBarView();
            barViewTimer();
          }
          break;
      }
      return true;
    });
  }

  // 设置按钮监听
  private void setButtonListener(ControlPacket controlPacket) {
    smallView.buttonRotate.setOnClickListener(v -> {
      controlPacket.sendRotateEvent();
      changeBarView();
    });
    smallView.buttonBack.setOnClickListener(v -> controlPacket.sendKeyEvent(4, 0, -1));
    smallView.buttonHome.setOnClickListener(v -> controlPacket.sendKeyEvent(3, 0, -1));
    smallView.buttonSwitch.setOnClickListener(v -> controlPacket.sendKeyEvent(187, 0, -1));
    smallView.buttonNavBar.setOnClickListener(v -> {
      setNavBarHide(smallView.navBar.getVisibility() == View.GONE);
      barViewTimer();
    });
    smallView.buttonMini.setOnClickListener(v -> {
      clientView.changeToMini(0);
      barViewTimer();
    });
    smallView.buttonFull.setOnClickListener(v -> {
      clientView.changeToFull();
      barViewTimer();
    });
    smallView.buttonClose.setOnClickListener(v -> {
      clientView.updateDevice();
      clientView.onClose.run();
    });
    if (clientView.mode == 1) smallView.buttonTransfer.setImageResource(R.drawable.share_in);
    smallView.buttonTransfer.setOnClickListener(v -> {
      clientView.changeMode.run(clientView.mode == 0 ? 1 : 0);
      barViewTimer();
    });
    smallView.buttonLight.setOnClickListener(v -> {
      controlPacket.sendLightEvent(Display.STATE_ON);
      clientView.lightState = true;
      barViewTimer();
    });
    smallView.buttonLightOff.setOnClickListener(v -> {
      controlPacket.sendLightEvent(Display.STATE_UNKNOWN);
      clientView.lightState = false;
      barViewTimer();
    });
    smallView.resetLocation.setOnClickListener(v -> {
      clientView.device.small_p_p_x = 0;
      clientView.device.small_p_p_y = 0;
      clientView.device.small_p_l_x = 0;
      clientView.device.small_p_l_y = 0;
      clientView.device.small_l_p_x = 0;
      clientView.device.small_l_p_y = 0;
      clientView.device.small_l_l_x = 0;
      clientView.device.small_l_l_y = 0;
      clientView.device.small_free_x = 0;
      clientView.device.small_free_y = 0;
      clientView.device.small_p_p_width = shortEdge * 4 / 5;
      clientView.device.small_p_p_height = longEdge * 4 / 5;
      clientView.device.small_p_l_width = shortEdge * 4 / 5;
      clientView.device.small_p_l_height = longEdge * 4 / 5;
      clientView.device.small_l_p_width = longEdge * 4 / 5;
      clientView.device.small_l_p_height = shortEdge * 4 / 5;
      clientView.device.small_l_l_width = longEdge * 4 / 5;
      clientView.device.small_l_l_height = shortEdge * 4 / 5;
      clientView.device.small_free_width = shortEdge * 4 / 5;
      clientView.device.small_free_height = longEdge * 4 / 5;
      clientView.updateMaxSize(clientView.getMaxSize());
      barViewTimer();
    });
    smallView.buttonPower.setOnClickListener(v -> {
      controlPacket.sendPowerEvent();
      barViewTimer();
    });
  }

  // 导航栏隐藏
  private void setNavBarHide(boolean isShow) {
    smallView.navBar.setVisibility(isShow ? View.VISIBLE : View.GONE);
    smallView.buttonNavBar.setImageResource(isShow ? R.drawable.not_equal : R.drawable.equals);
  }

  private void changeBarView() {
    if (clientView == null) return;
    boolean toShowView = smallView.barView.getVisibility() == View.GONE;
    clientView.viewAnim(smallView.barView, toShowView, 0, PublicTools.dp2px(-40f), (isStart -> {
      if (isStart && toShowView) smallView.barView.setVisibility(View.VISIBLE);
      else if (!isStart && !toShowView) smallView.barView.setVisibility(View.GONE);
    }));
  }

  private void changeBar(int Show) {
    if (clientView == null) return;
    if (Show == 1 & smallView.bar.getVisibility() == View.VISIBLE) return;
    if (Show == -1 & smallView.bar.getVisibility() == View.GONE) return;
    boolean toShow = smallView.bar.getVisibility() == View.GONE;
    clientView.viewAnim(smallView.bar, toShow, 0, PublicTools.dp2px(-20f), (isStart -> {
      if (isStart && toShow) smallView.bar.setVisibility(View.VISIBLE);
      else if (!isStart && !toShow) smallView.bar.setVisibility(View.GONE);
    }));
  }

  private Thread barTimerThread = null;
  private void barTimer() {
    if (barTimerThread != null) barTimerThread.interrupt();
    barTimerThread = new Thread(() -> {
      try {
        Thread.sleep(10000);
        if (smallView.bar.getVisibility() == View.VISIBLE)
          AppData.uiHandler.post(() -> changeBar(0));
      } catch (InterruptedException ignored) {
      }
    });
    barTimerThread.start();
  }

  private Thread barViewTimerThread = null;
  private void barViewTimer() {
    if (barViewTimerThread != null) barViewTimerThread.interrupt();
    barViewTimerThread = new Thread(() -> {
      try {
        Thread.sleep(2000);
        if (smallView.barView.getVisibility() == View.VISIBLE)
          AppData.uiHandler.post(this::changeBarView);
      } catch (InterruptedException ignored) {
      }
    });
    barViewTimerThread.start();
  }

  // 设置悬浮窗大小拖动按钮监听控制
  @SuppressLint("ClickableViewAccessibility")
  private void setReSizeListener() {
    int minSize = PublicTools.dp2px(150f);
    smallView.reSize.setOnTouchListener((v, event) -> {
      if (!InitPos) return true;
      if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
        int sizeX = (int) (event.getRawX() - smallViewParams.x);
        int sizeY = (int) (event.getRawY() - smallViewParams.y);
        if (sizeX < minSize || sizeY < minSize) return true;

        if (clientView.mode == 1 && clientView.device.setResolution) {
          clientView.reCalculateTextureViewSize(sizeX, sizeY);

          clientView.device.small_free_width = sizeX;
          clientView.device.small_free_height = sizeY;
        }
        else {
          clientView.updateMaxSize(new Pair<>(sizeX, sizeY));

          if (sizeX < sizeY) {
            if (LocalIsPortrait()) {
              clientView.device.small_p_p_width = sizeX;
              clientView.device.small_p_p_height = sizeY;
            } else {
              clientView.device.small_l_p_width = sizeX;
              clientView.device.small_l_p_height = sizeY;
            }
          } else {
            if (LocalIsPortrait()) {
              clientView.device.small_p_l_width = sizeX;
              clientView.device.small_p_l_height = sizeY;
            } else {
              clientView.device.small_l_l_width = sizeX;
              clientView.device.small_l_l_height = sizeY;
            }
          }
        }
      } else if (event.getActionMasked() == MotionEvent.ACTION_UP
              && clientView.mode == 1 && clientView.device.setResolution) {
        needSetResolution = true;
        InitPos = false;
        clientView.updateMaxSize(new Pair<>(clientView.textureView.getWidth(), clientView.textureView.getHeight()));
      }
      return true;
    });
  }

  // 设置键盘监听
  private void setKeyEvent(ControlPacket controlPacket) {
    smallView.editText.setInputType(InputType.TYPE_NULL);
    smallView.editText.setOnKeyListener((v, keyCode, event) -> {
      if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode != KeyEvent.KEYCODE_VOLUME_UP && keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) {
        controlPacket.sendKeyEvent(event.getKeyCode(), event.getMetaState(), 0);
        return true;
      }
      return false;
    });
  }

  @Override
  public void getOutline(View view, Outline outline) {
    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), AppData.main.getResources().getDimension(R.dimen.round));
  }

  static {
    @SuppressLint("InternalInsetResource") int resourceId = AppData.main.getResources().getIdentifier("status_bar_height", "dimen", "android");
    if (resourceId > 0) {
      statusBarHeight = AppData.main.getResources().getDimensionPixelSize(resourceId);
    }
  }

}
