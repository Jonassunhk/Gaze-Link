package com.demo.opencv;
import android.content.Context;
import android.graphics.Bitmap;

import org.checkerframework.checker.units.qual.A;
import org.opencv.core.Mat;

import java.io.IOException;

public interface ContractInterface {
    interface View {
        void switchMode(String activityName); // method to switch between different modes
        void displayImage(int code, Bitmap bitmap);
        void updateLiveData(AppLiveData appLiveData);
    }

    interface Model {
        void analyzeGazeOutput(); // method that runs with new user gaze input
        void initialize(Context context) throws IOException; // method to initialize models in the database (OpenCV, TensorFlow Lite, ChatGPT, etc.)
        void setCalibrationTemplates(Context context, Bitmap[] leftEye, Bitmap[] rightEye);
        DetectionOutput classifyGaze(Mat rgbMat); // determine gaze code with output
        Bitmap[] getLeftCalibrationData();
        Bitmap[] getRightCalibrationData();
    }

    interface Presenter {
        void onDestroy(); // method to destroy lifecycle of MainActivity
        void onFrame(Mat rgbMat); // runs every frame
        void initialize(Context context) throws IOException; // method to initialize presenter/model
        void updateCalibration();
    }
}