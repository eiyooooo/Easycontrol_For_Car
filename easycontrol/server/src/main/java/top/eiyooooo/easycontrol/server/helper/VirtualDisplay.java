package top.eiyooooo.easycontrol.server.helper;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.util.Pair;
import android.view.Display;
import top.eiyooooo.easycontrol.server.entity.Device;

import static top.eiyooooo.easycontrol.server.Server.postDelayed;

import java.util.ArrayList;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class VirtualDisplay {
    private static final int VIRTUAL_DISPLAY_FLAG_DESTROY_CONTENT_ON_REMOVAL = 1 << 8;
    private static final int VIRTUAL_DISPLAY_FLAG_SHOULD_SHOW_SYSTEM_DECORATIONS = 1 << 9;
    private static final int VIRTUAL_DISPLAY_FLAG_TRUSTED = 1 << 10;
    private static final int VIRTUAL_DISPLAY_FLAG_OWN_DISPLAY_GROUP = 1 << 11;
    private static final int VIRTUAL_DISPLAY_FLAG_ALWAYS_UNLOCKED = 1 << 12;
    private static DisplayManager displayManager;
    static android.hardware.display.VirtualDisplay virtualDisplay;
    private static int width;
    private static int height;
    private static int density;
    private static Pair<ArrayList<application>, ArrayList<application>> applications = new Pair<>(new ArrayList<>(), new ArrayList<>());
    private static int firstVisibleApp = 0;

    @SuppressLint("WrongConstant")
    public static int create() throws Exception {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            throw new Exception("Virtual display is not supported before Android 11");
        }

        // 未设置强制桌面模式

        displayManager = DisplayManager.class.getDeclaredConstructor(Context.class).newInstance(FakeContext.get());

        // 获取width、height、density
        String dumpsysDisplayOutput = Device.execReadOutput("dumpsys display");
        Pattern regex = Pattern.compile(
                "^    mOverrideDisplayInfo=DisplayInfo\\{\".*?, displayId " + Display.DEFAULT_DISPLAY
                        + ".*?real ([0-9]+) x ([0-9]+).*?, " + "density ([0-9]+)", Pattern.MULTILINE);
        Matcher m = regex.matcher(dumpsysDisplayOutput);
        if (!m.find()) throw new Exception("Could not get display info from \"dumpsys display\" output");
        width = Integer.parseInt(Objects.requireNonNull(m.group(1)));
        height = Integer.parseInt(Objects.requireNonNull(m.group(2)));
        density = Integer.parseInt(Objects.requireNonNull(m.group(3)));

        int flags = DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC | DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | VIRTUAL_DISPLAY_FLAG_DESTROY_CONTENT_ON_REMOVAL | DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            flags |= VIRTUAL_DISPLAY_FLAG_TRUSTED | VIRTUAL_DISPLAY_FLAG_OWN_DISPLAY_GROUP | VIRTUAL_DISPLAY_FLAG_ALWAYS_UNLOCKED;
        }

        virtualDisplay = displayManager.createVirtualDisplay("easycontrol", width, height, density, null, flags);
        return virtualDisplay.getDisplay().getDisplayId();
    }

    public static void release() {
        for (application a : applications.second) {
            if (firstVisibleApp != 0 && a.id == firstVisibleApp) continue;
            try {
                Device.execReadOutput("am display move-stack " + a.id + " " + Display.DEFAULT_DISPLAY);
            } catch (Exception ignored) {}
        }

        if (firstVisibleApp != 0) {
            try {
                Device.execReadOutput("am display move-stack " + firstVisibleApp + " " + Display.DEFAULT_DISPLAY);
            } catch (Exception ignored) {
            }
        }

        if (virtualDisplay != null) {
            virtualDisplay.release();
        }
    }

    public static void applicationMonitor() {
        try {
            applications = getStackList();

            if (applications.second.isEmpty()) {
                int localVisibleApp = 0;
                for (application a : applications.first) {
                    if (a.visible) {
                        localVisibleApp = a.id;
                        break;
                    }
                }
                if (localVisibleApp == 0) throw new Exception("No visible app found on local display");
                Device.execReadOutput("am display move-stack " + localVisibleApp + " " + Device.displayId);
                firstVisibleApp = localVisibleApp;
                ControlPacket.sendDisplayId(Device.displayId);
            }

            postDelayed(VirtualDisplay::applicationMonitor, 2000);
        } catch (Exception ignored0) {
            try {
                ControlPacket.sendDisplayId(-Device.displayId);
            } catch (Exception ignored1) {}

            postDelayed(VirtualDisplay::applicationMonitor, 2000);
        }
    }

    private static Pair<ArrayList<application>, ArrayList<application>> getStackList() throws Exception {
        String stackList = Device.execReadOutput("am stack list").replaceAll("\n", "");

        String[] input = null;
        if (stackList.contains("RootTask id")) input = stackList.split("RootTask ");
        else if (stackList.contains("Stack id")) input = stackList.split("Stack ");
        else throw new Exception("Could not get stack list from \"am stack list\" output");

        return stackListParser(input);
    }

    private static Pair<ArrayList<application>, ArrayList<application>> stackListParser(String[] strings) {
        Pattern regex1 = Pattern.compile("^id=([0-9]+).*?displayId=([0-9]+)", Pattern.MULTILINE);
        Pattern regex2 = Pattern.compile("taskId=([0-9]+): (.*?) bounds.*?visible=([a-z]+)");

        ArrayList<application> local = new ArrayList<>();
        ArrayList<application> remote = new ArrayList<>();

        for (String s : strings) {
            Matcher m1 = regex1.matcher(s);
            if (m1.find()) {
                int displayId = Integer.parseInt(m1.group(2));
                if (displayId != Display.DEFAULT_DISPLAY && displayId != Device.displayId) continue;
                int id = Integer.parseInt(m1.group(1));
                application app = new application(id, displayId);

                Matcher m2 = regex2.matcher(s);
                while (m2.find()) {
                    app.addTask(Integer.parseInt(m2.group(1)), m2.group(2), "true".equals(m2.group(3)));
                }

                if (displayId == Display.DEFAULT_DISPLAY) local.add(app);
                else remote.add(app);
            }
        }
        return new Pair<>(local, remote);
    }

    private static class application {
        public int id;
        public int displayId;
        public boolean visible = false;
        public application(int id, int displayId) {
            this.id = id;
            this.displayId = displayId;
        }

        public ArrayList<task> tasks = new ArrayList<>();
        public static class task {
            public int taskId;
            public String activityName;
            public boolean visible;
            public task(int taskId, String activityName, boolean visible) {
                this.taskId = taskId;
                this.activityName = activityName;
                this.visible = visible;
            }
        }
        public void addTask(int taskId, String activityName, boolean visible) {
            tasks.add(new task(taskId, activityName, visible));
            if (visible) this.visible = true;
        }
    }
}
