package top.eiyooooo.easycontrol.server.wrappers;

import android.annotation.TargetApi;
import android.os.IInterface;
import android.view.IDisplayFoldListener;
import android.view.IRotationWatcher;
import top.eiyooooo.easycontrol.server.utils.L;

import java.lang.reflect.Method;
import java.util.Objects;

public final class WindowManager {
    private static IInterface manager;
    private static Class<?> CLASS;
    private static Method freezeDisplayRotationMethod = null;
    private static Method isDisplayRotationFrozenMethod = null;
    private static Method thawDisplayRotationMethod = null;
    private static int freezeDisplayRotationMethodVersion;
    private static int isDisplayRotationFrozenMethodVersion;
    private static int thawDisplayRotationMethodVersion;
    private static Method getRotationMethod = null;
    private static Method watchRotationExMethod = null;
    private static Method watchRotationMethod = null;
    private static Method removeRotationWatcherMethod = null;
    private static Method registerDisplayFoldListenerMethod = null;
    private static IRotationWatcher rotationWatcher_saved;

    public static void init(IInterface m) {
        manager = m;
        if (manager == null) {
            L.e("Error in WindowManager.init: manager is null");
            return;
        }
        CLASS = manager.getClass();
    }

    private static Method getFreezeDisplayRotationMethod() throws ReflectiveOperationException {
        if (freezeDisplayRotationMethod == null) {
            try {
                freezeDisplayRotationMethod = manager.getClass().getMethod("freezeDisplayRotation", int.class, int.class, String.class);
                freezeDisplayRotationMethodVersion = 0;
            } catch (ReflectiveOperationException e) {
                try {
                    freezeDisplayRotationMethod = manager.getClass().getMethod("freezeDisplayRotation", int.class, int.class);
                    freezeDisplayRotationMethodVersion = 1;
                } catch (ReflectiveOperationException e1) {
                    freezeDisplayRotationMethod = manager.getClass().getMethod("freezeRotation", int.class);
                    freezeDisplayRotationMethodVersion = 2;
                }
            }
        }
        return freezeDisplayRotationMethod;
    }

    private static Method getIsDisplayRotationFrozenMethod() throws ReflectiveOperationException {
        if (isDisplayRotationFrozenMethod == null) {
            try {
                isDisplayRotationFrozenMethod = manager.getClass().getMethod("isDisplayRotationFrozen", int.class);
                isDisplayRotationFrozenMethodVersion = 0;
            } catch (ReflectiveOperationException e) {
                isDisplayRotationFrozenMethod = manager.getClass().getMethod("isRotationFrozen");
                isDisplayRotationFrozenMethodVersion = 1;
            }
        }
        return isDisplayRotationFrozenMethod;
    }

    private static Method getThawDisplayRotationMethod() throws ReflectiveOperationException {
        if (thawDisplayRotationMethod == null) {
            try {
                thawDisplayRotationMethod = manager.getClass().getMethod("thawDisplayRotation", int.class, String.class);
                thawDisplayRotationMethodVersion = 0;
            } catch (ReflectiveOperationException e) {
                try {
                    thawDisplayRotationMethod = manager.getClass().getMethod("thawDisplayRotation", int.class);
                    thawDisplayRotationMethodVersion = 1;
                } catch (ReflectiveOperationException e1) {
                    thawDisplayRotationMethod = manager.getClass().getMethod("thawRotation");
                    thawDisplayRotationMethodVersion = 2;
                }
            }
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

    private static Method getRegisterDisplayFoldListenerMethod() throws ReflectiveOperationException {
        if (registerDisplayFoldListenerMethod == null) {
            if (CLASS == null) {
                L.e("Error in getRegisterDisplayFoldListenerMethod: CLASS is null");
                return null;
            }
            registerDisplayFoldListenerMethod = CLASS.getMethod("registerDisplayFoldListener", IDisplayFoldListener.class);
        }
        return registerDisplayFoldListenerMethod;
    }

    public static void freezeRotation(int displayId, int rotation) {
        try {
            Method method = getFreezeDisplayRotationMethod();
            switch (freezeDisplayRotationMethodVersion) {
                case 0:
                    method.invoke(manager, displayId, rotation, "scrcpy#freezeRotation");
                    break;
                case 1:
                    method.invoke(manager, displayId, rotation);
                    break;
                default:
                    if (displayId != 0) {
                        L.e("Secondary display rotation not supported on this device");
                        return;
                    }
                    method.invoke(manager, rotation);
                    break;
            }
        } catch (Exception e) {
            L.e("Could not invoke method", e);
        }
    }

    public static boolean isRotationFrozen(int displayId) {
        try {
            Method method = getIsDisplayRotationFrozenMethod();
            switch (isDisplayRotationFrozenMethodVersion) {
                case 0:
                    return (boolean) method.invoke(manager, displayId);
                default:
                    if (displayId != 0) {
                        L.e("Secondary display rotation not supported on this device");
                        return false;
                    }
                    return (boolean) method.invoke(manager);
            }
        } catch (Exception e) {
            L.e("Could not invoke method", e);
            return false;
        }
    }

    public static void thawRotation(int displayId) {
        try {
            Method method = getThawDisplayRotationMethod();
            switch (thawDisplayRotationMethodVersion) {
                case 0:
                    method.invoke(manager, displayId, "scrcpy#thawRotation");
                    break;
                case 1:
                    method.invoke(manager, displayId);
                    break;
                default:
                    if (displayId != 0) {
                        L.e("Secondary display rotation not supported on this device");
                        return;
                    }
                    method.invoke(manager);
                    break;
            }
        } catch (Exception e) {
            L.e("Could not invoke method", e);
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
            L.e("registerRotationWatcher error, retrying", e);
            new Thread(() -> {
                try {
                    Thread.sleep(500);
                    registerRotationWatcher(rotationWatcher, displayId);
                } catch (InterruptedException ignored) {
                }
            }).start();
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

    @TargetApi(29)
    public static void registerDisplayFoldListener(IDisplayFoldListener displayFoldListener) {
        try {
            Objects.requireNonNull(getRegisterDisplayFoldListenerMethod()).invoke(manager, displayFoldListener);
        } catch (Exception e) {
            L.e("Could not register display fold listener", e);
        }
    }
}