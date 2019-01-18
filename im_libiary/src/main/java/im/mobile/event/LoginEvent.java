package im.mobile.event;

public class LoginEvent {

    public static final int TYPE_LOGIN = 0;
    public static final int TYPE_LIKE_CLOSE = 1;
    public int type;
    public int code;
    public String msg;

    public LoginEvent(int type, int code, String msg) {
        this.type = type;
        this.code = code;
        this.msg = msg;
    }

}
