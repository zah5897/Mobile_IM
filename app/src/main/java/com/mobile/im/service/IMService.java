package com.mobile.im.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import com.mobile.im.activity.ChatActivity;
import com.mobile.im.activity.MainActivity;

import org.greenrobot.eventbus.EventBus;

import im.mobile.IMClientManager;
import im.mobile.callback.Callback;
import im.mobile.event.LoginEvent;
import im.mobile.model.IMessage;
import im.mobile.utils.AppNotifier;
import im.mobile.utils.CommUtils;

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
        String username = null;
        String password = null;
        if (intent != null && intent.getBooleanExtra("from_loginpage", false)) {
            username = intent.getStringExtra("username");
            password = intent.getStringExtra("password");
        } else {
            username = CommUtils.getShareSPFValue(getApplicationContext(), "id");
            password = CommUtils.getShareSPFValue(getApplicationContext(), "token");
        }
        IMClientManager.getInstance(getApplicationContext()).login(username, password, new Callback() {
            @Override
            public void onBack(int code, Object msg) {
                if (code != 0) {
                    EventBus.getDefault().post(new LoginEvent(LoginEvent.TYPE_LOGIN, code, ""));
                }
            }
        });
        IMClientManager.getInstance().
                getNotifyer().
                setProvider(new AppNotifier.NotificationInfoProvider() {
                    @Override
                    public Intent getLaunchIntent(IMessage message) {
                        Intent i = new Intent(getApplicationContext(), ChatActivity.class);
                        i.putExtra("username", message.from);
                        return i;
                    }
                });
        return super.onStartCommand(intent, flags, startId);
    }


}
