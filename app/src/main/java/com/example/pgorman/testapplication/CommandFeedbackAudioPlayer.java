package com.example.pgorman.testapplication;

import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import java.io.IOException;

/**
 * Created by patri_000 on 1/1/2017.
 */

public class CommandFeedbackAudioPlayer {

    private static final String LOG_TAG = "COMMAND_AUDIO";
    private MediaPlayer mediaPlayer;

    public CommandFeedbackAudioPlayer() {
        mediaPlayer = null;
    }

    public synchronized void PlayCommandTimeout(AppCompatActivity activity){
        playSoundAsset(activity, "StarTrekVoiceTimeout.wav");
    }

    public synchronized void PlayCommandInit(AppCompatActivity activity){
        playSoundAsset(activity, "StarTrekComputerBeepInit.wav");
    }

    public synchronized void PlayCommandAck(AppCompatActivity activity){
        playSoundAsset(activity, "StarTrekComputerBeepAck.wav");
    }

    private void playSoundAsset(AppCompatActivity activity, String assetName) {
        releaseMediaPlayer();

        try {
            AssetFileDescriptor afd = activity.getAssets().openFd(assetName);
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(afd.getFileDescriptor(),afd.getStartOffset(),afd.getLength());
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch(IOException e) {
            Log.e(LOG_TAG, "Exception occurred when playing audio: " + e.toString());
        }
    }

    public void release() {
        releaseMediaPlayer();
    }

    private void releaseMediaPlayer() {
        if(mediaPlayer != null) {
            mediaPlayer.release();
        }
    }

}
