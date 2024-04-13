package com.demo.opencv.socialMedia;

import org.opencv.core.Mat;

public class SocialMediaData {
    public int cameraState = -1; // -1 = idle, 3 - 1 = counting down, 0 = photo taken
    public String textBox = "";
    public boolean logged_in = false;
    public Mat cameraImage;
}
