package top.eiyooooo.easycontrol.server.wrappers;

import android.os.IInterface;

import java.lang.reflect.Method;
import java.util.Objects;

import top.eiyooooo.easycontrol.server.utils.L;

public class UiModeManager {
    private static IInterface manager;
    private static Class<?> CLASS;
    private static Method getGetNightModeMethod = null;
    private static Method getSetNightModeMethod = null;

    public static void init(IInterface m) {
        manager = m;
        if (manager == null) {
            L.e("Error in IPackageManager.init: manager is null");
            return;
        }
        CLASS = manager.getClass();
    }

    private static Method getGetNightModeMethod() throws ReflectiveOperationException {
        if (getGetNightModeMethod == null) {
            if (CLASS == null) {
                L.e("Error in getGetNightModeMethod: CLASS is null");
                return null;
            }
            getGetNightModeMethod = CLASS.getDeclaredMethod("getNightMode");
        }
        return getGetNightModeMethod;
    }

    private static Method getSetNightModeMethod() throws ReflectiveOperationException {
        if (getSetNightModeMethod == null) {
            if (CLASS == null) {
                L.e("Error in getGetNightModeMethod: CLASS is null");
                return null;
            }
            getSetNightModeMethod = CLASS.getDeclaredMethod("setNightMode", int.class);
        }
        return getSetNightModeMethod;
    }

    private static int currentNightMode = -1;

    public static int getNightMode() {
        try {
            currentNightMode = (int) Objects.requireNonNull(getGetNightModeMethod()).invoke(manager);
            return currentNightMode;
        } catch (ReflectiveOperationException e) {
            L.e("Error in getNightMode: " + e.getMessage());
            return -1;
        }
    }

    public static void setNightMode(int mode) {
        try {
            if (currentNightMode == mode) return;
            Objects.requireNonNull(getSetNightModeMethod()).invoke(manager, mode);
            currentNightMode = mode;
        } catch (ReflectiveOperationException e) {
            L.e("Error in setNightMode: " + e.getMessage());
        }
    }
}