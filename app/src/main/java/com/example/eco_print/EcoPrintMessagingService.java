package com.example.eco_print;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.content.ContextCompat;

import com.example.eco_print.utils.PushNotificationManager;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class EcoPrintMessagingService
        extends FirebaseMessagingService {

    public static final String CHANNEL_ID =
            "eco_updates";

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);

        if (token == null || token.trim().isEmpty()) {
            return;
        }

        PushNotificationManager.saveToken(
                this,
                token.trim()
        );
    }

    @Override
    public void onMessageReceived(
            RemoteMessage remoteMessage
    ) {
        super.onMessageReceived(remoteMessage);

        String title =
                remoteMessage.getData().get("title");

        String message =
                remoteMessage.getData().get("message");

        String reportId =
                remoteMessage.getData().get("report_id");


        /*
         * Support Firebase notification payloads too.
         *
         * If title/message are missing from the data payload,
         * use Firebase's normal notification title/body.
         */
        if (remoteMessage.getNotification() != null) {

            if ((title == null || title.trim().isEmpty())
                    && remoteMessage
                    .getNotification()
                    .getTitle() != null) {

                title =
                        remoteMessage
                                .getNotification()
                                .getTitle();
            }

            if ((message == null || message.trim().isEmpty())
                    && remoteMessage
                    .getNotification()
                    .getBody() != null) {

                message =
                        remoteMessage
                                .getNotification()
                                .getBody();
            }
        }


        /*
         * Safe fallback values.
         */
        if (title == null || title.trim().isEmpty()) {

            title = "Eco-Print";
        }

        if (message == null || message.trim().isEmpty()) {

            message =
                    "Your plastic-waste report has been updated.";
        }


        showNotification(
                title,
                message,
                reportId
        );
    }

    private void showNotification(
            String title,
            String message,
            String reportId
    ) {

        NotificationManager manager =
                (NotificationManager)
                        getSystemService(
                                NOTIFICATION_SERVICE
                        );

        if (manager == null) {
            return;
        }


        createChannel(manager);


        /*
         * Decide where to send the citizen
         * when the notification is tapped.
         */
        Intent intent;

        if (reportId != null
                && !reportId.trim().isEmpty()) {

            intent = new Intent(
                    this,
                    ReportDetailsActivity.class
            );

            intent.putExtra(
                    ReportDetailsActivity.EXTRA_REPORT_ID,
                    reportId
            );

        } else {

            intent = new Intent(
                    this,
                    NotificationActivity.class
            );
        }


        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
        );


        /*
         * Give notifications belonging to different
         * reports different PendingIntent request codes.
         */
        int requestCode;

        if (reportId == null
                || reportId.trim().isEmpty()) {

            requestCode =
                    (int) System.currentTimeMillis();

        } else {

            requestCode =
                    reportId.hashCode();
        }


        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        this,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE
                );


        Notification.Builder builder;


        if (Build.VERSION.SDK_INT
                >= Build.VERSION_CODES.O) {

            builder =
                    new Notification.Builder(
                            this,
                            CHANNEL_ID
                    );

        } else {

            builder =
                    new Notification.Builder(
                            this
                    );
        }


        builder
                .setSmallIcon(
                        android.R.drawable.ic_dialog_info
                )
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(
                        new Notification.BigTextStyle()
                                .bigText(message)
                )
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(
                        Notification.PRIORITY_HIGH
                );


        /*
         * Android 13+ requires runtime notification
         * permission before displaying notifications.
         */
        if (Build.VERSION.SDK_INT
                >= Build.VERSION_CODES.TIRAMISU) {

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {

                return;
            }
        }


        /*
         * Unique ID so notifications don't replace
         * each other.
         */
        int notificationId =
                (int) System.currentTimeMillis();


        manager.notify(
                notificationId,
                builder.build()
        );
    }

    public static void createChannel(
            NotificationManager manager
    ) {

        if (Build.VERSION.SDK_INT
                < Build.VERSION_CODES.O) {

            return;
        }


        NotificationChannel channel =
                new NotificationChannel(
                        CHANNEL_ID,
                        "Eco-Print Updates",
                        NotificationManager.IMPORTANCE_HIGH
                );


        channel.setDescription(
                "Report status, collection and Eco Point updates"
        );


        channel.enableVibration(true);


        channel.setVibrationPattern(
                new long[]{
                        0,
                        250,
                        150,
                        250
                }
        );


        manager.createNotificationChannel(
                channel
        );
    }
}