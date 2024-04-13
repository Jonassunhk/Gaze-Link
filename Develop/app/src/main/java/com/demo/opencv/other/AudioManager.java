package com.demo.opencv.other;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

public class AudioManager extends Activity {
    MediaPlayer mediaPlayer = new MediaPlayer();
    private SpeechRecognizer speechRecognizer;
    TextToSpeech textToSpeech;
    MediaRecorder recorder;
    private String fileName;
    String finalText;

    public interface AudioManagerListener {
        void onAudioUpdated(String key, String value);
    }
    AudioManager.AudioManagerListener audioManagerListener;

    public void setAudioManagerListener(AudioManagerListener audioManagerListener) {
        this.audioManagerListener = audioManagerListener;
    }

    public void initialize(Context mContext) {
        textToSpeech = new TextToSpeech(mContext, status -> {
            if (status != TextToSpeech.ERROR) {
                textToSpeech.setLanguage(Locale.US);
                Log.d("AudioManager", "initialized");
                //textToSpeech.speak("Thank you for using gaze link.", TextToSpeech.QUEUE_FLUSH, null, null);
            } else {
                Log.d("AudioManager", "Error: " + status);
            }
        });
        fileName = Objects.requireNonNull(mContext.getExternalCacheDir()).getAbsolutePath();
        fileName += "/contextAudio.3gp";

        setSpeechRecognizer(mContext); // set speech to text
    }
    public void speakText(String text) {
        Log.d("AudioManager", text);
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }

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

    public void startRecording() {
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setOutputFile(fileName);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            recorder.prepare();
        } catch (IOException e) {
            Log.e("AudioManager", "prepare() failed");
        }
        recorder.start();
    }

    public void stopRecording() {
        recorder.stop();
        recorder.release();
        recorder = null;
    }


    public void startListening() {
        Log.d("AudioManager", "Began recording");
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
        speechRecognizer.startListening(intent);
    }

    public void stopListening() {
        speechRecognizer.stopListening();
    }

    public void setSpeechRecognizer(Context context) {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {

            }

            @Override
            public void onBeginningOfSpeech() {

            }

            @Override
            public void onRmsChanged(float rmsdB) {

            }

            @Override
            public void onBufferReceived(byte[] buffer) {

            }

            @Override
            public void onEndOfSpeech() {

            }

            @Override
            public void onError(int error) {

            }

            // Implement the necessary methods
            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null) {
                    finalText = matches.get(0).toLowerCase();
                    audioManagerListener.onAudioUpdated("Context", finalText);
                    Log.d("AudioManager", "Text recognized: " + finalText);
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) { }

            @Override
            public void onEvent(int eventType, Bundle params) { }

            // Implement other abstract methods like onReadyForSpeech, onError, etc.
        });
    }
}
