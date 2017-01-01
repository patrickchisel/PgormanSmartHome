package com.example.pgorman.testapplication;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Much of the code inspired by http://stackoverflow.com/questions/8499042/android-audiorecord-example and
 * http://stackoverflow.com/questions/19145213/android-audio-capture-silence-detection
 */
public class MainActivity extends AppCompatActivity {

    private final static String Log_Tag = "gorman_smart_home";
    public static final String DefaultHomeServerBaseUrl = "http://192.168.1.149:8080";
    private File pcmAudioFile = null;

    public final static int REQUEST_AUDIO_RESULT = 5;
    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private AudioRecord recorder = null;
    private Thread recordingThread = null;
    private boolean isRecording = false;

    HueServerClient hueServerClient;

    int BufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
    int BytesPerElement = 2; // 2 bytes in 16bit format

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RequestUserPermission(Manifest.permission.RECORD_AUDIO);
        RequestUserPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        RequestUserPermission(Manifest.permission.INTERNET);

        pcmAudioFile = new File(this.getApplicationContext().getFileStreamPath("audiorecord.pcm").getPath());
        hueServerClient = new HueServerClient(DefaultHomeServerBaseUrl);
    }

    public void startRecordEvent(View view) {
        setRecording(true);
    }

    public void endRecordEvent(View view) {
        setRecording(false);
    }

    private synchronized void setRecording(boolean record) {
        if(record == isRecording) {
            return;
        }

        if(record) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    private void startRecording() {
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, BufferElements2Rec * BytesPerElement);

        recorder.startRecording();
        isRecording = true;

        // Start the listening thread which listens for a maximum period of time and adds sound data
        // to the audio file.
        recordingThread = new Thread(new Runnable() {
            public void run() {
                writeAudioDataToFile();
            }
        }, "AudioRecorder Thread");
        recordingThread.start();
    }

    private void stopRecording() {
        // stops the recording activity
        if (null != recorder) {
            isRecording = false;
            recorder.stop();
            recorder.release();
            recorder = null;
            recordingThread = null;
        }
    }

    private void writeAudioDataToFile() {
        String filePath = pcmAudioFile.getPath();
        short sData[] = new short[BufferElements2Rec];

        FileOutputStream os = null;
        try {
            os = new FileOutputStream(filePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        while (isRecording) {
            recorder.read(sData, 0, BufferElements2Rec);
            System.out.println("Short wirting to file" + sData.toString());
            try {
                // // writes the data to file from buffer
                // // stores the voice buffer
                byte bData[] = short2byte(sData);
                os.write(bData, 0, BufferElements2Rec * BytesPerElement);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //convert short to byte
    private byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;
    }

    // TODO this will get the permission but will crash the app, how do I handle user results.
    // TODO for now doesn't really matter because the permissions are on my phone.
    private void RequestUserPermission(String permission) {
        if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED)
        {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                Log.i(Log_Tag, "SHOULD SHOW");
            } else {
                Log.i(Log_Tag, "SHOULD NOT SHOW");
                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{permission}, REQUEST_AUDIO_RESULT);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            // TODO this actually doesn't work, it is just cached from last time. Fix it later.
            Log.i(Log_Tag, "Already have permissions to " + permission);
        }
    }

    private static final int SPEECH_REQUEST_CODE = 0;


    public void sendToServer(View view) {
        final byte[] pcmBytes;
        try {
            pcmBytes = FileUtils.readStreamToByteArray(new FileInputStream(pcmAudioFile));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {

                try {
                    hueServerClient.SendAudioData(pcmBytes);
                } catch(IOException e) {
                    Log.i(Log_Tag, e.toString());
                }

                return null;
            }
        };
        task.execute();
    }
}
