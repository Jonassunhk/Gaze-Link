package com.demo.opencv;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.ToneGenerator;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;


public class Presenter extends AppCompatActivity implements ContractInterface.Presenter {

    Context mContext;
    public boolean presenterBusy = false;
    private boolean first = true;
    private ContractInterface.View mainView; // creating object of View Interface
    private final ContractInterface.Model model; // creating object of Model Interface
    private final KeyboardManager keyboardManager = new KeyboardManager();
    private final TextEntryManager textEntryManager = new TextEntryManager();
    AppLiveData appliveData = new AppLiveData();
    openAIManager openAIManager = new openAIManager();
    AudioManager audioManager = new AudioManager();
    int tempNum = 7;
    int calibrationState = -1; // -1 = idle, 0 - tempNum = during calibration
    Bitmap[] leftCalibrationData = new Bitmap[tempNum];
    Bitmap[] rightCalibrationData = new Bitmap[tempNum];
    String[] calibrationMessages = {"Look left and down", "Look right and down", "Look straight", "Look up", "Look down", "Look left and up", "Look right and up"};
    ToneGenerator toneGenerator;
    // instantiating the objects of View and Model Interface
    public Presenter(ContractInterface.View mainView, ContractInterface.Model model) {
        this.mainView = mainView;
        this.model = model;
    }
    Mat prevMat;

    @Override
    public void onDestroy() {
        super.onDestroy();
        mainView = null;
    }

    private Bitmap matToBitmap(Mat mat) {
        Bitmap bm = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat,bm);
        return bm;
    }

    @Override
    public void initialize(Context mContext, Context applicationContext) throws IOException {

        Log.d("MVPPresenter", "Model Initialized");
        model.initialize(mContext); // MVP model initialization
        keyboardManager.initialize(); // keyboard initialization
        textEntryManager.initialize(mContext); // text prediction initialization

        // calibration
        leftCalibrationData = model.getLeftCalibrationData(); // get original calibration data
        rightCalibrationData = model.getRightCalibrationData();
        appliveData.calibrationInstruction = "";
        this.mContext = mContext;

        toneGenerator = new ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 100);

        // openAI
        openAIManager.initialize(mContext);
        audioManager.initialize(mContext);

        prevMat = new Mat();

        //openAIManager.textToSpeech(response);

    }

    @Override
    public void updateCalibration() {
        if (calibrationState == -1) { // restart calibration
            Log.d("Calibration", "Starting calibration");

            calibrationState = 0;
            leftCalibrationData[0] = null;
            rightCalibrationData[0] = null;
            appliveData.calibrationInstruction = calibrationMessages[0];

        } else { // during calibration
            Mat[] eyeMats = appliveData.DetectionOutput.testingMats;
            if (eyeMats[0] != null && eyeMats[1] != null) { // the images are collectible
                Log.d("Calibration", "Recorded successfully");
                leftCalibrationData[calibrationState] = matToBitmap(eyeMats[0]);
                rightCalibrationData[calibrationState] = matToBitmap(eyeMats[1]);
                calibrationState += 1; // next state

                if (calibrationState == tempNum) { // finished calibration
                    appliveData.calibrationInstruction = "Calibration Finished!";
                    Log.d("Calibration", "Finished calibration");
                    calibrationState = -1;
                    model.setCalibrationTemplates(mContext, leftCalibrationData, rightCalibrationData);
                } else { // continue
                    leftCalibrationData[calibrationState] = null;
                    rightCalibrationData[calibrationState] = null;
                    appliveData.calibrationInstruction = calibrationMessages[calibrationState];
                }

            } else {
                Log.d("Calibration", "Detection failed, please try again");
            }
        }
    }

    @Override
    public void manageTextGeneration(String data) { // manage data from openAI text generation

    }

    private boolean equal(Mat a, Mat b) {
        if (a.size().equals(b.size())) {
            if (a.channels() == b.channels()) {
                Mat diff = new Mat();
                Core.subtract(a, b, diff);
                Imgproc.cvtColor(diff, diff, Imgproc.COLOR_RGB2GRAY);
                return Core.countNonZero(diff) == 0;
            }
        }
        return false;
    }

    @Override
    public void onFrame(Mat rgbMat) {
        presenterBusy = true;
        if (first) {
            first = false;
        } else if (equal(rgbMat, prevMat)) {
            return;
        }
        prevMat = rgbMat;
        Log.d("MVPPresenter", "Frame loaded");
        DetectionOutput detectionOutput = model.classifyGaze(rgbMat); // get raw data from model
        Log.d("MVPPresenter", "Frame processed");

        if (detectionOutput.AnalyzedData != null) {
            int gazeType = detectionOutput.gestureOutput;
            if (gazeType != 0) {
                Log.d("IrisDetection", "Gesture Output: " + gazeType);
                toneGenerator.startTone(ToneGenerator.TONE_CDMA_PIP, 150);
            }

            textEntryManager.manageUserInput(gazeType);
            KeyboardData keyboardData = textEntryManager.getDisplays();
            appliveData.setKeyboardDisplays(keyboardData);
        }
        appliveData.setDetectionOutput(detectionOutput);
        appliveData.leftTemplates = leftCalibrationData; // templates shown in calibration screen
        appliveData.rightTemplates = rightCalibrationData;
        mainView.updateLiveData(appliveData); // display the gaze data and testing mats




        presenterBusy = false;
    }
}
