package com.sergeystasyuk.screenoffwidget;

import android.app.ActivityManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class MyWidgetProvider extends AppWidgetProvider {

    public static final String MyOnClick = "myOnClickTag";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
                         int[] appWidgetIds) {

        ComponentName thisWidget = new ComponentName(context, MyWidgetProvider.class);
        int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        for (int widgetId : allWidgetIds) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
                    R.layout.widget_layout);
            remoteViews.setOnClickPendingIntent(R.id.update, getPendingSelfIntent(context, MyOnClick));
            appWidgetManager.updateAppWidget(widgetId, remoteViews);

        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass, Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    protected PendingIntent getPendingSelfIntent(Context context, String action) {
        Intent intent = new Intent(context, MyWidgetService.class);
        intent.setAction(action);
        return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
    }

    @Override
    public void onReceive(Context context, Intent intent){
        if(intent.getAction().equals(MyOnClick)) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
                    R.layout.widget_layout);
            // Create a fresh intent
            Intent serviceIntent = new Intent(context, MyWidgetService.class);
            if(isMyServiceRunning(MyWidgetService.class, context)) {
                context.stopService(serviceIntent);
                context.startService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
            ComponentName componentName = new ComponentName(context, MyWidgetService.class);
            AppWidgetManager.getInstance(context).updateAppWidget(componentName, remoteViews);
        }
        else if (intent.getAction().equals(AppWidgetManager.ACTION_APPWIDGET_DISABLED)){
            Intent serviceIntent = new Intent(context, MyWidgetService.class);
            if(isMyServiceRunning(MyWidgetService.class, context)) context.stopService(serviceIntent);
        }
        super.onReceive(context, intent);
    }

}