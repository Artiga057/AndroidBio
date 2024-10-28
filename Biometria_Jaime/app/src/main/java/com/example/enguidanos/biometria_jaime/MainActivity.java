package com.example.enguidanos.biometria_jaime;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String URL_DESTINO = "http://172.20.10.14:8080/api/values";
    private static final String ETIQUETA_LOG = ">>>>";
    private static final String CHANNEL_ID = "AlertaSensor";
    private RequestQueue requestQueue;
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1;
    private static final int REQUEST_POST_NOTIFICATIONS_PERMISSION = 2;
    private static final int INTERVALO_MAXIMO_MS = 20000;
    private static final int LIMITE_OZONO_ALTO = 200;
    private static final int LIMITE_OZONO_BAJO = 0;
    private static final long INTERVALO_NOTIFICACION_MS = 15000; // Intervalo de 15 segundos entre notificaciones
    private long ultimaNotificacion = 0;
    private BluetoothLeScanner elEscanner;
    private ScanCallback callbackDelEscaneo = null;
    private boolean buscandoTodos = false;
    private List<String> dispositivosBuscados = new ArrayList<>();
    private long ultimaRecepcion = 0;
    private boolean busquedaActiva = false; // Nueva variable para controlar el estado de búsqueda
    private Handler temporizadorHandler; // Para manejar el temporizador de desconexión

    private LocationManager locationManager;
    private LocationListener locationListener;
    private String coordenadasGPS = "";

    private void checkBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_SCAN
                }, REQUEST_BLUETOOTH_PERMISSIONS);
            } else {
                inicializarBlueTooth();
            }
        } else {
            inicializarBlueTooth();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Alerta de Sensor";
            String description = "Notificaciones de estado del sensor de ozono";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void sendAlertNotification(String title, String message) {
        long tiempoActual = System.currentTimeMillis();
        if (tiempoActual - ultimaNotificacion < INTERVALO_NOTIFICACION_MS) {
            return; // No envía la notificación si se ha enviado recientemente
        }
        ultimaNotificacion = tiempoActual; // Actualiza la última notificación enviada

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_alert)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, builder.build());
    }

    private void iniciarTemporizadorDesconexion() {
        if (!busquedaActiva) { // Solo iniciar el temporizador si la búsqueda está activa
            return;
        }

        // Detenemos cualquier temporizador previo
        if (temporizadorHandler != null) {
            temporizadorHandler.removeCallbacksAndMessages(null);
        }

        temporizadorHandler = new Handler();
        temporizadorHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                long tiempoActual = System.currentTimeMillis();
                if (tiempoActual - ultimaRecepcion > INTERVALO_MAXIMO_MS) {
                    sendAlertNotification("Alerta de sensor", "No se reciben datos del sensor de ozono");
                }
                iniciarTemporizadorDesconexion();
            }
        }, INTERVALO_MAXIMO_MS);
    }

    @SuppressLint("MissingPermission")
    private void iniciarEscaneo() {
        if (callbackDelEscaneo != null) {
            Log.d(ETIQUETA_LOG, "Ya hay un escaneo en curso.");
            return;
        }

        callbackDelEscaneo = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult resultado) {
                super.onScanResult(callbackType, resultado);
                ultimaRecepcion = System.currentTimeMillis(); // Actualiza la última recepción
                BluetoothDevice dispositivo = resultado.getDevice();
                String nombreDispositivo = dispositivo.getName();
                if (buscandoTodos) {
                    mostrarInformacionDispositivoBTLE(resultado);
                }

                // Verificar si el dispositivo buscado está en la lista
                if (dispositivosBuscados.contains(nombreDispositivo)) {
                    byte[] bytes = resultado.getScanRecord().getBytes();
                    TramaIBeacon trama = new TramaIBeacon(bytes);
                    int ozono = Utilidades.bytesToInt(trama.getMajor());
                    int temp = Utilidades.bytesToInt(trama.getMinor());

                    if (ozono < LIMITE_OZONO_BAJO || ozono > LIMITE_OZONO_ALTO) {
                        String mensajeAlerta = String.format("Lectura de ozono fuera de rango: %d\nHora: %s\nCoordenadas: %s",
                                ozono, obtenerHoraActual(), coordenadasGPS);
                        sendAlertNotification("Alerta de sensor", mensajeAlerta);
                    }

                    Server.guardarMedicion(String.valueOf(ozono), String.valueOf(temp), requestQueue);
                }

                // Reinicia el temporizador si se recibe un resultado
                iniciarTemporizadorDesconexion();
            }
        };

        List<ScanFilter> filtros = new ArrayList<>();
        for (String nombre : dispositivosBuscados) {
            ScanFilter filtro = new ScanFilter.Builder().setDeviceName(nombre).build();
            filtros.add(filtro);
        }

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        elEscanner.startScan(filtros, settings, callbackDelEscaneo);
    }

    @SuppressLint("MissingPermission")
    private void detenerBusquedaDispositivosBTLE() {
        if (this.callbackDelEscaneo == null) {
            return;
        }
        this.elEscanner.stopScan(this.callbackDelEscaneo);
        this.callbackDelEscaneo = null;
        buscandoTodos = false;
        busquedaActiva = false; // Restablece el estado de búsqueda
        dispositivosBuscados.clear();

        // Detenemos el temporizador de desconexión
        if (temporizadorHandler != null) {
            temporizadorHandler.removeCallbacksAndMessages(null);
        }
    }

    @SuppressLint("MissingPermission")
    private void inicializarBlueTooth() {
        BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();
        if (!bta.isEnabled()) {
            bta.enable();
        }
        this.elEscanner = bta.getBluetoothLeScanner();
        inicializarGPS(); // Inicia la captura de GPS
    }

    private void inicializarGPS() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                coordenadasGPS = location.getLatitude() + ", " + location.getLongitude();
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onProviderDisabled(String provider) {}
        };

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, locationListener);
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }

    private String obtenerHoraActual() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        createNotificationChannel();
        checkBluetoothPermissions();
        requestQueue = Volley.newRequestQueue(this);
        iniciarTemporizadorDesconexion();
        Server.guardarMedicion("100", "25", requestQueue);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_POST_NOTIFICATIONS_PERMISSION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                inicializarBlueTooth();
            }
        } else if (requestCode == REQUEST_POST_NOTIFICATIONS_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(ETIQUETA_LOG, "Permiso de notificación concedido.");
            }
        } else if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                inicializarGPS();
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void mostrarInformacionDispositivoBTLE(ScanResult resultado) {
        BluetoothDevice dispositivo = resultado.getDevice();
        Log.d(ETIQUETA_LOG, "Dispositivo: " + dispositivo.getName() + ", RSSI: " + resultado.getRssi());
    }

    @SuppressLint("MissingPermission")
    private void buscarTodosLosDispositivosBTLE() {
        buscandoTodos = true;
        busquedaActiva = true; // Marcamos la búsqueda como activa
        iniciarEscaneo();
        iniciarTemporizadorDesconexion(); // Iniciamos el temporizador de desconexión
    }

    @SuppressLint("MissingPermission")
    private void buscarEsteDispositivoBTLE(final String dispositivoBuscado) {
        if (!dispositivosBuscados.contains(dispositivoBuscado)) {
            dispositivosBuscados.add(dispositivoBuscado);
        }
        busquedaActiva = true; // Marcamos la búsqueda como activa
        iniciarEscaneo();
        iniciarTemporizadorDesconexion(); // Iniciamos el temporizador de desconexión
    }

    public void botonBuscarDispositivosBTLEPulsado(View v) {
        buscarTodosLosDispositivosBTLE();
    }

    public void botonBuscarNuestroDispositivoBTLEPulsado(View v) {
        buscarEsteDispositivoBTLE("GTI-3A");
    }

    public void botonDetenerBusquedaDispositivosBTLEPulsado(View v) {
        detenerBusquedaDispositivosBTLE();
    }
}
