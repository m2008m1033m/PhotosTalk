package com.photostalk.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.photostalk.NotificationsActivity;
import com.photostalk.PhotoActivity;
import com.photostalk.R;
import com.photostalk.core.User;

import java.util.Map;


public class PhotosTalkFirebaseMessagingService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        User user = User.getInstance();

        if (!user.isLoggedIn()) return;

        Map<String, String> data = remoteMessage.getData();
        if (!data.containsKey("type")) return;

        Intent intent = null;
        Class activity = null;

        if ((data.get("type").equals("comment") && user.getSettings().getCommentNotifications()) ||
                (data.get("type").equals("like") && user.getSettings().getLikeNotifications())) {

            intent = new Intent(this, PhotoActivity.class);
            intent.putExtra(PhotoActivity.PHOTO_ID, data.get("photo_id"));
            activity = PhotoActivity.class;

        } else if ((data.get("type").equals("follow") && user.getSettings().getFollowNotifications()) ||
                (data.get("type").equals("follow_request") && user.getSettings().getFollowRequestNotifications()) ||
                (data.get("type").equals("accepted_follow_request") && user.getSettings().getAcceptFollowRequestNotifications())) {

            intent = new Intent(this, NotificationsActivity.class);
            activity = NotificationsActivity.class;

        }

        if (intent == null || activity == null) return;

        TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(this);
        taskStackBuilder.addParentStack(activity);
        taskStackBuilder.addNextIntent(intent);
        PendingIntent resultPendingIntent = taskStackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        sendNotification(resultPendingIntent, data.get("title"), data.get("body"));

    }

    private void sendNotification(PendingIntent pendingIntent, String title, String messageBody) {
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(messageBody)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);
        notificationBuilder.setAutoCancel(true);
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }
}
