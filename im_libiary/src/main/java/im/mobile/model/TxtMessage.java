package im.mobile.model;

import java.util.Date;
import java.util.UUID;

public class TxtMessage extends IMessage {
    public TxtMessage(String to, String content) {
        this.type = IMessageType.TXT;
        this.fingerPrint = UUID.randomUUID().toString();
        this.to = to;
        this.content = content;
    }

    public TxtMessage() {
        this.type = IMessageType.TXT;
    }

    public static TxtMessage create(String from, String to, String content, String fingerPrint, long serverTime) {
        TxtMessage img = new TxtMessage();
        img.from = from;
        img.content = content;
        img.to = to;
        img.fingerPrint = fingerPrint;
        img.serverTime = serverTime;
        return img;
    }
}
