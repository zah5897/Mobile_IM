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
 * LoginActivity.java at 2017-5-1 21:08:44, code by Jack Jiang.
 * You can contact author with jack.jiang@52im.net or jb2011@163.com.
 */
package com.mobile.im.activity;

import java.util.Observer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager.BadTokenException;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.mobile.im.R;
import com.mobile.im.utils.NetworkUtil;

import net.openmob.mobileimsdk.server.protocal.ErrorCode;

import im.mobile.IMClientManager;
import im.mobile.callback.Callback;
import im.mobile.callback.IMListener;


public class LoginActivity extends Activity implements IMListener {
    private final static String TAG = MainActivity.class.getSimpleName();


    private EditText editLoginName = null;
    private EditText editLoginPsw = null;
    private Button btnLogin = null;
    private TextView viewVersion = null;
    /**
     * 登陆进度提示
     */
    private OnLoginProgress onLoginProgress = null;
    /**
     * 收到服务端的登陆完成反馈时要通知的观察者（因登陆是异步实现，本观察者将由
     * ChatBaseEvent 事件的处理者在收到服务端的登陆反馈后通知之）
     */

    /**
     * Called when the activity is first created.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //
        this.setContentView(R.layout.login_activity_layout);

        // 界面UI基本设置
        initViews();
        initListeners();
        // 确保MobileIMSDK被初始化哦（整个APP生生命周期中只需调用一次哦）
        // 提示：在不退出APP的情况下退出登陆后再重新登陆时，请确保调用本方法一次，不然会报code=203错误哦！
        // IMClientManager.getInstance(getApplicationContext()).initMobileIMSDK(getApplicationContext());
    }

    /**
     */
    @Override
    public void onBackPressed() {
        super.onBackPressed();

        // ** 注意：Android程序要么就别处理，要处理就一定
        //			要退干净，否则会有意想不到的问题哦！
        finish();
        System.exit(0);
    }

    private void initViews() {
        btnLogin = (Button) this.findViewById(R.id.login_btn);
        editLoginName = (EditText) this.findViewById(R.id.loginName_editText);
        editLoginPsw = (EditText) this.findViewById(R.id.loginPsw_editText);
        this.setTitle("IMSDK");
    }

    private void initListeners() {
        btnLogin.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                doLogin();
            }
        });
    }

    /**
     * 登陆处理。
     */
    private void doLogin() {
        if (!NetworkUtil.checkNetworkState(this)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setIcon(android.R.drawable.ic_dialog_alert);
            builder.setTitle("当前网络不可用");//
            builder.setMessage("当前网络不可用，去设置？");//
            builder.setPositiveButton("设置", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)); //直接进入手机中的wifi网络设置界面
                }
            });
            builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            builder.create();
            builder.show();
            return;
        }
        // 设置服务器地址和端口号
        String username = editLoginName.getText().toString().trim();
        String password = editLoginPsw.getText().toString().trim();
        onLoginProgress = new OnLoginProgress(this);
        onLoginProgress.showProgressing(true);
        IMClientManager.getInstance(getApplicationContext()).login(username, password, new Callback() {
            @Override
            public void onBack(int code, Object msg) {
                if (code == 0) {
                    Log.d(MainActivity.class.getSimpleName(), "登陆/连接信息已成功发出！");
                } else if (code == ErrorCode.ForC.LOCAL_NETWORK_NOT_WORKING) {
                    Toast.makeText(getApplicationContext(), "当前网络不可用", Toast.LENGTH_SHORT).show();
                    // * 登陆信息没有成功发出时当然无条件取消显示登陆进度条
                    onLoginProgress.showProgressing(false);
                } else {
                    Toast.makeText(getApplicationContext(), "登陆失败，code=" + code, Toast.LENGTH_SHORT).show();
                    // * 登陆信息没有成功发出时当然无条件取消显示登陆进度条
                    onLoginProgress.showProgressing(false);
                }
            }
        });
    }


    @Override
    public void onStart() {
        super.onStart();
        IMClientManager.getInstance(getApplicationContext()).registIMListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        IMClientManager.getInstance(getApplicationContext()).unRegistIMListener(this);
    }

    @Override
    public void onLogin(int code, String msg) {
        // * 已收到服务端登陆反馈则当然应立即取消显示登陆进度条
        onLoginProgress.showProgressing(false);
        // 登陆成功
        if (code == 0) {
            //** 提示：登陆/连接 MobileIMSDK服务器成功后的事情在此实现即可
            // 进入主界面
            startActivity(new Intent(LoginActivity.this, ConversationListActivity.class));
            // 同时关闭登陆界面
            finish();
        } else if (code == 7) {
            Toast.makeText(getApplicationContext(), "密码错误", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplicationContext(), "登陆异常，code=" + code, Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onLinkCloseMessage(int code, String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }


    //-------------------------------------------------------------------------- inner classes

    /**
     * 登陆进度提示和超时检测封装实现类.
     */
    private class OnLoginProgress {
        /**
         * 登陆的超时时间定义
         */
        private final static int RETRY_DELAY = 6000;

        private Handler handler = null;
        private Runnable runnable = null;
        // 重试时要通知的观察者
        private Observer retryObsrver = null;

        private ProgressDialog progressDialogForPairing = null;
        private Activity parentActivity = null;

        public OnLoginProgress(Activity parentActivity) {
            this.parentActivity = parentActivity;
            init();
        }

        private void init() {
            progressDialogForPairing = new ProgressDialog(parentActivity);
            progressDialogForPairing.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialogForPairing.setTitle("登陆中");
            progressDialogForPairing.setMessage("正在登陆中，请稍候。。。");
            progressDialogForPairing.setCanceledOnTouchOutside(false);

            handler = new Handler();
            runnable = new Runnable() {
                @Override
                public void run() {
                    onTimeout();
                }
            };
        }

        /**
         * 登陆超时后要调用的方法。
         */
        private void onTimeout() {
            // 本观察者中由用户选择是否重试登陆或者取消登陆重试
            new AlertDialog.Builder(LoginActivity.this)
                    .setTitle("超时了")
                    .setMessage("登陆超时，可能是网络故障或服务器无法连接，是否重试？")
                    .setPositiveButton("重试！", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // 确认要重试时（再次尝试登陆哦）
                            doLogin();
                        }
                    })
                    .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // 不需要重试则要停止“登陆中”的进度提示哦
                            OnLoginProgress.this.showProgressing(false);
                        }
                    })
                    .show();
        }

        /**
         * 显示进度提示.
         *
         * @param show
         */
        public void showProgressing(boolean show) {
            // 显示进度提示的同时即启动超时提醒线程
            if (show) {
                showLoginProgressGUI(true);

                // 先无论如何保证利重试检测线程在启动前肯定是处于停止状态
                handler.removeCallbacks(runnable);
                // 启动
                handler.postDelayed(runnable, RETRY_DELAY);
            }
            // 关闭进度提示
            else {
                // 无条件停掉延迟重试任务
                handler.removeCallbacks(runnable);

                showLoginProgressGUI(false);
            }
        }

        /**
         * 进度提示时要显示或取消显示的GUI内容。
         *
         * @param show true表示显示gui内容，否则表示结速gui内容显示
         */
        private void showLoginProgressGUI(boolean show) {
            // 显示登陆提示信息
            if (show) {
                try {
                    if (parentActivity != null && !parentActivity.isFinishing())
                        progressDialogForPairing.show();
                } catch (BadTokenException e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            }
            // 关闭登陆提示信息
            else {
                // 此if语句是为了保证延迟线程里不会因Activity已被关闭而此处却要非法地执行show的情况（此判断可趁为安全的show方法哦！）
                if (parentActivity != null && !parentActivity.isFinishing())
                    progressDialogForPairing.dismiss();
            }
        }
    }
}
