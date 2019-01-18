package im.mobile.event;

public class IMessageDownloadSuccessEvent {
    public String fingerPrint;

    public IMessageDownloadSuccessEvent(String fingerPrint) {
        this.fingerPrint = fingerPrint;
    }
}
