package com.example.enguidanos.biometria_jaime;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;

import androidx.activity.ComponentActivity;

public class PruebaNotificaciones extends ComponentActivity {

    private CounterNotificationService counterNotificationService;

    private Switch sensor1Switch;
    private EditText sensor1InputField;
    private Button sensor1ValidateButton;

    private Switch sensor2Switch;
    private EditText sensor2InputField;
    private Button sensor2ValidateButton;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable sensor1SwitchOffRunnable;
    private Runnable sensor2SwitchOffRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.prueba_notificaciones);

        counterNotificationService = new CounterNotificationService(this);

        // Referencias a los elementos del sensor 1
        sensor1Switch = findViewById(R.id.sensor1Switch);
        sensor1InputField = findViewById(R.id.sensor1InputField);
        sensor1ValidateButton = findViewById(R.id.sensor1ValidateButton);

        // Referencias a los elementos del sensor 2
        sensor2Switch = findViewById(R.id.sensor2Switch);
        sensor2InputField = findViewById(R.id.sensor2InputField);
        sensor2ValidateButton = findViewById(R.id.sensor2ValidateButton);

        setupSensor1Listeners();
        setupSensor2Listeners();
    }

    private void setupSensor1Listeners() {
        sensor1Switch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked) {
                sensor1SwitchOffRunnable = () -> counterNotificationService.showNotification(0, "Sensor 1");
                handler.postDelayed(sensor1SwitchOffRunnable, 30000);
            } else {
                if (sensor1SwitchOffRunnable != null) {
                    handler.removeCallbacks(sensor1SwitchOffRunnable);
                }
            }
        });

        sensor1ValidateButton.setOnClickListener(v -> {
            String inputText = sensor1InputField.getText().toString();
            Integer input = null;
            try {
                input = Integer.parseInt(inputText);
            } catch (NumberFormatException ignored) {
            }
            if (input == null || input < 0 || input > 100) {
                counterNotificationService.showNotification2(0, "Sensor 1");
            }
        });
    }

    private void setupSensor2Listeners() {
        sensor2Switch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked) {
                sensor2SwitchOffRunnable = () -> counterNotificationService.showNotification(0, "Sensor 2");
                handler.postDelayed(sensor2SwitchOffRunnable, 30000);
            } else {
                if (sensor2SwitchOffRunnable != null) {
                    handler.removeCallbacks(sensor2SwitchOffRunnable);
                }
            }
        });

        sensor2ValidateButton.setOnClickListener(v -> {
            String inputText = sensor2InputField.getText().toString();
            Integer input = null;
            try {
                input = Integer.parseInt(inputText);
            } catch (NumberFormatException ignored) {
            }
            if (input == null || input < 0 || input > 100) {
                counterNotificationService.showNotification2(0, "Sensor 2");
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sensor1SwitchOffRunnable != null) {
            handler.removeCallbacks(sensor1SwitchOffRunnable);
        }
        if (sensor2SwitchOffRunnable != null) {
            handler.removeCallbacks(sensor2SwitchOffRunnable);
        }
    }
}