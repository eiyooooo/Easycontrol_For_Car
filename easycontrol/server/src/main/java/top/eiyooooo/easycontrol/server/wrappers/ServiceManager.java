package top.eiyooooo.easycontrol.server.wrappers;

import android.annotation.SuppressLint;
import android.os.IBinder;
import android.os.IInterface;
import top.eiyooooo.easycontrol.server.utils.L;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ServiceManager {
    private static Method GET_SERVICE_METHOD;

    @SuppressLint({"DiscouragedPrivateApi", "PrivateApi"})
    public static void setManagers() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        GET_SERVICE_METHOD = Class.forName("android.os.ServiceManager").getDeclaredMethod("getService", String.class);
        // 1
        WindowManager.init(getService("window", "android.view.IWindowManager"));
        // 2
        DisplayManager.init(Class.forName("android.hardware.display.DisplayManagerGlobal").getDeclaredMethod("getInstance").invoke(null));
        // 3
        Class<?> inputManagerClass;
        try {
            inputManagerClass = Class.forName("android.hardware.input.InputManagerGlobal");
        } catch (ClassNotFoundException e) {
            inputManagerClass = android.hardware.input.InputManager.class;
        }
        InputManager.init(inputManagerClass.getDeclaredMethod("getInstance").invoke(null));
        // 4
        ClipboardManager.init(getService("clipboard", "android.content.IClipboard"));
        // 5
        SurfaceControl.init();
        // 6
        try {
            IPackageManager.init(getService("package", "android.content.pm.IPackageManager"));
        } catch (Exception e) {
            L.e("Failed to init IPackageManager", e);
        }
    }

    private static IInterface getService(String service, String type) {
        try {
            IBinder binder = (IBinder) GET_SERVICE_METHOD.invoke(null, service);
            Method asInterfaceMethod = Class.forName(type + "$Stub").getMethod("asInterface", IBinder.class);
            return (IInterface) asInterfaceMethod.invoke(null, binder);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
