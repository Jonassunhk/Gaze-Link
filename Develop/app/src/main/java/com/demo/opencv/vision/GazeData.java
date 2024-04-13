package com.demo.opencv.vision;


public class GazeData {
    public boolean Success;
    public int GazeType;
    int GazeLength;
    public float GazeProbability;
    private final String[] reference = {"Straight", "Left", "Right", "Up", "Down", "Closed", "LeftUp", "RightUp", "ClosedLong"};

    public void setGazeData(Boolean success, int gazeType, int gazeLength, float gazeProbability) {
        this.Success = success;
        this.GazeType = gazeType;
        this.GazeLength = gazeLength;
        this.GazeProbability = gazeProbability;
    }

    public String getTypeString(int index) {
        return reference[index];
    }

}
