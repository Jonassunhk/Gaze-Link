package com.demo.opencv;

// Gaze input format for more input information:

/*
Success (Bool): (Is the input valid enough to be usable)

Gaze Types (Int): (The type of user gaze)
0 - unclassified, 1 - left, 2 - right, 3 - top, 4 - down

Gaze Lengths (Int): (Length of user gaze)
Example: 1000 milliseconds, 1500 milliseconds, etc.

 */

import org.opencv.core.Mat;
import org.opencv.core.Point;

import java.util.ArrayList;

public class DetectionOutput {
    GazeData LeftData; GazeData RightData; GazeData AnalyzedData;

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

    int gestureOutput; // an input from the user (not the raw data from the frame)
    Point leftNIC, rightNIC; // Normalized Iris Center for both eyes
    Mat[] testingMats; // testing mats to show process of analysis
    ArrayList<String> prevInputs; // array to store previous inputs, null until presenter
}
