package im.mobile.callback;

import net.openmob.mobileimsdk.server.protocal.Protocal;

import java.util.ArrayList;

import im.mobile.model.IMessage;

public interface IMessageListener {
    void onReceive(IMessage msg);
    void onMsgBeReceived(String theFingerPrint);

    void onOfflineMsgLoad();

}
