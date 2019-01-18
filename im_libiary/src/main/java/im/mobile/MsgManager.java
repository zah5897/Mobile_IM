package im.mobile;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import net.openmob.mobileimsdk.android.core.LocalUDPDataSender;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

import im.mobile.callback.Callback;
import im.mobile.db.DBHelper;
import im.mobile.event.ConversationRefreshEvent;
import im.mobile.event.IMessageBeReceiveEvent;
import im.mobile.event.IMessageDownloadSuccessEvent;
import im.mobile.http.HttpUtil;
import im.mobile.model.Conversation;
import im.mobile.model.IMessage;
import im.mobile.model.ImgMessage;
import im.mobile.model.TxtMessage;
import im.mobile.model.VoiceMessage;
import im.mobile.utils.AppNotifier;

public class MsgManager {

    private static MsgManager manager;
    private DBHelper dbHelper;
    private AppNotifier notifyer;

    private MsgManager() {
    }

    public AppNotifier getNotifyer() {
        return notifyer;
    }

    public static MsgManager getManager() {
        if (manager == null) {
            manager = new MsgManager();
        }
        return manager;
    }

    public void release() {
        notifyer = null;
        closeDB();
    }

    public void initAppNotifier(Context context) {
        notifyer = new AppNotifier(context);
    }

    public void initDB(Context context) {
        dbHelper = new DBHelper(context);
        dbHelper.init();
    }

    public void closeDB() {
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

    @SuppressLint("NewApi")
    public void sendMsg(Context context, final IMessage msg, final Callback callback) {
        msg.from = IMClientManager.getInstance().getCurrentLoginUsername();
        msg.serverTime = IMClientManager.getInstance().getCurrentServerTime();
        dbHelper.saveMsg(msg);
        new LocalUDPDataSender.SendCommonDataAsync(context, msg)//, true)
        {
            @Override
            protected void onPostExecute(Integer code) {
                if (code == 0) {
                    message.state = IMessage.IMessageState.SEND_SUCCESS;
                } else {
                    message.state = IMessage.IMessageState.SEND_FAILED;
                }
                dbHelper.updateMsgState(message.fingerPrint, IMClientManager.getInstance().getCurrentServerTime(), message.state.ordinal());
                callback.onBack(code, message); //code=0 成功
            }
        }.execute();
    }

    public void receiveMsg(String fingerPrint, String from, String dataContent, int typeu, long serverTime) {
        IMessage.IMessageType type = IMessage.IMessageType.values()[typeu];
        IMessage msg = null;
        String loginUsername = IMClientManager.getInstance().getCurrentLoginUsername();
        switch (type) {
            case TXT:
                msg = TxtMessage.create(from, loginUsername, dataContent, fingerPrint, serverTime);
                msg.readState = 1;
                break;
            case IMG:
                msg = ImgMessage.create(from, loginUsername, dataContent, fingerPrint, serverTime);
                msg.readState = 1;
                break;
            case AUDIO:
                msg = VoiceMessage.create(from, loginUsername, dataContent, fingerPrint, serverTime);
                msg.praseContent();
                msg.download();
                break;
        }
        if (!IMClientManager.getInstance().hasForegroundActivies()) {
            notifyer.notify(msg);
        }
        dbHelper.saveMsg(msg);
        //接收到新消息
        EventBus.getDefault().post(msg);
    }


    public List<Conversation> loadConversations() {
        return dbHelper.loadConversations();
    }

    public int getUnReadCount(String friendUsername) {
        return dbHelper.getUnReadCount(friendUsername);
    }

    public void updateRead(String friendUsername) {
        dbHelper.updateRead(friendUsername);
    }

    public void saveOfflineMsgs(List<IMessage> msgs) {
        dbHelper.saveOfflineMsgs(msgs);
        startLoadOfflineMsgs();
    }

    public void messagesBeReceived(String fingerPrint, long serverTime) {
        if (fingerPrint != null) {
            dbHelper.updateMsgStateBeReceived(fingerPrint, serverTime);
            EventBus.getDefault().post(new IMessageBeReceiveEvent(fingerPrint));
        }
    }

    //获取离校消息
    public void startLoadOfflineMsgs() {
        HttpUtil.getOfflineMsgs(IMClientManager.getInstance().getCurrentLoginUsername(), "", 20);
    }

    public void updateFileMsgState(String fingerPrint, String content, long serverTime, int state) {
        dbHelper.updateFileMsgState(fingerPrint, content, IMClientManager.getInstance().getCurrentServerTime(), state);
    }

    public void updateVoiceLocalPath(String finger_print, String localPath, int state) {
        dbHelper.updateVoiceLocalPath(finger_print, localPath, state);
    }


    public List<IMessage> loadMessages(String friendName) {
        return dbHelper.loadMessages(friendName);
    }

    public void notifyConversationRefresh() {
        EventBus.getDefault().post(new ConversationRefreshEvent());
    }

    public void notifyMsgDownloadSuccess(String fingerPrint) {
        EventBus.getDefault().post(new IMessageDownloadSuccessEvent(fingerPrint));
    }


}
