package com.demo.opencv;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import java.io.IOException;


public class Presenter implements ContractInterface.Presenter {

    private ContractInterface.View mainView; // creating object of View Interface
    private final ContractInterface.Model model; // creating object of Model Interface
    private final KeyboardManager keyboardManager = new KeyboardManager();
    private final int[] keyboardInputMat = {-1, 1, 3, 0, -1, 2, -1, -1}; // convert gaze type into keyboard input
    AppLiveData appliveData = new AppLiveData();
    int tempNum = 7;
    int calibrationState = -1; // -1 = idle, 0 - tempNum = during calibration
    Bitmap[] calibrateLeft = new Bitmap[tempNum];
    Bitmap[] calibrateRight = new Bitmap[tempNum];
    String[] calibrationMessages = {"Look Left", "Look Right", "Look Straight", "Look Up", "Look Down", "Look Left and Up", "Look Right and Up"};

    // instantiating the objects of View and Model Interface
    public Presenter(ContractInterface.View mainView, ContractInterface.Model model) {
        this.mainView = mainView;
        this.model = model;
    }
    @Override
    public void onDestroy() {
        mainView = null;
    }

    private Bitmap matToBitmap(Mat mat) {
        Bitmap bm = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat,bm);
        return bm;
    }

    @Override
    public void initialize(Context mContext) throws IOException {

        Log.d("MVPPresenter", "Model Initialized");
        model.initialize(mContext); // MVP model initialization
        keyboardManager.initialize(); // keyboard initialization

        // calibration
        appliveData.calibrationInstruction = "";
        //KeyboardData keyboardData = keyboardManager.getDisplays();
        //mainView.transferKeyboardInput(keyboardData);
    }

    @Override
    public void updateCalibration() {
        if (calibrationState == -1) { // restart calibration
            Log.d("Calibration", "Starting calibration");
            calibrateLeft = new Bitmap[tempNum];
            calibrateRight = new Bitmap[tempNum];
            calibrationState = 0;
            appliveData.calibrationInstruction = calibrationMessages[0];

        } else if (calibrationState == tempNum) { // after calibration
            Log.d("Calibration", "Finished calibration");

        } else { // during calibration
            Mat[] eyeMats = appliveData.DetectionOutput.testingMats;
            if (eyeMats[0] != null && eyeMats[1] != null) { // the images are collectible
                calibrateLeft[calibrationState] = matToBitmap(eyeMats[0]);
                calibrateRight[calibrationState] = matToBitmap(eyeMats[1]);
                calibrationState += 1; // next state
                appliveData.calibrationInstruction = calibrationMessages[calibrationState];
            } else {
                Log.d("Calibration", "Detection failed, please try again");
            }
        }
    }

    @Override
    public void onFrame(Mat rgbMat) {
        Log.d("MVPPresenter", "Frame loaded");
        DetectionOutput detectionOutput = model.classifyGaze(rgbMat); // get raw data from model
        Log.d("MVPPresenter", "Frame processed");

        if (detectionOutput.AnalyzedData != null) {
            int gazeType = detectionOutput.gestureOutput;
            Log.d("MVPPresenter", "Gesture Output: " + gazeType);
            int keyboardInput = keyboardInputMat[gazeType]; // get the keyboard input from the gaze type

            if (keyboardInput != -1) { // if valid (new input)
                keyboardManager.processInput(keyboardInput);
                KeyboardData keyboardData = keyboardManager.getDisplays();
                appliveData.setKeyboardDisplays(keyboardData);
            }
        }
        appliveData.setDetectionOutput(detectionOutput);
        mainView.updateLiveData(appliveData); // display the gaze data and testing mats
    }


}
