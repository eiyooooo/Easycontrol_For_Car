package top.eiyooooo.easycontrol.server.helper;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.view.Display;
import top.eiyooooo.easycontrol.server.entity.Device;

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
        if (virtualDisplay != null) {
            virtualDisplay.release();
        }
    }
}
