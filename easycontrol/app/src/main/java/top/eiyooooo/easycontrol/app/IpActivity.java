package top.eiyooooo.easycontrol.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.os.Bundle;
import android.util.Pair;
import android.widget.Toast;

import java.util.ArrayList;

import top.eiyooooo.easycontrol.app.entity.AppData;
import top.eiyooooo.easycontrol.app.helper.PublicTools;
import top.eiyooooo.easycontrol.app.databinding.ActivityIpBinding;
import top.eiyooooo.easycontrol.app.databinding.ItemTextBinding;

public class IpActivity extends Activity {
  private ActivityIpBinding ipActivity;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    PublicTools.setStatusAndNavBar(this);
    PublicTools.setLocale(this);
    ipActivity = ActivityIpBinding.inflate(this.getLayoutInflater());
    setContentView(ipActivity.getRoot());
    setButtonListener();
    // 绘制UI
    drawUi();
    super.onCreate(savedInstanceState);
  }

  private void drawUi() {
    // 添加IP
    Pair<ArrayList<String>, ArrayList<String>> listPair = PublicTools.getIp();
    Context context = this;
    for (String i : listPair.first) {
      ItemTextBinding text = PublicTools.createTextCard(context, i, () -> {
        AppData.clipBoard.setPrimaryClip(ClipData.newPlainText(ClipDescription.MIMETYPE_TEXT_PLAIN, i));
        Toast.makeText(context, getString(R.string.ip_copy), Toast.LENGTH_SHORT).show();
      });
      ipActivity.ipv4.addView(text.getRoot());
    }
    for (String i : listPair.second) {
      ItemTextBinding text = PublicTools.createTextCard(context, i, () -> {
        AppData.clipBoard.setPrimaryClip(ClipData.newPlainText(ClipDescription.MIMETYPE_TEXT_PLAIN, i));
        Toast.makeText(context, getString(R.string.ip_copy), Toast.LENGTH_SHORT).show();
      });
      ipActivity.ipv6.addView(text.getRoot());
    }
    // 扫描局域网地址
    scanAddress(context);
  }

  // 设置返回按钮监听
  private void setButtonListener() {
    ipActivity.backButton.setOnClickListener(v -> finish());
  }

  // 扫描局域网地址
  private void scanAddress(Context context) {
    ipActivity.scanned.removeAllViews();
    ipActivity.scanning.setOnClickListener(null);
    new Thread(() -> {
      ArrayList<String> scannedAddresses = PublicTools.scanAddress();
      AppData.uiHandler.post(() -> {
        if (scannedAddresses.isEmpty()) ipActivity.scanning.setText(R.string.ip_scan_finish_none);
        else ipActivity.scanning.setText(R.string.ip_scan_finish);
        ipActivity.scanning.setOnClickListener(v -> {
          scanAddress(context);
          ipActivity.scanning.setText(R.string.ip_scanning_device);
        });
        for (String i : scannedAddresses) {
          ItemTextBinding text = PublicTools.createTextCard(context, i, () -> {
            AppData.clipBoard.setPrimaryClip(ClipData.newPlainText(ClipDescription.MIMETYPE_TEXT_PLAIN, i));
            Toast.makeText(context, getString(R.string.ip_copy), Toast.LENGTH_SHORT).show();
          });
          ipActivity.scanned.addView(text.getRoot());
        }
      });
    }).start();
  }
}
