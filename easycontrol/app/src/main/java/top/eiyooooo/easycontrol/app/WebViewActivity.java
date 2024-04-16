package top.eiyooooo.easycontrol.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import top.eiyooooo.easycontrol.app.databinding.ActivityWebViewBinding;
import top.eiyooooo.easycontrol.app.helper.PublicTools;

public class WebViewActivity extends Activity {
    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PublicTools.setStatusAndNavBar(this);
        PublicTools.setLocale(this);
        ActivityWebViewBinding webViewActivity = ActivityWebViewBinding.inflate(this.getLayoutInflater());
        setContentView(webViewActivity.getRoot());
        webViewActivity.backButton.setOnClickListener(v -> finish());
        String url = getIntent().getStringExtra("url");
        if (url == null) finish();
        else {
            if (webViewActivity.nightModeDetector.getCurrentTextColor() == 0xFFDEDEDE)
                url = url.replace(".html", "_dark.html");
            webViewActivity.webview.loadUrl(url);
            WebSettings webSettings = webViewActivity.webview.getSettings();
            webSettings.setJavaScriptEnabled(true);
            webViewActivity.webview.setWebViewClient(new WebViewClient(){
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    webViewActivity.webview.setVisibility(View.VISIBLE);
                }
            });
        }
    }
}