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
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.Toast;

import com.mobile.im.R;
import com.mobile.im.service.IMService;

import im.mobile.IMClientManager;
import im.mobile.callback.IMListener;

/**
 * 应用程序启动类：显示闪屏界面并跳转到主界面.
 *
 * @author liux, Jack Jiang
 * @version 1.0
 * @created 2012-3-21
 */
public class SplashScreenActivity extends Activity implements IMListener {
    private BroadcastReceiver broadcastReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // FIX: 以下代码是为了解决Android自level 1以来的[安装完成点击“Open”后导致的应用被重复启动]的Bug
        // @see https://code.google.com/p/android/issues/detail?id=52247
        // @see https://code.google.com/p/android/issues/detail?id=2373
        // @see https://code.google.com/p/android/issues/detail?id=26658
        // @see https://github.com/cleverua/android_startup_activity
        // @see http://stackoverflow.com/questions/4341600/how-to-prevent-multiple-instances-of-an-activity-when-it-is-launched-with-differ/
        // @see http://stackoverflow.com/questions/12111943/duplicate-activities-on-the-back-stack-after-initial-installation-of-apk
        // 加了以下代码还得确保Manifast里加上权限申请：“android.permission.GET_TASKS”
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
        IMClientManager.getInstance(getApplicationContext()).registIMListener(this);


        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(IMService.ACTION_LOGING);
        intentFilter.addAction(IMService.ACTION_NEED_INPUT_TOEKN);
        intentFilter.addAction(IMService.ACTION_NETWORK_BAD);
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent != null) {
                    if (intent.getAction().equals(IMService.ACTION_NEED_INPUT_TOEKN)) {
                        redirectToLogin();
                    } else if (intent.getAction().equals(IMService.ACTION_NETWORK_BAD)) {
                        redirectToConversation();
                    }
                }
            }
        };
        registerReceiver(broadcastReceiver, intentFilter);
    }

    @Override
    protected void onDestroy() {
        if (broadcastReceiver != null) {
            unregisterReceiver(broadcastReceiver);
        }
        IMClientManager.getInstance(getApplicationContext()).unRegistIMListener(this);
        super.onDestroy();
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

    @Override
    public void onLogin(final int code, String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (code == 0) {
                    redirectToConversation();
                } else if (code == 7) {
                    new AlertDialog.Builder(SplashScreenActivity.this).setMessage("密码错误，重新登录？").setNegativeButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            IMClientManager.getInstance(getApplicationContext()).clearLoginCache();
                            redirectToLogin();
                        }
                    }).show();
                } else if (code == -2) {
                    new AlertDialog.Builder(SplashScreenActivity.this).setMessage("登录超时").setNegativeButton("重试", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startService(new Intent(getApplicationContext(), IMService.class));
                        }
                    }).show();
                } else {
                    new AlertDialog.Builder(SplashScreenActivity.this).setMessage("服务器连接异常，code=" + code).setNegativeButton("关闭", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    }).show();
                }
            }
        });


    }

    @Override
    public void onLinkCloseMessage(int code, String msg) {

    }
}