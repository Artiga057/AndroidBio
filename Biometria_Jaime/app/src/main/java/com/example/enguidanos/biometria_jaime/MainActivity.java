package com.example.enguidanos.biometria_jaime;

// ------------------------------------------------------------------
// ------------------------------------------------------------------

import static androidx.core.location.LocationManagerCompat.requestLocationUpdates;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import com.example.enguidanos.biometria_jaime.R;
import com.example.enguidanos.biometria_jaime.TramaIBeacon;
import com.example.enguidanos.biometria_jaime.Utilidades;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

// ------------------------------------------------------------------
// ------------------------------------------------------------------

public class MainActivity extends AppCompatActivity {

    // --------------------------------------------------------------
    // --------------------------------------------------------------
    private static final String URL_DESTINO = "http://172.20.10.14:8080/api/values";
    private static final String ETIQUETA_LOG = ">>>>";

    private RequestQueue requestQueue;
    private static final int CODIGO_PETICION_PERMISOS = 11223344;

    // --------------------------------------------------------------
    // --------------------------------------------------------------
    private BluetoothLeScanner elEscanner;

    private TextView dis;  // TextView para mostrar la distancia
    private Button btnCalcular; // Botón para calcular la distancia


    private ScanCallback callbackDelEscaneo = null;

    // Variables para manejar el estado de las búsquedas
    private boolean buscandoTodos = false;

    private List<String> dispositivosBuscados = new ArrayList<>();

    // --------------------------------------------------------------
    // --------------------------------------------------------------
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
                Log.d(ETIQUETA_LOG, "onScanResult()");

                BluetoothDevice dispositivo = resultado.getDevice();
                String nombreDispositivo = dispositivo.getName();

                if (buscandoTodos) {
                    mostrarInformacionDispositivoBTLE(resultado);
                }

                if (dispositivosBuscados.contains(nombreDispositivo)) {
                    Log.d("España", "Dispositivo específico encontrado: " + nombreDispositivo);
                    byte[] bytes = resultado.getScanRecord().getBytes();
                    TramaIBeacon trama = new TramaIBeacon(bytes);
                    int ozono = Utilidades.bytesToInt(trama.getMajor());
                    int temp  = Utilidades.bytesToInt(trama.getMinor());
                    Log.d("España", "ozono " + ozono);
                    Log.d("España", "temperatura " + temp);
                    Server.guardarMedicion(String.valueOf(ozono), String.valueOf(temp), requestQueue);
                    // Opcional: Detener la búsqueda específica después de encontrarlo
                    // dispositivosBuscados.remove(nombreDispositivo);
                    // if (dispositivosBuscados.isEmpty() && !buscandoTodos) {
                    //     detenerBusquedaDispositivosBTLE();
                    // }
                }
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                super.onBatchScanResults(results);
                Log.d(ETIQUETA_LOG, "onBatchScanResults()");
                for (ScanResult result : results) {
                    onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, result);
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                Log.d(ETIQUETA_LOG, "onScanFailed(): Código de error = " + errorCode);
                callbackDelEscaneo = null;
            }
        };

        List<ScanFilter> filtros = new ArrayList<>();

        // Agregar filtros para dispositivos específicos
        for (String nombre : dispositivosBuscados) {
            ScanFilter filtro = new ScanFilter.Builder().setDeviceName(nombre).build();
            filtros.add(filtro);
        }

        // Configurar los parámetros de escaneo
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        Log.d(ETIQUETA_LOG, "Iniciando escaneo BLE con " + (filtros.isEmpty() ? "sin filtros" : "filtros específicos"));

        elEscanner.startScan(filtros, settings, callbackDelEscaneo);
    }

    // --------------------------------------------------------------
    // --------------------------------------------------------------
    @SuppressLint("MissingPermission")
    private void detenerBusquedaDispositivosBTLE() {
        Log.d(ETIQUETA_LOG, "Deteniendo escaneo BLE");
        if (this.callbackDelEscaneo == null) {
            Log.d(ETIQUETA_LOG, "No hay escaneo en curso para detener.");
            return;
        }
        this.elEscanner.stopScan(this.callbackDelEscaneo);
        this.callbackDelEscaneo = null;
        buscandoTodos = false;
        dispositivosBuscados.clear();
        Log.d(ETIQUETA_LOG, "Escaneo detenido.");
    }

    // --------------------------------------------------------------
    // --------------------------------------------------------------
    @SuppressLint("MissingPermission")
    private void buscarTodosLosDispositivosBTLE() {
        Log.d(ETIQUETA_LOG, "Iniciando búsqueda de todos los dispositivos BTLE");
        buscandoTodos = true;
        iniciarEscaneo();

    }

    // --------------------------------------------------------------
    // --------------------------------------------------------------
    @SuppressLint("MissingPermission")
    private void buscarEsteDispositivoBTLE(final String dispositivoBuscado) {
        Log.d(ETIQUETA_LOG, "Iniciando búsqueda del dispositivo específico: " + dispositivoBuscado);
        if (!dispositivosBuscados.contains(dispositivoBuscado)) {
            dispositivosBuscados.add(dispositivoBuscado);
        }
        iniciarEscaneo();
    }

    // --------------------------------------------------------------
    // --------------------------------------------------------------
    @SuppressLint("MissingPermission")
    private void mostrarInformacionDispositivoBTLE(ScanResult resultado) {

        BluetoothDevice bluetoothDevice = resultado.getDevice();
        byte[] bytes = resultado.getScanRecord().getBytes();
        int rssi = resultado.getRssi();

        Log.d(ETIQUETA_LOG, "******************");
        Log.d(ETIQUETA_LOG, "** DISPOSITIVO DETECTADO BTLE ****** ");
        Log.d(ETIQUETA_LOG, "******************");
        Log.d(ETIQUETA_LOG, "nombre = " + bluetoothDevice.getName());
        Log.d(ETIQUETA_LOG, "toString = " + bluetoothDevice.toString());

        Log.d(ETIQUETA_LOG, "dirección = " + bluetoothDevice.getAddress());
        Log.d(ETIQUETA_LOG, "rssi = " + rssi);

        Log.d(ETIQUETA_LOG, "bytes = " + new String(bytes));
        Log.d(ETIQUETA_LOG, "bytes (" + bytes.length + ") = " + Utilidades.bytesToHexString(bytes));

        TramaIBeacon tib = new TramaIBeacon(bytes);

        Log.d(ETIQUETA_LOG, "----------------------------------------------------");
        Log.d(ETIQUETA_LOG, "prefijo  = " + Utilidades.bytesToHexString(tib.getPrefijo()));
        Log.d(ETIQUETA_LOG, "advFlags = " + Utilidades.bytesToHexString(tib.getAdvFlags()));
        Log.d(ETIQUETA_LOG, "advHeader = " + Utilidades.bytesToHexString(tib.getAdvHeader()));
        Log.d(ETIQUETA_LOG, "companyID = " + Utilidades.bytesToHexString(tib.getCompanyID()));
        Log.d(ETIQUETA_LOG, "iBeacon type = " + Integer.toHexString(tib.getiBeaconType()));
        Log.d(ETIQUETA_LOG, "iBeacon length 0x = " + Integer.toHexString(tib.getiBeaconLength()) + " ( " + tib.getiBeaconLength() + " ) ");
        Log.d(ETIQUETA_LOG, "uuid  = " + Utilidades.bytesToHexString(tib.getUUID()));
        Log.d(ETIQUETA_LOG, "uuid  = " + Utilidades.bytesToString(tib.getUUID()));
        Log.d(ETIQUETA_LOG, "major  = " + Utilidades.bytesToHexString(tib.getMajor()) + "( " + Utilidades.bytesToInt(tib.getMajor()) + " ) ");
        Log.d(ETIQUETA_LOG, "minor  = " + Utilidades.bytesToHexString(tib.getMinor()) + "( " + Utilidades.bytesToInt(tib.getMinor()) + " ) ");
        Log.d(ETIQUETA_LOG, "txPower  = " + Integer.toHexString(tib.getTxPower()) + " ( " + tib.getTxPower() + " )");
        Log.d(ETIQUETA_LOG, "******************");

    }

    // --------------------------------------------------------------
    // --------------------------------------------------------------
    @SuppressLint("MissingPermission")
    private void inicializarBlueTooth() {
        Log.d(ETIQUETA_LOG, "inicializarBlueTooth(): obtenemos adaptador BT ");

        BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();

        Log.d(ETIQUETA_LOG, "inicializarBlueTooth(): habilitamos adaptador BT ");

        if (!bta.isEnabled()) {
            bta.enable();
        }

        Log.d(ETIQUETA_LOG, "inicializarBlueTooth(): habilitado =  " + bta.isEnabled());

        Log.d(ETIQUETA_LOG, "inicializarBlueTooth(): estado =  " + bta.getState());

        Log.d(ETIQUETA_LOG, "inicializarBlueTooth(): obtenemos escaner btle ");

        this.elEscanner = bta.getBluetoothLeScanner();

        if (this.elEscanner == null) {
            Log.d(ETIQUETA_LOG, "inicializarBlueTooth(): Socorro: NO hemos obtenido escaner btle  !!!!");
        }

        Log.d(ETIQUETA_LOG, "inicializarBlueTooth(): voy a pedir permisos (si no los tuviera) !!!!");

        if (
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED
                        || ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED
                        || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                    MainActivity.this,
                    new String[]{Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_FINE_LOCATION},
                    CODIGO_PETICION_PERMISOS);
        } else {
            Log.d(ETIQUETA_LOG, "inicializarBlueTooth(): parece que YA tengo los permisos necesarios !!!!");
        }
    }
    //historia 21
    private static final int LOCATION_PERMISSION_CODE = 100;
    private static final String CHANNEL_ID = "ozone_alert_channel";
    private static final double OZONE_THRESHOLD = 100.0; // Límite en µg/m³

    private LocationManager locationManager;
    private double currentLatitude = 0.0;
    private double currentLongitude = 0.0;



    // --------------------------------------------------------------
    // --------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(ETIQUETA_LOG, "onCreate(): empieza ");

        inicializarBlueTooth();

        Log.d(ETIQUETA_LOG, "onCreate(): termina ");
        requestQueue = Volley.newRequestQueue(this);

        // Enviar una medición (esto puede estar en un temporizador o en otra parte de tu código)
        Server.guardarMedicion("100", "25", requestQueue);

        // Configuración inicial
        /*createNotificationChannel();
        requestLocationUpdates();

        // Simulación de niveles de ozono
        simulateOzoneMonitoring();
*/
        dis = findViewById(R.id.distanciavalue); // Obtener el TextView para mostrar la distancia
        btnCalcular = findViewById(R.id.btnCalcular); // Botón para calcular la distancia

        // Configurar el botón para realizar el cálculo de distancia
        btnCalcular.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Aquí es donde se llama a la función cDistancia con tus valores
                int txPower = -59;  // Valor de txPower de ejemplo (en dBm)
                int rssi = -70;     // Valor de rssi de ejemplo (en dBm)

                // Llamada a la función cDistancia
                double d = cDistancia(txPower, rssi, 2);  // Calcula la distancia

                // Llamada a la función para mostrar la distancia
                mostrarDistancia(d);  // Muestra la distancia calculada
            }
        });



    }

    // --------------------------------------------------------------
    // --------------------------------------------------------------
    // Función para calcular la distancia con el modelo de propagación ajustado
    public double cDistancia(int txPower, int rssi, double n) {
        if (rssi == 0) {
            return -1.0; // Valor no válido, la señal no se detecta
        }

        double ratio = rssi * 1.0 / txPower;
        if (ratio < 1.0) {
            return Math.pow(ratio, 10);
        } else {
            double distance = (0.89976) * Math.pow(ratio, 7.7095) + 0.111;
            return distance;
        }
    }

    // Función para mostrar la distancia en el TextView y mostrar el mensaje apropiado
    private void mostrarDistancia(double distancia) {
        if (distancia == -1) {
            dis.setText("No se detecta la señal");  // Si no se detecta la señal
        } else if (distancia < 2) {
            dis.setText("Estás al lado del sensor");
        } else if (distancia >= 2 && distancia <= 5) {
            dis.setText("Estás cerca del sensor");
        } else if (distancia > 5) {
            dis.setText("Estás lejos del sensor");
        }

        // Formatear la distancia a 2 decimales para mostrarla
        dis.setText(String.format("Distancia: %.2f metros", distancia));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case CODIGO_PETICION_PERMISOS:
                // Si la petición es cancelada, los arrays de resultados están vacíos.
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Log.d(ETIQUETA_LOG, "onRequestPermissionResult(): permisos concedidos  !!!!");
                    // Continuar con la acción que requiere permisos.
                } else {

                    Log.d(ETIQUETA_LOG, "onRequestPermissionResult(): Socorro: permisos NO concedidos  !!!!");
                    // Manejar la falta de permisos, posiblemente deshabilitando funcionalidades.

                }

                return;

        }
        if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestLocationUpdates();
            }
        }

        // Otras 'case' para verificar otros permisos que la app pudiera solicitar.
    }

    // --------------------------------------------------------------
    // --------------------------------------------------------------
    // Botones
    public void botonBuscarDispositivosBTLEPulsado(View v) {
        Log.d(ETIQUETA_LOG, "Botón 'Buscar Todos los Dispositivos BTLE' pulsado");
        buscarTodosLosDispositivosBTLE();
    }

    public void botonBuscarNuestroDispositivoBTLEPulsado(View v) {
        Log.d(ETIQUETA_LOG, "Botón 'Buscar Nuestro Dispositivo BTLE' pulsado");
        buscarEsteDispositivoBTLE("JAINIS-ES-UN-SOL");
    }

    public void botonDetenerBusquedaDispositivosBTLEPulsado(View v) {
        Log.d(ETIQUETA_LOG, "Botón 'Detener Búsqueda Dispositivos BTLE' pulsado");
        detenerBusquedaDispositivosBTLE();
    }

    private void requestLocationUpdates() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_CODE);
            return;
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                currentLatitude = location.getLatitude();
                currentLongitude = location.getLongitude();
            }
        });
    }

    /*private void simulateOzoneMonitoring() {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                double simulatedOzoneLevel = Math.random() * 200; // Simulación de 0 a 200 µg/m³
                Log.d("OzoneAlert", "Nivel de ozono: " + simulatedOzoneLevel);

                if (simulatedOzoneLevel > OZONE_THRESHOLD) {
                    sendAlert(simulatedOzoneLevel);
                }
                Log.d("OzoneAlert", "Nivel de ozono detectado: " + simulatedOzoneLevel);
                Log.d("OzoneAlert", "¿Se activa la alerta? " + (simulatedOzoneLevel > OZONE_THRESHOLD));
                handler.postDelayed(this, 10000); // Verificar cada 10 segundos
            }
        }, 10000);
    }
    */

   /* private void sendAlert(double ozoneLevel) {
        String timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        String message = String.format(Locale.getDefault(),
                "¡Alerta! Nivel de ozono: %.2f µg/m³\nUbicación: %.6f, %.6f\nHora: %s",
                ozoneLevel, currentLatitude, currentLongitude, timestamp);

        // Enviar notificación
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Alerta de Ozono")
                .setContentText("Nivel crítico detectado. Toca para más detalles.")
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(getAlertSound());

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        builder.setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, builder.build());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    private Uri getAlertSound() {
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
    }
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Ozone Alert Channel";
            String description = "Canal para alertas de ozono";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

*/

} // class
// --------------------------------------------------------------
// --------------------------------------------------------------
// --------------------------------------------------------------
// --------------------------------------------------------------