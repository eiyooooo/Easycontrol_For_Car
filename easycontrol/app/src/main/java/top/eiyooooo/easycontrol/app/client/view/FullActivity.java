package top.eiyooooo.easycontrol.app.client.view;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;

import top.eiyooooo.easycontrol.app.helper.PublicTools;
import top.eiyooooo.easycontrol.app.R;
import top.eiyooooo.easycontrol.app.client.Client;
import top.eiyooooo.easycontrol.app.databinding.ActivityFullBinding;
import top.eiyooooo.easycontrol.app.entity.AppData;

public class FullActivity extends Activity {
  private ClientView clientView;
  private ActivityFullBinding fullActivity;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    fullActivity = ActivityFullBinding.inflate(this.getLayoutInflater());
    setContentView(fullActivity.getRoot());
    Client client;
    try {
      client = Client.allClient.get(getIntent().getIntExtra("index", 0));
      if (client.isClosed()) throw new Exception();
      if (client.clientView.textureView.getParent() != null) client.clientView.hide(false);
    } catch (Exception ignored) {
      finish();
      return;
    }
    clientView = client.clientView;
    clientView.setFullView(this);
    // 监听
    setButtonListener();
    setKeyEvent();
    fullActivity.textureViewLayout.addOnLayoutChangeListener((view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom)->{
      if (left == oldLeft && top == oldTop && right == oldRight && bottom == oldBottom) return;
      updateMaxSize();
    });
    // 更新textureView
    fullActivity.textureViewLayout.addView(clientView.textureView, 0);
    setNavBarHide(AppData.setting.getDefaultShowNavBar());
    changeMode(-clientView.mode);
  }

  public Pair<Integer, Integer> fullMaxSize;

  private void updateMaxSize() {
    fullMaxSize = new Pair<>(fullActivity.textureViewLayout.getMeasuredWidth(), fullActivity.textureViewLayout.getMeasuredHeight());
    clientView.updateMaxSize(fullMaxSize);
    if (clientView.mode == 1 && clientView.device.setResolution)
      clientView.changeSize((float) fullMaxSize.first / (float) fullMaxSize.second);
  }

  @Override
  protected void onPause() {
    if (isChangingConfigurations()) fullActivity.textureViewLayout.removeView(clientView.textureView);
    super.onPause();
  }

  @Override
  protected void onResume() {
    if (AppData.setting.getSetFullScreen()) PublicTools.setFullScreen(this);
    super.onResume();
  }

  @Override
  public void onBackPressed() {
    Toast.makeText(AppData.main, getString(R.string.error_refused_back), Toast.LENGTH_SHORT).show();
  }

  public void hide() {
    try {
      fullActivity.textureViewLayout.removeView(clientView.textureView);
      clientView.setFullView(null);
      clientView = null;
      finish();
    } catch (Exception ignored) {
    }
  }

  public void changeMode(int mode) {
    fullActivity.buttonSwitch.setVisibility(mode == 0 ? View.VISIBLE : View.INVISIBLE);
    fullActivity.buttonHome.setVisibility(mode == 0 ? View.VISIBLE : View.INVISIBLE);
    if (mode == 0) fullActivity.buttonTransfer.setImageResource(R.drawable.share_out);
    else fullActivity.buttonTransfer.setImageResource(R.drawable.share_in);
    if (mode > 0 && clientView.mode == 1 && clientView.device.setResolution) {
      updateMaxSize();
    }
  }

  // 设置按钮监听
  private void setButtonListener() {
    fullActivity.buttonRotate.setOnClickListener(v -> {
      clientView.controlPacket.sendRotateEvent();
      barViewTimer();
    });
    fullActivity.buttonBack.setOnClickListener(v -> clientView.controlPacket.sendKeyEvent(4, 0, -1));
    fullActivity.buttonHome.setOnClickListener(v -> clientView.controlPacket.sendKeyEvent(3, 0, -1));
    fullActivity.buttonSwitch.setOnClickListener(v -> clientView.controlPacket.sendKeyEvent(187, 0, -1));
    fullActivity.buttonMore.setOnClickListener(v -> {
      changeBarView();
      barViewTimer();
    });
    fullActivity.buttonNavBar.setOnClickListener(v -> {
      setNavBarHide(fullActivity.navBar.getVisibility() == View.GONE);
      barViewTimer();
    });
    fullActivity.buttonClose.setOnClickListener(v -> clientView.onClose.run());
    if (clientView.mode == 1) fullActivity.buttonTransfer.setImageResource(R.drawable.share_in);
    fullActivity.buttonTransfer.setOnClickListener(v -> {
      clientView.changeMode.run(clientView.mode == 0 ? 1 : 0);
      barViewTimer();
    });
    if (!clientView.lightState) fullActivity.buttonLightOff.setImageResource(R.drawable.lightbulb);
    fullActivity.buttonLightOff.setOnClickListener(v -> {
      if (clientView.lightState) {
        clientView.controlPacket.sendLightEvent(Display.STATE_UNKNOWN);
        fullActivity.buttonLightOff.setImageResource(R.drawable.lightbulb);
        clientView.lightState = false;
      } else {
        clientView.controlPacket.sendLightEvent(Display.STATE_ON);
        fullActivity.buttonLightOff.setImageResource(R.drawable.lightbulb_off);
        clientView.lightState = true;
      }
      barViewTimer();
    });
    fullActivity.buttonPower.setOnClickListener(v -> {
      clientView.controlPacket.sendPowerEvent();
      barViewTimer();
    });
    DisplayMetrics metrics = getResources().getDisplayMetrics();
    if (metrics.widthPixels > metrics.heightPixels) orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
    else orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
    setRequestedOrientation(orientation);
  }

  // 导航栏隐藏
  private void setNavBarHide(boolean isShow) {
    fullActivity.navBar.setVisibility(isShow ? View.VISIBLE : View.GONE);
    fullActivity.buttonNavBar.setImageResource(isShow ? R.drawable.not_equal : R.drawable.equals);
  }

  private int orientation;

  private void changeBarView() {
    if (clientView == null) return;
    boolean toShowView = fullActivity.barView.getVisibility() == View.GONE;
    boolean isLandscape = orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
    clientView.viewAnim(fullActivity.barView, toShowView, 0, PublicTools.dp2px(40f) * (isLandscape ? -1 : 1), (isStart -> {
      if (isStart && toShowView) fullActivity.barView.setVisibility(View.VISIBLE);
      else if (!isStart && !toShowView) fullActivity.barView.setVisibility(View.GONE);
    }));
  }

  private Thread barViewTimerThread = null;
  private void barViewTimer() {
    if (barViewTimerThread != null) barViewTimerThread.interrupt();
    barViewTimerThread = new Thread(() -> {
      try {
        Thread.sleep(2000);
        AppData.uiHandler.post(() -> {
          if (fullActivity.barView.getVisibility() == View.VISIBLE) changeBarView();
        });
      } catch (InterruptedException ignored) {
      }
    });
    barViewTimerThread.start();
  }

  // 设置键盘监听
  private void setKeyEvent() {
    fullActivity.editText.requestFocus();
    fullActivity.editText.setInputType(InputType.TYPE_NULL);
    fullActivity.editText.setOnKeyListener((v, keyCode, event) -> {
      if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode != KeyEvent.KEYCODE_VOLUME_UP && keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) {
        clientView.controlPacket.sendKeyEvent(event.getKeyCode(), event.getMetaState(), 0);
        return true;
      }
      return false;
    });
  }
}