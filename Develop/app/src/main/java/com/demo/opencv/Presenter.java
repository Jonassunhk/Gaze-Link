package com.demo.opencv;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.IOException;

public class Presenter implements ContractInterface.Presenter, ContractInterface.Presenter.OnFrameListener, ContractInterface.Model.OnFinishedListener {

    private ContractInterface.View mainView; // creating object of View Interface
    private ContractInterface.Model model; // creating object of Model Interface
    Mat eyeROI, faceROI, loadedImage, resized;

    // instantiating the objects of View and Model Interface
    public Presenter(ContractInterface.View mainView, ContractInterface.Model model) {
        this.mainView = mainView;
        this.model = model;
    }

    @Override
    public void onUserInput() {

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
    public void updateDeveloperMats() {
    }

    @Override
    public void initialize(Context mContext) throws IOException {
        Log.d("MVPPresenter", "Model Initialized");
        model.initializeModels(mContext);
    }

    @Override
    public void onFrame(Mat rgbMat) {
        Log.d("MVPPresenter", "Frame loaded");
        ContractInterface.Model.OnFinishedListener listener = null;
        model.classifyGaze(listener, rgbMat);

    }

    @Override
    public void onFinished(GazeInput gazeInput, Mat[] testingMats) {
        Log.d("MVPPresenter", "Frame processed");

        for (int i = 0; i < 3; i++) {
            mainView.displayImage(i, matToBitmap(testingMats[i]));
        }
        updateDeveloperMats();
    }
}
