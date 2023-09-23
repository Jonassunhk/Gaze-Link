package com.demo.opencv;

// Gaze input format for more input information:

/*
Success (Bool): (Is the input valid enough to be usable)

Gaze Types (Int): (The type of user gaze)
0 - unclassified, 1 - left, 2 - right, 3 - top, 4 - down

Gaze Lengths (Int): (Length of user gaze)
Example: 1000 milliseconds, 1500 milliseconds, etc.

 */

import android.graphics.Bitmap;

import org.opencv.core.Mat;

import java.util.ArrayList;

public class GazeInput {
    Boolean Success;
    String gazeType;
    int gazeLength;
    float gazeProbability;
    Mat[] testingMats;
    ArrayList<String> prevInputs;
}
