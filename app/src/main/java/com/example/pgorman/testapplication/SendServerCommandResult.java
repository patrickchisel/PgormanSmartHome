package com.example.pgorman.testapplication;

/**
 * Created by patri_000 on 12/31/2016.
 */

public class SendServerCommandResult {

    private boolean success;
    private String errorMessage;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public SendServerCommandResult(boolean success, String errorMessage) {
        this.setSuccess(success);
        this.setErrorMessage(errorMessage);
    }
}
