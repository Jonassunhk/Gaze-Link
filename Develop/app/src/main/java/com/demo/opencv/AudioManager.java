package com.demo.opencv;

import android.content.Context;
import android.media.MediaPlayer;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.io.File;
import java.util.Locale;

public class AudioManager {
    MediaPlayer mediaPlayer = new MediaPlayer();
    TextToSpeech textToSpeech;
    public void playAudio(File audio) {
        try {
            String path = audio.getAbsolutePath();
            mediaPlayer.setDataSource(path);
            mediaPlayer.prepare();
            mediaPlayer.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void initialize(Context mContext) {
        textToSpeech = new TextToSpeech(mContext, status -> {
            if (status != TextToSpeech.ERROR) {
                textToSpeech.setLanguage(Locale.US);
                Log.d("AudioPlayer", "initialized");
                textToSpeech.speak("Hello this is my day", TextToSpeech.QUEUE_FLUSH, null, null);
            } else {
                Log.d("AudioPlayer", "Error: " + status);
            }
        });
    }
    public void speakText(String text) {
        Log.d("AudioPlayer", text);
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }
}
