package top.eiyooooo.easycontrol.app.client.view;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
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
import android.widget.LinearLayout;
import android.widget.Toast;

import top.eiyooooo.easycontrol.app.helper.PublicTools;
import top.eiyooooo.easycontrol.app.R;
import top.eiyooooo.easycontrol.app.client.Client;
import top.eiyooooo.easycontrol.app.databinding.ActivityFullBinding;
import top.eiyooooo.easycontrol.app.entity.AppData;

public class FullActivity extends Activity implements SensorEventListener {
  private ClientView clientView;
  private ActivityFullBinding fullActivity;

  private Pair<Integer, Integer> fullMaxSize;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    PublicTools.setLocale(this);
    fullActivity = ActivityFullBinding.inflate(this.getLayoutInflater());
    setContentView(fullActivity.getRoot());
    clientView = Client.allClient.get(getIntent().getIntExtra("index", 0)).clientView;
    clientView.setFullView(this);
    // 按键监听
    setButtonListener();
    setKeyEvent();
    // 更新textureView
    fullActivity.textureViewLayout.addView(clientView.textureView, 0);
    fullActivity.textureViewLayout.post(() -> {
      fullMaxSize = new Pair<>(fullActivity.textureViewLayout.getMeasuredWidth(), fullActivity.textureViewLayout.getMeasuredHeight());
      clientView.updateMaxSize(fullMaxSize);
      setNavBarHide(AppData.setting.getDefaultShowNavBar());
      changeMode(-clientView.mode);
    });
    // 页面自动旋转
    AppData.sensorManager.registerListener(this, AppData.sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
    super.onCreate(savedInstanceState);
  }

  @Override
  protected void onPause() {
    AppData.sensorManager.unregisterListener(this);
    if (isChangingConfigurations()) fullActivity.textureViewLayout.removeView(clientView.textureView);
    else if (clientView != null) {
      if (AppData.setting.getFullToMiniOnExit()) clientView.changeToMini(2);
      else clientView.changeToSmall();
    }
    super.onPause();
  }

  @Override
  protected void onResume() {
    PublicTools.setFullScreen(this);
    super.onResume();
  }

  @Override
  public void onMultiWindowModeChanged(boolean isInMultiWindowMode, Configuration newConfig) {
    fullActivity.textureViewLayout.post(() -> {
      fullMaxSize = new Pair<>(fullActivity.textureViewLayout.getMeasuredWidth(), fullActivity.textureViewLayout.getMeasuredHeight());
      clientView.updateMaxSize(fullMaxSize);
    });
    super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig);
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
      clientView.changeSize(getRatio(fullActivity.navBar.getVisibility() == View.VISIBLE));
      clientView.updateMaxSize(fullMaxSize);
    }
  }

  // 获取去除操作栏后的屏幕大小，用于修改宽高比例使用
  public float getRatio(boolean barIsShow) {
    int width = fullMaxSize.first;
    int height = fullMaxSize.second;
    LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) fullActivity.navBar.getLayoutParams();
    if (barIsShow) {
      if (width > height) return (float) (width - layoutParams.width) / (float) height;
      else return (float) width / (float) (height - layoutParams.height);
    } else {
      return (float) width / (float) height;
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
    if (!AppData.setting.getAlwaysFullMode()) {
      fullActivity.buttonMini.setOnClickListener(v -> clientView.changeToMini(0));
      fullActivity.buttonFullExit.setOnClickListener(v -> clientView.changeToSmall());
    } else {
      fullActivity.buttonMini.setOnClickListener(v -> PublicTools.logToast(getString(R.string.error_mode_not_support)));
      fullActivity.buttonFullExit.setOnClickListener(v -> PublicTools.logToast(getString(R.string.error_mode_not_support)));
    }
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
    if (AppData.setting.getAlwaysFullMode()) {
      lockOrientation = true;
      fullActivity.buttonLock.setImageResource(R.drawable.unlock);
      fullActivity.buttonLock.setOnClickListener(v -> PublicTools.logToast(getString(R.string.error_mode_not_support)));
      DisplayMetrics metrics = getResources().getDisplayMetrics();
      int orientation;
      if (metrics.widthPixels > metrics.heightPixels) orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
      else orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
      setRequestedOrientation(orientation);
      lastOrientation = orientation;
    } else {
      fullActivity.buttonLock.setOnClickListener(v -> {
        lockOrientation = !lockOrientation;
        fullActivity.buttonLock.setImageResource(lockOrientation ? R.drawable.unlock : R.drawable.lock);
        barViewTimer();
      });
    }
  }

  // 导航栏隐藏
  private void setNavBarHide(boolean isShow) {
    fullActivity.navBar.setVisibility(isShow ? View.VISIBLE : View.GONE);
    fullActivity.buttonNavBar.setImageResource(isShow ? R.drawable.not_equal : R.drawable.equals);
    if (clientView.mode == 1 && clientView.device.setResolution) clientView.changeSize(getRatio(isShow));
  }

  private void changeBarView() {
    if (clientView == null) return;
    boolean toShowView = fullActivity.barView.getVisibility() == View.GONE;
    boolean isLandscape = lastOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE || lastOrientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
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

  private boolean lockOrientation = false;
  private int lastOrientation = -1;

  @Override
  public void onSensorChanged(SensorEvent sensorEvent) {
    if (lockOrientation || Sensor.TYPE_ACCELEROMETER != sensorEvent.sensor.getType()) return;
    float[] values = sensorEvent.values;
    float x = values[0];
    float y = values[1];
    int newOrientation = lastOrientation;

    if (x > -3 && x < 3 && y >= 4.5) newOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
    else if (y > -3 && y < 3 && x >= 4.5) newOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
    else if (y > -3 && y < 3 && x <= -4.5) newOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
    else if (x > -3 && x < 3 && y <= -4.5) newOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;

    if (lastOrientation != newOrientation) {
      lastOrientation = newOrientation;
      setRequestedOrientation(newOrientation);
    }
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int i) {

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