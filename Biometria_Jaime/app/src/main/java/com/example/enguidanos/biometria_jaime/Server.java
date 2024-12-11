package com.example.enguidanos.biometria_jaime;

import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import java.util.HashMap;
import java.util.Map;

public class Server {
    private static final String URL = "http://192.168.1.116/prueba/guardarMedicion.php?";
    public static void guardarMedicion(final String ozono, final String temperatura, RequestQueue requestQueue) {
        StringRequest stringRequest = new StringRequest(
                Request.Method.POST,    // Método HTTP (POST).
                URL,                // URL del servidor.
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
                // Se definen los parámetros a enviar en la solicitud (nivel de CO2 y temperatura).
                params.put("ozono", ozono);
                params.put("temperatura", temperatura);
                return params;

            }
        };
        // Se agrega la solicitud a la cola de solicitudes para su procesamiento.
        requestQueue.add(stringRequest);
    }
}
