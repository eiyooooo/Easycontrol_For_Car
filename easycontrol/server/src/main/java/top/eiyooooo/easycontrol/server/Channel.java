package top.eiyooooo.easycontrol.server;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.TaskInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.os.Build;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.SurfaceView;
import org.json.JSONArray;
import org.json.JSONObject;
import top.eiyooooo.easycontrol.server.utils.L;
import top.eiyooooo.easycontrol.server.utils.Workarounds;
import top.eiyooooo.easycontrol.server.wrappers.IPackageManager;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Channel {
    DisplayMetrics displayMetrics;
    Configuration configuration;
    Context context;
    boolean hasRealContext = false;

    public Channel() {
        L.d("Construct Channel without a context");
        displayMetrics = new DisplayMetrics();
        displayMetrics.setToDefaults();
        configuration = new Configuration();
        configuration.setToDefaults();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    L.d("Runnable run");

                    Looper.prepare();

                    context = Workarounds.fillAppInfo();
                    ContextWrapperWrapper wrapper = new ContextWrapperWrapper(context);

                    DisplayManager dm = (DisplayManager) wrapper.getSystemService(Context.DISPLAY_SERVICE);
                    dm.registerDisplayListener(new DisplayManager.DisplayListener() {
                        @Override
                        public void onDisplayAdded(int displayId) {
                            L.d("onDisplayAdded invoked displayId:" + displayId);
                        }

                        @Override
                        public void onDisplayRemoved(int displayId) {
                            L.d("onDisplayRemoved invoked displayId:" + displayId);

                        }

                        @Override
                        public void onDisplayChanged(int displayId) {
                            L.d("onDisplayChanged invoked displayId:" + displayId);
                        }
                    }, null);

                    L.d("Context: " + context.toString());

                    L.d("PHONE_INFO->" + getPhoneInfo());

                    Looper.loop();
                } catch (Exception e) {
                    L.w("Channel Runnable run error:", e);
                }
            }
        }).start();
    }

    public static JSONObject getPhoneInfo() throws Exception {
        JSONObject info = new JSONObject();
        info.put("Build.MANUFACTURER", Build.MANUFACTURER);
        info.put("Build.MODEL", Build.MODEL);
        info.put("Build.DEVICE", Build.DEVICE);
        info.put("Build.VERSION.RELEASE", Build.VERSION.RELEASE);
        info.put("Build.VERSION.SDK_INT", Build.VERSION.SDK_INT);
        info.put("Build.BOARD", Build.BOARD);
        info.put("Build.PRODUCT", Build.PRODUCT);
        info.put("Build.USER", Build.USER);
        info.put("Build.BRAND", Build.BRAND);
        info.put("Build.HARDWARE", Build.HARDWARE);
        info.put("Build.SUPPORTED_ABIS", Arrays.toString(Build.SUPPORTED_ABIS));
        return info;
    }


    static class ContextWrapperWrapper extends ContextWrapper {
        public ContextWrapperWrapper(Context base) {
            super(base);
        }

        @Override
        public String getPackageName() {
            return "com.android.shell";
        }
    }

    public synchronized String Bitmap2file(String packageName) throws Exception {
        Bitmap bitmap = Drawable2Bitmap(packageName);
        String path = "/data/local/tmp/" + packageName + ".png";
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] bytes = stream.toByteArray();
        stream.close();
        try (java.io.FileOutputStream file = new java.io.FileOutputStream(path)) {
            file.write(bytes);
            file.flush();
        }
        return path;
    }

    public synchronized Bitmap Drawable2Bitmap(String packageName) throws
            InvocationTargetException, IllegalAccessException {
        Drawable icon;

        PackageInfo packageInfo = getPackageInfo(packageName);
        ApplicationInfo applicationInfo = packageInfo.applicationInfo;
        if (applicationInfo == null) {
            L.e("applicationInfo == null");
            return null;
        }

        AssetManager assetManager;
        try {
            assetManager = AssetManager.class.newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            L.e("getBitmap", e);
            return null;
        }

        try {
            assetManager.getClass().getMethod("addAssetPath", String.class).invoke(assetManager, applicationInfo.sourceDir);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            L.e("getBitmap", e);
            return null;
        }

        Resources resources = new Resources(assetManager, displayMetrics, configuration);
        try {
            icon = resources.getDrawable(applicationInfo.icon, null);
        } catch (Exception e) {
            L.e("getBitmap package error:" + applicationInfo.packageName);
            return null;
        }

        return Drawable2Bitmap(icon);
    }

    private Bitmap Drawable2Bitmap(Drawable icon) {
        try {
            if (icon == null) return null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && icon instanceof AdaptiveIconDrawable) {
                Bitmap bitmap = Bitmap.createBitmap(icon.getIntrinsicWidth(), icon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                icon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                icon.draw(canvas);
                return bitmap;
            } else {
                int w = icon.getIntrinsicWidth();
                int h = icon.getIntrinsicHeight();
                Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                icon.setBounds(0, 0, w, h);
                icon.draw(canvas);
                return bitmap;
            }
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    public List<String> getAppPackages() {
        if (context != null) {
            PackageManager pm = context.getPackageManager();
            List<String> packages = new ArrayList<>();
            List<PackageInfo> infos = pm.getInstalledPackages(PackageManager.GET_UNINSTALLED_PACKAGES);
            for (PackageInfo info : infos) {
                packages.add(info.packageName);
            }
            return packages;
        } else {
            return IPackageManager.getInstalledPackages(PackageManager.GET_UNINSTALLED_PACKAGES);
        }
    }

    public PackageInfo getPackageInfo(String packageName) throws InvocationTargetException, IllegalAccessException {
        return getPackageInfo(packageName, PackageManager.GET_UNINSTALLED_PACKAGES);
    }

    public PackageInfo getPackageInfo(String packageName, int flag) throws InvocationTargetException, IllegalAccessException {
        if (context != null) {
            PackageManager pm = context.getPackageManager();
            PackageInfo info = null;
            try {
                info = pm.getPackageInfo(packageName, flag);
            } catch (PackageManager.NameNotFoundException e) {
                L.e("getPackageInfo", e);
            }
            return info;
        } else {
            return IPackageManager.getPackageInfo(packageName, flag);
        }
    }

    public String getAllAppInfo(int appType) throws InvocationTargetException, IllegalAccessException {
        List<String> packages = getAppPackages();
        if (packages == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String packageName : packages) {
            PackageInfo packageInfo = getPackageInfo(packageName);
            if (packageInfo == null) continue;
            int resultTag = packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM;
            if (appType != 0) {
                if (appType == 1) {
                    if (resultTag > 0) {
                        // system app
                        continue;
                    }
                } else {
                    if (resultTag == 0) {
                        // user app
                        continue;
                    }
                }
            }
            phasePackageInfo(builder, packageInfo);
        }
        return builder.toString().trim();
    }

    private void phasePackageInfo(StringBuilder builder, PackageInfo packageInfo) {
        ApplicationInfo applicationInfo = packageInfo.applicationInfo;
        builder.append(applicationInfo.packageName);
        builder.append("<!@r@!>").append(getLabel(applicationInfo));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.append("<!@r@!>").append(packageInfo.applicationInfo.minSdkVersion);
        } else {
            builder.append("<!@r@!>").append("null");
        }
        builder.append("<!@r@!>").append(applicationInfo.targetSdkVersion);
        builder.append("<!@r@!>").append(packageInfo.versionName);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            builder.append("<!@r@!>").append(packageInfo.getLongVersionCode());
        } else {
            builder.append("<!@r@!>").append(packageInfo.versionCode);
        }
        builder.append("<!@r@!>").append(applicationInfo.enabled);
        try {
            // Hidden apps will not be obtained
            getPackageInfo(packageInfo.packageName, PackageManager.GET_DISABLED_COMPONENTS);
            builder.append("<!@r@!>").append(false);
        } catch (InvocationTargetException e) {
            L.d(packageInfo.packageName + "is hidden");
            builder.append("<!@r@!>").append(true);
        } catch (IllegalAccessException e) {
            builder.append("<!@r@!>").append("unknown");
        }
        builder.append("<!@r@!>").append(applicationInfo.uid);
        builder.append("<!@r@!>").append(applicationInfo.sourceDir);
        builder.append("<!@n@!>");
    }

    public String getLabel(ApplicationInfo info) {
        if (context != null) {
            PackageManager pm = context.getPackageManager();
            return (String) info.loadLabel(pm);
        }
        int res = info.labelRes;
        if (info.nonLocalizedLabel != null) {
            return (String) info.nonLocalizedLabel;
        }
        if (res != 0) {
            AssetManager assetManager = getAssetManagerFromPath(info.sourceDir);
            Resources resources = new Resources(assetManager, displayMetrics, configuration);
            return (String) resources.getText(res);
        }
        return null;
    }

    AssetManager getAssetManagerFromPath(String path) {
        AssetManager assetManager = null;
        try {
            assetManager = AssetManager.class.newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            L.e("getAssetManagerFromPath", e);
        }
        try {
            assert assetManager != null;
            assetManager.getClass().getMethod("addAssetPath", String.class).invoke(assetManager, path);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            L.e("getAssetManagerFromPath", e);
        }
        return assetManager;
    }

    public String openApp(String packageName, String activity, int displayId) {
        if (!hasRealContext) {
            String cmd;
            if (displayId != 0) cmd = "am start --display " + displayId + " -n " + packageName + "/" + activity;
            else cmd = "am start -n " + packageName + "/" + activity;
            L.d("start activity cmd: " + cmd);
            try {
                execReadOutput(cmd);
            } catch (Exception e) {
                L.e("openApp", e);
                return e.toString();
            }
            return null;
        }
        try {
            Intent intent = new Intent();
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            // no animation
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT | Intent.FLAG_ACTIVITY_NEW_TASK);
            } else {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            ActivityOptions options = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && displayId != 0) {
                options = ActivityOptions.makeBasic().setLaunchDisplayId(displayId);
            }
            ComponentName cName = new ComponentName(packageName, activity);
            intent.setComponent(cName);
            if (options != null) {
                context.startActivity(intent, options.toBundle());
            } else {
                context.startActivity(intent);
            }
        } catch (Exception e) {
            L.e("openApp", e);
        }
        return null;
    }

    public String getAppMainActivity(String packageName) {
        if (context != null) {
            PackageManager pm = context.getPackageManager();
            Intent launchIntent = pm.getLaunchIntentForPackage(packageName);
            if (launchIntent != null) {
                return launchIntent.getComponent().getClassName();
            } else {
                L.d(packageName + " get main activity failed");
                return "";
            }
        }
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> appList = IPackageManager.queryIntentActivities(mainIntent, null, 0, 0);
        if (appList != null) {
            for (ResolveInfo resolveInfo : appList) {
                String packageStr = resolveInfo.activityInfo.packageName;
                if (packageStr.equals(packageName)) {
                    return resolveInfo.activityInfo.name;
                }
            }
        }
        return "";
    }

    public String getAppDetail(String data) {
        StringBuilder builder = new StringBuilder();
        try {
            PackageInfo packageInfo = getPackageInfo(data);
            builder.append(packageInfo.firstInstallTime).append("<!@r@!>");
            builder.append(packageInfo.lastUpdateTime).append("<!@r@!>");
            builder.append(packageInfo.applicationInfo.dataDir).append("<!@r@!>");
            builder.append(packageInfo.applicationInfo.nativeLibraryDir);
        } catch (InvocationTargetException | IllegalAccessException e) {
            L.e("getPackageInfo error:", e);
        }
        return builder.toString();
    }

    @SuppressWarnings("unchecked")
    private List<ActivityManager.RecentTaskInfo> getRecentTasks(int maxNum, int flags,
                                                                int userId) throws Exception {
        Object iam = getIAM();
        Object tasksParcelled = iam.getClass().getMethod("getRecentTasks", Integer.TYPE,
                Integer.TYPE, Integer.TYPE).invoke(iam, maxNum, flags, userId);
        if (tasksParcelled == null) return null;
        return (List<ActivityManager.RecentTaskInfo>) tasksParcelled.getClass().getMethod("getList").invoke(tasksParcelled);
    }

    private static Object getIAM() throws Exception {
        return ActivityManager.class.getMethod("getService").invoke(null);
    }

    @SuppressLint("BlockedPrivateApi")
    public JSONObject getRecentTasksJson(int maxNum, int flags, int userId) throws Exception {
        List<ActivityManager.RecentTaskInfo> tasks = getRecentTasks(maxNum, flags, userId);
        if (tasks == null) throw new Exception("getRecentTasks failed");

        JSONObject jsonObjectResult = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        for (ActivityManager.RecentTaskInfo taskInfo : tasks) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", taskInfo.id);
            jsonObject.put("persistentId", taskInfo.persistentId);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) jsonObject.put("taskId", taskInfo.taskId);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Field field = TaskInfo.class.getDeclaredField("displayId");
                    field.setAccessible(true);
                    Object displayId = field.get(taskInfo);
                    jsonObject.put("displayId", displayId);
                }
                jsonObject.put("topPackage", taskInfo.topActivity == null ? "" : taskInfo.topActivity.getPackageName());
                jsonObject.put("topActivity", taskInfo.topActivity == null ? "" : taskInfo.topActivity.getClassName());
                if (taskInfo.topActivity != null) {
                    PackageInfo packageInfo = getPackageInfo(taskInfo.topActivity.getPackageName());
                    jsonObject.put("label", getLabel(packageInfo.applicationInfo));
                } else {
                    jsonObject.put("label", "");
                }
            }
            jsonArray.put(jsonObject);
        }
        jsonObjectResult.put("data", jsonArray);
        return jsonObjectResult;
    }

    public VirtualDisplay createVirtualDisplay(int width, int height, int density) throws Exception {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R)
            throw new Exception("Virtual display is not supported before Android 11");
        Surface surface;
        try {
            SurfaceView surfaceView = new SurfaceView(new ContextWrapperWrapper(context));
            surface = surfaceView.getHolder().getSurface();
        } catch (Exception ignored) {
            L.w("Failed to create SurfaceView, trying MediaCodec");
            surface = MediaCodec.createPersistentInputSurface();
        }
        if (surface == null) throw new Exception("Failed to create surface");
        android.hardware.display.DisplayManager displayManager = DisplayManager.class.getDeclaredConstructor(Context.class).newInstance(new ContextWrapperWrapper(context));
        int flags = getVirtualDisplayFlags();
        return displayManager.createVirtualDisplay("easycontrol_for_car", width, height, density, surface, flags);
    }

    private static int getVirtualDisplayFlags() {
        int VIRTUAL_DISPLAY_FLAG_PUBLIC = 1;
        int VIRTUAL_DISPLAY_FLAG_PRESENTATION = 1 << 1;
        int VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY = 1 << 3;
        int VIRTUAL_DISPLAY_FLAG_DESTROY_CONTENT_ON_REMOVAL = 1 << 8;
        int VIRTUAL_DISPLAY_FLAG_TRUSTED = 1 << 10;
        int VIRTUAL_DISPLAY_FLAG_OWN_DISPLAY_GROUP = 1 << 11;
        int VIRTUAL_DISPLAY_FLAG_ALWAYS_UNLOCKED = 1 << 12;

        int flags = VIRTUAL_DISPLAY_FLAG_PUBLIC | VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | VIRTUAL_DISPLAY_FLAG_DESTROY_CONTENT_ON_REMOVAL | VIRTUAL_DISPLAY_FLAG_PRESENTATION;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            flags |= VIRTUAL_DISPLAY_FLAG_TRUSTED | VIRTUAL_DISPLAY_FLAG_OWN_DISPLAY_GROUP | VIRTUAL_DISPLAY_FLAG_ALWAYS_UNLOCKED;
        }
        return flags;
    }

    public static String execReadOutput(String cmd) throws IOException, InterruptedException {
        Process process = new ProcessBuilder().command("sh", "-c", cmd).start();
        StringBuilder builder = new StringBuilder();
        String line;
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            while ((line = bufferedReader.readLine()) != null) builder.append(line).append("<!@n@!>");
        }
        int exitCode = process.waitFor();
        if (exitCode != 0)
            throw new IOException("command: " + cmd + "<!@n@!>failed with exit code: " + exitCode + "<!@n@!>debug info: " + builder);
        return builder.toString();
    }
}
