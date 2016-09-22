package com.android.screenoffwidget;

import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
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
                else context.startActivity(new Intent()
                            .setComponent(new ComponentName("com.android.settings", "com.android.settings.DeviceAdminSettings"))
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
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
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(LockScreenActivity.MY_NOTIFICATION_ID);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
