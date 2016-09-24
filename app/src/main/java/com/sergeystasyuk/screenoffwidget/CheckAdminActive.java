package com.sergeystasyuk.screenoffwidget;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

class CheckAdminActive {

    private DevicePolicyManager deviceManger;
    private ComponentName compName;

    CheckAdminActive(Context context){
        deviceManger = (DevicePolicyManager)context.getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        compName = new ComponentName(context, MyAdmin.class);
    }

    boolean isAdminActive(){
        return  deviceManger.isAdminActive(compName);
    }

    void lockTheScreen(){
        deviceManger.lockNow();
    }

    Intent getIntentToEnableActiveAdmin(){
        Intent intent = new Intent(DevicePolicyManager
                .ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                compName);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                App.getContext().getResources().getString(R.string.admin_permission_explanation));
        return intent;
    }

    void disableActiveAdmin(){
        deviceManger.removeActiveAdmin(compName);
    }
}
