package im.mobile.model;

import im.mobile.IMClientManager;
import im.mobile.MsgManager;

public class Conversation {
    public String friendUsername;
    public String editTxt;
    public String last_finger_print;
    public IMessage lastMsg;

    public int getUnReadCount() {
        return MsgManager.getManager().getUnReadCount(friendUsername);
    }

    public void updateRead() {
        MsgManager.getManager().updateRead(friendUsername);
    }
}
