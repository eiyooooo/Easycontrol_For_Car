/*
 * 本项目大量借鉴学习了开源投屏软件：Scrcpy，在此对该项目表示感谢
 */
package top.eiyooooo.easycontrol.server.wrappers;

import android.os.IInterface;
import android.view.IRotationWatcher;

import java.lang.reflect.Method;

public final class WindowManager {
  private static IInterface manager;
  private static Class<?> CLASS;
  private static Method freezeRotationMethod = null;
  private static Method freezeDisplayRotationMethod = null;
  private static Method isRotationFrozenMethod = null;
  private static Method isDisplayRotationFrozenMethod = null;
  private static Method thawRotationMethod = null;
  private static Method thawDisplayRotationMethod = null;

  public static void init(IInterface m) {
    manager = m;
    CLASS = manager.getClass();
    try {
      try {
        freezeDisplayRotationMethod = manager.getClass().getMethod("freezeDisplayRotation", int.class, int.class);
      } catch (Exception ignored) {
        freezeRotationMethod = manager.getClass().getMethod("freezeRotation", int.class);
      }
      try {
        isDisplayRotationFrozenMethod = manager.getClass().getMethod("isDisplayRotationFrozen", int.class);
      } catch (Exception ignored) {
        isRotationFrozenMethod = manager.getClass().getMethod("isRotationFrozen");
      }
      try {
        thawDisplayRotationMethod = manager.getClass().getMethod("thawDisplayRotation", int.class);
      } catch (Exception ignored) {
        thawRotationMethod = manager.getClass().getMethod("thawRotation");
      }
    } catch (Exception ignored) {}
  }

  public static void freezeRotation(int displayId, int rotation) {
    if (freezeDisplayRotationMethod != null) {
      try {
        freezeDisplayRotationMethod.invoke(manager, displayId, rotation);
      } catch (Exception ignored) {}
    } else {
      if (freezeRotationMethod == null) return;
      try {
        freezeRotationMethod.invoke(manager, rotation);
      } catch (Exception ignored) {}
    }
  }

  public static boolean isRotationFrozen(int displayId) {
    if (isDisplayRotationFrozenMethod != null) {
      try {
        return (boolean) isDisplayRotationFrozenMethod.invoke(manager, displayId);
      } catch (Exception ignored) {
        return false;
      }
    } else {
      if (isRotationFrozenMethod == null) return false;
      try {
        return (boolean) isRotationFrozenMethod.invoke(manager);
      } catch (Exception ignored) {
        return false;
      }
    }
  }

  public static void thawRotation(int displayId) {
    if (thawDisplayRotationMethod != null) {
      try {
        thawDisplayRotationMethod.invoke(manager, displayId);
      } catch (Exception ignored) {}
    } else {
      if (thawRotationMethod == null) return;
      try {
        thawRotationMethod.invoke(manager);
      } catch (Exception ignored) {}
    }
  }

  public static void registerRotationWatcher(IRotationWatcher rotationWatcher, int displayId) {
    try {
      try {
        CLASS.getMethod("watchRotation", IRotationWatcher.class, int.class).invoke(manager, rotationWatcher, displayId);
      } catch (NoSuchMethodException e) {
        CLASS.getMethod("watchRotation", IRotationWatcher.class).invoke(manager, rotationWatcher);
      }
    } catch (Exception ignored) {
    }
  }

}
