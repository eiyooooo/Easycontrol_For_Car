package top.eiyooooo.easycontrol.app.entity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

public final class RemoteAppInfo {
  public String packageName;
  public String label;
  public byte[] iconBytes;
  private Bitmap icon;

  public Bitmap getIcon() {
    if (icon == null && iconBytes != null) {
      try {
        icon = BitmapFactory.decodeByteArray(iconBytes, 0, iconBytes.length);
      } catch (Exception ignored) {
      }
      iconBytes = null;
    }
    return icon;
  }

  public static RemoteAppInfo parse(String[] appInfo) {
    RemoteAppInfo info = new RemoteAppInfo();
    info.packageName = appInfo[0];
    info.label = appInfo[1] == null || appInfo[1].isEmpty() || "null".equals(appInfo[1]) ? null : appInfo[1];
    if (appInfo.length > 10 && appInfo[10] != null && !appInfo[10].isEmpty() && !"null".equals(appInfo[10])) {
      try {
        info.iconBytes = Base64.decode(appInfo[10], Base64.DEFAULT);
      } catch (Exception ignored) {
      }
    }
    return info;
  }
}
