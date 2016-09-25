package com.sergeystasyuk.screenoffwidget;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

public class MyWidgetService extends Service {
    public CheckAdminActive checkAdminActive;

    @Override
    public void onCreate() {
        super.onCreate();
        checkAdminActive = new CheckAdminActive(getApplicationContext());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (checkAdminActive.isAdminActive()) checkAdminActive.lockTheScreen();
        else startActivity(checkAdminActive.getIntentToEnableActiveAdmin().setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        stopSelf();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
