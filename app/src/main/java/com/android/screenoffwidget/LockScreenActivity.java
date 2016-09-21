package com.android.screenoffwidget;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.analytics.FirebaseAnalytics;

public class LockScreenActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener  {
    private FirebaseAnalytics mFirebaseAnalytics;

    private SwitchCompat switchFloatButton;
    private SwitchCompat switchNotification;
    private SwitchCompat switchFullscreen;
    private RelativeLayout buttonToHideInApps;
    private ScrollView scrollView;
    private LinearLayout adViewContainer;

    static final int RESULT_ENABLED_FOR_HIDE_IN_APPS = 1;
    static final int RESULT_ENABLED_FOR_FLOAT_BUTTON = 2;
    static final int RESULT_ENABLED_FOR_NOTIFICATION = 3;
    private static final int MY_NOTIFICATION_ID = 1234;
    public static final String ACTION_NOTIFICATION_CLICKED = "ACTION_NOTIFICATION_CLICKED";

    public static final String MyPREFERENCES = "LockScreenActivity";
    public SharedPreferences sharedpreferences;
    public static final String SeekBarScale = "SeekBarScale";
    public static final String FullScreenSwitcher = "FullScreenSwitcher";

    ActivityManager activityManager;
    public CheckAdminActive checkAdminActive;

    public ImageView icon;
    public SeekBar seekBar;
    public final int defaultSeekBarProgress = 50;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setSubtitle(R.string.app_settings);
            actionBar.setLogo(R.mipmap.ic_launcher);
        }

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        adViewContainer = (LinearLayout) findViewById(R.id.adViewContainer);

        registerNetworkBroadcastReceiver();
        setUpAdMob();

        sharedpreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);

        scrollView = (ScrollView)findViewById(R.id.scrollView);

        //bottomView = (ImageView)findViewById(R.id.separator4);
        icon = (ImageView)findViewById(R.id.imageView);
        icon.setScaleType(ImageView.ScaleType.CENTER);

        seekBar = (SeekBar) findViewById(R.id.scale);
        seekBar.setOnSeekBarChangeListener(seekBarChangeListener);

        buttonToHideInApps = (RelativeLayout) findViewById(R.id.button_container);
        buttonToHideInApps.setClickable(false);
        buttonToHideInApps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setUpFirebase();
                if (!isAccessibilitySettingsOn(getApplicationContext())) {
                    Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    startActivityForResult(intent, RESULT_ENABLED_FOR_HIDE_IN_APPS);
                }
                else startActivity(new Intent(LockScreenActivity.this, ApplicationsListActivity.class));
            }
        });

        getSavedSeekBarScale();

        checkAdminActive = new CheckAdminActive(this);
        activityManager = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);

        switchFloatButton = (SwitchCompat) findViewById(R.id.switch_float_button);
        switchNotification = (SwitchCompat) findViewById(R.id.switch_notification);
        switchFullscreen = (SwitchCompat) findViewById(R.id.switch_fullscreen);
        getSavedFullScreenSwitcherCondition();

        updateFloatingButtonSwitcher();
        updateNotificationSwitcher();
        updateHideInAppsButton();

        switchFloatButton.setOnCheckedChangeListener(this);
        switchNotification.setOnCheckedChangeListener(this);
        switchFullscreen.setOnCheckedChangeListener(this);

    }

    private BroadcastReceiver networkStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case ConnectivityManager.CONNECTIVITY_ACTION:
                    if (NetworkUtil.getConnectivityStatus(context) == 0)
                        adViewContainer.setVisibility(View.GONE);
                    else
                        adViewContainer.setVisibility(View.VISIBLE);
                    break;
            }
        }
    };

    public void registerNetworkBroadcastReceiver(){
        registerReceiver(networkStateReceiver, new IntentFilter(
                ConnectivityManager.CONNECTIVITY_ACTION));
    }

    public void setUpAdMob(){
        MobileAds.initialize(this, getResources().getString(R.string.banner_ad_unit_id));
        final AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .addTestDevice("923FD3F5F3D07BA8F35509FFCC5AC800")
                .build();
        mAdView.loadAd(adRequest);
        mAdView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
            }

            @Override
            public void onAdFailedToLoad(int i) {
                super.onAdFailedToLoad(i);
            }
        });
    }

    public void setUpFirebase(){
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "R.id.button_container");
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "buttonToHideInApps");
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "layout");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    private SeekBar.OnSeekBarChangeListener seekBarChangeListener =
            new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress,
                                              boolean fromUser) {
                    float scale = getResources().getDisplayMetrics().density;
                    int size = (int) ((50 * scale + 0.5f) * (0.7f + (float)progress/100f));
                    Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_float_button);
                    Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, size, size, false);
                    int iconWidth = resizedBitmap.getWidth();
                    int iconHeight = resizedBitmap.getHeight();
                    ViewGroup.LayoutParams layoutParams = icon.getLayoutParams();
                    layoutParams.width = iconWidth;
                    layoutParams.height = iconHeight;
                    icon.setLayoutParams(layoutParams);
                    icon.setImageBitmap(resizedBitmap);
                    scrollView.scrollTo(0, scrollView.getBottom());

                    //icon.setPivotX(icon.getWidth()/2);
                    //icon.setPivotY(0);
                    //icon.setScaleX(0.7f + (float)progress/100f);
                    //icon.setScaleY(0.7f + (float)progress/100f);
                }
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            };

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        switch (compoundButton.getId()) {
            case R.id.switch_float_button:
                if (b) {
                    if (checkAdminActive.isAdminActive()) enableFloatButton();
                    else startActivityForResult(checkAdminActive.getIntentToEnableActiveAdmin(), RESULT_ENABLED_FOR_FLOAT_BUTTON);
                }
                else stopService(new Intent(this, FloatButtonService.class));
                updateHideInAppsButton();
                break;
            case R.id.switch_notification:
                if (b) {
                    if (checkAdminActive.isAdminActive()) showNotification();
                    else startActivityForResult(checkAdminActive.getIntentToEnableActiveAdmin(), RESULT_ENABLED_FOR_NOTIFICATION);
                }
                else {
                    NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    notificationManager.cancel(MY_NOTIFICATION_ID);
                    stopService(new Intent(this, NotificationService.class));
                }
                break;
            case R.id.switch_fullscreen:
                saveFullScreenSwitcherCondition(b);
                break;
        }
    }

    private boolean isAccessibilitySettingsOn(Context mContext) {
        int accessibilityEnabled = 0;
        final String service = getPackageName() + "/" + WindowsChangeDetectingService.class.getCanonicalName();
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                    mContext.getApplicationContext().getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
            Log.v("LockScreenActivity", "accessibilityEnabled = " + accessibilityEnabled);
        } catch (Settings.SettingNotFoundException e) {
            Log.e("LockScreenActivity", "Error finding setting, default accessibility to not found: "
                    + e.getMessage());
        }
        TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');

        if (accessibilityEnabled == 1) {
            Log.v("LockScreenActivity", "ACCESSIBILITY IS ENABLED");
            String settingValue = Settings.Secure.getString(
                    mContext.getApplicationContext().getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                mStringColonSplitter.setString(settingValue);
                while (mStringColonSplitter.hasNext()) {
                    String accessibilityService = mStringColonSplitter.next();

                    Log.v("LockScreenActivity", "accessibilityService :: " + accessibilityService + " " + service);
                    if (accessibilityService.equalsIgnoreCase(service)) {
                        Log.v("LockScreenActivity", "We've found the correct setting - accessibility is switched on!");
                        return true;
                    }
                }
            }
        } else {
            Log.v("LockScreenActivity", "***ACCESSIBILITY IS DISABLED***");
        }

        return false;
    }

    public void enableFloatButton(){
        float scale = getResources().getDisplayMetrics().density;
        int pixels = (int) (50 * scale + 0.5f);
        startService(new Intent(this, FloatButtonService.class).
                putExtra("icon_size", pixels * (0.7f + (float)seekBar.getProgress()/100f)));
        updateFloatingButtonSwitcher();
    }
    
    public void showNotification() {
        startService(new Intent(this, NotificationService.class));
        Intent intent = new Intent(ACTION_NOTIFICATION_CLICKED);
        PendingIntent pi = PendingIntent.getBroadcast(this, 0, intent, Intent.FILL_IN_ACTION);
        Resources r = getResources();
        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(android.R.drawable.ic_menu_report_image)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setContentTitle(r.getString(R.string.app_name))
                .setContentText(getResources().getString(R.string.lock_screen_activity_notification_title))
                .setContentIntent(pi)
                .setAutoCancel(false)
                .setOngoing(true)
                .setShowWhen(false)
                .build();

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(MY_NOTIFICATION_ID, notification);
    }

    @Override
    protected void onStop() {
        saveSeekBarScaleToSharedPrefs();
        super.onStop();
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void updateFloatingButtonSwitcher(){
        if(isMyServiceRunning(FloatButtonService.class))
            switchFloatButton.setChecked(true);
        else
            switchFloatButton.setChecked(false);
    }

    private void updateHideInAppsButton(){
        if(isMyServiceRunning(FloatButtonService.class)) {
            buttonToHideInApps.setClickable(true);
            for (int i = 0; i < buttonToHideInApps.getChildCount(); i++)
                buttonToHideInApps.getChildAt(i).setEnabled(true);
        }
        else {
            buttonToHideInApps.setClickable(false);
            for (int i = 0; i < buttonToHideInApps.getChildCount(); i++)
                buttonToHideInApps.getChildAt(i).setEnabled(false);
        }
    }

    private void updateNotificationSwitcher() {
        switchNotification.setChecked(isMyServiceRunning(NotificationService.class));

    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case RESULT_ENABLED_FOR_FLOAT_BUTTON:
                if (resultCode == Activity.RESULT_OK) {
                    enableFloatButton();
                } else {
                    switchFloatButton.setChecked(false);
                }
                updateHideInAppsButton();
                break;
            case RESULT_ENABLED_FOR_NOTIFICATION:
                if (resultCode == Activity.RESULT_OK) {
                    showNotification();
                } else {
                    switchNotification.setChecked(false);
                }
                break;
            case RESULT_ENABLED_FOR_HIDE_IN_APPS:
                if (isAccessibilitySettingsOn(getApplicationContext())) {
                    startActivity(new Intent(LockScreenActivity.this, ApplicationsListActivity.class));
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void saveFullScreenSwitcherCondition(boolean isSwitched){
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putBoolean(FullScreenSwitcher, isSwitched);
        editor.apply();
    }

    public void getSavedFullScreenSwitcherCondition(){
        boolean progress = sharedpreferences.getBoolean(FullScreenSwitcher, false);
        switchFullscreen.setChecked(progress);
    }

    public void saveSeekBarScaleToSharedPrefs(){
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putInt(SeekBarScale, seekBar.getProgress());
        editor.apply();
    }

    public void getSavedSeekBarScale(){
        int progress = sharedpreferences.getInt(SeekBarScale, defaultSeekBarProgress);
        seekBar.setProgress(progress);
    }

}