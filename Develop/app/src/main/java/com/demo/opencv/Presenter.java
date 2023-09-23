package com.demo.opencv;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

public class Presenter implements ContractInterface.Presenter {

    private ContractInterface.View mainView; // creating object of View Interface
    private ContractInterface.Model model; // creating object of Model Interface
    private GazeInput gazeInput;

    // instantiating the objects of View and Model Interface
    public Presenter(ContractInterface.View mainView, ContractInterface.Model model) {
        this.mainView = mainView;
        this.model = model;
    }

    @Override
    public void analyzeGazeInput() { // determine whether input is valid or not
        if (gazeInput.Success) { // if the input is valid
            if (!Objects.equals(gazeInput.gazeType, "Straight")) { // the input is not straight
                if (gazeInput.prevInputs == null) {
                    gazeInput.prevInputs = new ArrayList<>();
                }
                gazeInput.prevInputs.add(gazeInput.gazeType);
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
        model.initializeModels(mContext);
    }

    @Override
    public void onFrame(Mat rgbMat) {
        Log.d("MVPPresenter", "Frame loaded");
        gazeInput = model.classifyGaze(rgbMat);
        Log.d("MVPPresenter", "Frame processed");

        mainView.displayDetectData(gazeInput);
        analyzeGazeInput();
    }
}
