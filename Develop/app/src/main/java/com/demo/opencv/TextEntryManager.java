package com.demo.opencv;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class TextEntryManager extends AppCompatActivity {
    public boolean LLMEnabled = true;
    List<String> wordPredictions = new ArrayList<>(); // the word predictions shown in word mode
    int wordsPerPage = 4;
    int NWPWords = 4;
    int currentPage = 1;
    int mode = 1; // 0: word mode, 1: blurry input
    String context = ""; // the context content for context-based predictions
    KeyboardData keyboardData = new KeyboardData(); // data structure for UI display
    Context mContext;
    OpenAIManager openAIManager = new OpenAIManager(); // openAI services
    AudioManager audioManager = new AudioManager(); // text-to-speech
    BlurryInput blurryInput = new BlurryInput();
    TextInfo current = new TextInfo();
    void initialize(Context mContext) throws IOException {
        //AssetFileDescriptor textFile = mContext.getAssets().openFd("5000-words.txt");
        this.mContext = mContext;
        openAIManager.initialize(mContext);
        audioManager.initialize(mContext);
        blurryInput.initialize(mContext);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(mMessageReceiver, new IntentFilter("textGenerationEvent"));
        //context = "What do you want to eat?";
    }

    public void manageUserInput(int input) {
        // left gaze = 1, right gaze = 2, up gaze = 0, down gaze = -1, closed = 3, left-up gaze = 4, right-up gaze = 5
        final int[] keyboardInputMat = {-1, 1, 2, 0, -1, 3, 4, 5}; // convert gaze type into keyboard input
        int selection = keyboardInputMat[input];

        if (selection != -1) {
            if (mode == 0) { // baseline text-entry, mode 0
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
            {"SPEAK", "NO MATCH", "NO MATCH", "SWITCH", "NO MATCH", "NO MATCH", "NOPQRST", "UVWXYZ", "ABCDEF", "GHIJKLM"};
    int[] wordIndex = {-1, 2, 3, -1, 0, 1, -1, -1, -1, -1};
    void WordMode(int selection) {
        switch (selection) {
            case 0 -> utterText();
            case 1 -> {selectWord(2); nextWordPrediction(NWPWords); sentencePrediction(); }
            case 2 -> {selectWord(3); nextWordPrediction(NWPWords); sentencePrediction(); }
            case 3 -> mode = 1;
            case 4 -> {selectWord(0); nextWordPrediction(NWPWords); sentencePrediction(); }
            case 5 -> {selectWord(1); nextWordPrediction(NWPWords); sentencePrediction(); }
        }
    }

    // up gaze = 0, left gaze = 1, right gaze = 2, closed = 3, left-up gaze = 4, right-up gaze = 5
    String[] letterModeUI = {
            "DELETE", "NOPQRST", "UVWXYZ", "SWITCH", "ABCDEF", "GHIJKLM", "NO MATCH","NO MATCH", "NO MATCH", "NO MATCH"
    };
    int[] wordIndex2 = {-1, -1, -1, -1, -1, -1, 2, 3, 0, 1};
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
            case 3 -> mode = 0;
            case 4 -> current.addBlurryInput("0");
            case 5 -> current.addBlurryInput("1");
        }
        if (!Objects.equals(current.BlurryInput, "")) { // blurry input not empty
            wordPredictions = blurryInput.getMatchingWords(current.BlurryInput);
        }
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
            keyboardData.sentence = "Enable LLM to generate";
        }
        if (Objects.equals(context, "")) {
            keyboardData.context = "Enter context: ";
        } else {
            keyboardData.context = "Context: " + context;
        }

    }

    public void updateContext(String newContext) {
        Log.d("TextGeneration", "Context updated: " + newContext);
        context = newContext;
        contextWordPrediction(50);
    }

    public KeyboardData getDisplays() {
        return this.keyboardData;
    }

    private void sentencePrediction() { // predicting the sentences based on keywords
        if (!LLMEnabled || current.Words.size() == 0) return; // LLM enabled, at least one word
        String finalString = current.getWords();
        current.Sentence = "Generating...";
        openAIManager.generateText("SP",
                "Keywords: " + finalString + ". Context: " + context);
    }

    public void nextWordPrediction(int num) { // predicting the next word based on current keywords
        if (!LLMEnabled) return;
        if (current.Words.size() > 0 && Objects.equals(current.BlurryInput, "")) { // no blurry input, at least one word
            openAIManager.generateText("NWP",
                    "Given the word '" + current.Words.get(current.Words.size() - 1) +
                            "Predict " + num + " words that will mostly follow that word in a sentence" +
                            "Only output in the format: word, word, word."
            );
        }
    }

    public void contextWordPrediction(int number) {
        if (!LLMEnabled) return;
        if (!Objects.equals(context, "")) { // if there is context
            openAIManager.generateText("CWP",
                    "You are predicting words for a motor neuron disease patient. " +
                            "Give me " + number + " specific or proper nouns that might be part of a response to the phrase:" + context +
                            "during a conversation. " +
                            "For example, for the context: What food do you like? Give words like fries, burgers, sushi, ramen." +
                            "ONLY output in the format: word, word, word."
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
        audioManager.speakText(text);
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mMessageReceiver);
        super.onDestroy();
    }
}
