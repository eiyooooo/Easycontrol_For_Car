package top.eiyooooo.easycontrol.app.helper;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.os.Build;

public class ShortcutHelper {
    public static void addShortcut(Context context, Class<?> targetActivity, String label, int iconResourceId, String uuid) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);

            if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported()) {
                Intent shortcutIntent = new Intent(context, targetActivity);
                shortcutIntent.setAction(Intent.ACTION_VIEW);
                if (uuid != null) shortcutIntent.putExtra("uuid", uuid);
                else uuid = "default";

                ShortcutInfo shortcutInfo = new ShortcutInfo.Builder(context, uuid)
                        .setShortLabel(label)
                        .setIcon(Icon.createWithResource(context, iconResourceId))
                        .setIntent(shortcutIntent)
                        .build();

                shortcutManager.requestPinShortcut(shortcutInfo, null);
            }
        } else {
            Intent shortcutIntent = new Intent();
            shortcutIntent.setComponent(new ComponentName(context.getPackageName(), targetActivity.getName()));
            shortcutIntent.setAction(Intent.ACTION_VIEW);
            if (uuid != null) shortcutIntent.putExtra("uuid", uuid);

            Intent addIntent = new Intent();
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, label);
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                    Intent.ShortcutIconResource.fromContext(context, iconResourceId));
            addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
            context.sendBroadcast(addIntent);
        }
    }

    public static void addShortcut(Context context, Class<?> targetActivity, String label, Bitmap iconBitmap, String uuid) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);

            if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported()) {
                Intent shortcutIntent = new Intent(context, targetActivity);
                shortcutIntent.setAction(Intent.ACTION_VIEW);
                if (uuid != null) shortcutIntent.putExtra("uuid", uuid);
                else uuid = "default";

                ShortcutInfo shortcutInfo = new ShortcutInfo.Builder(context, uuid)
                        .setShortLabel(label)
                        .setIcon(Icon.createWithBitmap(iconBitmap))
                        .setIntent(shortcutIntent)
                        .build();

                shortcutManager.requestPinShortcut(shortcutInfo, null);
            }
        } else {
            Intent shortcutIntent = new Intent();
            shortcutIntent.setComponent(new ComponentName(context.getPackageName(), targetActivity.getName()));
            shortcutIntent.setAction(Intent.ACTION_VIEW);
            if (uuid != null) shortcutIntent.putExtra("uuid", uuid);

            Intent addIntent = new Intent();
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, label);
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, iconBitmap);
            addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
            context.sendBroadcast(addIntent);
        }
    }
}