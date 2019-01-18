/*
 * Copyright (C) 2017  即时通讯网(52im.net) & Jack Jiang.
 * The MobileIMSDK_X (MobileIMSDK v3.x) Project.
 * All rights reserved.
 *
 * > Github地址: https://github.com/JackJiang2011/MobileIMSDK
 * > 文档地址: http://www.52im.net/forum-89-1.html
 * > 即时通讯技术社区：http://www.52im.net/
 * > 即时通讯技术交流群：320837163 (http://www.52im.net/topic-qqgroup.html)
 *
 * "即时通讯网(52im.net) - 即时通讯开发者社区!" 推荐开源工程。
 *
 * SplashScreenActivity.java at 2017-5-1 21:08:44, code by Jack Jiang.
 * You can contact author with jack.jiang@52im.net or jb2011@163.com.
 */
package com.mobile.im.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

import com.mobile.im.R;
import com.mobile.im.service.IMService;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import im.mobile.IMClientManager;
import im.mobile.event.LoginEvent;

/**
 * 应用程序启动类：显示闪屏界面并跳转到主界面.
 *
 * @author liux, Jack Jiang
 * @version 1.0
 * @created 2012-3-21
 */
public class SplashScreenActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (!isTaskRoot()) {// FIX START: by Jack Jiang 2013-11-07
            final Intent intent = getIntent();
            final String intentAction = intent.getAction();
            if (intent.hasCategory(Intent.CATEGORY_LAUNCHER) &&
                    intentAction != null && intentAction.equals(Intent.ACTION_MAIN)) {
                finish();
            }
        }// FIX END

        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        final View view = View.inflate(this, R.layout.splash_screen_activity_layout, null);
        setContentView(view);
        startService(new Intent(getApplicationContext(), IMService.class));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (IMClientManager.getInstance(getApplicationContext()).isLogined()) {
            redirectToConversation();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    /**
     * 跳转到...
     */
    private void redirectToLogin() {
        Intent intent = new Intent(SplashScreenActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void redirectToConversation() {
        Intent intent = new Intent(SplashScreenActivity.this, ConversationListActivity.class);
        startActivity(intent);
        finish();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLogin(LoginEvent event) {
        if (event.type == LoginEvent.TYPE_LOGIN) {
            if (event.code == 0) {
                redirectToConversation();
            } else {
                redirectToLogin();
            }
        } else {
            //onLinkCloseMessage
        }

    }


}