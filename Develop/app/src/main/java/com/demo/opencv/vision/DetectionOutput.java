package com.demo.opencv.vision;

import com.demo.opencv.vision.GazeData;

import org.opencv.core.Mat;
import org.opencv.core.Point;

import java.util.ArrayList;

public class DetectionOutput {
    public GazeData LeftData; public GazeData RightData; public GazeData AnalyzedData;
    public void setEyeData(int type, Boolean Success, int GazeType, int GazeLength, float GazeProbability) {
        // 0: left, 1: right, 2: final
        if (type == 0) {
            // set left eye data
            this.LeftData.setGazeData(Success, GazeType, GazeLength, GazeProbability);
        } else if (type == 1) {
            // set right eye data
            this.RightData.setGazeData(Success, GazeType, GazeLength, GazeProbability);
        } else {
            // set analyzed gaze data
            this.AnalyzedData.setGazeData(Success, GazeType, GazeLength, GazeProbability);
        }
    }

    public void initialize(int MatNum) {
        this.RightData = new GazeData();
        this.LeftData = new GazeData();
        this.AnalyzedData = new GazeData();
        testingMats = new Mat[MatNum];
    }

    public int gestureOutput; // an input from the user (not the raw data from the frame)

    public Point leftNIC; // Normalized Iris Center for both eyes
    public Mat[] testingMats; // testing mats to show process of analysis
    public ArrayList<String> prevInputs; // array to store previous inputs, null until presenter
}
