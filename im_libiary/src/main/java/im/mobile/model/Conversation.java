package im.mobile.model;

import im.mobile.IMClientManager;

public class Conversation {
    public String friendUsername;
    public String editTxt;
    public String last_finger_print;
    public IMessage lastMsg;

    public int getUnReadCount() {
        return IMClientManager.getInstance().getDbHelper().getUnReadCount(friendUsername);
    }

    public void updateRead() {
        IMClientManager.getInstance().getDbHelper().updateRead(friendUsername);
    }
}
