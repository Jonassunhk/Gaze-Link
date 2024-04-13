package com.demo.opencv.socialMedia;

import android.content.Context;
import android.os.CountDownTimer;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.demo.opencv.UserDataManager;

import org.opencv.core.Mat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SocialMediaManager {
    UserDataManager userDataManager;
    TwitterClient twitterClient = new TwitterClient();
    Context mContext, applicationContext;
    public boolean closeSocialMedia = false;
    List<String> sentences = new ArrayList<>();
    SocialMediaData socialMediaData = new SocialMediaData();
    Mat currentFrame;
    boolean uploadPhoto = false;

    public void initialize(Context context, Context applicationContext) {
        this.mContext = context;
        this.applicationContext = applicationContext;
        userDataManager = (UserDataManager) applicationContext;
        twitterClient.init(applicationContext); // init twitter API
        socialMediaData.logged_in = !(Objects.equals(userDataManager.getAccessToken(), ""));
    }

    public void updateCameraFrame(Mat cameraFrame) {
        this.currentFrame = cameraFrame;
    }

    public void manageUserInput(int input, boolean isGazeType) {
        // left gaze = 1, right gaze = 2, up gaze = 0, down gaze = -1, closed = 3, left-up gaze = 4, right-up gaze = 5
        int selection = input;
        if (isGazeType) { // if is gaze type, then process it (is false for touchscreen)
            final int[] keyboardInputMat = {-1, 1, 2, 0, -1, 3, 4, 5}; // convert gaze type into keyboard input
            selection = keyboardInputMat[input];
        }

        // up gaze = 0, left gaze = 1, right gaze = 2, closed = 3, left-up gaze = 4, right-up gaze = 5
        if (selection != -1) {
            switch (selection) {
                case 0 -> deleteSentence(); // open finished page
                case 1 -> updateCamera(); // capturing -> captured, captured -> capturing
                case 4 -> closeSocialMedia = true; // close social media page
                case 5 -> postTweet(); // post tweet on twitter
            }
        }
        updateDisplays();
    }

    void updateCamera() { // update the camera
        if (socialMediaData.cameraState == -1) { // idle, begin taking photo
            socialMediaData.cameraState = 3;
            cameraCountdown();
        } else if (socialMediaData.cameraState == 0) { // finished photo, so cancel the photo
            socialMediaData.cameraState = -1;
            uploadPhoto = false;
        }
    }

    void cameraCountdown() {
        ContextCompat.getMainExecutor(mContext).execute(()  -> {
            new CountDownTimer(3000, 1000) { // 3 seconds, countdown per second
                public void onTick(long millisUntilFinished) { // tick every second
                    socialMediaData.cameraState = (int) millisUntilFinished / 1000;
                }
                public void onFinish() { // finish countdown, take the photo
                    socialMediaData.cameraState = 0;
                    uploadPhoto = true;
                    socialMediaData.cameraImage = currentFrame;
                }

            }.start();
        });

    }

    public void addSentence(String newSentence) {
        sentences.add(newSentence);
    }

    private void deleteSentence() { // delete the last sentence
        if (sentences.size() > 0) {
            sentences.remove(sentences.size() - 1);
        }
    }

    String getFullText() { // turning all sentences into a string
        StringBuilder stringBuilder = new StringBuilder();
        for (String sentence: sentences) {
            stringBuilder.append(sentence);
            stringBuilder.append(" ");
        }
        return stringBuilder.toString();
    }

    void postTweet() { // posting a tweet
        String finalString = getFullText();
        if (!Objects.equals(finalString, "")) {
            if (uploadPhoto && socialMediaData.cameraImage != null) {
                Log.d("SocialMediaPost", "Uploading tweet with image");
                twitterClient.uploadTweetWithPhoto(mContext, socialMediaData.cameraImage, finalString);
            } else {
                Log.d("SocialMediaPost", "Uploading tweet without image");
                twitterClient.sendTweet(finalString, "");
            }
        }
    }

    public void updateDisplays() {
        if (socialMediaData.cameraState != 0) {
            socialMediaData.cameraImage = currentFrame;
        }
        socialMediaData.textBox = getFullText();
    }

    public SocialMediaData getDisplays() {
        return this.socialMediaData;
    }

}
