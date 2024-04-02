package top.eiyooooo.easycontrol.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.widget.Toast;
import top.eiyooooo.easycontrol.app.client.Client;
import top.eiyooooo.easycontrol.app.entity.AppData;
import top.eiyooooo.easycontrol.app.entity.Device;
import top.eiyooooo.easycontrol.app.helper.DeviceListAdapter;

public class StartDeviceActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String uuid = getIntent().getStringExtra("uuid");

        if (AppData.main == null) AppData.init(this);

        if (uuid != null) {
            Device device = AppData.dbHelper.getByUUID(uuid);
            UsbDevice usbDevice = null;
            if (device.isLinkDevice()) {
                queryUSB();
                if (DeviceListAdapter.linkDevices.containsKey(device.uuid)) {
                    usbDevice = DeviceListAdapter.linkDevices.get(device.uuid);
                } else {
                    Toast.makeText(this, AppData.main.getString(R.string.error_device_not_found), Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
            }
            int mode = AppData.setting.getTryStartDefaultInAppTransfer() || (device.specified_app != null && !device.specified_app.isEmpty()) ? 1 : 0;
            new Client(device, usbDevice, mode);
        } else {
            queryUSB();
            boolean found = false;
            for (Device device : AppData.dbHelper.getAll()) {
                UsbDevice usbDevice = null;
                if (!device.connectOnStart) continue;
                if (device.isLinkDevice()) {
                    if (DeviceListAdapter.linkDevices.containsKey(device.uuid)) {
                        usbDevice = DeviceListAdapter.linkDevices.get(device.uuid);
                    } else continue;
                }
                found = true;
                new Client(device, usbDevice, AppData.setting.getTryStartDefaultInAppTransfer() ? 1 : 0);
            }
            if (!found) Toast.makeText(this, AppData.main.getString(R.string.error_default_device_not_found), Toast.LENGTH_SHORT).show();
        }
        DeviceListAdapter.startedDefault = true;
        finish();
    }

    @SuppressLint("MutableImplicitPendingIntent")
    private void queryUSB() {
        for (String k : AppData.usbManager.getDeviceList().keySet()) {
            UsbDevice device = AppData.usbManager.getDeviceList().get(k);
            if (!AppData.usbManager.hasPermission(device)) {
                AppData.usbManager.requestPermission(
                        device,
                        PendingIntent.getBroadcast(getApplicationContext(),
                                0,
                                new Intent("top.eiyooooo.easycontrol.app.USB_PERMISSION"),
                                PendingIntent.FLAG_MUTABLE));
            } else if (device != null) {
                DeviceListAdapter.linkDevices.put(device.getSerialNumber(), device);
            }
        }
    }
}