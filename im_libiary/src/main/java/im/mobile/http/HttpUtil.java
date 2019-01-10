package im.mobile.http;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import im.mobile.IMClientManager;
import im.mobile.model.IMessage;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HttpUtil {
    static OkHttpClient okHttpClient;
    private static final String ROOT_URL = "http://192.168.50.15:8081";

    private static OkHttpClient getClient() {
        if (okHttpClient == null) {
            okHttpClient = new OkHttpClient.Builder()
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();
        }
        return okHttpClient;
    }

    public static void getMsgHistory(String withUser, String loginUsername, String fingerPrint, int pageSize) {
        Request request = new Request.Builder().url(ROOT_URL + "/msg/history/" + loginUsername + "?target=" + withUser + "&fingerPrint=" + fingerPrint + "&pageSize=" + pageSize).build();
        getClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body().string();
                try {
                    JSONObject jsonObject = new JSONObject(body);
                    JSONArray msgsArray = jsonObject.optJSONArray("data");
                    int len = msgsArray.length();
                    for (int i = 0; i < len; i++) {
                        JSONObject obj = msgsArray.getJSONObject(i);


                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }


    public static void getOfflineMsgs(final String loginUsername, final String fingerPrint, final int pageSize) {
        String url = ROOT_URL + "/msg/offline/" + loginUsername + "?fingerPrint=" + fingerPrint + "&pageSize=" + pageSize;
        Request request = new Request.Builder().url(url).build();
        getClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body().string();
                try {
                    JSONObject jsonObject = new JSONObject(body);
                    JSONArray msgsArray = jsonObject.optJSONArray("data");
                    int len = msgsArray.length();
                    String last_fingerPrint = null;
                    List<IMessage> msgs = new ArrayList<>();
                    for (int i = 0; i < len; i++) {
                        IMessage msg = jsonToIMessage(msgsArray.getJSONObject(i));
                        if (i == 0) {
                            last_fingerPrint = msg.fingerPrint;
                        }
                        msg.readState = 1;//未读
                        msgs.add(msg);
                    }
                    IMClientManager.getInstance().getDbHelper().saveOfflineMsgs(msgs);
                    if (!TextUtils.isEmpty(last_fingerPrint)) {
                        getOfflineMsgs(loginUsername, last_fingerPrint, pageSize);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }


    private static IMessage jsonToIMessage(JSONObject obj) {

        IMessage msg = new IMessage();
        msg.from = obj.optString("_from");
        msg.to = obj.optString("username");
        msg.serverTime = obj.optLong("receiveTime");
        msg.type = IMessage.IMessageType.values()[obj.optInt("msgType")];
        msg.content = obj.optString("content");
        msg.fingerPrint = obj.optString("fingerPrint");
//        chatType: "Chat",
        return msg;
    }
}
