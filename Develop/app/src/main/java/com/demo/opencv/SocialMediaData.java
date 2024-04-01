package com.demo.opencv;

import org.opencv.core.Mat;

public class SocialMediaData {
    int cameraState = -1; // -1 = idle, 3 - 1 = counting down, 0 = photo taken
    String textBox = "";
    String userName = "";
    Mat cameraImage;
}
