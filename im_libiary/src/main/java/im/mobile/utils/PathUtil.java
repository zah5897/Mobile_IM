package im.mobile.utils;

import android.os.Environment;

import java.io.File;

public class PathUtil {

    public static final String ROOT_PATH = "/m_im";
    public static final String PATH_VOICE = "/voices";

    public static String getVoicePath(String name) {
        String path = Environment.getExternalStorageDirectory().getPath() + ROOT_PATH + PATH_VOICE;
        File dir = new File(path);
        dir.mkdirs();
        return dir.getAbsolutePath() + name;
    }
}
