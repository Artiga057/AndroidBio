package com.example.enguidanos.biometria_jaime;

import android.os.Handler;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import java.util.HashMap;
import java.util.Map;

public class Server {
<<<<<<< Updated upstream
    private static final String URL = "http://10.236.38.232/Prueba/src/api/guardarMedicion.php?";
    private static final String URL2 = "http://10.236.38.232/sp3/src/api/guardarsonda.php";
    private static final int INTERVALO_ENVIO_MS = 10000; // Intervalo de 10 segundos
    private static long ultimaEjecucion = 0; // Almacena el tiempo de la última ejecución

    private static final Handler handler = new Handler(); // Handler para manejar tareas programadas

    // Método para guardar medición con control de tiempo
=======
    private static final String URL = "http://192.168.1.79/prueba/src/api/guardarMedicion.php?";
>>>>>>> Stashed changes
    public static void guardarMedicion(final String ozono, final String temperatura, RequestQueue requestQueue) {
        long tiempoActual = System.currentTimeMillis();

        // Verifica si han pasado al menos 10 segundos desde la última ejecución
        if (tiempoActual - ultimaEjecucion >= INTERVALO_ENVIO_MS) {
            ultimaEjecucion = tiempoActual; // Actualiza el tiempo de la última ejecución

            StringRequest stringRequest = new StringRequest(
                    Request.Method.POST,    // Método HTTP (POST).
                    URL,                // URL del servidor.
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            Log.d("Respuesta", "Datos enviados correctamente: " + response);
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.d("Errores.", error.toString());
                        }
                    }

            ) {
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    Map<String, String> params = new HashMap<>();
                    // Se definen los parámetros a enviar en la solicitud (nivel de ozono y temperatura).
                    params.put("ozono", ozono);
                    params.put("temperatura", temperatura);
                    return params;
                }
            };

            // Se agrega la solicitud a la cola de solicitudes para su procesamiento.
            requestQueue.add(stringRequest);

        } else {
        }
    }
    public static void guardarSonda(final String MACsensor, RequestQueue requestQueue) {
        StringRequest stringRequest = new StringRequest(
                Request.Method.POST,    // Método HTTP (POST).
                URL2,                // URL del servidor.
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("Errores.", error.toString());
                    }
                }

        ) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                // Se definen los parámetros a enviar en la solicitud .
                params.put("MACsensor", MACsensor);
                return params;

            }
        };
        // Se agrega la solicitud a la cola de solicitudes para su procesamiento.
        requestQueue.add(stringRequest);
    }

}
