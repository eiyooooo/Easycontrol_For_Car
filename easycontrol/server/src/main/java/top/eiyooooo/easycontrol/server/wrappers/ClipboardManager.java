package top.eiyooooo.easycontrol.server.wrappers;

import android.content.ClipData;
import android.content.IOnPrimaryClipChangedListener;
import android.os.Build;
import android.os.IInterface;
import top.eiyooooo.easycontrol.server.helper.FakeContext;
import top.eiyooooo.easycontrol.server.utils.L;

import java.lang.reflect.Method;

public class ClipboardManager {
    private static IInterface manager;
    private static Method getPrimaryClipMethod = null;
    private static Method setPrimaryClipMethod = null;
    private static Method addPrimaryClipChangedListener = null;
    private static int getMethodVersion;
    private static int setMethodVersion;
    private static int addListenerMethodVersion;

    public static void init(IInterface m) {
        manager = m;
    }

    private static void getGetPrimaryClipMethod() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            try {
                getPrimaryClipMethod = manager.getClass().getMethod("getPrimaryClip", String.class);
            } catch (ReflectiveOperationException e) {
                L.e("getGetPrimaryClipMethod error", e);
            }
        } else {
            for (int i = 0; i < 7; i++) {
                try {
                    getMethodVersion = i;
                    switch (i) {
                        case 0:
                            getPrimaryClipMethod = manager.getClass().getMethod("getPrimaryClip", String.class, int.class);
                            return;
                        case 1:
                            getPrimaryClipMethod = manager.getClass().getMethod("getPrimaryClip", String.class, String.class, int.class);
                            return;
                        case 2:
                            getPrimaryClipMethod = manager.getClass().getMethod("getPrimaryClip", String.class, String.class, int.class, int.class);
                            return;
                        case 3:
                            getPrimaryClipMethod = manager.getClass().getMethod("getPrimaryClip", String.class, int.class, String.class);
                            return;
                        case 4:
                            getPrimaryClipMethod = manager.getClass().getMethod("getPrimaryClip", String.class, String.class, int.class, int.class, boolean.class);
                            return;
                        case 5:
                            getPrimaryClipMethod = manager.getClass().getMethod("getPrimaryClip", String.class, String.class, String.class, String.class, int.class, int.class, boolean.class);
                            return;
                        case 6:
                            getPrimaryClipMethod = manager.getClass().getMethod("getPrimaryClip", String.class, String.class, int.class, int.class, String.class);
                            return;
                    }
                } catch (ReflectiveOperationException e) {
                    if (i == 6) L.e("getGetPrimaryClipMethod error", e);
                }
            }
        }
    }

    private static void getSetPrimaryClipMethod() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            try {
                setPrimaryClipMethod = manager.getClass().getMethod("setPrimaryClip", ClipData.class, String.class);
            } catch (ReflectiveOperationException e) {
                L.e("getSetPrimaryClipMethod error", e);
            }
        } else {
            for (int i = 0; i < 4; i++) {
                try {
                    setMethodVersion = i;
                    switch (i) {
                        case 0:
                            setPrimaryClipMethod = manager.getClass().getMethod("setPrimaryClip", ClipData.class, String.class, int.class);
                            return;
                        case 1:
                            setPrimaryClipMethod = manager.getClass().getMethod("setPrimaryClip", ClipData.class, String.class, String.class, int.class);
                            return;
                        case 2:
                            setPrimaryClipMethod = manager.getClass().getMethod("setPrimaryClip", ClipData.class, String.class, String.class, int.class, int.class);
                            return;
                        case 3:
                            setPrimaryClipMethod = manager.getClass().getMethod("setPrimaryClip", ClipData.class, String.class, String.class, int.class, int.class, boolean.class);
                            return;
                    }
                } catch (ReflectiveOperationException e) {
                    if (i == 3) L.e("getSetPrimaryClipMethod error", e);
                }
            }
        }
    }

    private static void getAddPrimaryClipChangedListenerMethod() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            try {
                addPrimaryClipChangedListener = manager.getClass().getMethod("addPrimaryClipChangedListener", IOnPrimaryClipChangedListener.class, String.class);
            } catch (ReflectiveOperationException e) {
                L.e("getAddPrimaryClipChangedListenerMethod error", e);
            }
        } else {
            for (int i = 0; i < 3; i++) {
                try {
                    addListenerMethodVersion = i;
                    switch (i) {
                        case 0:
                            addPrimaryClipChangedListener = manager.getClass().getMethod("addPrimaryClipChangedListener", IOnPrimaryClipChangedListener.class, String.class, int.class);
                            return;
                        case 1:
                            addPrimaryClipChangedListener = manager.getClass().getMethod("addPrimaryClipChangedListener", IOnPrimaryClipChangedListener.class, String.class, String.class, int.class);
                            return;
                        case 2:
                            addPrimaryClipChangedListener = manager.getClass().getMethod("addPrimaryClipChangedListener", IOnPrimaryClipChangedListener.class, String.class, String.class, int.class, int.class);
                            return;
                    }
                } catch (ReflectiveOperationException e) {
                    if (i == 2) L.e("getAddPrimaryClipChangedListenerMethod error", e);
                }
            }
        }
    }

    public static void addPrimaryClipChangedListener(IOnPrimaryClipChangedListener listener) {
        if (manager == null) {
            L.e("Error in addPrimaryClipChangedListener: manager is null");
            return;
        }
        if (addPrimaryClipChangedListener == null) getAddPrimaryClipChangedListenerMethod();
        try {
            if (addPrimaryClipChangedListener == null) throw new NoSuchMethodException("addPrimaryClipChangedListener");
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                addPrimaryClipChangedListener.invoke(manager, listener, FakeContext.PACKAGE_NAME);
            } else {
                switch (addListenerMethodVersion) {
                    case 0:
                        addPrimaryClipChangedListener.invoke(manager, listener, FakeContext.PACKAGE_NAME, FakeContext.ROOT_UID);
                        break;
                    case 1:
                        addPrimaryClipChangedListener.invoke(manager, listener, FakeContext.PACKAGE_NAME, null, FakeContext.ROOT_UID);
                        break;
                    default:
                        addPrimaryClipChangedListener.invoke(manager, listener, FakeContext.PACKAGE_NAME, null, FakeContext.ROOT_UID, 0);
                        break;
                }
            }
        } catch (Exception e) {
            L.e("addPrimaryClipChangedListener error", e);
        }
    }

    public static String getText() {
        if (manager == null) {
            L.e("Error in getText: manager is null");
            return null;
        }
        if (getPrimaryClipMethod == null) getGetPrimaryClipMethod();
        try {
            if (getPrimaryClipMethod == null) throw new NoSuchMethodException("getPrimaryClip");
            ClipData clipData;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                clipData = (ClipData) getPrimaryClipMethod.invoke(manager, FakeContext.PACKAGE_NAME);
            } else {
                switch (getMethodVersion) {
                    case 0:
                        clipData = (ClipData) getPrimaryClipMethod.invoke(manager, FakeContext.PACKAGE_NAME, FakeContext.ROOT_UID);
                        break;
                    case 1:
                        clipData = (ClipData) getPrimaryClipMethod.invoke(manager, FakeContext.PACKAGE_NAME, null, FakeContext.ROOT_UID);
                        break;
                    case 2:
                        clipData = (ClipData) getPrimaryClipMethod.invoke(manager, FakeContext.PACKAGE_NAME, null, FakeContext.ROOT_UID, 0);
                        break;
                    case 3:
                        clipData = (ClipData) getPrimaryClipMethod.invoke(manager, FakeContext.PACKAGE_NAME, FakeContext.ROOT_UID, null);
                        break;
                    case 4:
                        clipData = (ClipData) getPrimaryClipMethod.invoke(manager, FakeContext.PACKAGE_NAME, null, FakeContext.ROOT_UID, 0, true);
                        break;
                    case 5:
                        clipData = (ClipData) getPrimaryClipMethod.invoke(manager, FakeContext.PACKAGE_NAME, null, null, null, FakeContext.ROOT_UID, 0, true);
                        break;
                    default:
                        clipData = (ClipData) getPrimaryClipMethod.invoke(manager, FakeContext.PACKAGE_NAME, null, FakeContext.ROOT_UID, 0, null);
                        break;
                }
            }
            if (clipData == null) return null;
            if (clipData.getItemCount() == 0) return null;
            return String.valueOf(clipData.getItemAt(0).getText());
        } catch (Exception e) {
            L.e("getText error", e);
            return null;
        }
    }

    public static void setText(String text) {
        if (manager == null) {
            L.e("Error in setText: manager is null");
            return;
        }
        if (setPrimaryClipMethod == null) getSetPrimaryClipMethod();
        ClipData clipData = ClipData.newPlainText("easycontrol_for_car", text);
        try {
            if (setPrimaryClipMethod == null) throw new NoSuchMethodException("setPrimaryClip");
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                setPrimaryClipMethod.invoke(manager, clipData, FakeContext.PACKAGE_NAME);
            } else {
                switch (setMethodVersion) {
                    case 0:
                        setPrimaryClipMethod.invoke(manager, clipData, FakeContext.PACKAGE_NAME, FakeContext.ROOT_UID);
                        break;
                    case 1:
                        setPrimaryClipMethod.invoke(manager, clipData, FakeContext.PACKAGE_NAME, null, FakeContext.ROOT_UID);
                        break;
                    case 2:
                        setPrimaryClipMethod.invoke(manager, clipData, FakeContext.PACKAGE_NAME, null, FakeContext.ROOT_UID, 0);
                        break;
                    default:
                        setPrimaryClipMethod.invoke(manager, clipData, FakeContext.PACKAGE_NAME, null, FakeContext.ROOT_UID, 0, true);
                        break;
                }
            }
        } catch (Exception e) {
            L.e("setText error", e);
        }
    }
}
