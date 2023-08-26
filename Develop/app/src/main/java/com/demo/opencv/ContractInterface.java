package com.demo.opencv;
import android.content.Context;
import android.graphics.Bitmap;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.core.Mat;

import java.io.IOException;

public interface ContractInterface {
    interface View {
        void switchMode(String activityName); // method to switch between different modes
        void showInput(int gazeCode);

        void displayText(int code, String text);
        void displayImage(int code, Bitmap bitmap);
    }

    interface Model {
        // nested interface to be
        interface OnFinishedListener {
            // function to be called once the Handler of Model class completes its execution
            void onFinished(GazeInput gazeInput, Mat[] testingMats);
        }
        void initializeModels(Context context) throws IOException; // method to initialize models in the database (OpenCV, TensorFlow Lite, ChatGPT, etc.)
        void classifyGaze(ContractInterface.Model.OnFinishedListener onFinishedListener, Mat frame); // determine gaze code with output
    }

    interface Presenter {

        void onUserInput(); // method that runs with new user gaze input
        void onDestroy(); // method to destroy lifecycle of MainActivity

        interface OnFrameListener {
            void onFrame(Mat rgbMat);
        }
        void updateDeveloperMats();
        void initialize(Context context) throws IOException; // method to initialize presenter/model
    }
}
