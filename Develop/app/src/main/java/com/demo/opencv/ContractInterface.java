package com.demo.opencv;
import android.content.Context;
import android.graphics.Bitmap;

import org.opencv.core.Mat;

import java.io.IOException;

public interface ContractInterface {
    interface View {
        void switchMode(String activityName); // method to switch between different modes
        void showInput(int gazeCode);

        void displayText(int code, String text);
        void displayImage(int code, Bitmap bitmap);
        void displayDetectData(GazeInput gazeInput);
    }

    interface Model {
        void initializeModels(Context context) throws IOException; // method to initialize models in the database (OpenCV, TensorFlow Lite, ChatGPT, etc.)
        GazeInput classifyGaze(Mat frame); // determine gaze code with output
    }

    interface Presenter {
        void analyzeGazeInput(); // method that runs with new user gaze input
        void onDestroy(); // method to destroy lifecycle of MainActivity
        void onFrame(Mat rgbMat);
        void initialize(Context context) throws IOException; // method to initialize presenter/model
    }
}