package com.example.enguidanos.biometria_jaime;


import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import androidx.camera.core.ExperimentalGetImage;

@androidx.camera.core.ExperimentalGetImage
public class QRScanner extends AppCompatActivity {
    private static final int REQUEST_PERMISSIONS = 100;
    private static final String ETIQUETA_LOG = "ScannerActivity";
    private static final int REQUEST_ENABLE_BT = 1;
    private ExecutorService cameraExecutor;
    private String qrText = "";
    private TextView qrTextView;
    private BluetoothAdapter bluetoothAdapter;
    Button scanQrButton;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // UUID genérico para Bluetooth SPP
    // RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.qr_activity);
        qrTextView = findViewById(R.id.qrTextView);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Verificar si el dispositivo soporta Bluetooth
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth no está disponible en este dispositivo", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Verificar permisos para la cámara
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_PERMISSIONS);
        } else {
            // Si los permisos ya están concedidos, inicia la cámara
            startCamera();
        }
        //requestQueue = Volley.newRequestQueue(this);
        cameraExecutor = Executors.newSingleThreadExecutor();

    }

    // Método para verificar si una dirección MAC es válida
    private boolean esDireccionMacValida(String mac) {
        return mac != null && mac.matches("^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$");
    }

    // Método para guardar una dirección MAC en la base de datos (simulado)
    private void guardarMacEnBD(String mac) {
        Log.d(ETIQUETA_LOG, "Guardando MAC en base de datos: " + mac);
        // Lógica de guardado en BD aquí
    }

    // Método para manejar la lógica del servicio BTLE
    private void iniciarServicio(String macAddress) {
        Log.d(ETIQUETA_LOG, "Iniciando servicio con dirección MAC: " + macAddress);
        // Lógica para manejar Bluetooth directamente
    }

    private void detenerServicio() {
        Log.d(ETIQUETA_LOG, "Deteniendo servicio Bluetooth");
        // Lógica para detener Bluetooth
    }

    private void solicitarPermisos() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.CAMERA
                }, REQUEST_PERMISSIONS);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.CAMERA
                }, REQUEST_PERMISSIONS);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSIONS) {
            boolean permissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    permissionsGranted = false;
                    break;
                }
            }

            if (permissionsGranted) {
                Log.d(ETIQUETA_LOG, "Permisos concedidos");
            } else {
                Toast.makeText(this, "Se requieren todos los permisos para el escaneo y búsqueda de dispositivos", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @androidx.camera.core.ExperimentalGetImage
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // Configurar Preview
                Preview preview = new Preview.Builder().build();

                PreviewView previewView = findViewById(R.id.previewView);
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // Configurar ImageAnalysis
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                        .build();
                imageAnalysis.setAnalyzer(cameraExecutor, this::processImageProxy);

                // Seleccionar cámara trasera
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                // Desvincular antes de vincular para evitar conflictos
                cameraProvider.unbindAll();

                // Vincular Preview y ImageAnalysis al ciclo de vida de la actividad
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }



    private void detenerCamara() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                cameraProvider.unbindAll();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @androidx.camera.core.ExperimentalGetImage
    private void processImageProxy(@NonNull ImageProxy imageProxy) {
        if (imageProxy.getImage() == null) {
            Log.e(ETIQUETA_LOG, "La imagen es null, no se puede procesar");
            imageProxy.close();
            return;
        }

        InputImage image = InputImage.fromMediaImage(
                imageProxy.getImage(),
                imageProxy.getImageInfo().getRotationDegrees()
        );

        BarcodeScanning.getClient().process(image)
                .addOnSuccessListener(barcodes -> {
                    for (Barcode barcode : barcodes) {
                        if (barcode.getValueType() == Barcode.TYPE_TEXT) {
                            qrText = barcode.getDisplayValue();
                            runOnUiThread(() -> qrTextView.setText(qrText));

                            // Verificar si el contenido del QR es una dirección MAC válida
                            if (esDireccionMacValida(qrText)) {
                                Log.d(ETIQUETA_LOG, "Dirección MAC válida encontrada: " + qrText);

                                // Guardar la MAC en la base de datos
                                guardarSondaEnBaseDeDatos(qrText);
                                // Mostrar un mensaje al usuario
                                runOnUiThread(() -> Toast.makeText(this, "Código QR procesado correctamente.", Toast.LENGTH_SHORT).show());

                                // Intent para volver a la actividad anterior
                                Intent intent = new Intent(this, MainActivity.class); // Cambia MainActivity por la actividad anterior
                                intent.putExtra("dispositivoBuscado", "HOLA SOY ALEX 22"); // Hardcoded device name
                                setResult(RESULT_OK, intent);
                                startActivity(intent);

                                // Detener la cámara
                                detenerCamara();



                            } else {
                                Log.d(ETIQUETA_LOG, "El texto escaneado no es una dirección MAC válida.");
                                runOnUiThread(() -> Toast.makeText(this, "Código QR no válido.", Toast.LENGTH_SHORT).show());
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(ETIQUETA_LOG, "Error al procesar la imagen: ", e))
                .addOnCompleteListener(task -> imageProxy.close());
    }

    @Override
    protected void onPause() {
        super.onPause();
        detenerCamara();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startCamera();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }

    private void guardarSondaEnBaseDeDatos(String mac) {
        RequestQueue requestQueue = VolleySingleton.getInstance(this).getRequestQueue(); // Obtén la instancia de RequestQueue
        Server.guardarSonda(mac, requestQueue); // Llama al método de Server
    }
}