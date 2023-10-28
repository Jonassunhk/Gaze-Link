package com.demo.opencv;

public class AppLiveData {
    DetectionOutput DetectionOutput; // detection output from model
    KeyboardData KeyboardData; // what to display for the keyboard
    String calibrationInstruction;
    void setDetectionOutput(DetectionOutput detectionOutput) { this.DetectionOutput = detectionOutput; }
    void setKeyboardDisplays(KeyboardData keyboardData) {
        this.KeyboardData = keyboardData;
    }
    void setCalibrationInstruction(String calibrationInstruction) { this.calibrationInstruction = calibrationInstruction; }
}
