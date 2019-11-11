package com.pttiot.rtcim;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

public class App extends Application {

    private Handler globHandler =new Handler(Looper.getMainLooper());
    private static Context mAppContext;
    private static App myApp;

    @Override
    public void onCreate() {
        super.onCreate();
        myApp = this;

    }
    public Handler getGlobHandler() {
        return globHandler;
    }
    public static App getApplication() {
        return myApp;
    }
    /**获取系统上下文：用于ToastUtil类*/
    public static Context getAppContext()
    {
        return mAppContext;
    }
}
