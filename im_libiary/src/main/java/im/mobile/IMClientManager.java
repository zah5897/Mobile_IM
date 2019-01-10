package im.mobile;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.util.Log;

import net.openmob.mobileimsdk.android.ClientCoreSDK;
import net.openmob.mobileimsdk.android.conf.ConfigEntity;
import net.openmob.mobileimsdk.android.core.LocalUDPDataSender;
import net.openmob.mobileimsdk.android.core.LocalUDPSocketProvider;
import net.openmob.mobileimsdk.android.event.ChatBaseEvent;
import net.openmob.mobileimsdk.android.event.ChatTransDataEvent;
import net.openmob.mobileimsdk.android.event.MessageQoSEvent;
import net.openmob.mobileimsdk.server.protocal.Protocal;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import im.mobile.callback.Callback;
import im.mobile.callback.ConversationListener;
import im.mobile.callback.IMListener;
import im.mobile.callback.IMessageListener;
import im.mobile.db.DBHelper;
import im.mobile.http.HttpUtil;
import im.mobile.model.IMessage;
import im.mobile.model.TxtMessage;


public class IMClientManager {
    private static String TAG = IMClientManager.class.getSimpleName();
    private static final String IP = "192.168.50.15";
    private static final int PORT = 8091;
    private static IMClientManager instance = null;

    /**
     * MobileIMSDK是否已被初始化. true表示已初化完成，否则未初始化.
     */
    private boolean init = false;


    private Context context;
    private List<IMListener> imListeners;
    private List<IMessageListener> iMessageListeners;
    private List<ConversationListener> conversationListeners;

    private DBHelper dbHelper;
    private SharedPreferences spf;

    private IMClientManager(Context context) {
        this.context = context;
        imListeners = new ArrayList<>();
        iMessageListeners = new ArrayList<>();
        conversationListeners = new ArrayList<>();
    }

    public static IMClientManager getInstance(Context context) {
        if (instance == null) {
            instance = new IMClientManager(context);
        }
        return instance;
    }


    public void registIMListener(IMListener imListener) {
        if (imListener != null && !imListeners.contains(imListener)) {
            imListeners.add(imListener);
        }
    }

    public void unRegistIMListener(IMListener imListener) {
        if (imListener != null && imListeners.contains(imListener)) {
            imListeners.remove(imListener);
        }
    }


    public void registIMessageListener(IMessageListener imListener) {
        if (imListener != null && !iMessageListeners.contains(imListener)) {
            iMessageListeners.add(imListener);
        }
    }

    public void unRegistIMessageListener(IMessageListener imListener) {
        if (imListener != null && iMessageListeners.contains(imListener)) {
            iMessageListeners.remove(imListener);
        }
    }


    public void registConversationListener(ConversationListener listener) {
        if (listener != null && !conversationListeners.contains(listener)) {
            conversationListeners.add(listener);
        }
    }

    public void unRegistConversationListener(ConversationListener listener) {
        if (listener != null && conversationListeners.contains(listener)) {
            iMessageListeners.remove(listener);
        }
    }


    public List<IMessageListener> getIMessageListeners() {
        List<IMessageListener> listeners = new ArrayList<>();
        listeners.addAll(iMessageListeners);
        return listeners;
    }


    public void doNotifyConversationRefresh() {
        List<ConversationListener> listeners = new ArrayList<>();
        listeners.addAll(conversationListeners);
        for (ConversationListener listener : listeners) {
            listener.onChange();
        }
    }


    public void doNotifyOfflineLoad() {
        List<IMessageListener> listeners = getIMessageListeners();
        for (IMessageListener listener : listeners) {
            listener.onOfflineMsgLoad();
        }
    }

    public static IMClientManager getInstance() {
        return instance;
    }

    private void initLocalRes() {
        dbHelper = new DBHelper(context);
        dbHelper.init();
    }

    //获取离校消息
    public void startLoadOfflineMsgs() {
        HttpUtil.getOfflineMsgs(getCurrentLoginUsername(), "", 20);
    }

    public void initMobileIMSDK() {
        if (!init) {
            // 设置AppKey
            ConfigEntity.appKey = "5418023dfd98c579b6001741";
            // MobileIMSDK核心IM框架的敏感度模式设置
//			ConfigEntity.setSenseMode(SenseMode.MODE_10S);

            // 开启/关闭DEBUG信息输出
//	    	ClientCoreSDK.DEBUG = false;


            // 【特别注意】请确保首先进行核心库的初始化（这是不同于iOS和Java端的地方)
            ClientCoreSDK.getInstance().init(context);
            spf = context.getSharedPreferences("_im_l_cache", Context.MODE_PRIVATE);
            // 设置事件回调
            ClientCoreSDK.getInstance().setChatBaseEvent(new ChatBaseEvent() {
                @Override
                public void onLoginMessage(int dwErrorCode) {
                    Log.d(TAG, "onLoginMessage:code=" + dwErrorCode);
                    if (dwErrorCode == 0) {
                        cacheLoginInfo();
                        //开始获取离线消息
                        startLoadOfflineMsgs();
                    }
                    //通知回调
                    for (IMListener listener : imListeners) {
                        if (dwErrorCode == 0) {
                            listener.onLogin(dwErrorCode, "IM服务器登录/重连成功");

                        } else {
                            listener.onLogin(dwErrorCode, "IM服务器登录/连接失败");
                        }
                    }
                }

                @Override
                public void onLinkCloseMessage(int dwErrorCode) {
                    Log.e(TAG, "【DEBUG_UI】与IM服务器的网络连接出错关闭了，error：" + dwErrorCode);
                    for (IMListener listener : imListeners) {
                        listener.onLinkCloseMessage(dwErrorCode, "与IM服务器的连接已断开, 自动登陆/重连将启动!");
                    }
                }
            });
            ClientCoreSDK.getInstance().setChatTransDataEvent(new ChatTransDataEvent() {
                @Override
                public void onTransBuffer(String fingerPrintOfProtocal, String userid, String dataContent, int typeu, long serverTime) {
                    Log.d(TAG, "【DEBUG_UI】[typeu=" + typeu + "]收到来自用户" + userid + "的消息:" + dataContent);

                    List<IMessageListener> listeners = getIMessageListeners();

                    IMessage msg = new IMessage();
                    msg.type = IMessage.IMessageType.values()[typeu];
                    msg.content = dataContent;
                    msg.from = userid;
                    msg.to = getCurrentLoginUsername();
                    msg.fingerPrint = fingerPrintOfProtocal;
                    msg.serverTime = serverTime;
                    if (listeners.isEmpty()) {
                        //这里说明并无在聊天界面，则消息为未读
                        msg.readState = 1;
                    }
                    dbHelper.saveMsg(msg);
                    for (IMessageListener listener : listeners) {
                        listener.onReceive(msg);
                    }
                }

                @Override
                public void onErrorResponse(int errorCode, String errorMsg) {
                    Log.d(TAG, "【DEBUG_UI】收到服务端错误消息，errorCode=" + errorCode + ", errorMsg=" + errorMsg);
//                    if (errorCode == ErrorCode.ForS.RESPONSE_FOR_UNLOGIN)
//                        this.mainGUI.showIMInfo_brightred("服务端会话已失效，自动登陆/重连将启动! (" + errorCode + ")");
//                    else
//                        this.mainGUI.showIMInfo_red("Server反馈错误码：" + errorCode + ",errorMsg=" + errorMsg);
                }
            });
            ClientCoreSDK.getInstance().setMessageQoSEvent(new MessageQoSEvent() {
                @Override
                public void messagesLost(ArrayList<Protocal> lostMessages) {
                    Log.d(TAG, "【DEBUG_UI】收到系统的未实时送达事件通知，当前共有" + lostMessages.size() + "个包QoS保证机制结束，判定为【无法实时送达】！");
//
//                    if (this.mainGUI != null) {
//                        this.mainGUI.showIMInfo_brightred("[消息未成功送达]共" + lostMessages.size() + "条!(网络状况不佳或对方id不存在)");
//                    }
                }

                @Override
                public void messagesBeReceived(String theFingerPrint, long serverTime) {
                    if (theFingerPrint != null) {
                        dbHelper.updateMsgStateBeReceived(theFingerPrint, serverTime);
                        List<IMessageListener> listeners = getIMessageListeners();
                        for (IMessageListener listener : listeners) {
                            listener.onMsgBeReceived(theFingerPrint);
                        }
                        Log.d(TAG, "【DEBUG_UI】收到对方已收到消息事件的通知，fp=" + theFingerPrint);
//                        if (this.mainGUI != null) {
//                            this.mainGUI.showIMInfo_blue("[收到对方消息应答]fp=" + theFingerPrint);
//                        }
                    }
                }
            });
            init = true;
        }

    }


    @SuppressLint("NewApi")
    public void reLogin(Callback callback) {
        String username = spf.getString("id", null);
        String token = spf.getString("token", null);

        if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(token)) {
            login(username, token, callback);
        } else {
            callback.onBack(-1, "请到登陆页");
        }
    }


    public void release() {
        ClientCoreSDK.getInstance().release();
        resetInitFlag();
    }

    /**
     * 重置init标识。
     * <p>
     * <b>重要说明：</b>不退出APP的情况下，重新登陆时记得调用一下本方法，不然再
     */
    public void resetInitFlag() {
        init = false;
    }


    public void login(String username, String password, Callback callback) {
        login(username, password, IP, PORT, callback);
    }


    public void login(String username, String password, String ip, int port, final Callback callback) {
        LocalUDPSocketProvider.getInstance().closeLocalUDPSocket();
        ConfigEntity.serverIP = ip;
        if (port > 65536 || port < 1000) {
            throw new RuntimeException("端口数据异常");
        }
        ConfigEntity.serverUDPPort = port;
        LocalUDPSocketProvider.getInstance().closeLocalUDPSocket();


        new LocalUDPDataSender.SendLoginDataAsync(context, username, password) {
            /**
             * 登陆信息发送完成后将调用本方法（注意：此处仅是登陆信息发送完成
             * ，真正的登陆结果要在异步回调中处理哦）。
             *
             * @param code 数据发送返回码，0 表示数据成功发出，否则是错误码
             */
            @Override
            protected void fireAfterSendLogin(int code) {
                initLocalRes();//此时已经保存有登陆信息，可以执行初始化数据库操作
                if (callback != null) {
                    callback.onBack(code, "");
                }
            }
        }.execute();
    }

    public void cacheLoginInfo() {


        String username = getCurrentLoginUsername();
        if (TextUtils.isEmpty(username)) {
            return;
        }
        String token = ClientCoreSDK.getInstance().getCurrentLoginToken();
        if (TextUtils.isEmpty(token)) {
            return;
        }
        SharedPreferences.Editor editor = spf.edit();
        editor.putString("id", username);
        editor.putString("token", token);
        editor.commit();
    }


    public String getCurrentLoginUsername() {
        return ClientCoreSDK.getInstance().getCurrentLoginUsername();
    }

    public long getCurrentServerTime() {
        return ClientCoreSDK.getInstance().getServerTime();
    }

    public boolean isConnectedToServer() {
        return ClientCoreSDK.getInstance().isConnectedToServer();
    }


    public int sendLogout() {
        return LocalUDPDataSender.getInstance(context).sendLoginout();
    }


    @TargetApi(Build.VERSION_CODES.CUPCAKE)
    public void sendMsg(final IMessage msg, final Callback callback) {
        msg.from = getCurrentLoginUsername();
        msg.serverTime = getCurrentServerTime();
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
                dbHelper.updateMsgState(message.fingerPrint, getCurrentServerTime(), message.state.ordinal());
                callback.onBack(code, message); //code=0 成功
            }
        }.execute();
    }


    public static final int PAGE_SIZE = 20;

    public void loadHistoryMsg(String with, Long lastId) {
        // HttpUtil.getMsgHistory(with, getCurrentLoginUsername(), lastId == null ? Long.MAX_VALUE : lastId, PAGE_SIZE);
    }

    public DBHelper getDbHelper() {
        return dbHelper;
    }
}
