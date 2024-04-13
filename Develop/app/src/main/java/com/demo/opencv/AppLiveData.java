package com.demo.opencv;

import android.graphics.Bitmap;

import com.demo.opencv.socialMedia.SocialMediaData;
import com.demo.opencv.textEntry.KeyboardData;
import com.demo.opencv.textEntry.QuickChatData;
import com.demo.opencv.vision.DetectionOutput;

public class AppLiveData {
    public com.demo.opencv.vision.DetectionOutput DetectionOutput; // detection output from model
    public com.demo.opencv.textEntry.KeyboardData KeyboardData; // what to display for the keyboard
    public SocialMediaData socialMediaData;
    public QuickChatData quickChatData;

    // calibration interface
    public Bitmap[] leftTemplates;
    public Bitmap[] rightTemplates;
    public int calibrationState = -1;
    String calibrationInstruction;
    public boolean isRecording;
    void setQuickChatData(QuickChatData quickChatData) {this.quickChatData = quickChatData; }
    void setDetectionOutput(DetectionOutput detectionOutput) { this.DetectionOutput = detectionOutput; }
    void setKeyboardDisplays(KeyboardData keyboardData) {
        this.KeyboardData = keyboardData;
    }
    void setSocialMediaData(SocialMediaData socialMediaData) {this.socialMediaData = socialMediaData;}
}
