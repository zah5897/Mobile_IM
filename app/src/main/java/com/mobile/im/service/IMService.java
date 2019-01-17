package com.mobile.im.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.mobile.im.activity.MainActivity;

import im.mobile.IMClientManager;
import im.mobile.callback.Callback;

public class IMService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    public static final String ACTION_LOGING = "action.loging";
    public static final String ACTION_NEED_INPUT_TOEKN = "action.need_user_login";
    public static final String ACTION_NETWORK_BAD = "action.network_bad";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        IMClientManager.getInstance(getApplicationContext()).initMobileIMSDK();
        IMClientManager.getInstance(getApplicationContext()).localLogin(new Callback() {
            @Override
            public void onBack(int code, Object msg) {
                if (code == 0) {
                    Log.d(MainActivity.class.getSimpleName(), "登陆/连接信息已成功发出！");
                    sendBroadcast(new Intent(ACTION_LOGING));
                } else if (code == -1) {
                    sendBroadcast(new Intent(ACTION_NEED_INPUT_TOEKN));
                } else {
                    sendBroadcast(new Intent(ACTION_NETWORK_BAD));
                }
            }
        });
        return super.onStartCommand(intent, flags, startId);
    }


}
