package com.demo.opencv.textEntry;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.demo.opencv.other.AudioManager;
import com.demo.opencv.other.OpenAIManager;
import com.demo.opencv.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class TextEntryManager extends AppCompatActivity {
    public boolean LLMEnabled = true;
    public boolean openSocialMedia = false;
    List<String> wordPredictions = new ArrayList<>(); // the word predictions shown in word mode
    int wordsPerPage = 3;
    int NWPWords = 6;
    int currentPage = 1;
    String language = "English"; // default language
    int mode = 1; // 0: word mode, 1: blurry input
    String context = ""; // the context content for context-based predictions
    KeyboardData keyboardData = new KeyboardData(); // data structure for UI display
    Context mContext;
    OpenAIManager openAIManager = new OpenAIManager(); // openAI services
    AudioManager audioManager = new AudioManager(); // text-to-speech
    BlurryInput blurryInput = new BlurryInput();
    TextInfo current = new TextInfo();
    String NoMatchString = String.valueOf(R.string.TextEntry_NoMatch);
    String NextPageString = String.valueOf(R.string.TextEntry_NextPage);
    String SwitchString = String.valueOf(R.string.TextEntry_Switch);
    String DeleteString = String.valueOf(R.string.TextEntry_Delete);
    String FinishedString = String.valueOf(R.string.TextEntry_Finished);
    public void initialize(Context mContext, String language) throws IOException {
        this.mContext = mContext;
        this.language = language;
        openAIManager.initialize(mContext);
        audioManager.initialize(mContext);
        blurryInput.initialize(mContext, language);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(mMessageReceiver, new IntentFilter("textGenerationEvent"));
    }

    public void manageUserInput(int input, boolean isGazeType) {
        // left gaze = 1, right gaze = 2, up gaze = 0, down gaze = -1, closed = 3, left-up gaze = 4, right-up gaze = 5
        int selection = input;
        if (isGazeType) { // if is gaze type, then process it
            final int[] keyboardInputMat = {-1, 1, 2, 0, -1, 3, 4, 5}; // convert gaze type into keyboard input
            selection = keyboardInputMat[input];
        }

        if (selection != -1) {
            if (keyboardData.finished) {
                finishedMode(selection); // finished screen
            } else if (mode == 0) { // baseline text-entry, mode 0
                WordMode(selection);
            } else if (mode == 1){ // baseline text-entry, mode 1
                letterMode(selection);
            }
            //Log.d("TextGeneration", "Current Mode: " + mode);
        }
        updateDisplays();
    }

    // up gaze = 0, left gaze = 1, right gaze = 2, closed = 3, left-up gaze = 4, right-up gaze = 5
    String[] wordModeUI =
            {FinishedString, NoMatchString, NextPageString, SwitchString, NoMatchString, NoMatchString, "NOPQRST", "UVWXYZ", "ABCDEF", "GHIJKLM"};
    int[] wordIndex = {-1, 2, -1, -1, 0, 1, -1, -1, -1, -1};
    void WordMode(int selection) { // when the user is typing words
        switch (selection) {
            case 0 -> keyboardData.finished = true; // open finished page
            case 1 -> {selectWord(2); nextWordPrediction(NWPWords); sentencePrediction(); }
            case 2 -> nextPage();
            case 3 -> {mode = 1; currentPage = 1;}
            case 4 -> {selectWord(0); nextWordPrediction(NWPWords); sentencePrediction(); }
            case 5 -> {selectWord(1); nextWordPrediction(NWPWords); sentencePrediction(); }
        }
    }

    void finishedMode(int selection) { // when the user finished text entry
        switch (selection) {
            case 1 -> utterText(); // speak text
            case 2 -> {openSocialMedia = true; keyboardData.finished = false;} // open social media page
            case 5 -> keyboardData.finished = false;
        }

    }

    // up gaze = 0, left gaze = 1, right gaze = 2, closed = 3, left-up gaze = 4, right-up gaze = 5
    String[] letterModeUI = {
            DeleteString, "NOPQRST", "UVWXYZ", SwitchString, "ABCDEF", "GHIJKLM", NoMatchString, NoMatchString, NoMatchString, NoMatchString
    };
    int[] wordIndex2 = {-1, -1, -1, -1, -1, -1, 2, -1, 0, 1};
    void letterMode(int selection) {
        switch (selection) {
            case 0 -> { // delete blurry input or word
                if (Objects.equals(current.BlurryInput, "")) {
                    current.deleteWord();
                    sentencePrediction();
                } else {
                    current.deleteBlurryInput();
                }
            }
            case 1 -> current.addBlurryInput("2");
            case 2 -> current.addBlurryInput("3");
            case 3 -> {mode = 0; currentPage = 1;}
            case 4 -> current.addBlurryInput("0");
            case 5 -> current.addBlurryInput("1");
        }
        if (!Objects.equals(current.BlurryInput, "")) { // blurry input not empty
            wordPredictions = blurryInput.getMatchingWords(current.BlurryInput);
        }
    }

    public String getLLMSentence() {
        return current.Sentence;
    }

    public void updateDisplays() {

        if (wordPredictions.size() > 0) {
            String[] nextPageWords = blurryInput.getWordPage(wordPredictions, currentPage, wordsPerPage);
            wordModeUI = fillWords(nextPageWords, wordIndex, wordModeUI);
            letterModeUI = fillWords(nextPageWords, wordIndex2, letterModeUI);
        }
        switch (mode) {
            case 0 -> keyboardData.setOptions(wordModeUI);
            case 1 -> keyboardData.setOptions(letterModeUI);
        }
        // creating the text box for final output
        StringBuilder textBox = current.buildText();
        keyboardData.setTextInput(textBox.toString());
        if (LLMEnabled) {
            keyboardData.sentence = "LLM: " + current.Sentence;
        } else {
            keyboardData.sentence = String.valueOf(R.string.TextEntry_EnableLLM);
        }
        if (Objects.equals(context, "")) {
            keyboardData.context = String.valueOf(R.string.TextEntry_EnterContext);
        } else {
            keyboardData.context = "Context: " + context;
        }
    }

    public void updateContext(String newContext) {
        Log.d("TextGeneration", "Context updated: " + newContext);
        context = newContext;
        contextWordPrediction(50);
        updateDisplays();
    }

    public KeyboardData getDisplays() {
        return this.keyboardData;
    }

    private void sentencePrediction() { // predicting the sentences based on keywords
        if (!LLMEnabled || current.Words.size() == 0) return; // LLM enabled, at least one word
        String finalString = current.getWords();
        current.Sentence = "Generating...";
        openAIManager.generateText("SP", "Keywords: " + finalString + ". Context: " + context, language);
    }

    public void nextWordPrediction(int num) { // predicting the next word based on current keywords
        if (!LLMEnabled || Objects.equals(language, "Chinese")) return;
        if (current.Words.size() > 0 && Objects.equals(current.BlurryInput, "")) { // no blurry input, at least one word
            openAIManager.generateText("NWP",
                    "Given the word '" + current.Words.get(current.Words.size() - 1) +
                            "Predict " + num + " " + language + "words that will mostly follow that word in a sentence" +
                            "Only output in the format: word, word, word.", language
            );
        }
    }



    public void contextWordPrediction(int number) {
        if (!LLMEnabled || Objects.equals(language, "Chinese")) return;
        if (!Objects.equals(context, "")) { // if there is context
            openAIManager.generateText("CWP",
                    "You are predicting words for a " + language + " motor neuron disease patient. " +
                            "Give me " + number + " specific or proper nouns in " + language + " that might be part of a response to the " + language + " phrase:" + context +
                            "during a conversation. " +
                            "ONLY output in the format: word, word, word.", language
            );
        }
    }

    private final BroadcastReceiver mMessageReceiver = new BroadcastReceiver() { // tag 1: SP = sentence prediction, NWP = next word prediction, CWP = context word prediction
        @Override
        public void onReceive(Context context, Intent intent) {
            String output = intent.getStringExtra("message");
            if (output != null) {
                String[] splitArray = output.split("-");
                String tag = splitArray[0]; // identifier to check the usage of the generation
                String content = splitArray[1];

                Log.d("TextGeneration", "Data Received, " + tag + ": " + content);

                if (Objects.equals(tag, "SP")) { // sentence prediction: SP
                    current.Sentence = content;

                } else if (Objects.equals(tag, "NWP")) { // next word prediction: NWP
                    wordPredictions.clear();
                    String[] wordChoices = content.split(", "); // there should be n words, separated by comma
                    for (int i = 0; i < wordChoices.length; i++) {
                        wordPredictions.add(i, wordChoices[i]);
                    }

                } else if (Objects.equals(tag, "CWP")) { // context word prediction
                    String[] wordChoices = content.split(", ");
                    blurryInput.extendedWordList.clear();
                    blurryInput.extendedWordList.addAll(Arrays.asList(wordChoices));
                }
                updateDisplays();
            } else {
                Log.d("TextGeneration", "Fail to get data in text entry manager");
            }
        }
    };

    void selectWord(int index) {
        int wordIndex = index + (currentPage - 1) * wordsPerPage;
        if (wordPredictions.size() > wordIndex) { // input is valid
            current.addWord(wordPredictions.get(wordIndex));
            current.clearBlurryInputs();
        }
        currentPage = 1;
    }

    String[] fillWords(String[] words, int[] wordIndex, String[] options) { // fill words in a UI page
        Log.d("TextMode", Arrays.toString(words));
        for (int i = 0; i < wordIndex.length; i++) {
            if (wordIndex[i] != -1) { // check that there's enough words to output
                if (words.length > wordIndex[i]) { // a word is available to display
                    options[i] = words[wordIndex[i]];
                }
            }
        }
        return options;
    }

    void nextPage() {
        String[] words = blurryInput.getWordPage(wordPredictions,currentPage + 1, wordsPerPage);
        if (words == null) { // page doesn't exist
            Log.d("TextMode", "Max page reached");
            currentPage = 1;
        } else {
            Log.d("TextMode", "next page available");
            currentPage += 1; // add the page
        }
    }

    void utterText() {
        String text;
        if (LLMEnabled) {
            text = current.Sentence;
        } else {
            text = current.getWords();
        }
        //audioManager.speakText(text); // android built-in speech
        openAIManager.speechGenerationService(text); // openAI speech
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mMessageReceiver);
        super.onDestroy();
    }
}
