package top.eiyooooo.easycontrol.app.entity;

import android.content.SharedPreferences;

public final class Setting {
  private final SharedPreferences sharedPreferences;

  private final SharedPreferences.Editor editor;

  public String getDefaultLocale() {
    return sharedPreferences.getString("defaultLocale", "");
  }

  public void setDefaultLocale(String value) {
    editor.putString("defaultLocale", value);
    editor.apply();
  }

  public boolean getDefaultIsAudio() {
    return sharedPreferences.getBoolean("defaultIsAudio", true);
  }

  public void setDefaultIsAudio(boolean value) {
    editor.putBoolean("defaultIsAudio", value);
    editor.apply();
  }

  public int getDefaultMaxSize() {
    return sharedPreferences.getInt("defaultMaxSize", 1600);
  }

  public void setDefaultMaxSize(int value) {
    editor.putInt("defaultMaxSize", value);
    editor.apply();
  }

  public int getDefaultMaxFps() {
    return sharedPreferences.getInt("defaultMaxFps", 60);
  }

  public void setDefaultMaxFps(int value) {
    editor.putInt("defaultMaxFps", value);
    editor.apply();
  }

  public int getDefaultMaxVideoBit() {
    return sharedPreferences.getInt("defaultMaxVideoBit", 4);
  }

  public void setDefaultMaxVideoBit(int value) {
    editor.putInt("defaultMaxVideoBit", value);
    editor.apply();
  }

  public boolean getDefaultSetResolution() {
    return sharedPreferences.getBoolean("defaultSetResolution", false);
  }

  public void setDefaultSetResolution(boolean value) {
    editor.putBoolean("defaultSetResolution", value);
    editor.apply();
  }

  public boolean getDefaultUseH265() {
    return sharedPreferences.getBoolean("defaultUseH265", true);
  }

  public void setDefaultUseH265(boolean value) {
    editor.putBoolean("defaultUseH265", value);
    editor.apply();
  }

  public boolean getDefaultUseOpus() {
    return sharedPreferences.getBoolean("defaultUseOpus", true);
  }

  public void setDefaultUseOpus(boolean value) {
    editor.putBoolean("defaultUseOpus", value);
    editor.apply();
  }

  public boolean getDefaultFull() {
    return sharedPreferences.getBoolean("defaultFull", false);
  }

  public void setDefaultFull(boolean value) {
    editor.putBoolean("defaultFull", value);
    editor.apply();
  }

  public boolean getAutoBackOnStartDefault() {
    return sharedPreferences.getBoolean("autoBackOnStartDefault", false);
  }

  public void setAutoBackOnStartDefault(boolean value) {
    editor.putBoolean("autoBackOnStartDefault", value);
    editor.apply();
  }

  public boolean getTurnOnScreenIfStart() {
    return sharedPreferences.getBoolean("TurnOnScreenIfStart", true);
  }

  public void setTurnOnScreenIfStart(boolean value) {
    editor.putBoolean("TurnOnScreenIfStart", value);
    editor.apply();
  }

  public boolean getTurnOffScreenIfStart() {
    return sharedPreferences.getBoolean("TurnOffScreenIfStart", false);
  }

  public void setTurnOffScreenIfStart(boolean value) {
    editor.putBoolean("TurnOffScreenIfStart", value);
    editor.apply();
  }

  public boolean getTurnOffScreenIfStop() {
    return sharedPreferences.getBoolean("TurnOffScreenIfStop", false);
  }

  public void setTurnOffScreenIfStop(boolean value) {
    editor.putBoolean("TurnOffScreenIfStop", value);
    editor.apply();
  }

  public boolean getTurnOnScreenIfStop() {
    return sharedPreferences.getBoolean("TurnOnScreenIfStop", true);
  }

  public void setTurnOnScreenIfStop(boolean value) {
    editor.putBoolean("TurnOnScreenIfStop", value);
    editor.apply();
  }

  public boolean getKeepAwake() {
    return sharedPreferences.getBoolean("keepAwake", true);
  }

  public void setKeepAwake(boolean value) {
    editor.putBoolean("keepAwake", value);
    editor.apply();
  }

  public boolean getDefaultShowNavBar() {
    return sharedPreferences.getBoolean("defaultShowNavBar", true);
  }

  public void setDefaultShowNavBar(boolean value) {
    editor.putBoolean("defaultShowNavBar", value);
    editor.apply();
  }

  public boolean getDefaultMiniOnOutside() {
    return sharedPreferences.getBoolean("defaultMiniOnOutside", false);
  }

  public void setDefaultMiniOnOutside(boolean value) {
    editor.putBoolean("defaultMiniOnOutside", value);
    editor.apply();
  }

  public boolean getMiniRecoverOnTimeout() {
    return sharedPreferences.getBoolean("miniRecoverOnTimeout", false);
  }

  public void setMiniRecoverOnTimeout(boolean value) {
    editor.putBoolean("miniRecoverOnTimeout", value);
    editor.apply();
  }

  public boolean getFullToMiniOnExit() {
    return sharedPreferences.getBoolean("fullToMiniOnExit", true);
  }

  public void setFullToMiniOnExit(boolean value) {
    editor.putBoolean("fullToMiniOnExit", value);
    editor.apply();
  }

  public String getDefaultDevice() {
    return sharedPreferences.getString("defaultDevice", "");
  }

  public String getDefaultUsbDevice() {
    return sharedPreferences.getString("defaultUsbDevice", "");
  }

  public boolean getNeedStartDefaultUsbDevice() {
    return sharedPreferences.getBoolean("NeedStartDefaultUsbDevice", false);
  }

  public void setDefaultDevice(String value) {
    editor.putString("defaultDevice", value);
    editor.apply();
  }

  public void setDefaultUsbDevice(String value) {
    editor.putString("defaultUsbDevice", value);
    editor.apply();
  }

  public void setNeedStartDefaultUsbDevice(boolean value) {
    editor.putBoolean("NeedStartDefaultUsbDevice", value);
    editor.apply();
  }

  public boolean getForceDesktopMode() {
    return sharedPreferences.getBoolean("ForceDesktopMode", false);
  }

  public void setForceDesktopMode(boolean value) {
    editor.putBoolean("ForceDesktopMode", value);
    editor.apply();
  }

  public boolean getTryStartDefaultInAppTransfer() {
    return sharedPreferences.getBoolean("tryStartDefaultInAppTransfer", false);
  }

  public void setTryStartDefaultInAppTransfer(boolean value) {
    editor.putBoolean("tryStartDefaultInAppTransfer", value);
    editor.apply();
  }

  public Setting(SharedPreferences sharedPreferences) {
    this.sharedPreferences = sharedPreferences;
    this.editor = sharedPreferences.edit();
  }
}
