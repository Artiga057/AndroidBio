package com.example.enguidanos.biometria_jaime;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class BackgroundService extends Service {
    private static final String CHANNEL_ID = "BackgroundServiceChannel";
    private static final String TAG = "BackgroundService";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Servicio creado");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Servicio en Segundo Plano")
                .setContentText("Escaneando dispositivos y enviando datos...")
                .setSmallIcon(R.drawable.ic_alert) // Cambia esto por un icono válido
                .build();

        startForeground(1, notification); // Servicio en primer plano con notificación

        // Aquí puedes iniciar el escaneo Bluetooth o enviar datos periódicamente.
        iniciarEscaneo();

        return START_STICKY; // Indica que el servicio se reinicia automáticamente si se cierra
    }

    private void iniciarEscaneo() {
        Log.d(TAG, "Iniciando el escaneo Bluetooth");
        // Puedes reutilizar tu método `iniciarEscaneo` de MainActivity aquí.
        // Alternativamente, mueve la lógica relevante al servicio.
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Servicio destruido");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Este servicio no está diseñado para comunicación con clientes
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Canal del Servicio en Segundo Plano",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }
}
