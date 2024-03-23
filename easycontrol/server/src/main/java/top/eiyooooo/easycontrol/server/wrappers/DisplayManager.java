package top.eiyooooo.easycontrol.server.wrappers;

import android.util.Pair;
import android.view.Display;
import top.eiyooooo.easycontrol.server.Channel;
import top.eiyooooo.easycontrol.server.entity.DisplayInfo;
import top.eiyooooo.easycontrol.server.utils.L;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DisplayManager {
    private static Object manager;
    private static Method getDisplayIdsMethod = null;

    public static void init(Object m) {
        manager = m;
    }

    public static DisplayInfo parseDisplayInfo(String dumpsysDisplayOutput, int displayId) {
        Pattern regex = Pattern.compile(
                "^    mOverrideDisplayInfo=DisplayInfo\\{\".*?, displayId " + displayId + ".*?(, FLAG_.*)?, real ([0-9]+) x ([0-9]+).*?, "
                        + "rotation ([0-9]+).*?, density ([0-9]+).*?, layerStack ([0-9]+)",
                Pattern.MULTILINE);
        Matcher m = regex.matcher(dumpsysDisplayOutput);
        if (!m.find()) return null;
        int flags = parseDisplayFlags(m.group(1));
        int width = Integer.parseInt(Objects.requireNonNull(m.group(2)));
        int height = Integer.parseInt(Objects.requireNonNull(m.group(3)));
        int rotation = Integer.parseInt(Objects.requireNonNull(m.group(4)));
        int density = Integer.parseInt(Objects.requireNonNull(m.group(5)));
        int layerStack = Integer.parseInt(Objects.requireNonNull(m.group(6)));

        return new DisplayInfo(displayId, new Pair<>(width, height), density, rotation, layerStack, flags);
    }

    private static DisplayInfo getDisplayInfoFromDumpsysDisplay(int displayId) {
        try {
            String dumpsysDisplayOutput = Channel.execReadOutput("dumpsys display");
            return parseDisplayInfo(dumpsysDisplayOutput, displayId);
        } catch (Exception e) {
            L.e("getDisplayInfoFromDumpsysDisplay error", e);
            return null;
        }
    }

    private static int parseDisplayFlags(String text) {
        Pattern regex = Pattern.compile("FLAG_[A-Z_]+");
        if (text == null) return 0;

        int flags = 0;
        Matcher m = regex.matcher(text);
        while (m.find()) {
            String flagString = m.group();
            try {
                Field filed = Display.class.getDeclaredField(flagString);
                flags |= filed.getInt(null);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                // Silently ignore, some flags reported by "dumpsys display" are @TestApi
                L.d("Unknown display flag: " + flagString);
            }
        }
        return flags;
    }

    public static DisplayInfo getDisplayInfo(int displayId) {
        try {
            Object displayInfo = manager.getClass().getMethod("getDisplayInfo", int.class).invoke(manager, displayId);
            // fallback when displayInfo is null
            if (displayInfo == null) return getDisplayInfoFromDumpsysDisplay(displayId);
            Class<?> cls = displayInfo.getClass();
            // width and height already take the rotation into account
            int width = cls.getDeclaredField("logicalWidth").getInt(displayInfo);
            int height = cls.getDeclaredField("logicalHeight").getInt(displayInfo);
            int rotation = cls.getDeclaredField("rotation").getInt(displayInfo);
            int density = (int) cls.getDeclaredField("logicalDensityDpi").getFloat(displayInfo);
            int layerStack = cls.getDeclaredField("layerStack").getInt(displayInfo);
            int flags = cls.getDeclaredField("flags").getInt(displayInfo);
            return new DisplayInfo(displayId, new Pair<>(width, height), density, rotation, layerStack, flags);
        } catch (Exception e) {
            L.e("getDisplayInfo error", e);
            throw new AssertionError(e);
        }
    }

    private static Method getGetDisplayIdsMethod() throws NoSuchMethodException {
        if (getDisplayIdsMethod == null) getDisplayIdsMethod = manager.getClass().getMethod("getDisplayIds");
        return getDisplayIdsMethod;
    }

    public static int[] getDisplayIds() {
        try {
            return (int[]) getGetDisplayIdsMethod().invoke(manager);
        } catch (Exception e) {
            L.e("getDisplayIds error", e);
            throw new AssertionError(e);
        }
    }
}
