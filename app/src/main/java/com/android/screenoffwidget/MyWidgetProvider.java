package com.android.screenoffwidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class MyWidgetProvider extends AppWidgetProvider {

    private static final String MyOnClick = "myOnClickTag";
    public CheckAdminActive checkAdminActive;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
                         int[] appWidgetIds) {

        ComponentName thisWidget = new ComponentName(context, MyWidgetProvider.class);
        int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        for (int widgetId : allWidgetIds) {

            //int number = (new Random().nextInt(100));

            RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
                    R.layout.widget_layout);

            //remoteViews.setTextViewText(R.id.update, String.valueOf(number));

            remoteViews.setOnClickPendingIntent(R.id.update, getPendingSelfIntent(context, MyOnClick));
            appWidgetManager.updateAppWidget(widgetId, remoteViews);

        }
    }

    protected PendingIntent getPendingSelfIntent(Context context, String action) {
        Intent intent = new Intent(context, getClass());
        intent.setAction(action);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        //checkAdminActive = new CheckAdminActive(context);
    }

    @Override
    public void onReceive(Context context, Intent intent){
        super.onReceive(context, intent);
        checkAdminActive = new CheckAdminActive(context);
        switch (intent.getAction()){
            case MyOnClick:
            if (!checkAdminActive.isAdminActive())
                context.startActivity(new Intent()
                        .setComponent(new ComponentName("com.android.settings", "com.android.settings.DeviceAdminSettings"))
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            else
                checkAdminActive.lockTheScreen();
            break;
        }
    }

}