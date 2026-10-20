package top.eiyooooo.easycontrol.server;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.TaskInfo;
import android.content.ComponentName;
import android.content.Context;
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
import android.graphics.drawable.Drawable;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.os.Build;
import android.os.Looper;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.view.Surface;
import android.view.SurfaceView;
import org.json.JSONArray;
import org.json.JSONObject;
import top.eiyooooo.easycontrol.server.helper.FakeContext;
import top.eiyooooo.easycontrol.server.utils.L;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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

                    context = FakeContext.get();

                    DisplayManager dm = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
                    dm.registerDisplayListener(new DisplayManager.DisplayListener() {
                        @Override
                        public void onDisplayAdded(int displayId) {
                            L.logToFile("onDisplayAdded invoked displayId:" + displayId);
                        }

                        @Override
                        public void onDisplayRemoved(int displayId) {
                            L.logToFile("onDisplayRemoved invoked displayId:" + displayId);

                        }

                        @Override
                        public void onDisplayChanged(int displayId) {
                            L.logToFile("onDisplayChanged invoked displayId:" + displayId);
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

    public Bitmap Drawable2Bitmap(String packageName) throws
            InvocationTargetException, IllegalAccessException {
        return Drawable2Bitmap(packageName, 0);
    }

    // size > 0 时直接按目标尺寸绘制，避免先解码全尺寸 bitmap 再缩放的双重开销
    public Bitmap Drawable2Bitmap(String packageName, int size) throws
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

        return Drawable2Bitmap(icon, size);
    }

    private Bitmap Drawable2Bitmap(Drawable icon) {
        return Drawable2Bitmap(icon, 0);
    }

    private Bitmap Drawable2Bitmap(Drawable icon, int size) {
        try {
            if (icon == null) return null;
            int w = icon.getIntrinsicWidth();
            int h = icon.getIntrinsicHeight();
            if (w <= 0 || h <= 0) {
                w = size > 0 ? size : 48;
                h = w;
            }
            int tw = (size > 0 && (w > size || h > size)) ? size : w;
            int th = (size > 0 && (w > size || h > size)) ? size : h;
            Bitmap bitmap = Bitmap.createBitmap(tw, th, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            icon.setBounds(0, 0, tw, th);
            icon.draw(canvas);
            return bitmap;
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
        return getAllAppInfo(appType, false);
    }

    public String getAllAppInfo(int appType, boolean withIcon) throws InvocationTargetException, IllegalAccessException {
        long t0 = android.os.SystemClock.elapsedRealtime();
        List<String> packages = getAppPackages();
        long t1 = android.os.SystemClock.elapsedRealtime();
        // 每个应用需要 2 次包信息 binder 查询 + 图标解码，应用多时串行非常慢，并行预生成各行文本后按原顺序拼装
        int threads = Math.min(4, Math.max(1, Runtime.getRuntime().availableProcessors() - 2));
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        StringBuilder builder = new StringBuilder();
        int lines = 0;
        try {
            List<Future<String>> futures = new ArrayList<>();
            for (String packageName : packages) {
                futures.add(executor.submit(() -> buildAppInfoLine(packageName, appType, withIcon)));
            }
            for (Future<String> future : futures) {
                try {
                    String line = future.get();
                    if (line != null) {
                        builder.append(line);
                        lines++;
                    }
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    // 包信息反射查询失败按原语义向上抛（请求失败），其他异常跳过该应用
                    if (cause instanceof InvocationTargetException) throw (InvocationTargetException) cause;
                    if (cause instanceof IllegalAccessException) throw (IllegalAccessException) cause;
                    L.e("getAllAppInfo", cause);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            executor.shutdown();
        }
        L.d("getAllAppInfo: apps=" + lines + "/" + packages.size() + " threads=" + threads + " icon=" + withIcon
                + " getPackages=" + (t1 - t0) + "ms queryAndIcon=" + (android.os.SystemClock.elapsedRealtime() - t1) + "ms");
        return builder.toString().trim();
    }

    // 生成单个应用的信息行（被过滤或查询失败返回 null）
    private String buildAppInfoLine(String packageName, int appType, boolean withIcon) throws InvocationTargetException, IllegalAccessException {
        PackageInfo packageInfo = getPackageInfo(packageName);
        if (packageInfo == null) return null;
        int resultTag = packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM;
        if (appType != 0) {
            if (appType == 1) {
                if (resultTag > 0) {
                    // system app
                    return null;
                }
            } else {
                if (resultTag == 0) {
                    // user app
                    return null;
                }
            }
        }
        String iconBase64 = withIcon ? getIconBase64(packageInfo.applicationInfo.packageName, 64) : null;
        StringBuilder line = new StringBuilder();
        phasePackageInfo(line, packageInfo, iconBase64);
        return line.toString();
    }

    private void phasePackageInfo(StringBuilder builder, PackageInfo packageInfo, String iconBase64) {
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
        if (iconBase64 != null) {
            builder.append("<!@r@!>").append(iconBase64);
        }
        builder.append("<!@n@!>");
    }

    // 图标 base64 数据上限
    private static final int MAX_ICON_BYTES = 16 * 1024;

    // 获取应用图标并转为 base64 字符串（失败返回 null）
    public String getIconBase64(String packageName, int size) {
        try {
            Bitmap bitmap = Drawable2Bitmap(packageName, size);
            if (bitmap == null) return "null";
            byte[] bytes = compressIcon(bitmap);
            return Base64.encodeToString(bytes, Base64.NO_WRAP);
        } catch (Exception e) {
            L.e("getIconBase64 " + packageName, e);
            return "null";
        }
    }

    // 图标压缩到 16KB 以内；负责回收传入的 bitmap
    // API 30+ 用有损 WEBP（体积小、新编码器快）逐级降质量；旧系统 legacy WEBP 编码极慢，改用 PNG（quality 无效）+ 缩尺寸控制体积
    private byte[] compressIcon(Bitmap bitmap) {
        byte[] bytes = new byte[0];
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                int[] qualities = {90, 75, 50, 30};
                while (true) {
                    for (int quality : qualities) {
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, quality, stream);
                        bytes = stream.toByteArray();
                        stream.close();
                        if (bytes.length <= MAX_ICON_BYTES) return bytes;
                    }
                    int width = bitmap.getWidth();
                    if (width <= 32) return bytes;
                    int newWidth = Math.max(32, width / 2);
                    Bitmap scaled = Bitmap.createScaledBitmap(bitmap, newWidth, newWidth, true);
                    bitmap.recycle();
                    bitmap = scaled;
                }
            }
            // 旧系统：PNG 无损，降质量无效，直接缩尺寸直到满足
            while (true) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                bytes = stream.toByteArray();
                stream.close();
                int width = bitmap.getWidth();
                if (bytes.length <= MAX_ICON_BYTES || width <= 32) return bytes;
                int newWidth = Math.max(32, width / 2);
                Bitmap scaled = Bitmap.createScaledBitmap(bitmap, newWidth, newWidth, true);
                bitmap.recycle();
                bitmap = scaled;
            }
        } catch (Exception e) {
            L.e("compressIcon", e);
            return bytes;
        } finally {
            bitmap.recycle();
        }
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

    public Pair<VirtualDisplay, Surface> createVirtualDisplay(int width, int height, int density) throws Exception {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R)
            throw new Exception("Virtual display is not supported before Android 11");
        Surface surface = null;
		try {
            surface = MediaCodec.createPersistentInputSurface();
        } catch (Exception ignored) {
            L.w("Failed to create MediaCodec persistent input surface");
        }
        
        if (surface == null) {
            try {
	            SurfaceView surfaceView = new SurfaceView(FakeContext.get());
	            surface = surfaceView.getHolder().getSurface();
	        } catch (Exception ignored) {
	            L.w("Failed to create SurfaceView, trying MediaCodec");
	        }
        }
        if (surface == null) throw new Exception("Failed to create surface");
        android.hardware.display.DisplayManager displayManager = DisplayManager.class.getDeclaredConstructor(Context.class).newInstance(FakeContext.get());
        int flags = getVirtualDisplayFlags();
        VirtualDisplay virtualDisplay = displayManager.createVirtualDisplay("easycontrol_for_car", width, height, density, surface, flags);
        return new Pair<>(virtualDisplay, surface);
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
