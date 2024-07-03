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

  public boolean getDefaultClipboardSync() {
    return sharedPreferences.getBoolean("defaultClipboardSync", false);
  }

  public void setDefaultClipboardSync(boolean value) {
    editor.putBoolean("defaultClipboardSync", value);
    editor.apply();
  }

  public boolean getDefaultNightModeSync() {
    return sharedPreferences.getBoolean("defaultNightModeSync", false);
  }

  public void setDefaultNightModeSync(boolean value) {
    editor.putBoolean("defaultNightModeSync", value);
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

  public boolean getFillFull() {
    return sharedPreferences.getBoolean("fillFull", false);
  }

  public void setFillFull(boolean value) {
    editor.putBoolean("fillFull", value);
    editor.apply();
  }

  public boolean getNewMirrorMode() {
    return sharedPreferences.getBoolean("newMirrorMode", true);
  }

  public void setNewMirrorMode(boolean value) {
    editor.putBoolean("newMirrorMode", value);
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

  public boolean getShowReconnect() {
    return sharedPreferences.getBoolean("showReconnect", true);
  }

  public void setShowReconnect(boolean value) {
    editor.putBoolean("showReconnect", value);
    editor.apply();
  }

  public boolean getShowConnectUSB() {
    return sharedPreferences.getBoolean("showConnectUSB", true);
  }

  public void setShowConnectUSB(boolean value) {
    editor.putBoolean("showConnectUSB", value);
    editor.apply();
  }

  public String getCountdownTime() {
    return sharedPreferences.getString("countdownTime", "5");
  }

  public void setCountdownTime(String value) {
    editor.putString("countdownTime", value);
    editor.apply();
  }

  public boolean getAlwaysFullMode() {
    return sharedPreferences.getBoolean("alwaysFullMode", false);
  }

  public void setAlwaysFullMode(boolean value) {
    editor.putBoolean("alwaysFullMode", value);
    editor.apply();
  }

  public boolean getShowUsage() {
    return sharedPreferences.getBoolean("showUsage", false);
  }

  public void setShowUsage(boolean value) {
    editor.putBoolean("showUsage", value);
    editor.apply();
  }

  public boolean getEnableUSB() {
    return sharedPreferences.getBoolean("enableUSB", true);
  }

  public void setEnableUSB(boolean value) {
    editor.putBoolean("enableUSB", value);
    editor.apply();
    if (value) AppData.myBroadcastReceiver.checkConnectedUsb(AppData.main);
  }

  public boolean getSetFullScreen() {
    return sharedPreferences.getBoolean("setFullScreen", true);
  }

  public void setSetFullScreen(boolean value) {
    editor.putBoolean("setFullScreen", value);
    editor.apply();
  }

  public int getAudioChannel() {
    return sharedPreferences.getInt("audioChannel", 0);
  }

  public void setAudioChannel(int value) {
    editor.putInt("audioChannel", value);
    editor.apply();
  }

  public boolean getMonitorState() {
    return sharedPreferences.getBoolean("monitorState", false);
  }

  public void setMonitorState(boolean value) {
    editor.putBoolean("monitorState", value);
    editor.apply();
  }

  public int getMonitorLatency() {
    return sharedPreferences.getInt("monitorLatency", 1500);
  }

  public void setMonitorLatency(int value) {
    editor.putInt("monitorLatency", value);
    editor.apply();
  }

  public Setting(SharedPreferences sharedPreferences) {
    this.sharedPreferences = sharedPreferences;
    this.editor = sharedPreferences.edit();
  }
}
