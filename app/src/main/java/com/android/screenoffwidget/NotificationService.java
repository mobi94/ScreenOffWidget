package com.android.screenoffwidget;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.annotation.Nullable;

public class NotificationService extends Service {

    public CheckAdminActive checkAdminActive;
    public static final String ACTION_NOTIFICATION_CLICKED = "ACTION_NOTIFICATION_CLICKED";

    private final BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
            case ACTION_NOTIFICATION_CLICKED:
                if (checkAdminActive.isAdminActive()) checkAdminActive.lockTheScreen();
                else startActivity(checkAdminActive.getIntentToEnableActiveAdmin().setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                break;
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        checkAdminActive = new CheckAdminActive(getApplicationContext());
        registerReceiver(myReceiver, new IntentFilter(ACTION_NOTIFICATION_CLICKED));
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(myReceiver);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
