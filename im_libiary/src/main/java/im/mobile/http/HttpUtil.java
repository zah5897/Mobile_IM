package im.mobile.http;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import im.mobile.IMClientManager;
import im.mobile.MsgManager;
import im.mobile.model.IMessage;
import im.mobile.utils.PathUtil;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpUtil {
    static OkHttpClient okHttpClient;
    private static final String ROOT_URL = "http://180.150.184.207/simple_im";
//    private static final String ROOT_URL = "http://192.168.50.15:8081";
    private static final String MSG_IMG_PATH = "/static/msg/imgs";
    private static final String MSG_VOICE_PATH = "/static/msg/voice";


    public static String getMsgImageThumbUrl(String name) {
        return ROOT_URL + MSG_IMG_PATH + "/thumb/" + name;
    }

    public static String getMsgImageOriginUrl(String name) {
        return ROOT_URL + MSG_IMG_PATH + "/origin/" + name;
    }

    public static String getMsgVoiceUrl(String name) {
        return ROOT_URL + MSG_VOICE_PATH + "/" + name;
    }

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
                        IMessage msg = IMessage.jsonToIMessage(msgsArray.getJSONObject(i));
                        if (i == 0) {
                            last_fingerPrint = msg.fingerPrint;
                        }
                        msg.readState = 1;//未读
                        msgs.add(msg);
                    }
                    MsgManager.getManager().saveOfflineMsgs(msgs);
                    if (!TextUtils.isEmpty(last_fingerPrint)) {
                        getOfflineMsgs(loginUsername, last_fingerPrint, pageSize);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static void uploadImage(String username, String filePath, final im.mobile.callback.Callback callback) {
        String url = ROOT_URL + "/upload/image/" + username;
        String shortName = filePath.substring(filePath.lastIndexOf("/") + 1, filePath.length());
        RequestBody fileBody = RequestBody.create(MediaType.parse("application/octet-stream"), new File(filePath));
        MultipartBody body = new MultipartBody.Builder()
                .setType(MediaType.parse("multipart/form-data"))
                .addFormDataPart("file", shortName, fileBody)
                .build();
        Request request = new Request.Builder()
                .post(body)
                .url(url)
                .build();

        getClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onBack(-1, e.getCause());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body().string();
                try {
                    JSONObject jsonObject = new JSONObject(body);
                    Object data = jsonObject.get("data");
                    callback.onBack(jsonObject.optInt("code"), data);
                } catch (Exception e) {
                    callback.onBack(-1, e.getMessage());
                }
            }
        });
    }

    public static void uploadVoice(String username, String filePath, int voiceTimeLen, final im.mobile.callback.Callback callback) {
        String url = ROOT_URL + "/upload/voice/" + username;
        String shortName = filePath.substring(filePath.lastIndexOf("/") + 1, filePath.length());
        RequestBody fileBody = RequestBody.create(MediaType.parse("application/octet-stream"), new File(filePath));
        MultipartBody body = new MultipartBody.Builder()
                .setType(MediaType.parse("multipart/form-data"))
                .addFormDataPart("file", shortName, fileBody)
                .addFormDataPart("voiceTimeLen", String.valueOf(voiceTimeLen))
                .build();
        Request request = new Request.Builder()
                .post(body)
                .url(url)
                .build();

        getClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onBack(-1, e.getCause());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body().string();
                try {
                    JSONObject jsonObject = new JSONObject(body);
                    Object data = jsonObject.get("data");
                    callback.onBack(jsonObject.optInt("code"), data);
                } catch (Exception e) {
                    callback.onBack(-1, e.getMessage());
                }
            }
        });
    }


    public static void downloadVoice(final String fileName, final im.mobile.callback.Callback callback) {
        String url = getMsgVoiceUrl(fileName);
        Request request = new Request.Builder().url(url).build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onBack(-1, e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                InputStream is = null;
                byte[] buf = new byte[2048];
                int len = 0;
                FileOutputStream fos = null;
                // 储存下载文件的目录
                File file = new File(PathUtil.getVoicePath(fileName));
                try {
                    is = response.body().byteStream();
                    long total = response.body().contentLength();
                    fos = new FileOutputStream(file);
                    long sum = 0;
                    while ((len = is.read(buf)) != -1) {
                        fos.write(buf, 0, len);
                        sum += len;
                        int progress = (int) (sum * 1.0f / total * 100);
                        // 下载中更新进度条
                    }
                    fos.flush();
                    // 下载完成
                    callback.onBack(0, file.getAbsolutePath());
                } catch (Exception e) {
                    callback.onBack(-1, e.getMessage());
                } finally {
                    try {
                        if (is != null)
                            is.close();
                    } catch (IOException e) {
                    }
                    try {
                        if (fos != null)
                            fos.close();
                    } catch (IOException e) {
                    }
                }
            }
        });
    }

    public static void regist(String username, String password, final im.mobile.callback.Callback callback) {
        String url = ROOT_URL + "/user/regist";

        FormBody.Builder params = new FormBody.Builder();
        params.add("username", username);
        params.add("password", password);
        Request request = new Request.Builder().url(url).post(params.build()).build();
        getClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onBack(-1, e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body().string();
                try {
                    JSONObject jsonObject = new JSONObject(body);
                    callback.onBack(jsonObject.optInt("code"), jsonObject.optString("msg"));
                } catch (Exception e) {
                    callback.onBack(-1, e.getMessage());
                }
            }
        });

    }


}
