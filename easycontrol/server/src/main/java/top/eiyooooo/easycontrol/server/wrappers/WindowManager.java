package top.eiyooooo.easycontrol.server.wrappers;

import android.os.IInterface;
import android.view.IRotationWatcher;
import top.eiyooooo.easycontrol.server.utils.L;

import java.lang.reflect.Method;
import java.util.Objects;

public final class WindowManager {
    private static IInterface manager;
    private static Class<?> CLASS;
    private static Method freezeDisplayRotationMethod = null;
    private static Method freezeRotationMethod = null;
    private static Method isDisplayRotationFrozenMethod = null;
    private static Method isRotationFrozenMethod = null;
    private static Method thawDisplayRotationMethod = null;
    private static Method thawRotationMethod = null;
    private static Method getRotationMethod = null;
    private static Method watchRotationExMethod = null;
    private static Method watchRotationMethod = null;
    private static Method removeRotationWatcherMethod = null;
    private static IRotationWatcher rotationWatcher_saved;

    public static void init(IInterface m) {
        manager = m;
        if (manager == null) {
            L.e("Error in WindowManager.init: manager is null");
            return;
        }
        CLASS = manager.getClass();
    }

    private static Method getFreezeRotationMethod() throws ReflectiveOperationException {
        if (freezeRotationMethod == null) {
            if (CLASS == null) {
                L.e("Error in getFreezeRotationMethod: CLASS is null");
                return null;
            }
            freezeRotationMethod = CLASS.getMethod("freezeRotation", int.class);
        }
        return freezeRotationMethod;
    }

    private static Method getFreezeDisplayRotationMethod() throws ReflectiveOperationException {
        if (freezeDisplayRotationMethod == null) {
            if (CLASS == null) {
                L.e("Error in getFreezeDisplayRotationMethod: CLASS is null");
                return null;
            }
            freezeDisplayRotationMethod = CLASS.getMethod("freezeDisplayRotation", int.class, int.class);
        }
        return freezeDisplayRotationMethod;
    }

    private static Method getIsDisplayRotationFrozenMethod() throws ReflectiveOperationException {
        if (isDisplayRotationFrozenMethod == null) {
            if (CLASS == null) {
                L.e("Error in getIsDisplayRotationFrozenMethod: CLASS is null");
                return null;
            }
            isDisplayRotationFrozenMethod = CLASS.getMethod("isDisplayRotationFrozen", int.class);
        }
        return isDisplayRotationFrozenMethod;
    }

    private static Method getIsRotationFrozenMethod() throws ReflectiveOperationException {
        if (isRotationFrozenMethod == null) {
            if (CLASS == null) {
                L.e("Error in getIsRotationFrozenMethod: CLASS is null");
                return null;
            }
            isRotationFrozenMethod = CLASS.getMethod("isRotationFrozen");
        }
        return isRotationFrozenMethod;
    }

    private static Method getThawDisplayRotationMethod() throws ReflectiveOperationException {
        if (thawDisplayRotationMethod == null) {
            if (CLASS == null) {
                L.e("Error in getThawDisplayRotationMethod: CLASS is null");
                return null;
            }
            thawDisplayRotationMethod = CLASS.getMethod("thawDisplayRotation", int.class);
        }
        return thawDisplayRotationMethod;
    }

    private static Method getRotationMethod() throws ReflectiveOperationException {
        if (getRotationMethod == null) {
            if (CLASS == null) {
                L.e("Error in getRotationMethod: CLASS is null");
                return null;
            }
            try {
                getRotationMethod = CLASS.getMethod("getDefaultDisplayRotation");
            } catch (Exception ignored) {
                getRotationMethod = CLASS.getMethod("getRotation");
            }
        }
        return getRotationMethod;
    }

    private static Method getThawRotationMethod() throws ReflectiveOperationException {
        if (thawRotationMethod == null) {
            if (CLASS == null) {
                L.e("Error in getThawRotationMethod: CLASS is null");
                return null;
            }
            thawRotationMethod = CLASS.getMethod("thawRotation");
        }
        return thawRotationMethod;
    }

    private static Method getWatchRotationExMethod() throws ReflectiveOperationException {
        if (watchRotationExMethod == null) {
            if (CLASS == null) {
                L.e("Error in getWatchRotationExMethod: CLASS is null");
                return null;
            }
            watchRotationExMethod = CLASS.getMethod("watchRotation", IRotationWatcher.class, int.class);
        }
        return watchRotationExMethod;
    }

    private static Method getWatchRotationMethod() throws ReflectiveOperationException {
        if (watchRotationMethod == null) {
            if (CLASS == null) {
                L.e("Error in getWatchRotationMethod: CLASS is null");
                return null;
            }
            watchRotationMethod = CLASS.getMethod("watchRotation", IRotationWatcher.class);
        }
        return watchRotationMethod;
    }

    private static Method getRemoveRotationWatcherMethod() throws ReflectiveOperationException {
        if (removeRotationWatcherMethod == null) {
            if (CLASS == null) {
                L.e("Error in getRemoveRotationWatcherMethod: CLASS is null");
                return null;
            }
            removeRotationWatcherMethod = CLASS.getMethod("removeRotationWatcher", IRotationWatcher.class);
            removeRotationWatcherMethod.setAccessible(true);
        }
        return removeRotationWatcherMethod;
    }

    public static void freezeRotation(int displayId, int rotation) {
        try {
            Objects.requireNonNull(getFreezeDisplayRotationMethod()).invoke(manager, displayId, rotation);
            return;
        } catch (Exception e) {
            L.w("freezeDisplayRotation error, try freezeRotation", e);
        }
        try {
            if (displayId != 0) throw new Exception("displayId != 0, but freezeRotation is not supported");
            Objects.requireNonNull(getFreezeRotationMethod()).invoke(manager, rotation);
        } catch (Exception e) {
            L.e("freezeRotation error", e);
        }
    }

    public static boolean isRotationFrozen(int displayId) {
        try {
            return (boolean) Objects.requireNonNull(getIsDisplayRotationFrozenMethod()).invoke(manager, displayId);
        } catch (Exception e) {
            L.w("isDisplayRotationFrozen error, try isRotationFrozen", e);
        }
        try {
            if (displayId != 0) throw new Exception("displayId != 0, but isRotationFrozen is not supported");
            return (boolean) Objects.requireNonNull(getIsRotationFrozenMethod()).invoke(manager);
        } catch (Exception e) {
            L.e("isRotationFrozen error", e);
            return false;
        }
    }

    public static void thawRotation(int displayId) {
        try {
            Objects.requireNonNull(getThawDisplayRotationMethod()).invoke(manager, displayId);
            return;
        } catch (Exception e) {
            L.w("thawDisplayRotation error, try thawRotation", e);
        }
        try {
            if (displayId != 0) throw new Exception("displayId != 0, but thawRotation is not supported");
            Objects.requireNonNull(getThawRotationMethod()).invoke(manager);
        } catch (Exception e) {
            L.e("thawRotation error", e);
        }
    }

    public static int getRotation() {
        try {
            return (int) Objects.requireNonNull(getRotationMethod()).invoke(manager);
        } catch (Exception e) {
            L.e("getRotation error", e);
            return -1;
        }
    }

    public static void registerRotationWatcher(IRotationWatcher rotationWatcher, int displayId) {
        try {
            try {
                Objects.requireNonNull(getWatchRotationExMethod()).invoke(manager, rotationWatcher, displayId);
            } catch (Exception e) {
                if (displayId != 0) throw e;
                Objects.requireNonNull(getWatchRotationMethod()).invoke(manager, rotationWatcher);
            }
            rotationWatcher_saved = rotationWatcher;
        } catch (Exception e) {
            L.e("registerRotationWatcher error", e);
        }
    }

    public static void removeRotationWatcher() {
        if (rotationWatcher_saved == null) return;
        try {
            Objects.requireNonNull(getRemoveRotationWatcherMethod()).invoke(manager, rotationWatcher_saved);
        } catch (Exception e) {
            L.e("removeRotationWatcher error", e);
        }
    }
}