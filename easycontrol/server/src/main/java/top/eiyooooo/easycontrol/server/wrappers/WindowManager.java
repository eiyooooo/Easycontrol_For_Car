package top.eiyooooo.easycontrol.server.wrappers;

import android.os.IInterface;
import android.view.IRotationWatcher;
import top.eiyooooo.easycontrol.server.utils.L;

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
        } catch (Exception e) {
            L.e("WindowManager init error", e);
        }
    }

    public static void freezeRotation(int displayId, int rotation) {
        if (freezeDisplayRotationMethod != null) {
            try {
                freezeDisplayRotationMethod.invoke(manager, displayId, rotation);
            } catch (Exception e) {
                L.e("freezeRotation error", e);
            }
        } else {
            if (freezeRotationMethod == null) return;
            try {
                freezeRotationMethod.invoke(manager, rotation);
            } catch (Exception e) {
                L.e("freezeRotation error", e);
            }
        }
    }

    public static boolean isRotationFrozen(int displayId) {
        if (isDisplayRotationFrozenMethod != null) {
            try {
                return (boolean) isDisplayRotationFrozenMethod.invoke(manager, displayId);
            } catch (Exception e) {
                L.e("isRotationFrozen error", e);
                return false;
            }
        } else {
            if (isRotationFrozenMethod == null) return false;
            try {
                return (boolean) isRotationFrozenMethod.invoke(manager);
            } catch (Exception e) {
                L.e("isRotationFrozen error", e);
                return false;
            }
        }
    }

    public static void thawRotation(int displayId) {
        if (thawDisplayRotationMethod != null) {
            try {
                thawDisplayRotationMethod.invoke(manager, displayId);
            } catch (Exception e) {
                L.e("thawRotation error", e);
            }
        } else {
            if (thawRotationMethod == null) return;
            try {
                thawRotationMethod.invoke(manager);
            } catch (Exception e) {
                L.e("thawRotation error", e);
            }
        }
    }

    private static IRotationWatcher rotationWatcher_saved;
    private static Method removeRotationWatcherMethod = null;

    public static void registerRotationWatcher(IRotationWatcher rotationWatcher, int displayId) {
        try {
            try {
                CLASS.getMethod("watchRotation", IRotationWatcher.class, int.class).invoke(manager, rotationWatcher, displayId);
            } catch (NoSuchMethodException e) {
                CLASS.getMethod("watchRotation", IRotationWatcher.class).invoke(manager, rotationWatcher);
            }
            rotationWatcher_saved = rotationWatcher;
        } catch (Exception e) {
            L.e("registerRotationWatcher error", e);
        }
    }

    public static void removeRotationWatcher() {
        if (rotationWatcher_saved == null) return;
        try {
            if (removeRotationWatcherMethod == null) {
                removeRotationWatcherMethod = CLASS.getMethod("removeRotationWatcher", IRotationWatcher.class);
                removeRotationWatcherMethod.setAccessible(true);
            }
            removeRotationWatcherMethod.invoke(manager, rotationWatcher_saved);
        } catch (Exception e) {
            L.e("removeRotationWatcher error", e);
        }
    }

}
