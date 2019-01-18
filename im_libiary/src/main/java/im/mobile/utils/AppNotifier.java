/************************************************************
 *  * Hyphenate CONFIDENTIAL 
 * __________________ 
 * Copyright (C) 2016 Hyphenate Inc. All rights reserved. 
 *
 * NOTICE: All information contained herein is, and remains 
 * the property of Hyphenate Inc.
 * Dissemination of this information or reproduction of this material 
 * is strictly forbidden unless prior written permission is obtained
 * from Hyphenate Inc.
 */
package im.mobile.utils;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;


import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import im.mobile.model.IMessage;

/**
 * new message notifier class
 * <p>
 * this class is subject to be inherited and implement the relative APIs
 * <p>
 * <p>
 * 在Android 8.0之前的设备上:
 * 通知栏通知的声音和震动可以被demo设置中的'声音'和'震动'开关控制
 * 在Android 8.0设备上:
 * 通知栏通知的声音和震动不受demo设置中的'声音'和'震动'开关控制
 */
public class AppNotifier {
    private final static String TAG = "AppNotifier";

    protected final static String MSG_ENG = "%s contacts sent %s messages";
    protected final static String MSG_CH = "%s个联系人发来%s条消息";

    protected static int NOTIFY_ID = 0525; // start notification id

    protected static final String CHANNEL_ID = "simple_im_notification";
    protected static final long[] VIBRATION_PATTERN = new long[]{0, 180, 80, 120};

    protected NotificationManager notificationManager = null;

    protected HashSet<String> fromUsers = new HashSet<>();
    protected int notificationNum = 0;

    protected Context appContext;
    protected String packageName;
    protected String msg;
    protected long lastNotifyTime;
    protected Ringtone ringtone = null;
    protected AudioManager audioManager;
    protected Vibrator vibrator;

    private NotificationInfoProvider provider;

    @SuppressLint("NewApi")
    public AppNotifier(Context context) {
        appContext = context.getApplicationContext();
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= 26) {
            // Create the notification channel for Android 8.0
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "simple_im msg default channel.", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setVibrationPattern(VIBRATION_PATTERN);
            notificationManager.createNotificationChannel(channel);
        }
        packageName = appContext.getApplicationInfo().packageName;
        if (Locale.getDefault().getLanguage().equals("zh")) {
            msg = MSG_CH;
        } else {
            msg = MSG_ENG;
        }
        audioManager = (AudioManager) appContext.getSystemService(Context.AUDIO_SERVICE);
        vibrator = (Vibrator) appContext.getSystemService(Context.VIBRATOR_SERVICE);
    }

    public void setProvider(NotificationInfoProvider provider) {
        this.provider = provider;
    }

    /**
     * this function can be override
     */
    public void reset() {
        resetNotificationCount();
        cancelNotification();
    }

    void resetNotificationCount() {
        notificationNum = 0;
        fromUsers.clear();
    }

    void cancelNotification() {
        if (notificationManager != null)
            notificationManager.cancel(NOTIFY_ID);
    }

    /**
     * handle the new message
     * this function can be override
     *
     * @param message
     */
    public synchronized void notify(IMessage message) {
        //需要过滤非必要的通知类型消息

        //需要检查通知设置

        // check if app running background
        if (!CommUtils.isAppRunningForeground(appContext)) {
            notificationNum++;
            fromUsers.add(message.from);
            handleMessage(message);
        }
    }

    public synchronized void notify(List<IMessage> messages) {


        // check if app running background
        if (!CommUtils.isAppRunningForeground(appContext)) {
            for (IMessage message : messages) {
                notificationNum++;
                fromUsers.add(message.from);
            }
            handleMessage(messages.get(messages.size() - 1));
        }
    }

    public synchronized void notify(String content) {
        if (!CommUtils.isAppRunningForeground(appContext)) {
            try {
                NotificationCompat.Builder builder = generateBaseBuilder(content);
                Notification notification = builder.build();
                notificationManager.notify(NOTIFY_ID, notification);

                if (Build.VERSION.SDK_INT < 26) {
                    vibrateAndPlayTone(null);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * send it to notification bar
     * This can be override by subclass to provide customer implementation
     *
     * @param message
     */
    protected void handleMessage(IMessage message) {
        try {
            int fromUsersNum = fromUsers.size();
            String notifyText = String.format(msg, fromUsersNum, notificationNum);

            NotificationCompat.Builder builder = generateBaseBuilder(notifyText);
//            builder.setContentTitle(contentTitle);
            notifyText = getDisplayedText(message);
            if (notifyText != null) {
                builder.setTicker(notifyText);
            }
            Intent i = provider.getLaunchIntent(message);
            if (i != null) {
                PendingIntent pendingIntent = PendingIntent.getActivity(appContext, NOTIFY_ID, i, PendingIntent.FLAG_UPDATE_CURRENT);
                builder.setContentIntent(pendingIntent);
            }
            // builder.setSmallIcon(smallIcon);
            Notification notification = builder.build();
            notificationManager.notify(NOTIFY_ID, notification);

            if (Build.VERSION.SDK_INT < 26) {
                vibrateAndPlayTone(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getDisplayedText(IMessage msg) {
        String ticker = CommUtils.getMessageDigest(msg, appContext);
        if (msg.type == IMessage.IMessageType.TXT) {
            ticker = ticker.replaceAll("\\[.{2,3}\\]", "[表情]");
        }
        return msg.from + ": " + ticker;
    }

    /**
     * Generate a base Notification#Builder, contains:
     * 1.Use the app icon as default icon
     * 2.Use the app name as default title
     * 3.This notification would be sent immediately
     * 4.Can be cancelled by user
     * 5.Would launch the default activity when be clicked
     *
     * @return
     */
    @SuppressLint("NewApi")
    private NotificationCompat.Builder generateBaseBuilder(String content) {
        PackageManager pm = appContext.getPackageManager();
        String title = pm.getApplicationLabel(appContext.getApplicationInfo()).toString();
        Intent i = appContext.getPackageManager().getLaunchIntentForPackage(packageName);
        PendingIntent pendingIntent = PendingIntent.getActivity(appContext, NOTIFY_ID, i, PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(appContext.getApplicationInfo().icon)
                .setContentTitle(title)
                .setTicker(content)
                .setContentText(content)
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);
    }

    /**
     * vibrate and  play tone
     */
    @SuppressLint("MissingPermission")
    public void vibrateAndPlayTone(IMessage message) {
        //需要过滤非必要的通知类型消息
        //需要检查通知设置
        //TODO

        if (System.currentTimeMillis() - lastNotifyTime < 1000) {
            // received new messages within 2 seconds, skip play ringtone
            return;
        }
        //这里需要try，防止权限不够
        try {
            lastNotifyTime = System.currentTimeMillis();
            // check if in silent mode
            if (audioManager.getRingerMode() == AudioManager.RINGER_MODE_SILENT) {
                return;
            }
            //需要判断是否允许震动
            vibrator.vibrate(VIBRATION_PATTERN, -1);

            //需要判断是否播放通知声音
            if (ringtone == null) {
                Uri notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                ringtone = RingtoneManager.getRingtone(appContext, notificationUri);
                if (ringtone == null) {
                    return;
                }
            }
            if (!ringtone.isPlaying()) {
                String vendor = Build.MANUFACTURER;
                ringtone.play();
                if (vendor != null && vendor.toLowerCase().contains("samsung")) {
                    Thread ctlThread = new Thread() {
                        public void run() {
                            try {
                                Thread.sleep(3000);
                                if (ringtone.isPlaying()) {
                                    ringtone.stop();
                                }
                            } catch (Exception e) {
                            }
                        }
                    };
                    ctlThread.run();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public interface NotificationInfoProvider {
        Intent getLaunchIntent(IMessage message);
    }
}
