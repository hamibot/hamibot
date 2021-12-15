package com.hamibot.hamibot.notification;

import android.app.*;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Vibrator;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import com.hamibot.hamibot.R;
import com.hamibot.hamibot.ui.main.MainActivity;

public class Notification extends ContextWrapper {

    private NotificationManager mManager;
    public  String sID  ;
    public  String sName ;

    public Notification(Context base, String sID, String sName) {
        super(base);
        this.sID=sID;
        this.sName=sName;
    }

    public void sendNotification(String title, String content) {
        //获取随机值避免通知覆盖
        int random = (int) System.currentTimeMillis()%Integer.MAX_VALUE;

        if (Build.VERSION.SDK_INT >= 26) {
            createNotificationChannel();
            android.app.Notification notification = getNotification_26(title, content).build();
            getmManager().notify(random, notification);
        } else {
            android.app.Notification notification = getNotification_25(title, content).build();
            getmManager().notify(random, notification);
        }
    }

    private NotificationManager getmManager() {
        if (mManager == null) {
            mManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        }
        return mManager;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(sID, sName, NotificationManager.IMPORTANCE_HIGH);
        channel.enableVibration(true);
        getmManager().createNotificationChannel(channel);
    }

    public NotificationCompat.Builder getNotification_25(String title, String content) {

        // 以下是展示大图的通知
        NotificationCompat.BigPictureStyle style = new NotificationCompat.BigPictureStyle();
        style.setBigContentTitle("BigContentTitle");
        style.setSummaryText("SummaryText");
        style.bigPicture(BitmapFactory.decodeResource(getResources(), R.drawable.hamibot_icon));

        // 以下是展示多文本通知
        NotificationCompat.BigTextStyle style1 = new NotificationCompat.BigTextStyle();
        style1.setBigContentTitle(title);
        style1.bigText(content);

        return new NotificationCompat.Builder(getApplicationContext())
                .setContentTitle(title)
                .setContentText(content)
//                .setSmallIcon(R.drawable.hamibot_icon)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.hamibot_icon))
                .setStyle(style)
                .setAutoCancel(true);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public android.app.Notification.Builder getNotification_26(String title, String content) {
        Intent intent = new Intent(this.getBaseContext(), MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this.getBaseContext(),0,intent,0);

//        //播放通知声音
//        playNotificationRing(this.getBaseContext());
//        //手机震动一下
//        playNotificationVibrate(this.getBaseContext());

        return new android.app.Notification.Builder(getApplicationContext(), sID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.drawable.hamibot_icon)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.hamibot_icon))
                //.setStyle(new Notification.BigPictureStyle().bigPicture(BitmapFactory.decodeResource(getResources(), R.drawable.hamibot_icon)))
                .setNumber(1)
                .setDefaults(1)
                .setContentIntent(pendingIntent)
                .setGroupAlertBehavior(android.app.Notification.DEFAULT_SOUND)
                .setAutoCancel(true);
    }


    /**
     * 播放通知声音
     */
    private static void playNotificationRing(Context context) {
        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Ringtone rt = RingtoneManager.getRingtone(context, uri);
        rt.play();
    }

    /**
     * 手机震动一下
     */
    private static void playNotificationVibrate(Context context) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Service.VIBRATOR_SERVICE);
        long[] vibrationPattern = new long[]{0, 180, 80, 120};
        // 第一个参数为开关开关的时间，第二个参数是重复次数，振动需要添加权限
        vibrator.vibrate(vibrationPattern, -1);
    }
}
