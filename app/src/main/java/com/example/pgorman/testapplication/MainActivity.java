package com.example.pgorman.testapplication;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public final static String EXTRA_MESSAGE = "com.example.myfirstapp.MESSAGE";

    private final static String Log_Tag = "gorman_smart_home";

    public String lastString = "";

    public final static int REQUEST_AUDIO_RESULT = 5;

    private File audioFile = null;

    SpeechRecognizer recognizer;

    MediaRecorder myAudioRecorder = null;
    MediaPlayer mediaPlayer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        audioFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath(),  "/audiorecordtest.3gp");
        audioFile = new File(this.getApplicationContext().getFileStreamPath("audiorecordtest.3gp").getPath());


        RequestUserPermission(Manifest.permission.RECORD_AUDIO);
        RequestUserPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        RequestUserPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
    }

    private void RequestUserPermission(String permission) {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
        {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.RECORD_AUDIO)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                Log.i(Log_Tag, "SHOULD SHOW");
            } else {
                Log.i(Log_Tag, "SHOULD NOT SHOW");
                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        REQUEST_AUDIO_RESULT);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            // TODO this actually doesn't work, it is just cached from last time. Fix it later.
            Log.i(Log_Tag, "Already have permissions to record audio.");
        }
    }

    private static final int SPEECH_REQUEST_CODE = 0;

    public void startRecord(View view) {
        myAudioRecorder = new MediaRecorder();
        myAudioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        myAudioRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        myAudioRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
        myAudioRecorder.setOutputFile(audioFile.getPath());


        try {
            myAudioRecorder.prepare();
        } catch (IOException e) {
            Log.e(Log_Tag, "prepare() recorder failed");
            return;
        }

        myAudioRecorder.start();
    }

    public void endRecord(View view) {
        myAudioRecorder.stop();
        myAudioRecorder.release();
        myAudioRecorder = null;
    }

    public void startPlayback(View view) {
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(audioFile.getPath());
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            Log.e(Log_Tag, "prepare() player failed");
        }
    }

    public void endPlayback(View view){
        mediaPlayer.release();
        mediaPlayer = null;
    }

    private void StartSpeechRecognizerIntent() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,  RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra("android.speech.extra.GET_AUDIO_FORMAT", "audio/AMR");
        intent.putExtra("android.speech.extra.GET_AUDIO", true);
        // Start the activity, the intent will be populated with the speech text
        startActivityForResult(intent, SPEECH_REQUEST_CODE);
    }

    // This callback is invoked when the Speech Recognizer returns.
    // This is where you process the intent and extract the speech text from the intent.
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        // TODO use SpeecRecognizer class.

        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            try {
                Uri audioUri = data.getData();
                ContentResolver contentResolver = getContentResolver();
                InputStream filestream = contentResolver.openInputStream(audioUri);

                byte[] bytes = readBytes(filestream);
                Log.i(Log_Tag, "Found " + bytes.length + " bytes of audio.");
            } catch(Exception e) {
                int y = 5;
            }


        } else if(resultCode == RESULT_CANCELED) {
            int x = 5;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }



    public byte[] readBytes(InputStream inputStream) throws IOException {
        // this dynamically extends to take the bytes you read
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();

        // this is storage overwritten on each iteration with bytes
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        // we need to know how may bytes were read to write them to the byteBuffer
        int len = 0;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }

        // and then we can return your byte array.
        return byteBuffer.toByteArray();
    }
}
