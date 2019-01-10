package com.mobile.im.utils.http;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

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

    public static void getMsgHistory(String withUser, String loginUsername, long lastId, int pageSize) {
        Request request = new Request.Builder().url(ROOT_URL + "/msg/history/" + withUser + "?username=" + loginUsername + "&lastId=" + lastId + "&pageSize=" + pageSize).build();
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
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });


    }

}
