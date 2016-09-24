package com.sergeystasyuk.screenoffwidget;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class MyAdmin extends DeviceAdminReceiver {

    void showToast(Context context, CharSequence msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onEnabled(Context context, Intent intent) {
        showToast(context, context.getResources().getString(R.string.admin_permission_enabled));
    }

    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {
        return context.getResources().getString(R.string.admin_permission_disable_request);
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        showToast(context, context.getResources().getString(R.string.admin_permission_disabled));
    }

}