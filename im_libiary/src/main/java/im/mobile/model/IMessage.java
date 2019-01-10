package im.mobile.model;

import java.util.Date;

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

    @Override
    public boolean equals(Object obj) {
        IMessage msg = (IMessage) obj;
        return msg.fingerPrint.equals(fingerPrint);
    }

    public static enum IMessageType {
        TXT, IMG, AUDIO;
    }

    public static enum IMessageState {
        CREATE, SENDING, SEND_FAILED, SEND_SUCCESS, BERECEIVED;
    }
}

