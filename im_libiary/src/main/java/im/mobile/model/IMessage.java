package im.mobile.model;

import org.json.JSONObject;

import java.util.Date;

import im.mobile.IMClientManager;
import im.mobile.callback.Callback;

import static im.mobile.model.IMessage.IMessageState.CREATE;

public class IMessage {
    public String from;
    public String to;
    public String content;
    public String fingerPrint;
    public long serverTime;
    public IMessageState state = CREATE;
    public IMessageType type;
    public int readState = 0;//0已读，1未读

    public String localPath;

    @Override
    public boolean equals(Object obj) {
        IMessage msg = (IMessage) obj;
        return msg.fingerPrint.equals(fingerPrint);
    }

    public void send(final Callback callback) {
        IMClientManager.getInstance().sendMsg(this, new Callback() {
            @Override
            public void onBack(int code, Object obj) {
                callback.onBack(code, obj);
            }
        });
    }


    public void praseContent() {

    }

    public static IMessage jsonToIMessage(JSONObject obj) {


        IMessage.IMessageType type = IMessage.IMessageType.values()[obj.optInt("msgType")];


        String from = obj.optString("_from");
        String to = obj.optString("username");
        String content = obj.optString("content");
        IMessage msg = null;
        switch (type) {
            case TXT:
                msg = new TxtMessage(to, content);
                break;
            case IMG:
                msg = new ImgMessage(to, content);
                msg.content = content;
                msg.praseContent();
                break;
            case AUDIO:
                msg = new VoiceMessage(to, content, 0);
                msg.content = content;
                msg.praseContent();
                break;
        }
        msg.from = from;
        msg.fingerPrint = obj.optString("fingerPrint");
        long time = obj.optLong("receiveTime");
        msg.serverTime = time;
        return msg;
    }

    public void download() {
    }

    public static enum IMessageType {
        TXT, IMG, AUDIO;
    }

    public static enum IMessageState {
        CREATE, UPLOADING, UPLOAD_FAILED, UPLOAD_SUCCESS, SENDING, SEND_FAILED, SEND_SUCCESS, BERECEIVED, DOWN_SUCCESS;
    }
}

