package im.mobile;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
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

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import im.mobile.callback.Callback;
import im.mobile.db.DBHelper;
import im.mobile.event.LoginEvent;
import im.mobile.http.HttpUtil;
import im.mobile.model.IMessage;
import im.mobile.utils.AppNotifier;
import im.mobile.utils.CommUtils;


public class IMClientManager {
    private static String TAG = IMClientManager.class.getSimpleName();
    private static final String IP = "180.150.184.207";
    private static final int PORT = 8091;
    private static IMClientManager instance = null;
    /**
     * MobileIMSDK是否已被初始化. true表示已初化完成，否则未初始化.
     */
    private boolean init = false;
    private Context context;
    private Timer timer;

    private boolean isLogined;

    private IMClientManager(Context context) {
        this.context = context;
    }

    public boolean isLogined() {
        return isLogined;
    }

    public static IMClientManager getInstance(Context context) {
        if (instance == null) {
            instance = new IMClientManager(context);
        }
        return instance;
    }

    private List<Activity> activityList = new ArrayList<Activity>();

    public void pushActivity(Activity activity) {
        if (!activityList.contains(activity)) {
            activityList.add(0, activity);
        }
    }

    public void popActivity(Activity activity) {
        activityList.remove(activity);
    }

    public boolean hasForegroundActivies() {
        return activityList.size() != 0;
    }


    public static IMClientManager getInstance() {
        return instance;
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
            // 设置事件回调
            ClientCoreSDK.getInstance().setChatBaseEvent(new ChatBaseEvent() {
                @Override
                public void onLoginMessage(int dwErrorCode) {
                    Log.d(TAG, "onLoginMessage:code=" + dwErrorCode);
                    if (dwErrorCode == 0) {
                        isLogined = true;
                        cacheLoginInfo();
                        //开始获取离线消息
                        MsgManager.getManager().startLoadOfflineMsgs();
                    }
                    //通知回调
                    if (timer != null) {
                        timer.cancel();
                    }
                    EventBus.getDefault().post(new LoginEvent(LoginEvent.TYPE_LOGIN, dwErrorCode, dwErrorCode == 0 ? "IM服务器登录/重连成功" : "IM服务器登录/连接失败"));
                }

                @Override
                public void onLinkCloseMessage(int dwErrorCode) {
                    Log.e(TAG, "【DEBUG_UI】与IM服务器的网络连接出错关闭了，error：" + dwErrorCode);
                    EventBus.getDefault().post(new LoginEvent(LoginEvent.TYPE_LIKE_CLOSE, dwErrorCode, "与IM服务器的连接已断开, 自动登陆/重连将启动!"));
                }
            });
            ClientCoreSDK.getInstance().setChatTransDataEvent(new ChatTransDataEvent() {
                @Override
                public void onTransBuffer(String fingerPrintOfProtocal, String userid, String dataContent, int typeu, long serverTime) {
                    MsgManager.getManager().receiveMsg(fingerPrintOfProtocal, userid, dataContent, typeu, serverTime);
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
                }

                @Override
                public void messagesBeReceived(String theFingerPrint, long serverTime) {
                    MsgManager.getManager().messagesBeReceived(theFingerPrint, serverTime);
                }
            });
            MsgManager.getManager().initAppNotifier(context);
            init = true;
        }

    }

    public void release() {
        ClientCoreSDK.getInstance().release();
        MsgManager.getManager().release();
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

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            callback.onBack(-1, "跳转到登录页面");
            return;
        }
        login(username, password, IP, PORT, callback);
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                EventBus.getDefault().post(new LoginEvent(LoginEvent.TYPE_LOGIN, -2, "登录超时"));
            }
        }, 1000 * 10); //10秒超时
    }

    public void closeLocalUDPSocket() {
        LocalUDPSocketProvider.getInstance().closeLocalUDPSocket();
    }

    @SuppressLint("NewApi")
    public void login(String username, String password, String ip, int port, final Callback callback) {
        closeLocalUDPSocket();
        ConfigEntity.serverIP = ip;
        if (port > 65536 || port < 1000) {
            throw new RuntimeException("端口数据异常");
        }
        ConfigEntity.serverUDPPort = port;
        closeLocalUDPSocket();


        new LocalUDPDataSender.SendLoginDataAsync(context, username, password) {
            /**
             * 登陆信息发送完成后将调用本方法（注意：此处仅是登陆信息发送完成
             * ，真正的登陆结果要在异步回调中处理哦）。
             *
             * @param code 数据发送返回码，0 表示数据成功发出，否则是错误码
             */
            @Override
            protected void fireAfterSendLogin(int code) {
                MsgManager.getManager().initDB(context);//此时已经保存有登陆信息，可以执行初始化数据库操作
                if (callback != null) {
                    callback.onBack(code, "");
                }
                if (code != 0 && timer != null) { //说明消息没有发成功，需要做超时设定
                    timer.cancel();
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
        CommUtils.saveShareSPFValue(context, "id", username);
        CommUtils.saveShareSPFValue(context, "token", token);
    }

    public void clearLoginCache() {
        CommUtils.removeShareSPF(context, "id");
        CommUtils.removeShareSPF(context, "token");
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
        int code = LocalUDPDataSender.getInstance(context).sendLoginout();
        MsgManager.getManager().closeDB();
        isLogined = false;
        return code;
    }


    public void sendMsg(IMessage msg, Callback callback) {
        MsgManager.getManager().sendMsg(context, msg, callback);

    }

    public AppNotifier getNotifyer() {
        return MsgManager.getManager().getNotifyer();
    }
}
