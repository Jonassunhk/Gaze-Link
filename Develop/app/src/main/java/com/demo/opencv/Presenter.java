package com.demo.opencv;

import android.content.Context;
import android.util.Log;

import org.opencv.core.Mat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

public class Presenter implements ContractInterface.Presenter {

    private ContractInterface.View mainView; // creating object of View Interface
    private final ContractInterface.Model model; // creating object of Model Interface
    private DetectionOutput detectionOutput;
    private ArrayList<String> prevInputs;

    // instantiating the objects of View and Model Interface
    public Presenter(ContractInterface.View mainView, ContractInterface.Model model) {
        this.mainView = mainView;
        this.model = model;
    }

    @Override
    public void analyzeGazeInput() { // determine whether input is valid or not based on raw data of both eyes

        if (!detectionOutput.LeftData.Success && !detectionOutput.RightData.Success) { // both eyes not available
            detectionOutput.AnalyzedData = detectionOutput.LeftData;

        } else if (detectionOutput.LeftData.Success && !detectionOutput.RightData.Success) { // only left eye available
            detectionOutput.AnalyzedData = detectionOutput.LeftData;

        } else if (!detectionOutput.LeftData.Success) { // only right eye available
            detectionOutput.AnalyzedData = detectionOutput.RightData;

        } else { // if both eyes are successful, take the gaze input with the lower loss
            if (detectionOutput.LeftData.GazeProbability <= detectionOutput.RightData.GazeProbability) {
                detectionOutput.AnalyzedData = detectionOutput.LeftData;
            } else {
                detectionOutput.AnalyzedData = detectionOutput.RightData;
            }
        }
        if (detectionOutput.AnalyzedData.Success) { // if the input is valid
            if (prevInputs.size() > 50) {
                prevInputs.clear();
            }
            String gazeType = detectionOutput.AnalyzedData.getTypeString();
            if (!Objects.equals(gazeType, "Straight")) { // the input is not straight
                prevInputs.add(gazeType);
                detectionOutput.prevInputs = prevInputs;
            }
        }
    }
    @Override
    public void onDestroy() {
        mainView = null;
    }

    @Override
    public void initialize(Context mContext) throws IOException {
        Log.d("MVPPresenter", "Model Initialized");
        prevInputs = new ArrayList<>();
        model.initializeModels(mContext);
    }

    @Override
    public void onFrame(Mat rgbMat) {
        Log.d("MVPPresenter", "Frame loaded");
        detectionOutput = model.classifyGaze(rgbMat); // get raw data from model

        Log.d("MVPPresenter", "Frame processed");
        analyzeGazeInput();
        mainView.displayDetectData(detectionOutput); // display the gaze data and testing mats

    }
}
