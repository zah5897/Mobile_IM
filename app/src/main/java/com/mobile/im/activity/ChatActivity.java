package com.mobile.im.activity;

import android.app.Activity;
import android.content.Context;
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
import com.mobile.im.adapter.ChatAdapter;
import com.mobile.im.adapter.ConversationAdapter;
import com.mobile.im.utils.http.HttpUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import im.mobile.IMClientManager;
import im.mobile.callback.Callback;
import im.mobile.callback.IMessageListener;
import im.mobile.model.IMessage;
import im.mobile.model.TxtMessage;

public class ChatActivity extends Activity implements IMessageListener {
    private final static String TAG = ChatActivity.class.getSimpleName();
    private ListView listView;
    private ChatAdapter adapter;
    private TextView title;
    private EditText input;
    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.chat_activity_layout);
        username = getIntent().getStringExtra("username");
        initViews();
        IMClientManager.getInstance(getApplicationContext()).registIMessageListener(this);
        IMClientManager.getInstance(getApplicationContext()).loadHistoryMsg(username, null);
    }

    @Override
    public void onStart() {
        super.onStart();
        IMClientManager.getInstance(getApplicationContext()).registIMessageListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        IMClientManager.getInstance(getApplicationContext()).unRegistIMessageListener(this);
    }

    private void initViews() {
        findViewById(R.id.back).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        title = findViewById(R.id.title);
        listView = findViewById(R.id.listview);

        input = findViewById(R.id.input);
        updateAdapter();
        listView.setAdapter(adapter);
        listView.setSelection(adapter.getCount());
        title.setText("和" + username + "聊天中");

        findViewById(R.id.send).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                doSendTxtMessage();
            }
        });
    }

    private void doSendTxtMessage() {
        String content = input.getText().toString().trim();
        String friendId = username;
        if (content.length() > 0 && friendId.length() > 0) {
            IMessage msg = new TxtMessage(friendId, content);
            sendMsg(msg);
            input.setText("");
        } else {
            Log.e(ChatActivity.class.getSimpleName(), "msg.len=" + content.length() + ",friendId.len=" + friendId.length());
        }
    }


    private void sendMsg(IMessage msg) {
        IMClientManager.getInstance(getApplicationContext()).sendMsg(msg, new Callback() {
            @Override
            public void onBack(int code, Object obj) {
                if (code == 0) {
                    IMessage msg = (IMessage) obj;
                    adapter.updateMsg(msg);
                }
            }
        });
        adapter.addItem(msg);
        listView.smoothScrollToPosition(adapter.getCount());
    }


    private void updateAdapter() {
        if (adapter == null) {
            adapter = new ChatAdapter(this, IMClientManager.getInstance(getApplicationContext()).getDbHelper().loadMessages(username));
        } else {
            adapter.replaceData(IMClientManager.getInstance().getDbHelper().loadMessages(username));
        }

    }

    @Override
    public void onReceive(IMessage msg) {
        adapter.addItem(msg);
    }

    @Override
    public void onMsgBeReceived(String theFingerPrint) {
        adapter.updateMsgBeReceived(theFingerPrint);
    }

    @Override
    public void onOfflineMsgLoad() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

            }
        });
    }
}
