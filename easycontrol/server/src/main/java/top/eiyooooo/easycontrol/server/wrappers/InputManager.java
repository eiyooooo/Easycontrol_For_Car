package top.eiyooooo.easycontrol.server.wrappers;

import android.view.InputEvent;
import top.eiyooooo.easycontrol.server.utils.L;

import java.lang.reflect.Method;
import java.util.Objects;

public final class InputManager {
    public static final int INJECT_INPUT_EVENT_MODE_ASYNC = 0;

    private static Object manager;
    private static Class<?> CLASS;
    private static Method injectInputEventMethod = null;
    private static Method setDisplayIdMethod = null;

    public static void init(Object m) {
        manager = m;
        if (manager == null) {
            L.e("Error in InputManager.init: manager is null");
            return;
        }
        CLASS = manager.getClass();
    }

    private static Method getInjectInputEventMethod() throws ReflectiveOperationException {
        if (injectInputEventMethod == null) {
            if (CLASS == null) {
                L.e("Error in getInjectInputEventMethod: CLASS is null");
                return null;
            }
            injectInputEventMethod = CLASS.getMethod("injectInputEvent", InputEvent.class, int.class);
        }
        return injectInputEventMethod;
    }

    private static Method getSetDisplayIdMethod() throws ReflectiveOperationException {
        if (setDisplayIdMethod == null) {
            if (CLASS == null) {
                L.e("Error in getSetDisplayIdMethod: CLASS is null");
                return null;
            }
            setDisplayIdMethod = InputEvent.class.getMethod("setDisplayId", int.class);
        }
        return setDisplayIdMethod;
    }

    public static void setDisplayId(InputEvent inputEvent, int displayId) throws Exception {
        Objects.requireNonNull(getSetDisplayIdMethod()).invoke(inputEvent, displayId);
    }

    public static void injectInputEvent(InputEvent inputEvent, int mode) throws Exception {
        Objects.requireNonNull(getInjectInputEventMethod()).invoke(manager, inputEvent, mode);
    }
}
