package top.eiyooooo.easycontrol.server.wrappers;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.ResolveInfo;
import android.os.IInterface;
import top.eiyooooo.easycontrol.server.utils.L;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

public class IPackageManager {
    private static IInterface manager;
    private static Class<?> CLASS;
    private static Method getPackageInfoMethod = null;
    private static Method getQueryIntentActivitiesMethod = null;
    private static Method getInstalledPackagesMethod = null;

    public static void init(IInterface m) {
        manager = m;
        if (manager == null) {
            L.e("Error in IPackageManager.init: manager is null");
            return;
        }
        CLASS = manager.getClass();
    }

    private static Method getGetPackageInfoMethod() throws ReflectiveOperationException {
        if (getPackageInfoMethod == null) {
            if (CLASS == null) {
                L.e("Error in getGetPackageInfoMethod: CLASS is null");
                return null;
            }
            getPackageInfoMethod = CLASS.getDeclaredMethod("getPackageInfo", String.class, int.class);
            getPackageInfoMethod.setAccessible(true);
        }
        return getPackageInfoMethod;
    }

    private static Method getQueryIntentActivitiesMethod() throws ReflectiveOperationException {
        if (getQueryIntentActivitiesMethod == null) {
            if (CLASS == null) {
                L.e("Error in getGetPackageInfoMethod: CLASS is null");
                return null;
            }
            getQueryIntentActivitiesMethod = CLASS.getMethod("queryIntentActivities");
        }
        return getQueryIntentActivitiesMethod;
    }

    private static Method getGetInstalledPackagesMethod() throws ReflectiveOperationException {
        if (getInstalledPackagesMethod == null) {
            if (CLASS == null) {
                L.e("Error in getGetPackageInfoMethod: CLASS is null");
                return null;
            }
            getInstalledPackagesMethod = CLASS.getMethod("getAllPackages");
        }
        return getInstalledPackagesMethod;
    }

    public static PackageInfo getPackageInfo(String packageName, int flag) {
        try {
            return (PackageInfo) Objects.requireNonNull(getGetPackageInfoMethod()).invoke(manager, new Object[]{packageName, flag});
        } catch (Exception e) {
            L.e("Error in getPackageInfo", e);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static List<ResolveInfo> queryIntentActivities(Intent intent,
                                                          String resolvedType, int flags, int userId) {
        try {
            return (List<ResolveInfo>) Objects.requireNonNull(getQueryIntentActivitiesMethod()).invoke(manager, new Object[]{intent, resolvedType, flags, userId});
        } catch (Exception e) {
            L.e("Error in queryIntentActivities", e);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static List<String> getInstalledPackages(int flag) {
        try {
            return (List<String>) Objects.requireNonNull(getGetInstalledPackagesMethod()).invoke(manager, new Object[]{flag});
        } catch (Exception e) {
            L.e("Error in getInstalledPackages", e);
        }
        return null;
    }
}