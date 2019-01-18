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
 * MainActivity.java at 2017-5-1 21:08:44, code by Jack Jiang.
 * You can contact author with jack.jiang@52im.net or jb2011@163.com.
 */
package com.mobile.im.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.mobile.im.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import im.mobile.IMClientManager;

public class SettingActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.setting_activity_layout);
        TextView textView = findViewById(R.id.current_username);
        textView.setText("当前登录帐号为：" + IMClientManager.getInstance().getCurrentLoginUsername());
        findViewById(R.id.logout).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                doLogout();
            }
        });
    }

    private void doLogout() {
        // 发出退出登陆请求包（Android系统要求必须要在独立的线程中发送哦）
        showProgress();
        new AsyncTask<Object, Integer, Integer>() {
            @Override
            protected Integer doInBackground(Object... params) {
                int code = -1;
                try {
                    code = IMClientManager.getInstance().sendLogout();
                } catch (Exception e) {
                }
                //## BUG FIX: 20170713 START by JackJiang
                // 退出登陆时记得一定要调用此行，不然不退出APP的情况下再登陆时会报 code=203错误哦！
                IMClientManager.getInstance(getApplicationContext()).resetInitFlag();
                //## BUG FIX: 20170713 END by JackJiang
                return code;
            }

            @Override
            protected void onPostExecute(Integer code) {
                if (progressDialog != null) {
                    progressDialog.dismiss();
                }
                IMClientManager.getInstance().initMobileIMSDK();
                IMClientManager.getInstance().closeLocalUDPSocket();
                Intent intent = new Intent(SettingActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        }.execute();
    }

    ProgressDialog progressDialog;

    private void showProgress() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);

        progressDialog.setTitle("退出登录");
        progressDialog.setMessage("正在退出登录中，请稍候。。。");

        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();
    }
}
