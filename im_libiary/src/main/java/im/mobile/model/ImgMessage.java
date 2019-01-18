package im.mobile.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;
import java.util.UUID;

import im.mobile.IMClientManager;
import im.mobile.MsgManager;
import im.mobile.callback.Callback;
import im.mobile.http.HttpUtil;

import static im.mobile.model.IMessage.IMessageState.UPLOADING;
import static im.mobile.model.IMessage.IMessageState.UPLOAD_FAILED;
import static im.mobile.model.IMessage.IMessageState.UPLOAD_SUCCESS;

public class ImgMessage extends IMessage {

    public ImgMessage(String to, String imagePath) {
        this.type = IMessageType.IMG;
        this.localPath = imagePath;
        this.fingerPrint = UUID.randomUUID().toString();
        this.to = to;
        this.state = UPLOADING;
    }

    public ImgMessage() {
        this.type = IMessageType.IMG;
    }

    public static ImgMessage create(String from, String to, String content, String fingerPrint, long serverTime) {
        ImgMessage img = new ImgMessage();
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
            localPath = HttpUtil.getMsgImageThumbUrl(name);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void send(Callback callback) {
        this.from = IMClientManager.getInstance().getCurrentLoginUsername();
        uploadImage(callback);
    }

    public void uploadImage(final Callback callback) {
        HttpUtil.uploadImage(IMClientManager.getInstance().getCurrentLoginUsername(), localPath, new Callback() {
            @Override
            public void onBack(int code, Object msg) {
                if (code == 0) {
                    ImgMessage.this.content = msg.toString();
                    ImgMessage.this.state = UPLOAD_SUCCESS;
                } else {
                    ImgMessage.this.state = UPLOAD_FAILED;
                }

                MsgManager.getManager().updateFileMsgState(fingerPrint, content, IMClientManager.getInstance().getCurrentServerTime(), state.ordinal());
                callback.onBack(0, ImgMessage.this);
                ImgMessage.super.send(callback);
            }
        });
    }
}
