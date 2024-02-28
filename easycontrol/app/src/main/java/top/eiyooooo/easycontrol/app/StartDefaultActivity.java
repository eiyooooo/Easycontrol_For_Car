package top.eiyooooo.easycontrol.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;
import top.eiyooooo.easycontrol.app.entity.AppData;
import top.eiyooooo.easycontrol.app.client.Client;
import top.eiyooooo.easycontrol.app.entity.Device;

import static top.eiyooooo.easycontrol.app.client.Client.allClient;

public class StartDefaultActivity extends Activity {
  @SuppressLint("StaticFieldLeak")
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // 判断是否已经启动
    for (Client client : allClient) {
      if (AppData.setting.getDefaultDevice().equals(client.uuid)) {
        Toast.makeText(this, "默认设备已启动", Toast.LENGTH_SHORT).show();
        finish();
        return;
      }
    }

    // 初始化
    if (AppData.main == null) AppData.init(this);
    // 启动默认设备
    if (!AppData.setting.getDefaultDevice().equals("")) {
      Device device = AppData.dbHelper.getByUUID(AppData.setting.getDefaultDevice());
      if (device != null) {
        Toast.makeText(this, "通过此方法启动时某些功能无法正确工作，建议通过打开app启动", Toast.LENGTH_SHORT).show();
        new Client(device, null);
      }
      else {
        Toast.makeText(this, "默认设备不存在", Toast.LENGTH_SHORT).show();
        finish();
        return;
      }
    }
    else {
      Toast.makeText(this, "未设置默认设备", Toast.LENGTH_SHORT).show();
      finish();
      return;
    }

    // 等待启动
    new Thread(() -> {
      while (true) {
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        // 判断是否已经启动
        for (Client client : allClient) {
          if (AppData.setting.getDefaultDevice().equals(client.uuid)) {
            if (client.isStarted()) {
              try {
                Thread.sleep(500);
              } catch (InterruptedException e) {
                e.printStackTrace();
              }
              finish();
              return;
            }
          }
        }
      }
    }).start();
  }
}