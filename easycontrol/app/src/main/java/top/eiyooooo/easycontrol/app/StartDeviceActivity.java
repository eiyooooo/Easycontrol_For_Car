package top.eiyooooo.easycontrol.app;

import android.app.Activity;
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
        AppData.init(this);
        startDevice();
        finish();
    }

    private void startDevice() {
        String uuid = getIntent().getStringExtra("uuid");

        if (uuid != null) {
            Device device = AppData.dbHelper.getByUUID(uuid);
            UsbDevice usbDevice = null;
            if (device.isLinkDevice()) {
                if (DeviceListAdapter.linkDevices.containsKey(device.uuid)) {
                    usbDevice = DeviceListAdapter.linkDevices.get(device.uuid);
                } else {
                    Toast.makeText(this, getString(R.string.error_device_not_found), Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            int mode = AppData.setting.getTryStartDefaultInAppTransfer() || (device.specified_app != null && !device.specified_app.isEmpty()) ? 1 : 0;
            new Client(device, usbDevice, mode);
        } else {
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
            if (!found) {
                Toast.makeText(this, getString(R.string.error_default_device_not_found), Toast.LENGTH_SHORT).show();
                return;
            }
        }
        DeviceListAdapter.startedDefault = true;
    }
}