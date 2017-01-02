package com.example.pgorman.testapplication;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by patri_000 on 12/29/2016.
 */

public class HueServerClient {
    private static final String LOG_TAG = "HueClientLib";

    public static final String API_SPEECH_RECOGNITION = "/api/speechrecognition";
    public static final String API_INIT_RECOGNITION = "api/checkspeechinit";
    private String baseUrl;

    public HueServerClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public boolean SendInitAudioData(byte[] audioData) throws IOException {
        URL url = new URL(new URL(baseUrl), API_INIT_RECOGNITION);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        try {
            urlConnection.setDoOutput(true);
            urlConnection.setChunkedStreamingMode(0);
            writeBytesToRequest(audioData, urlConnection);

            int responseCode = urlConnection.getResponseCode();

            if(isSuccessStatusCode(responseCode)) {
                String responseBody = GetResponseStringFromConnection(urlConnection);

                try{
                    JSONObject jObject = new JSONObject(responseBody);
                    boolean result = jObject.getBoolean("Result");
                    return result;
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "Invalid response (" + responseBody + ") returned by server");
                    return false;
                }

            } else {
                Log.e(LOG_TAG, "Received status code " + responseCode + " from the server");
                return false;
            }
        } finally {
            urlConnection.disconnect();
        }
    }

    public boolean SendCommandAudioData(byte[] audioData) throws IOException {
        URL url = new URL(new URL(baseUrl), API_SPEECH_RECOGNITION);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        try {
            urlConnection.setDoOutput(true);
            urlConnection.setChunkedStreamingMode(0);

            OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
            out.write(audioData);
            out.flush();
            out.close();

            int responseCode = urlConnection.getResponseCode();
            if (isSuccessStatusCode(responseCode)) {
                String responseBody = GetResponseStringFromConnection(urlConnection);

                try {
                    JSONObject jObject = new JSONObject(responseBody);
                    return jObject.getBoolean("CommandExecuted");
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "Invalid response (" + responseBody + ") returned by server");
                    return false;
                }

            } else {
                Log.e(LOG_TAG, "Received status code " + responseCode + " from the server");
                return false;
            }
        }
        finally {
            urlConnection.disconnect();
        }
    }

    private void writeBytesToRequest(byte[] audioData, HttpURLConnection connection) throws IOException {
        OutputStream out = new BufferedOutputStream(connection.getOutputStream());
        out.write(audioData);
        out.flush();
        out.close();
    }

    private String GetResponseStringFromConnection(HttpURLConnection connection) throws IOException {
        StringBuffer sb = new StringBuffer();
        BufferedInputStream is = new BufferedInputStream(connection.getInputStream());
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String inputLine = "";
        while ((inputLine = br.readLine()) != null) {
            sb.append(inputLine);
        }
        String result = sb.toString();
        return result;
    }

    private boolean isSuccessStatusCode(int code) {
        return code >= 200 && code < 300;
    }
}
