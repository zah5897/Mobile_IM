package com.mobile.im;

import android.app.Application;


public class IMApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        LocationManger.getLocationManger().init(getApplicationContext());
    }
}
