package com.example.enguidanos.biometria_jaime;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import java.util.concurrent.Executor;

public class AccesoHuella extends AppCompatActivity {

    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.acceso_huella);

        // Inicializar el TextView
        statusText = findViewById(R.id.statusText1);

        // Iniciar autenticación biométrica
        authenticateUser();
    }

    /**
     * Método para iniciar la autenticación biométrica.
     */
    private void authenticateUser() {
        BiometricManager biometricManager = BiometricManager.from(this);

        // Verificar compatibilidad con biometría
        if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                != BiometricManager.BIOMETRIC_SUCCESS) {
            statusText.setText("El dispositivo no es compatible con autenticación biométrica.");
            return;
        }

        // Crear el executor para manejar los callbacks
        Executor executor = ContextCompat.getMainExecutor(this);

        // Configurar el BiometricPrompt
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        // Mostrar mensaje de éxito
                        statusText.setText("¡Autenticación exitosa! Bienvenido.");

                        // Navegar a MainActivity
                        Intent intent = new Intent(AccesoHuella.this, MainActivity.class);
                        startActivity(intent);
                        finish(); // Finalizar esta actividad para evitar volver a la pantalla de autenticación
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        // Mostrar mensaje de error y cerrar la aplicación
                        statusText.setText("Error: " + errString);
                        finish(); // Cerrar la app si hay un error crítico
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        // Mostrar mensaje de fallo
                        statusText.setText("Huella no reconocida. Intenta nuevamente.");
                    }
                });

        // Configurar el diálogo de autenticación
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Autenticación Biométrica")
                .setSubtitle("Verifica tu identidad")
                .setDescription("Usa tu huella dactilar registrada para continuar.")
                .setNegativeButtonText("Salir")
                .build();

        // Mostrar el diálogo de autenticación
        biometricPrompt.authenticate(promptInfo);
    }
}