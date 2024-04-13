package com.demo.opencv.other;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class Mp3Player {
    private MediaPlayer mediaPlayer;

    public void playMp3FromBytes(byte[] mp3SoundByteArray, Context context) {
        Log.d("SpeechGeneration", "Playing audio file");
        try {
            // Write the bytes to a temporary file
            File tempMp3 = File.createTempFile("temp", "mp3", context.getCacheDir());
            tempMp3.deleteOnExit();
            FileOutputStream fos = new FileOutputStream(tempMp3);
            fos.write(mp3SoundByteArray);
            fos.close();

            // MediaPlayer setup and start
            mediaPlayer = new MediaPlayer();

            // Release the MediaPlayer resource once the audio is completed
            mediaPlayer.setOnCompletionListener(MediaPlayer::release);

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                mp.release();
                return true;
            });

            mediaPlayer.setDataSource(tempMp3.getPath());
            mediaPlayer.prepare();
            mediaPlayer.start();
            Log.d("SpeechGeneration", "Finished playing audio file");

        } catch (IOException e) {
            e.printStackTrace();
            Log.d("SpeechGeneration", "Error playing audio file: " + e);
            // Handle exceptions
        }
    }

    // Call this method to stop and release the MediaPlayer resources
    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}