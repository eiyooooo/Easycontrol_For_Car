package top.eiyooooo.easycontrol.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import top.eiyooooo.easycontrol.app.entity.AppData;
import top.eiyooooo.easycontrol.app.helper.PublicTools;
import top.eiyooooo.easycontrol.app.databinding.ActivitySetBinding;
import top.eiyooooo.easycontrol.app.helper.ShortcutHelper;

public class SetActivity extends Activity {
  private ActivitySetBinding setActivity;

  private static final ArrayAdapter<String> audioChannelAdapter = new ArrayAdapter<>(AppData.main, R.layout.item_spinner_item, new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20"});

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
    setActivity.setDisplay.addView(PublicTools.createSwitchCard(this, getString(R.string.set_wake_up_screen_on_connect), getString(R.string.set_wake_up_screen_on_connect_detail), AppData.setting.getTurnOnScreenIfStart(),
            isChecked -> {
              if (!isChecked) {
                setActivity.setDisplay.removeViewAt(1);
                AppData.setting.setTurnOffScreenIfStart(false);
                setActivity.setDisplay.addView(PublicTools.createSwitchCardEx(this, getString(R.string.set_light_off_on_connect), getString(R.string.set_light_off_on_connect_detail), AppData.setting.getTurnOffScreenIfStart(),
                        (buttonView, isChecked1) -> {
                          if (!AppData.setting.getTurnOnScreenIfStart()) {
                            buttonView.setChecked(false);
                            Toast.makeText(this, getString(R.string.set_light_off_on_connect_error), Toast.LENGTH_SHORT).show();
                          }
                          else AppData.setting.setTurnOffScreenIfStart(isChecked1);
                        }).getRoot(), 1);
              }
              AppData.setting.setTurnOnScreenIfStart(isChecked);
            }).getRoot());

    setActivity.setDisplay.addView(PublicTools.createSwitchCardEx(this, getString(R.string.set_light_off_on_connect), getString(R.string.set_light_off_on_connect_detail), AppData.setting.getTurnOffScreenIfStart(),
            (buttonView, isChecked) -> {
              if (!AppData.setting.getTurnOnScreenIfStart()) {
                buttonView.setChecked(false);
                Toast.makeText(this, getString(R.string.set_light_off_on_connect_error), Toast.LENGTH_SHORT).show();
              }
              else AppData.setting.setTurnOffScreenIfStart(isChecked);
            }).getRoot());

    setActivity.setDisplay.addView(PublicTools.createSwitchCard(this, getString(R.string.set_lock_screen_on_close), getString(R.string.set_lock_screen_on_close_detail), AppData.setting.getTurnOffScreenIfStop(),
            isChecked -> {
              if (isChecked) {
                setActivity.setDisplay.removeViewAt(3);
                AppData.setting.setTurnOnScreenIfStop(false);
                setActivity.setDisplay.addView(PublicTools.createSwitchCardEx(this, getString(R.string.set_light_on_on_close), getString(R.string.set_light_on_on_close_detail), AppData.setting.getTurnOnScreenIfStop(),
                        (buttonView, isChecked1) -> {
                          if (AppData.setting.getTurnOffScreenIfStop()) {
                            buttonView.setChecked(false);
                            Toast.makeText(this, getString(R.string.set_light_on_on_close_error), Toast.LENGTH_SHORT).show();
                          }
                          else AppData.setting.setTurnOnScreenIfStop(isChecked1);
                        }).getRoot(), 3);
              }
              AppData.setting.setTurnOffScreenIfStop(isChecked);
            }).getRoot());

    setActivity.setDisplay.addView(PublicTools.createSwitchCardEx(this, getString(R.string.set_light_on_on_close), getString(R.string.set_light_on_on_close_detail), AppData.setting.getTurnOnScreenIfStop(),
            (buttonView, isChecked) -> {
              if (AppData.setting.getTurnOffScreenIfStop()) {
                buttonView.setChecked(false);
                Toast.makeText(this, getString(R.string.set_light_on_on_close_error), Toast.LENGTH_SHORT).show();
              }
              else AppData.setting.setTurnOnScreenIfStop(isChecked);
            }).getRoot());

    setActivity.setDisplay.addView(PublicTools.createSwitchCard(this, getString(R.string.set_display_keep_screen_awake), getString(R.string.set_display_keep_screen_awake_detail), AppData.setting.getKeepAwake(), isChecked -> AppData.setting.setKeepAwake(isChecked)).getRoot());
    setActivity.setDisplay.addView(PublicTools.createSwitchCard(this, getString(R.string.set_display_auto_back_on_start_default), getString(R.string.set_display_auto_back_on_start_default_detail), AppData.setting.getAutoBackOnStartDefault(), isChecked -> AppData.setting.setAutoBackOnStartDefault(isChecked)).getRoot());
    setActivity.setDisplay.addView(PublicTools.createSwitchCard(this, getString(R.string.set_display_default_mini_on_outside), getString(R.string.set_display_default_mini_on_outside_detail), AppData.setting.getDefaultMiniOnOutside(), isChecked -> AppData.setting.setDefaultMiniOnOutside(isChecked)).getRoot());
    setActivity.setDisplay.addView(PublicTools.createSwitchCard(this, getString(R.string.set_display_mini_recover_on_timeout), getString(R.string.set_display_mini_recover_on_timeout_detail), AppData.setting.getMiniRecoverOnTimeout(), isChecked -> AppData.setting.setMiniRecoverOnTimeout(isChecked)).getRoot());
    setActivity.setDisplay.addView(PublicTools.createSwitchCard(this, getString(R.string.set_display_full_to_mini_on_exit), getString(R.string.set_display_full_to_mini_on_exit_detail), AppData.setting.getFullToMiniOnExit(), isChecked -> AppData.setting.setFullToMiniOnExit(isChecked)).getRoot());
    setActivity.setDisplay.addView(PublicTools.createSwitchCard(this, getString(R.string.set_display_default_show_nav_bar), getString(R.string.set_display_default_show_nav_bar_detail), AppData.setting.getDefaultShowNavBar(), isChecked -> AppData.setting.setDefaultShowNavBar(isChecked)).getRoot());
    // 其他
    setActivity.setOther.addView(PublicTools.createSpinnerCard(this, getString(R.string.set_audio_channel), getString(R.string.set_audio_channel_detail), String.valueOf(AppData.setting.getAudioChannel()), audioChannelAdapter, str -> AppData.setting.setAudioChannel(Integer.parseInt(str))).getRoot());
    setActivity.setOther.addView(PublicTools.createSwitchCard(this, getString(R.string.set_always_full_mode), getString(R.string.set_always_full_mode_detail), AppData.setting.getAlwaysFullMode(), isChecked -> AppData.setting.setAlwaysFullMode(isChecked)).getRoot());
    setActivity.setOther.addView(PublicTools.createSwitchCard(this, getString(R.string.set_mirror_mode), getString(R.string.set_mirror_mode_detail), AppData.setting.getMirrorMode(), isChecked -> AppData.setting.setMirrorMode(isChecked)).getRoot());
    setActivity.setOther.addView(PublicTools.createSwitchCard(this, getString(R.string.set_force_desktop_mode), getString(R.string.set_force_desktop_mode_detail), AppData.setting.getForceDesktopMode(), isChecked -> AppData.setting.setForceDesktopMode(isChecked)).getRoot());
    setActivity.setOther.addView(PublicTools.createSwitchCard(this, getString(R.string.set_try_start_default_in_app_transfer), getString(R.string.set_try_start_default_in_app_transfer_detail), AppData.setting.getTryStartDefaultInAppTransfer(), isChecked -> AppData.setting.setTryStartDefaultInAppTransfer(isChecked)).getRoot());
    setActivity.setOther.addView(PublicTools.createTextCard(this, getString(R.string.set_create_startup_shortcut), () -> ShortcutHelper.addShortcut(AppData.main, StartDeviceActivity.class, getString(R.string.tip_default_device), R.drawable.phones, null)).getRoot());
    setActivity.setOther.addView(PublicTools.createTextCard(this, getString(R.string.set_other_log), () -> startActivity(new Intent(this, LogActivity.class))).getRoot());
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
    setActivity.setAbout.addView(PublicTools.createTextCard(this, getString(R.string.set_about_how_to_use), () -> PublicTools.openWebViewActivity(this, "file:///android_asset/usage.html")).getRoot());
    setActivity.setAbout.addView(PublicTools.createTextCard(this, getString(R.string.set_about_privacy), () -> PublicTools.startUrl(this, "https://github.com/eiyooooo/Easycontrol_For_Car/blob/main/PRIVACY.md")).getRoot());
    setActivity.setAbout.addView(PublicTools.createTextCard(this, getString(R.string.set_license), () -> PublicTools.openWebViewActivity(this, "file:///android_asset/license.html")).getRoot());
    setActivity.setAbout.addView(PublicTools.createTextCard(this, getString(R.string.set_about_version) + BuildConfig.VERSION_NAME, () -> PublicTools.startUrl(this, "https://github.com/eiyooooo/Easycontrol_For_Car/releases")).getRoot());
    setActivity.setAbout.addView(PublicTools.createTextCard(this, getString(R.string.car_version_message), () -> PublicTools.startUrl(this, "https://github.com/eiyooooo/Easycontrol_For_Car")).getRoot());
  }

  // 设置按钮监听
  private void setButtonListener() {
    setActivity.backButton.setOnClickListener(v -> finish());
  }
}
