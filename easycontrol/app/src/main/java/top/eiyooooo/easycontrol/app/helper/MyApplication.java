package top.eiyooooo.easycontrol.app.helper;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.lsposed.hiddenapibypass.HiddenApiBypass;
import top.eiyooooo.easycontrol.app.client.Client;
import top.eiyooooo.easycontrol.app.entity.AppData;

public class MyApplication extends Application implements Application.ActivityLifecycleCallbacks {
    private Thread releaseThread;
    private int activityCount = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(this);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            HiddenApiBypass.addHiddenApiExemptions("L");
        }
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        activityCount++;
        if (releaseThread != null) releaseThread.interrupt();
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {

    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {

    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {

    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        activityCount--;
        if (activityCount == 0) {
            releaseThread = new Thread(() -> {
                while (activityCount == 0 && !Thread.interrupted()) {
                    try {
                        Thread.sleep(12000);
                        if (Client.allClient.isEmpty()) {
                            AppData.release();
                            return;
                        }
                    } catch (Exception ignored) {
                    }
                }
            });
            releaseThread.start();
        }
    }
}