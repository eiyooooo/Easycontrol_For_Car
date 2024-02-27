package top.eiyooooo.easycontrol.app.helper;

import static android.content.ClipDescription.MIMETYPE_TEXT_PLAIN;

import android.app.Dialog;
import android.content.ClipData;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.Toast;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

import top.eiyooooo.easycontrol.app.client.Client;
import top.eiyooooo.easycontrol.app.entity.AppData;
import top.eiyooooo.easycontrol.app.entity.Device;
import top.eiyooooo.easycontrol.app.R;
import top.eiyooooo.easycontrol.app.databinding.ItemDevicesItemBinding;
import top.eiyooooo.easycontrol.app.databinding.ItemDevicesItemDetailBinding;
import top.eiyooooo.easycontrol.app.databinding.ItemSetDeviceBinding;

public class DeviceListAdapter extends BaseExpandableListAdapter {

  public final ArrayList<Device> devicesList = new ArrayList<>();
  public final HashMap<String, UsbDevice> linkDevices = new HashMap<>();
  private final Context context;
  private final ExpandableListView expandableListView;


  public DeviceListAdapter(Context c, ExpandableListView expandableListView) {
    this.expandableListView = expandableListView;
    queryDevices();
    context = c;
  }

  @Override
  public int getGroupCount() {
    return devicesList.size();
  }

  @Override
  public int getChildrenCount(int groupPosition) {
    return 1;
  }

  @Override
  public Object getGroup(int groupPosition) {
    return null;
  }

  @Override
  public Object getChild(int groupPosition, int childPosition) {
    return null;
  }

  @Override
  public long getGroupId(int groupPosition) {
    return 0;
  }

  @Override
  public long getChildId(int groupPosition, int childPosition) {
    return 0;
  }

  @Override
  public boolean hasStableIds() {
    return false;
  }

  @Override
  public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
    if (convertView == null) {
      ItemDevicesItemBinding devicesItemBinding = ItemDevicesItemBinding.inflate(LayoutInflater.from(context));
      convertView = devicesItemBinding.getRoot();
      convertView.setTag(devicesItemBinding);
    }
    // 获取设备
    Device device = devicesList.get(groupPosition);
    if (device.connection == -1) checkConnection(device);
    setView(convertView, device, isExpanded, groupPosition);
    return convertView;
  }

  @Override
  public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
    if (convertView == null) {
      ItemDevicesItemDetailBinding devicesItemDetailBinding = ItemDevicesItemDetailBinding.inflate(LayoutInflater.from(context));
      convertView = devicesItemDetailBinding.getRoot();
      convertView.setTag(devicesItemDetailBinding);
    }
    // 获取设备
    Device device = devicesList.get(groupPosition);
    setChildView(convertView, device);
    return convertView;
  }

  @Override
  public boolean isChildSelectable(int groupPosition, int childPosition) {
    return false;
  }

  // 创建主View
  private void setView(View view, Device device, boolean isExpanded, int groupPosition) {
    ItemDevicesItemBinding devicesItemBinding = (ItemDevicesItemBinding) view.getTag();
    // 设置展开图标
    devicesItemBinding.deviceExpand.setRotation(isExpanded ? 270 : 180);
    // 设置卡片值
    if (device.isLinkDevice())
      devicesItemBinding.deviceIcon.setImageResource(R.drawable.link);
    else if (device.connection == 0)
      devicesItemBinding.deviceIcon.setImageResource(R.drawable.wifi_checking_connection);
    else if (device.connection == 1)
      devicesItemBinding.deviceIcon.setImageResource(R.drawable.wifi_can_connect);
    else
      devicesItemBinding.deviceIcon.setImageResource(R.drawable.wifi_can_not_connect);
    devicesItemBinding.deviceName.setText(device.name);
    // 单击事件
    devicesItemBinding.getRoot().setOnClickListener(v -> {
      if (expandableListView.isGroupExpanded(groupPosition))
        expandableListView.collapseGroup(groupPosition);
      else
        expandableListView.expandGroup(groupPosition);
    });
    // 长按事件
    devicesItemBinding.getRoot().setOnLongClickListener(v -> {
      onLongClickCard(device);
      return true;
    });
  }

  // 创建子View
  private void setChildView(View view, Device device) {
    ItemDevicesItemDetailBinding devicesItemDetailBinding = (ItemDevicesItemDetailBinding) view.getTag();
    // 设置卡片值
    devicesItemDetailBinding.isAudio.setChecked(device.isAudio);
    devicesItemDetailBinding.defaultFull.setChecked(device.defaultFull);
    // 单击事件
    devicesItemDetailBinding.isAudio.setOnCheckedChangeListener((buttonView, isChecked) -> {
      device.isAudio = isChecked;
      AppData.dbHelper.update(device);
    });
    View isAudioParent = (View) devicesItemDetailBinding.isAudio.getParent();
    isAudioParent.setOnClickListener(v -> devicesItemDetailBinding.isAudio.toggle());
    devicesItemDetailBinding.defaultFull.setOnCheckedChangeListener((buttonView, isChecked) -> {
      device.defaultFull = isChecked;
      AppData.dbHelper.update(device);
    });
    View defaultFullParent = (View) devicesItemDetailBinding.defaultFull.getParent();
    defaultFullParent.setOnClickListener(v -> devicesItemDetailBinding.defaultFull.toggle());
    devicesItemDetailBinding.displayMirroring.setOnClickListener(v -> startDevice(device, 0));
    devicesItemDetailBinding.createDisplay.setOnClickListener(v -> startDevice(device, 1));
  }

  // 检查连接
  private final Object checkingConnection = new Object();
  private Thread checkingConnectionThread;
  private void checkConnection(Device device) {
    device.connection = 0;

    if (checkingConnectionThread != null) checkingConnectionThread.interrupt();
    checkingConnectionThread = new Thread(() -> {
      try {
        Thread.sleep(1000);
        synchronized (checkingConnection) {
          checkingConnection.notifyAll();
        }
      } catch (InterruptedException ignored) {
      }
    });
    checkingConnectionThread.start();

    new Thread(() -> {
      try {
        if (!device.isLinkDevice()) {
          Pair<String, Integer> address = PublicTools.getIpAndPort(device.address);
          Socket socket = new Socket();
          socket.connect(new InetSocketAddress(address.first, address.second), 800);
          socket.close();
        }
        synchronized (checkingConnection) {
          checkingConnection.wait();
          if (device.connection == 0) {
            device.connection = 1;
            for (Device d : devicesList) {
              if (d.uuid.equals(device.uuid)) {
                AppData.uiHandler.post(() -> expandableListView.collapseGroup(devicesList.indexOf(d)));
                AppData.uiHandler.postDelayed(() -> expandableListView.expandGroup(devicesList.indexOf(d)), 500);
              }
            }
          }
        }
      } catch (Exception ignored) {
        device.connection = 2;
      }
    }).start();
  }

  // 卡片长按事件
  private void onLongClickCard(Device device) {
    ItemSetDeviceBinding itemSetDeviceBinding = ItemSetDeviceBinding.inflate(LayoutInflater.from(context));
    Dialog dialog = PublicTools.createDialog(context, true, itemSetDeviceBinding.getRoot());
    // 有线设备
    if (device.isLinkDevice()) {
      itemSetDeviceBinding.buttonStartWireless.setVisibility(View.VISIBLE);
      itemSetDeviceBinding.buttonSetDefault.setText(R.string.set_device_button_set_default_link);
      itemSetDeviceBinding.buttonStartWireless.setOnClickListener(v -> {
        dialog.cancel();
        UsbDevice usbDevice = linkDevices.get(device.uuid);
        if (usbDevice == null) return;
        Client.restartOnTcpip(device, usbDevice, result -> AppData.uiHandler.post(() -> Toast.makeText(AppData.main, AppData.main.getString(result ? R.string.set_device_button_start_wireless_success : R.string.set_device_button_recover_error), Toast.LENGTH_SHORT).show()));
      });
      itemSetDeviceBinding.buttonSetDefault.setOnClickListener(v -> {
        dialog.cancel();
        if (!device.isLinkDevice()) return;
        AppData.setting.setDefaultUsbDevice(device.uuid);
      });
    } else {
      itemSetDeviceBinding.buttonStartWireless.setVisibility(View.GONE);
      itemSetDeviceBinding.buttonSetDefault.setText(R.string.set_device_button_set_default);

      itemSetDeviceBinding.buttonSetDefault.setOnClickListener(v -> {
        dialog.cancel();
        if (!device.isNormalDevice()) return;
        AppData.setting.setDefaultDevice(device.uuid);
      });
    }
    itemSetDeviceBinding.buttonRecover.setOnClickListener(v -> {
      dialog.cancel();
      if (device.isLinkDevice()) {
        UsbDevice usbDevice = linkDevices.get(device.uuid);
        if (usbDevice == null) return;
        Client.runOnceCmd(device, usbDevice, "wm size reset", result -> AppData.uiHandler.post(() -> Toast.makeText(AppData.main, AppData.main.getString(result ? R.string.set_device_button_recover_success : R.string.set_device_button_recover_error), Toast.LENGTH_SHORT).show()));
      } else Client.runOnceCmd(device, null, "wm size reset", result -> AppData.uiHandler.post(() -> Toast.makeText(AppData.main, AppData.main.getString(result ? R.string.set_device_button_recover_success : R.string.set_device_button_recover_error), Toast.LENGTH_SHORT).show()));
    });
    itemSetDeviceBinding.buttonGetUuid.setOnClickListener(v -> {
      dialog.cancel();
      AppData.clipBoard.setPrimaryClip(ClipData.newPlainText(MIMETYPE_TEXT_PLAIN, device.uuid));
      Toast.makeText(AppData.main, AppData.main.getString(R.string.set_device_button_get_uuid_success), Toast.LENGTH_SHORT).show();
    });
    itemSetDeviceBinding.buttonChange.setOnClickListener(v -> {
      dialog.cancel();
      PublicTools.createAddDeviceView(context, device, this).show();
    });
    itemSetDeviceBinding.buttonDelete.setOnClickListener(v -> {
      AppData.dbHelper.delete(device);
      update();
      dialog.cancel();
    });
    dialog.show();
  }

  private void queryDevices() {
    ArrayList<Device> rawDevices = AppData.dbHelper.getAll();
    ArrayList<Device> tmp1 = new ArrayList<>();
    ArrayList<Device> tmp2 = new ArrayList<>();
    for (Device device : rawDevices) {
      if (device.isLinkDevice() && linkDevices.containsKey(device.uuid)) tmp1.add(device);
      else if (device.isNormalDevice()) tmp2.add(device);
    }
    devicesList.clear();
    devicesList.addAll(tmp1);
    devicesList.addAll(tmp2);
  }

  public void startByUUID(String uuid) {
    for (Device device : devicesList) {
      if (Objects.equals(device.uuid, uuid)) startDevice(device);
    }
  }

  public void startDevice(Device device) {
    startDevice(device, 0);
  }

  public void startDevice(Device device, int mode) {
    if (device.isLinkDevice()) {
      UsbDevice usbDevice = linkDevices.get(device.uuid);
      if (usbDevice == null) return;
      new Client(device, usbDevice, mode);
    } else new Client(device, null, mode);
  }

  public void update() {
    for (int i = 0; i < devicesList.size(); i++)
      expandableListView.collapseGroup(i);
    queryDevices();
    notifyDataSetChanged();
  }

}
