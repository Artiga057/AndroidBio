package com.example.enguidanos.biometria_jaime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class CounterNotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        CounterNotificationService service = new CounterNotificationService(context);
        // Uncomment and implement the Counter logic if necessary
        // service.showNotification(++Counter.value);
    }
}