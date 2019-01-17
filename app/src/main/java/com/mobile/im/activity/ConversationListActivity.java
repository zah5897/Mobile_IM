package com.mobile.im.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.mobile.im.R;
import com.mobile.im.adapter.ConversationAdapter;

import im.mobile.IMClientManager;
import im.mobile.callback.ConversationListener;
import im.mobile.callback.IMListener;
import im.mobile.model.Conversation;


public class ConversationListActivity extends Activity implements ConversationListener, IMListener {
    /**
     * Called when the activity is first created.
     */
    private ListView listView;
    private ConversationAdapter adapter;
    private TextView title;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.conversion_activity_layout);

        listView = findViewById(R.id.listView);
        title = findViewById(R.id.title);
        refreshAdapter();
        listView.setAdapter(adapter);
        refreshConnectState();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Conversation conversation = adapter.getItem(position);
                conversation.updateRead();
                adapter.notifyDataSetChanged();
                Intent toChat = new Intent(getBaseContext(), ChatActivity.class);
                toChat.putExtra("username", conversation.friendUsername);
                startActivity(toChat);
            }
        });
        findViewById(R.id.new_chat).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final EditText input = new EditText(ConversationListActivity.this);
                input.setText("test");
                AlertDialog.Builder dialog = new AlertDialog.Builder(ConversationListActivity.this).setTitle("新建聊天").setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).setNeutralButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        String username = input.getText().toString().trim();
                        if (TextUtils.isEmpty(username)) {
                            Toast.makeText(getApplicationContext(), "对方不能为空", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Intent toChat = new Intent(getBaseContext(), ChatActivity.class);
                        toChat.putExtra("username", username);
                        startActivity(toChat);
                        dialog.dismiss();
                    }
                });
                dialog.setView(input).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshConnectState();
    }

    private void refreshAdapter() {
        if (adapter == null) {
            adapter = new ConversationAdapter(getBaseContext(), IMClientManager.getInstance(getApplicationContext()).getDbHelper().loadConversations());
        } else {
            adapter.updateData(IMClientManager.getInstance(getApplicationContext()).getDbHelper().loadConversations());
        }
    }

    public void refreshConnectState() {
        boolean connectedToServer = IMClientManager.getInstance().isConnectedToServer();
        String titleStr = null;
        if (connectedToServer) {
            titleStr = "会话列表";
        } else {
            titleStr = "会话列表 (连接断开)";
        }
        title.setText(titleStr);
    }

    @Override
    public void onStart() {
        super.onStart();
        IMClientManager.getInstance(getApplicationContext()).registIMListener(this);
        IMClientManager.getInstance(getApplicationContext()).registConversationListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        IMClientManager.getInstance(getApplicationContext()).unRegistIMListener(this);
        IMClientManager.getInstance(getApplicationContext()).unRegistConversationListener(this);
    }

    @Override
    public void onChange() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                refreshAdapter();
            }
        });
    }

    @Override
    public void onLogin(int code, String msg) {

    }

    @Override
    public void onLinkCloseMessage(int code, String msg) {
        refreshConnectState();
    }
}
