package top.eiyooooo.easycontrol.app.helper;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ScrollView;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;

import top.eiyooooo.easycontrol.app.R;
import top.eiyooooo.easycontrol.app.databinding.ItemReconnectBinding;
import top.eiyooooo.easycontrol.app.databinding.ModuleDialogBinding;
import top.eiyooooo.easycontrol.app.entity.AppData;
import top.eiyooooo.easycontrol.app.entity.Device;

public class ReconnectHelper {
    public static boolean status;

    private final Context context;

    public ReconnectHelper(Context c) {
        context = c;
    }

    public static void show(ReconnectHelper reconnectHelper, String uuid, int mode) {
        if (reconnectHelper != null && (status || AppData.setting.getAlwaysFullMode())) {
            AppData.uiHandler.post(() -> reconnectHelper.showDialog(uuid, mode));
        } else if (haveOverlayPermission()) {
            showOverlay(uuid, mode);
        }
    }

    public void showDialog(String uuid, int mode) {
        ItemReconnectBinding reconnectView = ItemReconnectBinding.inflate(LayoutInflater.from(context));

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCancelable(true);
        ScrollView dialogView = ModuleDialogBinding.inflate(LayoutInflater.from(context)).getRoot();
        dialogView.addView(reconnectView.getRoot());
        dialogView.setPadding(0, 0, 0, 0);
        builder.setView(dialogView);
        Dialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.setCancelable(true);
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();

        int reconnectTime;
        try {
            reconnectTime = Integer.parseInt(AppData.setting.getCountdownTime());
        } catch (NumberFormatException ignored) {
            reconnectTime = 0;
        }
        if (reconnectTime == 0 || !status) {
            reconnectView.buttonConfirm.setOnClickListener(v -> {
                DeviceListAdapter.startByUUID(uuid, mode);
                dialog.cancel();
            });
            reconnectView.buttonCancel.setOnClickListener(v -> dialog.cancel());
        } else {
            Runnable countdownRunnable = getCountdownRunnable(reconnectView, reconnectTime, true);
            AppData.uiHandler.post(countdownRunnable);
            reconnectView.buttonConfirm.setOnClickListener(v -> {
                AppData.uiHandler.removeCallbacks(countdownRunnable);
                DeviceListAdapter.startByUUID(uuid, mode);
                dialog.cancel();
            });
            reconnectView.buttonCancel.setOnClickListener(v -> {
                AppData.uiHandler.removeCallbacks(countdownRunnable);
                dialog.cancel();
            });
        }
    }

    public static final HashSet<Device> needStartDefaultUSB = new HashSet<>();
    public static boolean showingUSBDialog;

    public void showUSBDialog() {
        showingUSBDialog = true;
        ItemReconnectBinding reconnectView = ItemReconnectBinding.inflate(LayoutInflater.from(context));
        reconnectView.text.setText(AppData.main.getString(R.string.tip_default_usb));

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCancelable(true);
        ScrollView dialogView = ModuleDialogBinding.inflate(LayoutInflater.from(context)).getRoot();
        dialogView.addView(reconnectView.getRoot());
        dialogView.setPadding(0, 0, 0, 0);
        builder.setView(dialogView);
        Dialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.setCancelable(true);
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
        dialog.setOnCancelListener((dialog1) -> {
            needStartDefaultUSB.clear();
            showingUSBDialog = false;
        });
        dialog.show();

        int waitTime;
        try {
            waitTime = Integer.parseInt(AppData.setting.getCountdownTime());
        } catch (NumberFormatException ignored) {
            waitTime = 0;
        }
        if (waitTime == 0) {
            reconnectView.buttonConfirm.setOnClickListener(v -> {
                Iterator<Device> iterator = needStartDefaultUSB.iterator();
                while (iterator.hasNext()) {
                    Device device = iterator.next();
                    DeviceListAdapter.startDevice(device, AppData.setting.getTryStartDefaultInAppTransfer() ? 1 : 0);
                    iterator.remove();
                }
                dialog.cancel();
            });
            reconnectView.buttonCancel.setOnClickListener(v -> dialog.cancel());
        } else {
            Runnable countdownRunnable = getCountdownRunnable(reconnectView, waitTime, true);
            AppData.uiHandler.post(countdownRunnable);
            reconnectView.buttonConfirm.setOnClickListener(v -> {
                AppData.uiHandler.removeCallbacks(countdownRunnable);
                Iterator<Device> iterator = needStartDefaultUSB.iterator();
                while (iterator.hasNext()) {
                    Device device = iterator.next();
                    DeviceListAdapter.startDevice(device, AppData.setting.getTryStartDefaultInAppTransfer() ? 1 : 0);
                    iterator.remove();
                }
                dialog.cancel();
            });
            reconnectView.buttonCancel.setOnClickListener(v -> {
                AppData.uiHandler.removeCallbacks(countdownRunnable);
                dialog.cancel();
            });
        }
    }

    public static void showOverlay(String uuid, int mode) {
        ItemReconnectBinding reconnectView = ItemReconnectBinding.inflate(LayoutInflater.from(AppData.main));
        WindowManager.LayoutParams reconnectViewParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                PixelFormat.TRANSLUCENT
        );
        reconnectViewParams.gravity = Gravity.CENTER;
        AppData.windowManager.addView(reconnectView.getRoot(), reconnectViewParams);

        int reconnectTime;
        try {
            reconnectTime = Integer.parseInt(AppData.setting.getCountdownTime());
        } catch (NumberFormatException ignored) {
            reconnectTime = 0;
        }
        if (reconnectTime == 0) {
            reconnectView.buttonConfirm.setOnClickListener(v -> {
                DeviceListAdapter.startByUUID(uuid, mode);
                removeViewSafely(reconnectView.getRoot());
            });
            reconnectView.buttonCancel.setOnClickListener(v -> removeViewSafely(reconnectView.getRoot()));
        } else {
            Runnable countdownRunnable = getCountdownRunnable(reconnectView, reconnectTime, false);
            AppData.uiHandler.post(countdownRunnable);
            reconnectView.buttonConfirm.setOnClickListener(v -> {
                AppData.uiHandler.removeCallbacks(countdownRunnable);
                DeviceListAdapter.startByUUID(uuid, mode);
                removeViewSafely(reconnectView.getRoot());
            });
            reconnectView.buttonCancel.setOnClickListener(v -> {
                AppData.uiHandler.removeCallbacks(countdownRunnable);
                removeViewSafely(reconnectView.getRoot());
            });
        }
    }

    private static Runnable getCountdownRunnable(ItemReconnectBinding reconnectView, int reconnectTime, boolean detectStatus) {
        final int[] secondsLeft = {reconnectTime};
        return new Runnable() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                if (detectStatus && !status) {
                    reconnectView.buttonConfirm.setText(AppData.main.getString(R.string.confirm));
                    return;
                }
                if (secondsLeft[0] > 0) {
                    reconnectView.buttonConfirm.setText(AppData.main.getString(R.string.confirm) + " (" + secondsLeft[0] + "s)");
                    secondsLeft[0]--;
                    AppData.uiHandler.postDelayed(this, 1000);
                } else {
                    reconnectView.buttonConfirm.performClick();
                }
            }
        };
    }

    private static void removeViewSafely(View view) {
        if (view.isAttachedToWindow()) {
            AppData.windowManager.removeView(view);
        }
    }

    private static boolean haveOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) return Settings.canDrawOverlays(AppData.main);
        else return PublicTools.checkOpNoThrow(AppData.main, "OP_SYSTEM_ALERT_WINDOW", 24);
    }
}