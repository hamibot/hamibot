package com.hamibot.hamibot.notification;

import android.content.Context;
import android.util.Log;

import static com.hamibot.hamibot.Constants.LOG_TAG;


public class NotificationUtil {

    static Notification notificationUtils;

    /**
     * 初始化通知
     * @param mContext
     */
    public static void init(Context mContext){
        if(notificationUtils==null){
            notificationUtils =new Notification(mContext,"channel_id_1","通知");
        }
    }

    /**
     * 发送通知方法
     * @param title
     * @param text
     */
    public static void notify(String title,String text)
    {
        if(notificationUtils==null){
            Log.e(LOG_TAG, "Notify is uninitialized,please call init method first:Notify.init(context)");
        }
        notificationUtils.sendNotification(title, text);
    }

}
