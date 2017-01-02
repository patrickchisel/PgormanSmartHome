package com.example.pgorman.testapplication;

/**
 * Created by patri_000 on 1/1/2017.
 */

public class InitCommandTimeout {

    private long lastInitCommandReceivedMs = 0;
    private boolean timeoutCaptured = true;

    private static final int INIT_COMMAND_TIMEOUT_MS = 5000;

    public synchronized boolean checkIfUncapturedTimeout() {
        if(timeoutCaptured) {
            return false;
        }

        long currentMs = System.currentTimeMillis();
        boolean valid = currentMs <= (lastInitCommandReceivedMs + INIT_COMMAND_TIMEOUT_MS);

        if(!valid) {
            timeoutCaptured = true;
            return true;
        }
        return false;
    }

    public synchronized boolean checkAndRefreshInitCommand() {
        long currentMs = System.currentTimeMillis();
        boolean valid = currentMs <= (lastInitCommandReceivedMs + INIT_COMMAND_TIMEOUT_MS);
        if(valid) {
            setInitCommandReceived();
        }
        return valid;
    }

    public synchronized void setInitCommandReceived() {
        lastInitCommandReceivedMs = System.currentTimeMillis();
        timeoutCaptured = false;
    }

    public synchronized void resetCommandTimeout() {
        lastInitCommandReceivedMs = 0;
        timeoutCaptured = true;
    }

    public long getLastCommandReceivedMs() {
        return lastInitCommandReceivedMs;
    }
}
