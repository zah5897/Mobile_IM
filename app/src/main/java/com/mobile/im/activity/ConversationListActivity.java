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

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import im.mobile.IMClientManager;
import im.mobile.MsgManager;
import im.mobile.event.ConversationRefreshEvent;
import im.mobile.event.LoginEvent;
import im.mobile.model.Conversation;


public class ConversationListActivity extends Activity {
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
        listView.setEmptyView(findViewById(R.id.no_data_txt));
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

        findViewById(R.id.new_location).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final EditText input = new EditText(ConversationListActivity.this);
                AlertDialog.Builder dialog = new AlertDialog.Builder(ConversationListActivity.this).setTitle("发布位置给对方").setNegativeButton("取消", new DialogInterface.OnClickListener() {
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
                        Intent toChat = new Intent(getBaseContext(), MapActivity.class);
                        toChat.putExtra("username", username);
                        startActivity(toChat);
                        dialog.dismiss();
                    }
                });
                dialog.setView(input).show();
            }
        });

        findViewById(R.id.setting).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), SettingActivity.class));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        IMClientManager.getInstance().pushActivity(this);
        refreshConnectState();
        refreshAdapter();
    }

    @Override
    protected void onPause() {
        super.onPause();
        IMClientManager.getInstance().popActivity(this);
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

    private void refreshAdapter() {
        if (adapter == null) {
            adapter = new ConversationAdapter(getBaseContext(), MsgManager.getManager().loadConversations());
        } else {
            adapter.updateData(MsgManager.getManager().loadConversations());
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onConversationRefreshEvent(ConversationRefreshEvent event) {
        refreshAdapter();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLogin(LoginEvent event) {
        if (event.type == LoginEvent.TYPE_LIKE_CLOSE) {
            refreshConnectState();
        }
    }
}
