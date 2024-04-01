package com.demo.opencv;

import android.graphics.Bitmap;

public class AppLiveData {
    DetectionOutput DetectionOutput; // detection output from model
    KeyboardData KeyboardData; // what to display for the keyboard
    SocialMediaData socialMediaData;

    // calibration interface
    Bitmap[] leftTemplates, rightTemplates;
    int calibrationState;
    String calibrationInstruction;
    boolean isRecording;
    void setDetectionOutput(DetectionOutput detectionOutput) { this.DetectionOutput = detectionOutput; }
    void setKeyboardDisplays(KeyboardData keyboardData) {
        this.KeyboardData = keyboardData;
    }
    void setSocialMediaData(SocialMediaData socialMediaData) {this.socialMediaData = socialMediaData;}
}
