package com.sergeystasyuk.screenoffwidget;

import android.animation.ValueAnimator;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;

import com.facebook.rebound.SimpleSpringListener;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringConfig;
import com.facebook.rebound.SpringSystem;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

public class FloatButtonService extends Service {

    private WindowManager windowManager;
    private ImageView lockScreenImageView;
    private ImageView transparentViewToCheckFullScreen;
    private WindowManager.LayoutParams params;
    public CheckAdminActive checkAdminActive;
    private int iconWidth;
    private int iconHeight;
    private int screenHeight;
    private int screenWidth;
    private int statusBarHeight;
    public static final int delta = 50;
    private boolean shouldClick;
    private boolean shouldMove;
    private boolean isScreenOrientationPortrait;
    private boolean shouldDestroy;
    private boolean shouldHide;
    private boolean isLocked;
    private boolean isFullScreenIn;
    private boolean shouldCurrentAppHideIcon;
    private boolean isIconHidingAnimationActive;
    private DisplayMetrics metrics;
    public static final String PortraitX = "portraitX";
    public static final String PortraitY = "portraitY";
    public static final String LandscapeX = "landscapeX";
    public static final String LandscapeY = "landscapeY";
    public static final long iconShiftingAnimationTime = 200;
    public static final long iconHidingAnimationTime = 300;

    public SharedPreferences sharedpreferences;
    public SharedPreferences sharedpreferences2;
    public static final String MyPREFERENCES = "FloatButtonService";
    public static final String FullScreenSwitcher = "FullScreenSwitcher";

    public Runnable postDelayedHidingRunnable;
    public Handler handlerForHidingAnimation;
    /*public ObjectAnimator leftToRightAnimator;
    public ObjectAnimator rightToLeftAnimator;
    public ObjectAnimator alphaAnimator;*/

    private Spring spring;
    public static final double TENSION = 800;
    public static final double DAMPER = 15;

    public static final String ACTION_ON_TOP_ACTIVITY_CHANGED = "topScreenActivityChangedFilter";

    //нужен для снятия флага блокировки экрана
    //и скрытия плаваяющей кнопки при выбранных пользователем программах
    private BroadcastReceiver mScreenStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case Intent.ACTION_USER_PRESENT:
                    isLocked = false;
                    spring.setEndValue(0f);
                    if (shouldMove) shiftIconToScreenSide();
                    break;
                case ACTION_ON_TOP_ACTIVITY_CHANGED:
                    shouldCurrentAppHideIcon = false;
                    String packageName = intent.getStringExtra("packageName");
                    if (packageName != null)
                        getCheckedFromSharedPrefs(packageName);
                    break;
            }
        }
    };

    //используется для возведения флагов shouldHide и shouldClick,
    //а также для вызова метода отсроченной отрисовки анимации hideIcon()
    /*private Animator.AnimatorListener animatorListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animator) {}
        @Override
        public void onAnimationEnd(Animator animator) {
            shouldHide = true;
            shouldClick = true;
            isIconHidingAnimationActive = false;
            hideIcon();
        }
        @Override
        public void onAnimationCancel(Animator animator) {}
        @Override
        public void onAnimationRepeat(Animator animator) {}
    };*/

    @Override
    public void onCreate() {
        super.onCreate();

        //регистрация широковещательного ресивера
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_USER_PRESENT);
        filter.addAction(ACTION_ON_TOP_ACTIVITY_CHANGED);
        registerReceiver(mScreenStateReceiver, filter);

        //определение текущей ориентации устройства
        isScreenOrientationPortrait = isDeviceDefaultOrientationPortrait();
        statusBarHeight = getStatusBarHeight();

        //первичная инициализация флагов
        shouldDestroy = false;
        shouldHide = true;
        isLocked = false;
        isFullScreenIn = false;
        shouldCurrentAppHideIcon = false;
        isIconHidingAnimationActive = false;

        //определение размеров экрана в пикселях
        metrics = getApplicationContext().getResources().getDisplayMetrics();
        screenHeight = metrics.heightPixels;
        screenWidth = metrics.widthPixels;

        //инициализация флага наличия прав администратора относительно данного приложения
        checkAdminActive = new CheckAdminActive(getApplicationContext());

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        lockScreenImageView = new ImageView(this);
        /*lockScreenImageView.setImageResource(R.drawable.ic_float_button);
        Bitmap b = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_float_button);
        iconWidth = b.getWidth();
        iconHeight = b.getHeight();*/
        prepareSpringAnimation();
        setFullScreenChangeListener();
        lockScreenImageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if(shouldHide && !shouldMove){
                    //сброс коллбэка, использующего postDelayedHidingRunnable и
                    //установленного в методе hideIcon()
                    //нужно для того, чтобы иконка не начинала самопроизвольно возвращаться
                    //к границе экрана через установленный промежуток времени
                    handlerForHidingAnimation.removeCallbacks(postDelayedHidingRunnable);
                    Log.d("onLongClick", "longclicked");
                    spring.setEndValue(1f);
                    //предотвратит вызов метода отключения экрана и
                    //и разрешит перетаскивание
                    shouldClick = false;
                    shouldMove = true;
                }
                return true;
            }
        });

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.START;
        params.windowAnimations = android.R.style.Animation_Toast;

        initializationSavedCoordinates();

        lockScreenImageView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                //флаг isLocked нужен для предотвращения возможности
                //многократного перехода ко всей конструкции switch
                //при одиночном многократном нажатии на иконку
                if (!isLocked) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            Log.d("ACTION_DOWN", Float.toString(event.getRawX()) + ", " + Float.toString(event.getRawY()));
                            initialX = params.x;
                            initialY = params.y;
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();
                            if (!shouldHide && !isIconHidingAnimationActive) {
                                showIcon();
                                isIconHidingAnimationActive = true;
                            } else {
                                shouldClick = true;
                                shouldMove = false;
                            }
                            break;
                        case MotionEvent.ACTION_UP:
                            Log.d("ACTION_UP", Float.toString(event.getRawX()) + ", " + Float.toString(event.getRawY()));
                            if ((event.getRawX() >= initialTouchX && event.getRawX() <= initialTouchX + iconWidth) &&
                                    (event.getRawY() >= initialTouchY && event.getRawY() <= initialTouchY + iconHeight)) {
                                if (shouldHide && shouldClick && !shouldMove) {
                                    Log.d("AppFloat", "Click Detected");
                                    if (checkAdminActive.isAdminActive()) {
                                        if (!isLocked) {
                                            isLocked = true;
                                            checkAdminActive.lockTheScreen();
                                        }
                                    } else {
                                        startActivity(checkAdminActive.getIntentToEnableActiveAdmin().setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                                        //Toast.makeText(getApplicationContext(), "The program does not have admin rights!", Toast.LENGTH_LONG).show();
                                    }
                                    break;
                                }
                            }
                            spring.setEndValue(0f);
                            if (shouldMove) shiftIconToScreenSide();
                            shouldMove = false;
                            break;
                        case MotionEvent.ACTION_MOVE:
                            Log.d("ACTION_MOVE", Float.toString(event.getRawX()) + ", " + Float.toString(event.getRawY()));
                            if (shouldHide && shouldMove) {
                                handlerForHidingAnimation.removeCallbacks(postDelayedHidingRunnable);
                                params.x = initialX + (int) (event.getRawX() - initialTouchX);
                                params.y = initialY + (int) (event.getRawY() - initialTouchY);
                                if (params.y <= -delta/2) params.y = -delta/2;
                                if (params.y >= screenHeight - statusBarHeight - iconHeight - delta/2)
                                    params.y = screenHeight - statusBarHeight - iconHeight - delta/2;
                                if (params.x <= -delta/2) params.x = -delta/2;
                                if (params.x >= screenWidth - iconWidth - delta/2) params.x = screenWidth - iconWidth - delta/2;
                                windowManager.updateViewLayout(lockScreenImageView, params);
                                shouldClick = false;
                            }
                            break;
                    }
                }
                //возврат false нужен для совместной работы setOnTouchListener
                //и setOnLongClickListener
                return false;
            }
        });
        windowManager.addView(lockScreenImageView, params);

        //отложенная анимация сворачивания за границу экрана
        postDelayedHidingRunnable = new Runnable() {
            public void run() {

                lockScreenImageView.animate()
                        .alpha(0.6f)
                        .setDuration(iconHidingAnimationTime);
                shouldHide = false;
                shiftIconToScreenSide(1, -2*iconWidth/3 - delta/2,
                        screenWidth - iconWidth/3 - delta/2);

                /*if (params.x + iconWidth/2 <= screenWidth / 2)
                    lockScreenImageView.animate()
                            .alpha(0.6f)
                            .translationX(-2*iconWidth/3)
                            .setDuration(iconHidingAnimationTime);
                else
                    lockScreenImageView.animate()
                            .alpha(0.6f)
                            .translationX(2*iconWidth/3)
                            .setDuration(iconHidingAnimationTime);*/
            }
        };
        //хэндлер для анимации сворачивания
        handlerForHidingAnimation = new Handler();

        //первый вызов анимации сворачивания
        hideIcon();
    }

    public void setFullScreenChangeListener(){
        sharedpreferences2 = getSharedPreferences(LockScreenActivity.MyPREFERENCES, Context.MODE_PRIVATE);
        transparentViewToCheckFullScreen = new ImageView(this);
        transparentViewToCheckFullScreen.setLayoutParams(new ViewGroup.LayoutParams(1, 1));
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x=0;
        params.y=0;
        windowManager.addView(transparentViewToCheckFullScreen, params);
        transparentViewToCheckFullScreen.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view, int i, int i1, int i2, int i3, int i4, int i5, int i6, int i7) {
                if (sharedpreferences2.getBoolean(FullScreenSwitcher, false) && !shouldCurrentAppHideIcon) {
                    int[] locations = new int[2];
                    view.getLocationOnScreen(locations);
                    if (locations[1] == 0 && !isFullScreenIn) {
                        pauseFloatingButton();
                        isFullScreenIn = true;
                    }
                    else if (locations[1] == statusBarHeight && isFullScreenIn) {
                        isFullScreenIn = false;
                        resumeFloatingButton();
                    }
                }
            }
        });
    }

    public void pauseFloatingButton(){
        isLocked = true;
        if (shouldHide) {
            shouldMove = false;
            shouldClick = false;
        }
        shiftIconToScreenSide(3, -iconWidth - delta,
                screenWidth + iconWidth + delta);
    }

    public void resumeFloatingButton(){
        isLocked = false;
        if (shouldHide) {
            shiftIconToScreenSide(4, - delta/2,
                    screenWidth - iconWidth - delta/2);
        }
        else shiftIconToScreenSide(4, -2*iconWidth/3 - delta/2,
                screenWidth - iconWidth/3 - delta/2);
    }

    public void prepareSpringAnimation(){
        SpringSystem springSystem = SpringSystem.create();
        spring = springSystem.createSpring();
        spring.setSpringConfig(new SpringConfig(TENSION, DAMPER));
        spring.addListener(new SimpleSpringListener() {
            @Override
            public void onSpringUpdate(Spring spring) {
                float value = (float) spring.getCurrentValue();
                float scale = 1f - (value * 0.2f);
                lockScreenImageView.setScaleX(scale);
                lockScreenImageView.setScaleY(scale);
            }
        });
    }

    public void initializationSavedCoordinates(){
        sharedpreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
        if (isScreenOrientationPortrait){
            params.x = sharedpreferences.getInt(PortraitX, -delta/2);
            params.y = sharedpreferences.getInt(PortraitY, screenHeight/2);
        }
        else{
            params.x = sharedpreferences.getInt(LandscapeX, -delta/2);
            params.y = sharedpreferences.getInt(LandscapeY, screenHeight/2);
        }
    }

    public void saveCoordinatesToSharedPrefs(){
        SharedPreferences.Editor editor = sharedpreferences.edit();
        if (isScreenOrientationPortrait) {
            editor.putInt(PortraitX, params.x);
            editor.putInt(PortraitY, params.y);
        }
        else{
            editor.putInt(LandscapeX, params.x);
            editor.putInt(LandscapeY, params.y);
        }
        editor.apply();
    }

    public void shiftIconToScreenSide(){
        if (params.x + iconWidth/2 <= screenWidth / 2)  overlayAnimation(0, lockScreenImageView, params.x, -delta/2);
        else  overlayAnimation(0, lockScreenImageView, params.x, screenWidth - iconWidth - delta/2);
    }

    public void shiftIconToScreenSide(final int hideType, int endX1, int endX2){
        if (params.x + iconWidth/2 <= screenWidth / 2)  overlayAnimation(hideType, lockScreenImageView, params.x, endX1);
        else  overlayAnimation(hideType, lockScreenImageView, params.x, endX2);
    }

    public void updateViewLayout(View view, Integer x, Integer y, Integer w, Integer h){
        if (view!=null) {
            WindowManager.LayoutParams lp = (WindowManager.LayoutParams) view.getLayoutParams();

            if(x != null)lp.x=x;
            if(y != null)lp.y=y;
            if(w != null && w>0)lp.width=w;
            if(h != null && h>0)lp.height=h;

            if (!shouldDestroy) windowManager.updateViewLayout(view, lp);
        }
    }

    public void overlayAnimation(final int hideType, final View view2animate, int viewX, final int endX) {
        ValueAnimator translateLeft = ValueAnimator.ofInt(viewX, endX);
        translateLeft.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int val = (Integer) valueAnimator.getAnimatedValue();
                updateViewLayout(view2animate, val, null, null, null);
                if (val == endX) {
                    //if (endX >= -delta/2 || endX <= screenWidth - iconWidth - delta/2)
                    params.x = endX;
                    handlerForHidingAnimation.removeCallbacks(postDelayedHidingRunnable);
                    switch(hideType) {
                        case 0:
                            if (shouldMove) {
                                spring.setEndValue(0f);
                                shouldMove = false;
                            }
                            saveCoordinatesToSharedPrefs();
                            hideIcon();
                            break;
                        case 2:
                            shouldHide = true;
                            shouldClick = true;
                            isIconHidingAnimationActive = false;
                            hideIcon();
                            break;
                        case 4:
                            if (shouldMove) spring.setEndValue(0f);
                            if (shouldHide) hideIcon();
                            break;
                    }
                }
            }
        });
        translateLeft.setDuration(iconShiftingAnimationTime);
        translateLeft.start();
        /*else {
            saveCoordinatesToSharedPrefs();
            hideIcon();
        }*/
    }

    public boolean isDeviceDefaultOrientationPortrait() {
        Configuration config = getResources().getConfiguration();
        return config.orientation == Configuration.ORIENTATION_PORTRAIT;
    }

    public void updateScreenSizes(){
        metrics = getApplicationContext().getResources().getDisplayMetrics();
        screenHeight = metrics.heightPixels;
        screenWidth = metrics.widthPixels;
    }

    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public void hideIcon(){
        handlerForHidingAnimation.postDelayed(postDelayedHidingRunnable, 2000);
    }

    public void showIcon(){
        //lockScreenImageView.setAlpha(1.0f);
        /*lockScreenImageView.animate()
                .alpha(1.0f)
                .translationX(0)
                .setDuration(getResources().getInteger(android.R.integer.config_mediumAnimTime));
        shouldHide = true;
        shouldClick = true;
        hideIcon();*/

        /*if (params.x + iconWidth/2 <= screenWidth / 2) leftToRightAnimator.start();
        else rightToLeftAnimator.start();
        alphaAnimator.start();*/

        lockScreenImageView.animate()
                .alpha(1.0f)
                .setDuration(iconHidingAnimationTime);
        shiftIconToScreenSide(2, -delta/2,
                screenWidth - iconWidth - delta/2);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (shouldMove) {
            spring.setEndValue(0f);
            shouldMove = false;
            shouldClick = false;
        }
        super.onConfigurationChanged(newConfig);
        int orientation = newConfig.orientation;
        if (orientation == Configuration.ORIENTATION_PORTRAIT)
            isScreenOrientationPortrait = true;
        else if (orientation == Configuration.ORIENTATION_LANDSCAPE)
            isScreenOrientationPortrait = false;
        updateScreenSizes();
        initializationSavedCoordinates();
        windowManager.updateViewLayout(lockScreenImageView, params);
        hideIcon();
        //shiftIconToScreenSide();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int progress = sharedpreferences.getInt(LockScreenActivity.SeekBarScale, 50);
        float size = (int) ((50 * getResources().getDisplayMetrics().density + 0.5f) * (0.7f + (float)progress/100f));
        if (intent != null) size = (float)intent.getExtras().get("icon_size");
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_float_button);
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, (int)size, (int)size, false);
        iconWidth = resizedBitmap.getWidth();
        iconHeight = resizedBitmap.getHeight();

        ViewGroup.LayoutParams layoutParams = lockScreenImageView.getLayoutParams();
        layoutParams.width = iconWidth + delta;
        layoutParams.height = iconHeight + delta;
        lockScreenImageView.setLayoutParams(layoutParams);
        lockScreenImageView.setScaleType(ImageView.ScaleType.CENTER);
        lockScreenImageView.setImageBitmap(resizedBitmap);

        windowManager.updateViewLayout(lockScreenImageView, params);

        shiftIconToScreenSide();

        //инициализация объектов ObjectAnimator для отрисовки анимации появления иконки из-за экрана
        /*leftToRightAnimator = ObjectAnimator.ofFloat(lockScreenImageView, "translationX", -2*iconWidth/3, 0)
                .setDuration(iconHidingAnimationTime);
        rightToLeftAnimator = ObjectAnimator.ofFloat(lockScreenImageView, "translationX", 2*iconWidth/3, 0)
                .setDuration(iconHidingAnimationTime);
        alphaAnimator = ObjectAnimator.ofFloat(lockScreenImageView,"alpha",1.0f)
                .setDuration(iconHidingAnimationTime);
        leftToRightAnimator.addListener(animatorListener);
        rightToLeftAnimator.addListener(animatorListener);*/

        return START_STICKY;
    }

    public void getCheckedFromSharedPrefs(String appToHide){
        Gson gson = new Gson();
        boolean check = false;
        SharedPreferences sharedpreferences = getSharedPreferences(LockScreenActivity.MyPREFERENCES, Context.MODE_PRIVATE);
        String empty_list = gson.toJson(new ArrayList<Boolean>());
        List<Boolean> checked = gson.fromJson(sharedpreferences.getString(ApplicationsListActivity.AppsChecked, empty_list),
                new TypeToken<List<Boolean>>(){}.getType());
        empty_list = gson.toJson(new ArrayList<String>());
        List<String> applicationsPackageNameList = gson.
                fromJson(sharedpreferences.getString(ApplicationsListActivity.AppsToHide, empty_list),
                new TypeToken<ArrayList<String>>(){}.getType());
        for(int i=0; i < applicationsPackageNameList.size(); i++){
            if (appToHide.equals(applicationsPackageNameList.get(i))) {
                if (checked.get(i)) {
                    pauseFloatingButton();
                    shouldCurrentAppHideIcon = true;
                }
                else {
                    if (!isFullScreenIn && !shouldMove) {
                        resumeFloatingButton();
                        shouldCurrentAppHideIcon = false;
                        check = true;
                    }
                }
                break;
            }
        }
        if (!shouldCurrentAppHideIcon && !check && !shouldMove) {
            //на тот случай, когда проверяемая строка с названием пакета
            //отсутствует в списке applicationsPackageNameList
            if (!isFullScreenIn)
                resumeFloatingButton();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mScreenStateReceiver);
        shouldDestroy = true;
        if (lockScreenImageView != null) windowManager.removeView(lockScreenImageView);
        if (transparentViewToCheckFullScreen != null) windowManager.removeView(transparentViewToCheckFullScreen);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}