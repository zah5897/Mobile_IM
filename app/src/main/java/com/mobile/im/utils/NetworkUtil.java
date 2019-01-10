package com.mobile.im.utils;

import android.content.Context;
import android.net.ConnectivityManager;

public class NetworkUtil {
    public static boolean checkNetworkState(Context context) {
        boolean flag = false;
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        if (manager.getActiveNetworkInfo() != null) {
            flag = manager.getActiveNetworkInfo().isAvailable();
        }
        return flag;
    }
}
