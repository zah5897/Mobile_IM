package im.mobile.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.List;

import im.mobile.model.IMessage;

public class CommUtils {
    private static SharedPreferences spf;

    public static boolean isSdcardExist() {
        return android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
    }

    public static boolean isAppRunningForeground(Context paramContext) {
        ActivityManager localActivityManager = (ActivityManager) paramContext.getSystemService(Context.ACTIVITY_SERVICE);
        try {
            List localList = localActivityManager.getRunningTasks(1);
            if ((localList == null) || (localList.size() < 1))
                return false;
            boolean bool = paramContext.getPackageName().equalsIgnoreCase(((ActivityManager.RunningTaskInfo) localList.get(0)).baseActivity.getPackageName());
            return bool;
        } catch (SecurityException localSecurityException) {
            localSecurityException.printStackTrace();
        }
        return false;
    }

    public static String getMessageDigest(IMessage message, Context context) {
        String digest = "";
        switch (message.type) {
            case IMG:
                digest = "[图片]";
                break;
            case AUDIO:
                digest = "[语音]";
                break;
            case TXT:
                digest = message.content;
                break;
            default:
                return "";
        }

        return digest;
    }

    private static SharedPreferences getSharedPreferences(Context context) {

        if (spf == null) {
            spf = context.getSharedPreferences("_im_l_cache", Context.MODE_PRIVATE);
        }
        return spf;

    }

    public static void saveShareSPFValue(Context context, String key, String vale) {
        getSharedPreferences(context).edit().putString(key, vale).commit();
    }

    public static String getShareSPFValue(Context context, String key) {
        return getSharedPreferences(context).getString(key, "");
    }

    public static void removeShareSPF(Context context, String key) {
        getSharedPreferences(context).edit().remove(key).commit();
    }

}
