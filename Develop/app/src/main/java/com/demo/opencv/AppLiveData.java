package com.demo.opencv;

import android.graphics.Bitmap;

public class AppLiveData {
    DetectionOutput DetectionOutput; // detection output from model
    KeyboardData KeyboardData; // what to display for the keyboard
    Bitmap[] leftTemplates, rightTemplates;
    String calibrationInstruction;
    boolean isRecording;
    void setDetectionOutput(DetectionOutput detectionOutput) { this.DetectionOutput = detectionOutput; }
    void setKeyboardDisplays(KeyboardData keyboardData) {
        this.KeyboardData = keyboardData;
    }
}
