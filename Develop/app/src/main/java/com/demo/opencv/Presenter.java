package com.demo.opencv;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.ToneGenerator;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Objects;

public class Presenter extends AppCompatActivity implements ContractInterface.Presenter {

    Context mContext;
    public boolean presenterBusy = false;
    public String mode = ""; // Calibration, Text, Dev, Clinician
    private boolean first = true;
    private ContractInterface.View mainView; // creating object of View Interface
    private final ContractInterface.Model model; // creating object of Model Interface
    private final KeyboardManager keyboardManager = new KeyboardManager();
    private final TextEntryManager textEntryManager = new TextEntryManager();
    AppLiveData appliveData = new AppLiveData();
    OpenAIManager OpenAIManager = new OpenAIManager();
    AudioManager audioManager = new AudioManager();
    int tempNum = 7;
    int textEntryMode = 2;
    int calibrationState = -1; // -1 = idle, 0 - tempNum = during calibration
    Bitmap[] leftCalibrationData = new Bitmap[tempNum];
    Bitmap[] rightCalibrationData = new Bitmap[tempNum];
    HashMap<String, String> settings = new HashMap<>();
    String[] calibrationMessages = {"Look left and down", "Look right and down", "Look straight", "Look up", "Look down", "Look left and up", "Look right and up"};
    private final String[] reference = {"Straight", "Left", "Right", "Up", "Down", "Closed", "Left Up", "Right Up"};
    public ClinicalData clinicalData = new ClinicalData();
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
        this.mContext = mContext;

        model.initialize(mContext); // MVP model initialization
        keyboardManager.initialize(); // keyboard initialization
        textEntryManager.initialize(mContext); // text prediction initialization
        clinicalData = new ClinicalData();

        // calibration
        leftCalibrationData = model.getLeftCalibrationData(); // get original calibration data
        rightCalibrationData = model.getRightCalibrationData();
        // if there is no calibration data
        if (leftCalibrationData == null) { leftCalibrationData = new Bitmap[tempNum]; }
        if (rightCalibrationData == null) { rightCalibrationData = new Bitmap[tempNum]; }
        appliveData.calibrationInstruction = "";

        toneGenerator = new ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 100);

        //settings
        settings = model.getSettings();

        // openAI
        OpenAIManager.initialize(mContext);
        audioManager.initialize(mContext);

        prevMat = new Mat();
    }

    @Override
    public void updateCalibration() {
        if (calibrationState == -1) { // restart calibration
            Log.d("Calibration", "Starting calibration");
            calibrationState = 0;
            audioManager.speakText(calibrationMessages[calibrationState]);
            leftCalibrationData[0] = null;
            rightCalibrationData[0] = null;
            appliveData.calibrationInstruction = calibrationMessages[0];
            clinicalData.leftNICX.clear();
            clinicalData.leftNICY.clear();

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
                    audioManager.speakText(calibrationMessages[calibrationState]);
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
    public boolean getPresenterState() {
        return presenterBusy;
    }

    @Override
    public ClinicalData getClinicalData() {
        return clinicalData;
    }

    @Override
    public void setMode(String value) {
        mode = value;
        if (Objects.equals(mode, "Text")) {
            if (textEntryMode == 0) {
                KeyboardData keyboardData = keyboardManager.getDisplays();
                appliveData.setKeyboardDisplays(keyboardData);
            } else {
                textEntryManager.updateDisplays();
                KeyboardData keyboardData = textEntryManager.getDisplays();
                appliveData.setKeyboardDisplays(keyboardData);
            }
        }
    }

    @Override
    public String getMode() { return mode; }

    @Override
    public void onSettingValueChange(String valueName, String value) {
        if (Objects.equals(valueName, "TextEntryMode")) {
            if (Objects.equals(value, "0")) {
                Log.d("Presenter", "Switched to letter-by-letter mode");
            } else if (Objects.equals(value, "1")) {
                Log.d("Presenter", "Switched to ambiguous keyboard only mode");
                textEntryManager.LLMEnabled = false;
            } else if (Objects.equals(value, "2")) {
                Log.d("Presenter", "Switched to ambiguous keyboard + LLM mode");
                textEntryManager.LLMEnabled = true;
            }
            textEntryMode = Integer.parseInt(value);
        }
        if (Objects.equals(valueName, "Context")) {
            textEntryManager.updateContext(value);
        }
        settings.put(valueName, value);
        model.onSettingValueChange(valueName, value);
    }

    @Override
    public HashMap<String, String> getSettings() {
        return settings;
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
        } else if (equal(rgbMat, prevMat)) { // prevent identical frames from being processed
            return;
        }
        prevMat = rgbMat;
       // Log.d("MVPPresenter", "Frame loaded");
        DetectionOutput detectionOutput = model.classifyGaze(rgbMat); // get detection output from model
     //   Log.d("MVPPresenter", "Frame processed");

        if (detectionOutput.AnalyzedData != null) { // when the input is valid
            if (Objects.equals(mode, "Text") || Objects.equals(mode, "Dev")) { // not calibrating or in clinician mode, process the input
                int gazeType = detectionOutput.gestureOutput;

                if (gazeType != 0) { // gaze input is meaningful
                    Log.d("IrisDetection", "Gesture Output: " + gazeType);
                    toneGenerator.startTone(ToneGenerator.TONE_CDMA_PIP, 150);

                    if (Objects.equals(mode, "Text")) { // only do text input in text mode

                        String gazeLogInput = Calendar.getInstance().getTime() + ": " + reference[gazeType];
                        clinicalData.gazeLog.add(gazeLogInput);

                        if (textEntryMode == 0) { // letter-by-letter mode
                            keyboardManager.processInput(gazeType);
                            KeyboardData keyboardData = keyboardManager.getDisplays();
                            appliveData.setKeyboardDisplays(keyboardData);
                        } else { // other two modes
                            textEntryManager.manageUserInput(gazeType);
                            KeyboardData keyboardData = textEntryManager.getDisplays();
                            appliveData.setKeyboardDisplays(keyboardData);
                        }
                    }
                }
            } else if (calibrationState != -1 && Objects.equals(mode, "Calibration")) { // when the user is calibrating
                clinicalData.leftNICX.add((float)detectionOutput.leftNIC.x);
                clinicalData.leftNICY.add((float)detectionOutput.leftNIC.y);
            }
        }

        appliveData.setDetectionOutput(detectionOutput);
        appliveData.leftTemplates = leftCalibrationData; // templates shown in calibration screen
        appliveData.rightTemplates = rightCalibrationData;
        mainView.updateLiveData(appliveData); // display the gaze data and testing mats
        presenterBusy = false;
    }


}
