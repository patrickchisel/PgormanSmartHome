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
import android.widget.TextView;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private final static String Log_Tag = "gorman_smart_home";
    public static final String DefaultHomeServerBaseUrl = "http://192.168.1.149:8080";

    //region audio_consts
    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    // I think that this is derived from RECORDER_AUDIO_ENCODING
    private static final int BYTES_PER_SAMPLE = 2;
    private static final int NUM_SAMPLES_IN_BUFFER = 1024;
    private static final int BUFFER_SIZE = BYTES_PER_SAMPLE * NUM_SAMPLES_IN_BUFFER;
    //endregion

    //region silence_detection_consts
    private static final int PCM_FORCE_SILENCE_THRESHOLD = 500;
    private static final int MIN_SECOND_TIMEOUT = 2;
    //endregion

    HueServerClient hueServerClient;

    private AudioRecord recorder = null;
    private boolean isRecording = false;
    private boolean nonSilenceDetected = false;

    private static final int MAX_RECORDING_LENGTH_S = 8;
    private int currentSoundSampleCount;
    short[] currentSoundSamples;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RequestUserPermission(Manifest.permission.RECORD_AUDIO);
        RequestUserPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        RequestUserPermission(Manifest.permission.INTERNET);

        hueServerClient = new HueServerClient(DefaultHomeServerBaseUrl);
        SetUserFeedbackMessage("Welcome...");
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
        currentSoundSamples = new short[MAX_RECORDING_LENGTH_S * RECORDER_SAMPLERATE];
        currentSoundSampleCount = 0;

        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, BUFFER_SIZE);

        recorder.startRecording();
        isRecording = true;

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                captureAudioData();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);

                // TODO I think I need to sync this actually, because there is a race between setting isRecording to false and releasing the recorder?
                if(null != recorder) {
                    recorder.stop();
                    recorder.release();
                    recorder = null;

                    // TODO this is super weird, need to think about this better.
                    stopRecording();
                }

                SetUserFeedbackMessage("Recording Stopped...");
            }
        }.execute();

        SetUserFeedbackMessage("Recording...");
    }

    private void stopRecording() {
        if(isRecording) {
            isRecording = false;
            nonSilenceDetected = false;
        }
    }

    private void SetUserFeedbackMessage(String message) {
        TextView messageBox = (TextView)findViewById(R.id.user_feedback);
        messageBox.setText(message);
    }

    private void captureAudioData() {
        short sData[] = new short[NUM_SAMPLES_IN_BUFFER];
        while (isRecording) {
            int numSamplesRead = recorder.read(sData, 0, NUM_SAMPLES_IN_BUFFER);

            if(!nonSilenceDetected) {
                double newSampleAvgForce = computePcmForce(sData, 0, numSamplesRead - 1);
                if(newSampleAvgForce > PCM_FORCE_SILENCE_THRESHOLD) {
                    nonSilenceDetected = true;
                }
            }

            // We have reached our maximum listen timeout
            if(currentSoundSampleCount + numSamplesRead > currentSoundSamples.length) {
                break;
            }

            if(numSamplesRead > 0) {
                System.arraycopy(sData, 0, currentSoundSamples, currentSoundSampleCount, numSamplesRead);
                currentSoundSampleCount += numSamplesRead;
            }

            // Waiting at least until the 2nd second to avoid leading zeros issue
            // TODO remove leading zeroes to make this unnecessary.
            if(nonSilenceDetected && currentSoundSampleCount > RECORDER_SAMPLERATE * MIN_SECOND_TIMEOUT) {
                double lastSecondAvgForce = computePcmForce(
                        currentSoundSamples,
                        currentSoundSampleCount - RECORDER_SAMPLERATE,
                        currentSoundSampleCount);

                if(lastSecondAvgForce < PCM_FORCE_SILENCE_THRESHOLD) {
                    Log.i(Log_Tag, "Stopping because of silence.");
                    break;
                }
            }
        }
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

                // TODO what the hell is 2???
                ActivityCompat.requestPermissions(this,
                        new String[]{permission}, 2);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            // TODO this actually doesn't work, it is just cached from last time. Fix it later.
            Log.i(Log_Tag, "Already have permissions to " + permission);
        }
    }

    public void sendToServer(View view) {

        SetUserFeedbackMessage("Sending Command to Server...");

        AsyncTask<Void, Void, SendServerCommandResult> task = new AsyncTask<Void, Void, SendServerCommandResult>() {
            @Override
            protected SendServerCommandResult doInBackground(Void... params) {
                try {
                    byte[] soundByteData = short2byte(currentSoundSamples, currentSoundSampleCount);
                    hueServerClient.SendAudioData(soundByteData);
                } catch(IOException e) {
                    String errorMessage = "Error occurred while sending audio data to server: " + e.toString();
                    Log.i(Log_Tag, errorMessage);
                    return new SendServerCommandResult(false, errorMessage);
                }

                return new SendServerCommandResult(true, null);
            }

            @Override
            protected void onPostExecute(SendServerCommandResult result) {
                super.onPostExecute(result);

                if(result.isSuccess()) {
                    SetUserFeedbackMessage("Successfully Executed Command...");
                } else {
                    SetUserFeedbackMessage("Unexpected Error Occurred");
                }
            }
        };
        task.execute();
    }

    private double computePcmForce(short[] samples, int startIndex, int endIndex) {

        if(startIndex < 0 || startIndex > samples.length - 1) {
            throw new IllegalArgumentException("startIndex must be between 0 and the length of the array");
        }

        if(endIndex < 0 || endIndex > samples.length - 1 || endIndex < startIndex) {
            throw new IllegalArgumentException("endIndex must be between 0 and the length of the array, and greater than the start index.");
        }

        long sum = 0;
        for (int i=startIndex; i<=endIndex; i++) {
            sum += Math.abs(samples[i]);
        }

        return (double) sum / (double)(endIndex - startIndex + 1);
    }

    //convert short to byte
    private byte[] short2byte(short[] sData, int numSamples) {

        if(numSamples > sData.length) {
            throw new IllegalArgumentException("Can not specify to copy more elements from array than exist.");
        }

        int shortArrsize = sData.length;
        byte[] bytes = new byte[numSamples * 2];
        for (int i = 0; i < numSamples; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }

        return bytes;
    }

}
