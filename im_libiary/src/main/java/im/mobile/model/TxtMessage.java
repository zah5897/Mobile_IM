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
}
