package com.demo.opencv;

import android.content.Context;
import android.util.Log;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import java.io.IOException;


public class Model implements ContractInterface.Model {
    private EyeDetection detector = new EyeDetection(); // create eye detection object to analyze user input
    @Override
    public void initializeModels(Context context) throws IOException {

        // check and initialize OpenCV
        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCVDebug", "cannot init debug");
        } else {
            Log.d("OpenCVDebug", "success");
        }

        detector.mContext = context;
        int files[] = {R.raw.haarcascade_frontalface_alt, R.raw.haarcascade_lefteye_2splits};
        detector.loadTensorModel(); // load TensorFlow Lite Model
        detector.loadOpenCVModels(files); // load OpenCV Cascade Models
    }

    @Override
    public GazeInput classifyGaze(Mat frame) {
        GazeInput gazeInput = detector.inputDetection(frame); // detect gaze
        // testing data
        gazeInput.testingMats = new Mat[4];
        gazeInput.testingMats[0] = detector.eyeROI;
        gazeInput.testingMats[1] = detector.squaredROI;
        gazeInput.testingMats[2] = detector.originalImage;
        gazeInput.testingMats[3] = detector.faceROI;
        return gazeInput;
    }
}
