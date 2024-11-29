package com.example.enguidanos.biometria_jaime;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class CounterNotificationService {

    private final Context context;
    private final NotificationManager notificationManager;

    public CounterNotificationService(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @SuppressLint("NotificationPermission")
    public void showNotification(int counter, String buttonName) {
        Intent activityIntent = new Intent(context, PruebaNotificaciones.class);
        PendingIntent activityPendingIntent = PendingIntent.getActivity(
                context,
                1,
                activityIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0
        );

        PendingIntent incrementIntent = PendingIntent.getBroadcast(
                context,
                2,
                new Intent(context, CounterNotificationReceiver.class),
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0
        );

        Notification notification = new NotificationCompat.Builder(context, COUNTER_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_android_black_24dp)
                .setContentTitle("Aviso Inactividad")
                .setContentText("El sensor '" + buttonName + "' lleva 24 horas inactivo. Revise la actividad.")
                .setContentIntent(activityPendingIntent)
                .build();

        notificationManager.notify(1, notification);
    }

    @SuppressLint("NotificationPermission")
    public void showNotification2(int counter, String buttonName) {
        Intent activityIntent = new Intent(context, PruebaNotificaciones.class);
        PendingIntent activityPendingIntent = PendingIntent.getActivity(
                context,
                1,
                activityIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0
        );

        PendingIntent incrementIntent = PendingIntent.getBroadcast(
                context,
                2,
                new Intent(context, CounterNotificationReceiver.class),
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0
        );

        Notification notification2 = new NotificationCompat.Builder(context, COUNTER_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_android_black_24dp)
                .setContentTitle("Aviso Averia")
                .setContentText("El sensor '" + buttonName + "' esta dando lecturas erroneas. Revise la actividad.")
                .setContentIntent(activityPendingIntent)
                .build();

        notificationManager.notify(2, notification2);
    }

    public static final String COUNTER_CHANNEL_ID = "counter_channel";
}