package top.eiyooooo.easycontrol.app.helper;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.widget.ScrollView;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;

import top.eiyooooo.easycontrol.app.R;
import top.eiyooooo.easycontrol.app.databinding.ItemReconnectBinding;
import top.eiyooooo.easycontrol.app.databinding.ModuleDialogBinding;
import top.eiyooooo.easycontrol.app.entity.AppData;
import top.eiyooooo.easycontrol.app.entity.Device;

public class ConnectHelper {
    public static boolean status;

    private final Context context;

    public ConnectHelper(Context c) {
        context = c;
    }

    public static void show(ConnectHelper connectHelper, String uuid, int mode) {
        if (connectHelper != null) {
            AppData.uiHandler.post(() -> connectHelper.showDialog(uuid, mode));
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
            Runnable countdownRunnable = getCountdownRunnable(reconnectView, reconnectTime);
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
    public final Runnable showStartDefaultUSB = new Runnable() {
        @Override
        public void run() {
            if (status && !needStartDefaultUSB.isEmpty() && !showingUSBDialog) {
                showUSBDialog();
            }
        }
    };

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
            Runnable countdownRunnable = getCountdownRunnable(reconnectView, waitTime);
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

    private static Runnable getCountdownRunnable(ItemReconnectBinding reconnectView, int reconnectTime) {
        final int[] secondsLeft = {reconnectTime};
        return new Runnable() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                if (!status) {
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
}