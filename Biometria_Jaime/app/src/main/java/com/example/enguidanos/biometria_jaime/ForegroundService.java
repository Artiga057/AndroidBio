package com.example.enguidanos.biometria_jaime;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.core.app.NotificationCompat;

public class ForegroundService extends Service {

    private static final String CHANNEL_ID = "AlertaSensor";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("ForegroundService", "Servicio en primer plano creado");
        createNotificationChannel();
        startForeground(1, createNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("ForegroundService", "Servicio en primer plano ejecutándose");
        // Aquí puedes agregar la lógica que deseas ejecutar en el servicio.
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("ForegroundService", "Servicio en primer plano destruido");
    }

    // Método para crear el canal de notificación (necesario para Android 8.0 y versiones superiores)
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Alerta de Sensor", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Notificaciones de alertas de sensor de ozono");
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    // Método para crear la notificación que se mostrará cuando el servicio esté en primer plano
    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Servicio en primer plano")
                .setContentText("Escaneando sensores en segundo plano")
                .setSmallIcon(R.drawable.ic_alert)  // Ajusta el ícono según tus recursos
                .build();
    }
}
