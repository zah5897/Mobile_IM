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
import im.mobile.http.HttpUtil;


public class LoginActivity extends Activity implements IMListener {
    private final static String TAG = MainActivity.class.getSimpleName();


    private EditText editLoginName = null;
    private EditText editLoginPsw = null;
    private ProgressDialog progressDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //
        this.setContentView(R.layout.login_activity_layout);

        // 界面UI基本设置
        initViews();
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
        editLoginName = (EditText) this.findViewById(R.id.loginName_editText);
        editLoginPsw = (EditText) this.findViewById(R.id.loginPsw_editText);
        this.setTitle("IMSDK");
        findViewById(R.id.regist_btn).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                doRegist();
            }
        });
        findViewById(R.id.login_btn).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                doLogin();
            }
        });
    }


    private void showProgress(boolean isRegist) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        if (isRegist) {
            progressDialog.setTitle("注冊中");
            progressDialog.setMessage("正在注冊中，请稍候。。。");
        } else {
            progressDialog.setTitle("登陆中");
            progressDialog.setMessage("正在登陆中，请稍候。。。");
        }

        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();
    }


    public boolean checkNetwork() {
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
            return false;
        }
        return true;
    }

    /**
     * 注冊。
     */
    private void doRegist() {

        if (!checkNetwork()) {
            return;
        }
        // 设置服务器地址和端口号
        String username = editLoginName.getText().toString().trim();
        String password = editLoginPsw.getText().toString().trim();
        showProgress(true);
        HttpUtil.regist(username, password, new Callback() {
            @Override
            public void onBack(final int code, Object msg) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                        if (code == 0) {
                            Toast.makeText(getApplicationContext(), "注册成功", Toast.LENGTH_SHORT).show();
                        }else if(code==4){
                            Toast.makeText(getApplicationContext(), "注册失败,该用户已存在", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getApplicationContext(), "注册失败", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }


    /**
     * 登陆处理。
     */
    private void doLogin() {
        if (!checkNetwork()) {
            return;
        }
        // 设置服务器地址和端口号
        String username = editLoginName.getText().toString().trim();
        String password = editLoginPsw.getText().toString().trim();
        showProgress(false);
        IMClientManager.getInstance(getApplicationContext()).login(username, password, new Callback() {
            @Override
            public void onBack(int code, Object msg) {
                if (code == 0) {
                    Log.d(MainActivity.class.getSimpleName(), "登陆/连接信息已成功发出！");
                } else if (code == ErrorCode.ForC.LOCAL_NETWORK_NOT_WORKING) {
                    Toast.makeText(getApplicationContext(), "当前网络不可用", Toast.LENGTH_SHORT).show();
                    // * 登陆信息没有成功发出时当然无条件取消显示登陆进度条
                    progressDialog.dismiss();
                } else {
                    Toast.makeText(getApplicationContext(), "登陆失败，code=" + code, Toast.LENGTH_SHORT).show();
                    // * 登陆信息没有成功发出时当然无条件取消显示登陆进度条
                    progressDialog.dismiss();
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
    public void onLogin(final int code, String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // * 已收到服务端登陆反馈则当然应立即取消显示登陆进度条
                if (progressDialog != null) {
                    progressDialog.dismiss();
                }
                // 登陆成功
                if (code == 0) {
                    //** 提示：登陆/连接 MobileIMSDK服务器成功后的事情在此实现即可
                    // 进入主界面
                    startActivity(new Intent(LoginActivity.this, ConversationListActivity.class));
                    // 同时关闭登陆界面
                    finish();
                } else if (code == 7) {
                    Toast.makeText(getApplicationContext(), "密码错误", Toast.LENGTH_SHORT).show();
                } else if (code == -2) {
                    Toast.makeText(getApplicationContext(), "登录超时", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "服务器连接异常，code=" + code, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onLinkCloseMessage(int code, String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
}
