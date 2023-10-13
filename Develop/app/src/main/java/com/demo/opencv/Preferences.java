package com.demo.opencv;

import android.app.Application;

public class Preferences extends Application {
    private float sensitivity; // determines how sensitive the detection will mark something as a gaze input
    private int detectionInterval; // determines how may frames the app will consider to detect one gaze input

    public void initialize() { // initialize the settings for first time users
        this.sensitivity = 1.0f;
        this.detectionInterval = 12;
    }

    public float getSensitivity() {
        return sensitivity;
    }
    public void setSensitivity(float sensitivity) {
        this.sensitivity = sensitivity;
    }

    public int getDetectionInterval() {
        return detectionInterval;
    }
    public void setDetectionInterval(int detectionInterval) {
        this.detectionInterval = detectionInterval;
    }


}
