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
    public static void setManagers() {
        for (int i = 0; i < 7; i++) {
            try {
                switch (i) {
                    case 0:
                        GET_SERVICE_METHOD = Class.forName("android.os.ServiceManager").getDeclaredMethod("getService", String.class);
                        break;
                    case 1:
                        WindowManager.init(getService("window", "android.view.IWindowManager"));
                        break;
                    case 2:
                        DisplayManager.init(Class.forName("android.hardware.display.DisplayManagerGlobal").getDeclaredMethod("getInstance").invoke(null));
                        break;
                    case 3:
                        Class<?> inputManagerClass;
                        try {
                            inputManagerClass = Class.forName("android.hardware.input.InputManagerGlobal");
                        } catch (ClassNotFoundException e) {
                            inputManagerClass = android.hardware.input.InputManager.class;
                        }
                        InputManager.init(inputManagerClass.getDeclaredMethod("getInstance").invoke(null));
                        break;
                    case 4:
                        ClipboardManager.init(getService("clipboard", "android.content.IClipboard"));
                        break;
                    case 5:
                        SurfaceControl.init();
                        break;
                    case 6:
                        IPackageManager.init(getService("package", "android.content.pm.IPackageManager"));
                        break;
                }
            } catch (Exception e) {
                L.e("ServiceManager init error", e);
            }
        }
    }

    private static IInterface getService(String service, String type) throws Exception {
        IBinder binder = (IBinder) GET_SERVICE_METHOD.invoke(null, service);
        Method asInterfaceMethod = Class.forName(type + "$Stub").getMethod("asInterface", IBinder.class);
        return (IInterface) asInterfaceMethod.invoke(null, binder);
    }
}