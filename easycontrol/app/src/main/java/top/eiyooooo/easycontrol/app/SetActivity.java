package top.eiyooooo.easycontrol.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import top.eiyooooo.easycontrol.app.entity.AppData;
import top.eiyooooo.easycontrol.app.helper.PublicTools;
import top.eiyooooo.easycontrol.app.BuildConfig;
import top.eiyooooo.easycontrol.app.databinding.ActivitySetBinding;

public class SetActivity extends Activity {
  private ActivitySetBinding setActivity;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    PublicTools.setStatusAndNavBar(this);
    PublicTools.setLocale(this);
    setActivity = ActivitySetBinding.inflate(this.getLayoutInflater());
    setContentView(setActivity.getRoot());
    // 设置页面
    drawUi();
    setButtonListener();
    super.onCreate(savedInstanceState);
  }

  // 设置默认值
  private void drawUi() {
    // 默认参数
    PublicTools.createDeviceOptionSet(this, setActivity.setDefault, null);
    // 显示
    setActivity.setDisplay.addView(PublicTools.createSwitchCard(this, getString(R.string.set_turn_on_screen_if_start), getString(R.string.set_turn_on_screen_if_start_detail), AppData.setting.getTurnOnScreenIfStart(),
            isChecked -> {
              if (!isChecked) {
                setActivity.setDisplay.removeViewAt(1);
                AppData.setting.setTurnOffScreenIfStart(false);
                setActivity.setDisplay.addView(PublicTools.createSwitchCardEx(this, getString(R.string.set_turn_off_screen_if_start), getString(R.string.set_turn_off_screen_if_start_detail), AppData.setting.getTurnOffScreenIfStart(),
                        (buttonView, isChecked1) -> {
                          if (!AppData.setting.getTurnOnScreenIfStart()) {
                            buttonView.setChecked(false);
                            Toast.makeText(this, getString(R.string.set_turn_off_screen_if_start_error), Toast.LENGTH_SHORT).show();
                          }
                          else AppData.setting.setTurnOffScreenIfStart(isChecked1);
                        }).getRoot(), 1);
              }
              AppData.setting.setTurnOnScreenIfStart(isChecked);
            }).getRoot());

    setActivity.setDisplay.addView(PublicTools.createSwitchCardEx(this, getString(R.string.set_turn_off_screen_if_start), getString(R.string.set_turn_off_screen_if_start_detail), AppData.setting.getTurnOffScreenIfStart(),
            (buttonView, isChecked) -> {
              if (!AppData.setting.getTurnOnScreenIfStart()) {
                buttonView.setChecked(false);
                Toast.makeText(this, getString(R.string.set_turn_off_screen_if_start_error), Toast.LENGTH_SHORT).show();
              }
              else AppData.setting.setTurnOffScreenIfStart(isChecked);
            }).getRoot());

    setActivity.setDisplay.addView(PublicTools.createSwitchCard(this, getString(R.string.set_turn_off_screen_if_stop), getString(R.string.set_turn_off_screen_if_stop_detail), AppData.setting.getTurnOffScreenIfStop(),
            isChecked -> {
              if (isChecked) {
                setActivity.setDisplay.removeViewAt(3);
                AppData.setting.setTurnOnScreenIfStop(false);
                setActivity.setDisplay.addView(PublicTools.createSwitchCardEx(this, getString(R.string.set_turn_on_screen_if_stop), getString(R.string.set_turn_on_screen_if_stop_detail), AppData.setting.getTurnOnScreenIfStop(),
                        (buttonView, isChecked1) -> {
                          if (AppData.setting.getTurnOffScreenIfStop()) {
                            buttonView.setChecked(false);
                            Toast.makeText(this, getString(R.string.set_turn_on_screen_if_stop_error), Toast.LENGTH_SHORT).show();
                          }
                          else AppData.setting.setTurnOnScreenIfStop(isChecked1);
                        }).getRoot(), 3);
              }
              AppData.setting.setTurnOffScreenIfStop(isChecked);
            }).getRoot());

    setActivity.setDisplay.addView(PublicTools.createSwitchCardEx(this, getString(R.string.set_turn_on_screen_if_stop), getString(R.string.set_turn_on_screen_if_stop_detail), AppData.setting.getTurnOnScreenIfStop(),
            (buttonView, isChecked) -> {
              if (AppData.setting.getTurnOffScreenIfStop()) {
                buttonView.setChecked(false);
                Toast.makeText(this, getString(R.string.set_turn_on_screen_if_stop_error), Toast.LENGTH_SHORT).show();
              }
              else AppData.setting.setTurnOnScreenIfStop(isChecked);
            }).getRoot());

    setActivity.setDisplay.addView(PublicTools.createSwitchCard(this, getString(R.string.set_display_keep_screen_awake), getString(R.string.set_display_keep_screen_awake_detail), AppData.setting.getKeepAwake(), isChecked -> AppData.setting.setKeepAwake(isChecked)).getRoot());
    setActivity.setDisplay.addView(PublicTools.createSwitchCard(this, getString(R.string.set_display_auto_back_on_start_default), getString(R.string.set_display_auto_back_on_start_default_detail), AppData.setting.getAutoBackOnStartDefault(), isChecked -> AppData.setting.setAutoBackOnStartDefault(isChecked)).getRoot());
    setActivity.setDisplay.addView(PublicTools.createSwitchCard(this, getString(R.string.set_display_default_mini_on_outside), getString(R.string.set_display_default_mini_on_outside_detail), AppData.setting.getDefaultMiniOnOutside(), isChecked -> AppData.setting.setDefaultMiniOnOutside(isChecked)).getRoot());
    setActivity.setDisplay.addView(PublicTools.createSwitchCard(this, getString(R.string.set_display_default_show_nav_bar), getString(R.string.set_display_default_show_nav_bar_detail), AppData.setting.getDefaultShowNavBar(), isChecked -> AppData.setting.setDefaultShowNavBar(isChecked)).getRoot());
    // 其他
    setActivity.setOther.addView(PublicTools.createSwitchCard(this, getString(R.string.set_if_start_default_usb), getString(R.string.set_if_start_default_usb_detail), AppData.setting.getNeedStartDefaultUsbDevice(), isChecked -> AppData.setting.setNeedStartDefaultUsbDevice(isChecked)).getRoot());

    String defaultDevice = AppData.setting.getDefaultDevice();
    if (!defaultDevice.isEmpty()) {
      defaultDevice = AppData.dbHelper.getByUUID(defaultDevice).address;
      setActivity.setOther.addView(PublicTools.createTextCardDetail(this, getString(R.string.set_other_clear_default), defaultDevice, () -> {
        AppData.setting.setDefaultDevice("");
        setActivity.setOther.removeViewAt(1);
        setActivity.setOther.addView(PublicTools.createTextCardDetail(this, getString(R.string.set_other_clear_default), getString(R.string.set_other_no_default), () -> {
          Toast.makeText(this, getString(R.string.set_other_no_default), Toast.LENGTH_SHORT).show();
        }).getRoot(), 1);
        Toast.makeText(this, getString(R.string.set_other_clear_default_code), Toast.LENGTH_SHORT).show();
      }).getRoot());
    }
    else {
      setActivity.setOther.addView(PublicTools.createTextCardDetail(this, getString(R.string.set_other_clear_default), getString(R.string.set_other_no_default), () -> {
        Toast.makeText(this, getString(R.string.set_other_no_default), Toast.LENGTH_SHORT).show();
      }).getRoot());
    }

    String defaultUsbDevice = AppData.setting.getDefaultUsbDevice();
    if (!defaultUsbDevice.isEmpty()) {
      defaultUsbDevice = AppData.dbHelper.getByUUID(defaultUsbDevice).uuid;
      setActivity.setOther.addView(PublicTools.createTextCardDetail(this, getString(R.string.set_other_clear_default_usb), defaultUsbDevice, () -> {
        AppData.setting.setDefaultUsbDevice("");
        setActivity.setOther.removeViewAt(2);
        setActivity.setOther.addView(PublicTools.createTextCardDetail(this, getString(R.string.set_other_clear_default_usb), getString(R.string.set_other_no_default), () -> {
          Toast.makeText(this, getString(R.string.set_other_no_default), Toast.LENGTH_SHORT).show();
        }).getRoot(), 2);
        Toast.makeText(this, getString(R.string.set_other_clear_default_usb_code), Toast.LENGTH_SHORT).show();
      }).getRoot());
    }
    else {
      setActivity.setOther.addView(PublicTools.createTextCardDetail(this, getString(R.string.set_other_clear_default_usb), getString(R.string.set_other_no_default), () -> {
        Toast.makeText(this, getString(R.string.set_other_no_default), Toast.LENGTH_SHORT).show();
      }).getRoot());
    }

    setActivity.setOther.addView(PublicTools.createTextCard(this, getString(R.string.set_about_ip), () -> startActivity(new Intent(this, IpActivity.class))).getRoot());
    setActivity.setOther.addView(PublicTools.createTextCard(this, getString(R.string.set_other_custom_key), () -> startActivity(new Intent(this, AdbKeyActivity.class))).getRoot());
    setActivity.setOther.addView(PublicTools.createTextCard(this, getString(R.string.set_other_clear_key), () -> {
      AppData.reGenerateAdbKeyPair(this);
      Toast.makeText(this, getString(R.string.set_other_clear_key_code), Toast.LENGTH_SHORT).show();
    }).getRoot());
    setActivity.setOther.addView(PublicTools.createTextCard(this, getString(R.string.set_other_locale), () -> {
      AppData.setting.setDefaultLocale(!AppData.setting.getDefaultLocale().equals("zh") ? "zh" : "en");
      Toast.makeText(this, getString(R.string.set_other_locale_code), Toast.LENGTH_SHORT).show();
    }).getRoot());
    // 关于
    setActivity.setAbout.addView(PublicTools.createTextCard(this, getString(R.string.set_about_website), () -> PublicTools.startUrl(this, "https://github.com/eiyooooo/Easycontrol_For_Car")).getRoot());
    setActivity.setAbout.addView(PublicTools.createTextCard(this, getString(R.string.set_about_how_to_use), () -> PublicTools.startUrl(this, "https://github.com/eiyooooo/Easycontrol_For_Car/blob/main/HOW_TO_USE.md")).getRoot());
    setActivity.setAbout.addView(PublicTools.createTextCard(this, getString(R.string.set_about_privacy), () -> PublicTools.startUrl(this, "https://github.com/eiyooooo/Easycontrol_For_Car/blob/main/PRIVACY.md")).getRoot());
    setActivity.setAbout.addView(PublicTools.createTextCard(this, getString(R.string.set_about_version) + BuildConfig.VERSION_NAME, () -> PublicTools.startUrl(this, "https://github.com/eiyooooo/Easycontrol_For_Car/releases")).getRoot());
    setActivity.setAbout.addView(PublicTools.createTextCard(this, getString(R.string.car_version_message), () -> PublicTools.startUrl(this, "https://github.com/eiyooooo/Easycontrol_For_Car")).getRoot());
  }

  // 设置按钮监听
  private void setButtonListener() {
    setActivity.backButton.setOnClickListener(v -> finish());
  }
}
