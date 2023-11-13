package com.demo.opencv;


public class GazeData {
    boolean Success;
    int GazeType;
    int GazeLength;
    float GazeProbability;
    private final String[] reference = {"Straight", "Left", "Right", "Up", "Down", "Closed", "LeftUp", "RightUp"};

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
