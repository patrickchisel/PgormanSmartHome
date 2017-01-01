package com.example.pgorman.testapplication;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by patri_000 on 12/29/2016.
 */

public class HueServerClient {

    public static final String API_SPEECHRECOGNITION = "/api/speechrecognition";
    private String baseUrl;

    public HueServerClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public boolean CheckInit(byte[] audioData) {
        // TODO implement the api for this.
        return false;
    }

    public void SendAudioData(byte[] audioData) throws IOException {
        URL url = new URL(new URL(baseUrl), API_SPEECHRECOGNITION);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        try {
            urlConnection.setDoOutput(true);
            urlConnection.setChunkedStreamingMode(0);

            OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
            out.write(audioData);
            out.flush();
            out.close();

            int responseCode = urlConnection.getResponseCode();
            String responseMessage = urlConnection.getResponseMessage();
            System.out.println(responseCode + ":" + responseMessage);
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            urlConnection.disconnect();
        }
    }

    public void testConnection() throws IOException{
        URL url = new URL("http://192.168.1.149:8080/api/testspeechapi");
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        try {
            int responseCode = urlConnection.getResponseCode();
            String responseMessage = urlConnection.getResponseMessage();
            System.out.println(responseCode + ":" + responseMessage);
        } catch (IOException e) {
            e.printStackTrace();
        } catch(Exception e) {
            e.printStackTrace();
        }
        finally {
            urlConnection.disconnect();
        }
    }
}
