package top.eiyooooo.easycontrol.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;

import java.io.InputStream;

import top.eiyooooo.easycontrol.app.adb.AdbPairManager;
import top.eiyooooo.easycontrol.app.databinding.ActivityPairBinding;
import top.eiyooooo.easycontrol.app.entity.AppData;
import top.eiyooooo.easycontrol.app.helper.PublicTools;

public class PairActivity extends Activity {
    private ActivityPairBinding pairActivity;

    @SuppressLint({"SetJavaScriptEnabled", "SetTextI18n"})
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PublicTools.setStatusAndNavBar(this);
        PublicTools.setLocale(this);
        pairActivity = ActivityPairBinding.inflate(this.getLayoutInflater());
        setContentView(pairActivity.getRoot());
        pairActivity.backButton.setOnClickListener(v -> finish());
        String url = "file:///android_asset/pair.html";
        if (String.valueOf(pairActivity.nightModeDetector.getText()).contains("EasyControl")) {
            String tempUrl = url.replace(".html", "_en.html");
            if (ifAssetExists(tempUrl)) url = tempUrl;
        }
        if (pairActivity.nightModeDetector.getCurrentTextColor() == 0xFFDEDEDE) {
            String tempUrl = url.replace(".html", "_dark.html");
            if (ifAssetExists(tempUrl)) url = tempUrl;
        }
        if (!ifAssetExists(url)) {
            finish();
            return;
        }
        pairActivity.webview.loadUrl(url);
        WebSettings webSettings = pairActivity.webview.getSettings();
        webSettings.setJavaScriptEnabled(true);
        pairActivity.webview.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                pairActivity.webview.setVisibility(View.VISIBLE);
            }
        });

        if (pairActivity.panel != null) {
            pairActivity.panel.post(() -> {
                DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
                int screenHeight = displayMetrics.heightPixels;
                int panelHeight = pairActivity.panel.getHeight();
                if (panelHeight > screenHeight * 4 / 10) {
                    RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) pairActivity.panel.getLayoutParams();
                    layoutParams.height = screenHeight * 4 / 10;
                    pairActivity.panel.setLayoutParams(layoutParams);
                }
            });
        }

        AppData.uiHandler.post(() -> {
            try {
                AdbPairManager.init();
                pairActivity.certName.setText(AdbPairManager.keyName);
            } catch (Exception ignored) {
                pairActivity.certName.setText(R.string.pair_no_cert);
            }
            pairActivity.certRegenerate.setClickable(true);
        });

        pairActivity.runPair.setOnClickListener(v -> new Thread(() -> {
            if (AdbPairManager.INSTANCE == null) {
                PublicTools.logToast(getString(R.string.pair_no_cert));
                return;
            }
            pairActivity.pairing.post(() -> pairActivity.pairing.setText(getString(R.string.pair_pair) + "..."));

            String ip = String.valueOf(pairActivity.ip.getText());
            String pairingPort = String.valueOf(pairActivity.pairingPort.getText());
            String pairingCode = String.valueOf(pairActivity.pairingCode.getText());

            try {
                if (ip.isEmpty() || pairingPort.isEmpty() || pairingCode.isEmpty()) throw new Exception();
                AdbPairManager.INSTANCE.pair(ip, Integer.parseInt(pairingPort), pairingCode);
                pairActivity.pairing.post(() -> pairActivity.pairing.setText(getString(R.string.pair_pair) + getString(R.string.pair_success)));
            } catch (Exception ignored) {
                pairActivity.pairing.post(() -> pairActivity.pairing.setText(getString(R.string.pair_pair) + getString(R.string.pair_failed)));
            }
        }).start());

        pairActivity.runOpenPort.setOnClickListener(v -> new Thread(() -> {
            if (AdbPairManager.INSTANCE == null) {
                PublicTools.logToast(getString(R.string.pair_no_cert));
                return;
            }
            pairActivity.openingPort.post(() -> pairActivity.openingPort.setText(getString(R.string.pair_open_port) + "..."));

            String ip = String.valueOf(pairActivity.ip.getText());
            String debugPort = String.valueOf(pairActivity.debugPort.getText());

            try {
                if (ip.isEmpty() || debugPort.isEmpty()) throw new Exception();
                AdbPairManager.INSTANCE.connect(ip, Integer.parseInt(debugPort));
                AdbPairManager.INSTANCE.openStream("tcpip:5555");
                AdbPairManager.INSTANCE.disconnect();
                pairActivity.openingPort.post(() -> pairActivity.openingPort.setText(getString(R.string.pair_open_port) + getString(R.string.pair_success)));
            } catch (Exception ignored) {
                pairActivity.openingPort.post(() -> pairActivity.openingPort.setText(getString(R.string.pair_open_port) + getString(R.string.pair_failed)));
            }
        }).start());

        pairActivity.certRegenerate.setOnClickListener(v -> {
            pairActivity.certRegenerate.setClickable(false);
            pairActivity.certName.setText(R.string.pair_generating_cert);
            AppData.uiHandler.post(() -> {
                try {
                    AdbPairManager.regenerateKey();
                    pairActivity.certName.setText(AdbPairManager.keyName);
                } catch (Exception ignored) {
                    pairActivity.certName.setText(R.string.pair_no_cert);
                }
                pairActivity.certRegenerate.setClickable(true);
            });
        });
    }

    public boolean ifAssetExists(String filename) {
        AssetManager assetManager = getAssets();
        try {
            InputStream inputStream;
            if (filename.contains("file:///android_asset/")) {
                inputStream = assetManager.open(filename.substring(22));
            } else {
                inputStream = assetManager.open(filename);
            }
            inputStream.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}