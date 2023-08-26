package com.demo.opencv;

import android.content.Context;
import android.util.Log;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import java.io.IOException;


public class Model implements ContractInterface.Model {
    EyeDetection detector = new EyeDetection(); // create eye detection object to analyze user input
    Mat[] testingMats;
    @Override
    public void initializeModels(Context context) throws IOException {

        // check and initialize OpenCV
        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCVDebug", "cannot init debug");
        } else {
            Log.d("OpenCVDebug", "success");
        }

        detector.mContext = context;
        int files[] = {R.raw.haarcascade_frontalface_alt, R.raw.haarcascade_eye};
        detector.loadTensorModel(); // load TensorFlow Lite Model
        detector.loadOpenCVModels(files); // load OpenCV Cascade Models
    }

    @Override
    public void classifyGaze(OnFinishedListener onFinishedListener, Mat frame) {
        GazeInput gazeInput = detector.inputDetection(frame); // detect gaze
        // testing data
        testingMats[0] = detector.eyeROI;
        testingMats[1] = detector.faceROI;
        testingMats[2] = detector.loadedImage;
        testingMats[3] = detector.resized;

        onFinishedListener.onFinished(gazeInput, testingMats);
    }
}
