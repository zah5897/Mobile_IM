package im.mobile.callback;

public interface IMListener {
    void onLogin(int code, String msg);

    void onLinkCloseMessage(int code, String msg);
}
