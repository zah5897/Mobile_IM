package im.mobile.model;


import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

import im.mobile.IMClientManager;
import im.mobile.MsgManager;
import im.mobile.callback.Callback;
import im.mobile.http.HttpUtil;

import static im.mobile.model.IMessage.IMessageState.DOWN_SUCCESS;
import static im.mobile.model.IMessage.IMessageState.UPLOADING;
import static im.mobile.model.IMessage.IMessageState.UPLOAD_FAILED;
import static im.mobile.model.IMessage.IMessageState.UPLOAD_SUCCESS;

public class VoiceMessage extends IMessage {
    public int voiceTimeLen;

    public VoiceMessage(String to, String aarPath, int len) {
        this.type = IMessageType.AUDIO;
        this.localPath = aarPath;
        this.voiceTimeLen = len;
        this.fingerPrint = UUID.randomUUID().toString();
        this.to = to;
        this.state = UPLOADING;
    }

    public VoiceMessage() {
        this.type = IMessageType.AUDIO;
    }

    public static VoiceMessage create(String from, String to, String content, String fingerPrint, long serverTime) {
        VoiceMessage img = new VoiceMessage();
        img.from = from;
        img.content = content;
        img.to = to;
        img.fingerPrint = fingerPrint;
        img.serverTime = serverTime;
        img.praseContent();
        return img;
    }


    @Override
    public void praseContent() {
        try {
            JSONObject obj = new JSONObject(content);
            String name = obj.optString("name");
            if (localPath == null) {
                localPath = HttpUtil.getMsgVoiceUrl(name);
            }
            voiceTimeLen = obj.optInt("voiceTimeLen");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void download() {
        JSONObject obj = null;
        try {
            obj = new JSONObject(content);
            String name = obj.optString("name");
            HttpUtil.downloadVoice(name, new Callback() {
                @Override
                public void onBack(int code, Object msg) {
                    if (code == 0) {
                        localPath = msg.toString();
                        state = DOWN_SUCCESS;
                        MsgManager.getManager().updateVoiceLocalPath(fingerPrint, localPath, state.ordinal());
                    }
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void send(Callback callback) {
        this.from = IMClientManager.getInstance().getCurrentLoginUsername();
        uploadVoice(callback);
    }

    public void uploadVoice(final Callback callback) {
        HttpUtil.uploadVoice(IMClientManager.getInstance().getCurrentLoginUsername(), localPath, voiceTimeLen, new Callback() {
            @Override
            public void onBack(int code, Object msg) {
                if (code == 0) {
                    VoiceMessage.this.content = msg.toString();
                    VoiceMessage.this.state = UPLOAD_SUCCESS;
                } else {
                    VoiceMessage.this.state = UPLOAD_FAILED;
                }
                MsgManager.getManager().updateFileMsgState(fingerPrint, content, IMClientManager.getInstance().getCurrentServerTime(), state.ordinal());
                callback.onBack(0, VoiceMessage.this);
                VoiceMessage.super.send(callback);
            }
        });
    }
}
