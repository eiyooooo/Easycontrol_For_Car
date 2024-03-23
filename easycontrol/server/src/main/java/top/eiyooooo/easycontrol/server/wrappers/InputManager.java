package top.eiyooooo.easycontrol.server.wrappers;

import android.view.InputEvent;
import top.eiyooooo.easycontrol.server.utils.L;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class InputManager {

    public static final int INJECT_INPUT_EVENT_MODE_ASYNC = 0;

    private static Object manager;
    private static Method injectInputEventMethod;
    private static Method setDisplayIdMethod;

    public static void init(Object m) throws NoSuchMethodException {
        manager = m;
        injectInputEventMethod = manager.getClass().getMethod("injectInputEvent", InputEvent.class, int.class);
        try {
            setDisplayIdMethod = InputEvent.class.getMethod("setDisplayId", int.class);
        } catch (Exception e) {
            L.e("InputManager init ", e);
        }
    }

    public static void setDisplayId(InputEvent inputEvent, int displayId) throws InvocationTargetException, IllegalAccessException {
        if (setDisplayIdMethod != null) {
            setDisplayIdMethod.invoke(inputEvent, displayId);
        }
    }

    public static void injectInputEvent(InputEvent inputEvent, int mode) throws InvocationTargetException, IllegalAccessException {
        injectInputEventMethod.invoke(manager, inputEvent, mode);
    }
}
