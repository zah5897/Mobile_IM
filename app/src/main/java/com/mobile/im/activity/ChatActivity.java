package com.mobile.im.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.mobile.im.R;
import com.mobile.im.adapter.ChatAdapter;
import com.mobile.im.utils.GalleryUriUtils;
import com.mobile.im.view.VoiceRecorderView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import im.mobile.IMClientManager;
import im.mobile.MsgManager;
import im.mobile.callback.Callback;
import im.mobile.event.IMessageBeReceiveEvent;
import im.mobile.event.IMessageDownloadSuccessEvent;
import im.mobile.model.IMessage;
import im.mobile.model.ImgMessage;
import im.mobile.model.TxtMessage;
import im.mobile.model.VoiceMessage;
import pub.devrel.easypermissions.EasyPermissions;

public class ChatActivity extends Activity {
    private final static String TAG = ChatActivity.class.getSimpleName();
    private ListView listView;
    private ChatAdapter adapter;
    private TextView title;
    private EditText input;
    private String username;

    private ImageView typeVoice;
    private static int RESULT_LOAD_IMAGE = 10;
    //    private String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private String[] permissionsWS = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private String[] permissionsRA = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO, Manifest.permission.WAKE_LOCK};
    VoiceRecorderView voiceRecorderView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.chat_activity_layout);
        username = getIntent().getStringExtra("username");
        initViews();
//        IMClientManager.getInstance().loadHistoryMsg(username, null);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String newUsername = getIntent().getStringExtra("username");
        if (TextUtils.isEmpty(newUsername)) {
            Toast.makeText(getApplicationContext(), "当前聊天用户为空", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (newUsername.equals(username)) {
            updateAdapter();
        } else {
            username = newUsername;
            refreshConnectState();
            updateAdapter();
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
        typeVoice = findViewById(R.id.type_voice);
        voiceRecorderView = findViewById(R.id.voice_recorder);
        updateAdapter();
        listView.setAdapter(adapter);
        listView.setSelection(adapter.getCount());
        refreshConnectState();
        findViewById(R.id.send).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                doSendTxtMessage();
            }
        });
        findViewById(R.id.type_select).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.select_panel).setVisibility(View.VISIBLE);
            }
        });
        typeVoice.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.select_panel).setVisibility(View.GONE);
                if (input.getVisibility() == View.VISIBLE) {
                    input.setVisibility(View.GONE);
                    findViewById(R.id.make_voice).setVisibility(View.VISIBLE);
                    typeVoice.setImageResource(R.drawable.keyboard_btn_selector);
                } else {
                    input.setVisibility(View.VISIBLE);
                    findViewById(R.id.make_voice).setVisibility(View.GONE);
                    typeVoice.setImageResource(R.drawable.voide_btn_selector);
                }
            }
        });
        findViewById(R.id.make_voice).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (EasyPermissions.hasPermissions(getApplicationContext(), permissionsRA)) {
                    return voiceRecorderView.onPressToSpeakBtnTouch(v, event, new VoiceRecorderView.VoiceRecorderCallback() {

                        @Override
                        public void onVoiceRecordComplete(String voiceFilePath, int voiceTimeLength) {
                            IMessage msg = new VoiceMessage(username, voiceFilePath, voiceTimeLength);
                            sendMsg(msg);
                        }
                    });
                } else {
                    EasyPermissions.requestPermissions(ChatActivity.this, "需要获取您的录音权限", 1, permissionsRA);
                    return true;
                }
            }
        });
        findViewById(R.id.send_img).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                if (EasyPermissions.hasPermissions(getApplicationContext(), permissionsWS)) {
                    findViewById(R.id.select_panel).setVisibility(View.GONE);
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_PICK);
                    intent.setType("image/*");
                    startActivityForResult(intent, 2);
                } else {
                    EasyPermissions.requestPermissions(ChatActivity.this, "需要获取您的相册使用权限", 1, permissionsWS);
                }
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 2 && resultCode == RESULT_OK) {
            String photoPath = GalleryUriUtils.getRealPathFromUri(this, data.getData());
            IMessage msg = new ImgMessage(username, photoPath);
            sendMsg(msg);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        IMClientManager.getInstance().pushActivity(this);
        refreshConnectState();
    }

    @Override
    protected void onPause() {
        super.onPause();
        IMClientManager.getInstance().popActivity(this);
    }

    public void refreshConnectState() {
        boolean connectedToServer = IMClientManager.getInstance().isConnectedToServer();
        String titleStr = null;
        if (connectedToServer) {
            titleStr = "和" + username + "聊天中";
        } else {
            titleStr = "和" + username + "聊天中  (连接断开)";
        }
        title.setText(titleStr);
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
        msg.send(new Callback() {
            @Override
            public void onBack(int code, final Object obj) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        IMessage msg = (IMessage) obj;
                        adapter.updateMsg(msg);
                    }
                });

            }
        });
        adapter.addItem(msg);
        listView.smoothScrollToPosition(adapter.getCount());
    }

    private void updateAdapter() {
        if (adapter == null) {
            adapter = new ChatAdapter(this, MsgManager.getManager().loadMessages(username));
        } else {
            adapter.replaceData(MsgManager.getManager().loadMessages(username));
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onReceive(IMessage msg) {
        adapter.addItem(msg);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMsgBeReceived(IMessageBeReceiveEvent event) {
        adapter.updateMsgBeReceived(event.fingerPrint);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMsgDownloadSuccess(IMessageDownloadSuccessEvent event) {
        adapter.msgDownloadSuccess(event.fingerPrint);
    }
}
