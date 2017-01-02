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
    private static final int PCM_FORCE_SILENCE_THRESHOLD = 1000;
    private static final int MIN_SECOND_TIMEOUT = 2;
    //endregion

    HueServerClient hueServerClient;

    private AudioRecord recorder = null;
    private boolean appRunning = false;
    private boolean nonSilenceDetected = false;

    private static final int MAX_RECORDING_LENGTH_S = 8;
    private int currentSoundSampleCount;
    short[] currentSoundSamples;

    private InitCommandTimeout initCommandTimeout;

    CommandFeedbackAudioPlayer commandAudio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RequestUserPermission(Manifest.permission.RECORD_AUDIO);
        RequestUserPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        RequestUserPermission(Manifest.permission.INTERNET);

        hueServerClient = new HueServerClient(DefaultHomeServerBaseUrl);
    }

    @Override
    protected void onStart() {
        super.onStart();

        appRunning = true;
        commandAudio = new CommandFeedbackAudioPlayer();
        initCommandTimeout = new InitCommandTimeout();
        initCommandTimoutTask();

        if(null == recorder) {
            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                    RECORDER_AUDIO_ENCODING, BUFFER_SIZE);

            recorder.startRecording();

        }

        startSpeechDetectionCycle();
    }

    @Override
    protected void onStop() {
        super.onStop();
        appRunning = false;

        // TODO this is janky, I should use locking instead of sleeping to solve this.
        try {
            Thread.sleep(250);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        commandAudio.release();

        if(null != recorder) {
            System.out.println("Starting Destroy recorder");
            recorder.stop();
            recorder.release();
            recorder = null;
            System.out.println("Finish Destroy recorder");
        }
    }

    private void startSpeechDetectionCycle() {
        currentSoundSamples = new short[MAX_RECORDING_LENGTH_S * RECORDER_SAMPLERATE];
        currentSoundSampleCount = 0;

        TaskUtils.startTask(new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                SetUserFeedbackMessage("Recording...");
            }

            @Override
            protected Void doInBackground(Void... params) {
                captureAudioData();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                boolean commandDetected = nonSilenceDetected;
                resetSilenceDetection();
                SetUserFeedbackMessage("Recording Stopped...");

                // The app has been stopped.
                if(!appRunning) {
                    return;
                }

                if(commandDetected) {
                    if(initCommandTimeout.checkAndRefreshInitCommand()) {
                        SetUserFeedbackMessage("Command Detected, Sending to Server...");
                        sendCommandToServer();

                    } else {
                        SetUserFeedbackMessage("Init Detected, Sending to Server...");
                        sendInitToServer();
                    }
                } else {
                    startSpeechDetectionCycle();
                }
            }
        });
    }

    private void initCommandTimoutTask() {
        TaskUtils.startTask(new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                boolean isUncapturedTimeout = false;

                while(!isUncapturedTimeout && appRunning) {
                    isUncapturedTimeout = initCommandTimeout.checkIfUncapturedTimeout();

                    if(isUncapturedTimeout) {
                        break;
                    }

                    try {
                        Thread.sleep(250);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                // app is stopped.
                if(!appRunning) {
                    return;
                }

                commandAudio.PlayCommandTimeout(MainActivity.this);

                SetUserFeedbackMessage("*BEEP NOISE* -> Command timeout Ended.");
                initCommandTimoutTask();
            }
        });
    }

    private void resetSilenceDetection() {
        nonSilenceDetected = false;
    }

    private void SetUserFeedbackMessage(String message) {
        TextView messageBox = (TextView)findViewById(R.id.user_feedback);
        messageBox.setText(message);
    }

    private void captureAudioData() {
        short sData[] = new short[NUM_SAMPLES_IN_BUFFER];
        while (appRunning) {

            int numSamplesRead = recorder.read(sData, 0, NUM_SAMPLES_IN_BUFFER);

            // Once we detect non-silence, we begin to record up to MAX_RECORDING_LENGTH_S worth of audio.
            if(!nonSilenceDetected) {
                if (!IsSilenceDetected(sData, 0, numSamplesRead - 1)) {
                    nonSilenceDetected = true;
                }
                continue;
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
                if(IsSilenceDetected(currentSoundSamples, currentSoundSampleCount - RECORDER_SAMPLERATE, currentSoundSampleCount - 1)) {
                    Log.i(Log_Tag, "Stopping because of silence.");
                    break;
                }
            }
        }
    }

    private boolean IsSilenceDetected(short[] soundSamples, int startIndex, int endIndex) {
        return computePcmForce(soundSamples, startIndex, endIndex) < PCM_FORCE_SILENCE_THRESHOLD;
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
                ActivityCompat.requestPermissions(this, new String[]{permission}, 2);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            // TODO this actually doesn't work, it is just cached from last time. Fix it later.
            Log.i(Log_Tag, "Already have permissions to " + permission);
        }
    }

    // TODO share code with sendCommandToServer
    private void sendInitToServer() {
        TaskUtils.startTask(new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                SetUserFeedbackMessage("Sending Init to Server...");
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    byte[] soundByteData = short2byte(currentSoundSamples, currentSoundSampleCount);
                    boolean initReceived = hueServerClient.SendInitAudioData(soundByteData);
                    return initReceived;
                } catch(IOException e) {
                    String errorMessage = "Error occurred while sending init data to server: " + e.toString();
                    Log.i(Log_Tag, errorMessage);
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean result) {
                super.onPostExecute(result);
                if(result) {
                    commandAudio.PlayCommandInit(MainActivity.this);
                    initCommandTimeout.setInitCommandReceived();
                }
                startSpeechDetectionCycle();
            }
        });
    }

    private void sendCommandToServer() {
        TaskUtils.startTask(new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                SetUserFeedbackMessage("Sending Command to Server...");
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    byte[] soundByteData = short2byte(currentSoundSamples, currentSoundSampleCount);
                    boolean commandExecuted = hueServerClient.SendCommandAudioData(soundByteData);
                    return commandExecuted;
                } catch(IOException e) {
                    String errorMessage = "Error occurred while sending audio data to server: " + e.toString();
                    Log.i(Log_Tag, errorMessage);
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean result) {
                super.onPostExecute(result);

                if(result) {
                    commandAudio.PlayCommandAck(MainActivity.this);
                    SetUserFeedbackMessage("Successfully Executed Command...");
                    initCommandTimeout.resetCommandTimeout();
                } else {
                    SetUserFeedbackMessage("Unexpected Error Occurred");
                }

                startSpeechDetectionCycle();
            }
        });
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

        byte[] bytes = new byte[numSamples * 2];
        for (int i = 0; i < numSamples; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }

        return bytes;
    }

}
