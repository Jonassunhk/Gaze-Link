package com.demo.opencv;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TextEntryManager extends AppCompatActivity {
    private final List<String> wordList = new ArrayList<>(); // top 5000 word frequency list
    private final List<String> blurryInputProcessed = new ArrayList<>(); // processed string with 1, 2, 3, and 4 for blurry input
    public String currentBlurryInput = ""; // in blurry input format
    public StringBuilder textBox = new StringBuilder(); // the actual text that user inputted
    public List<String> inputtedWords = new ArrayList<>(); // the previous words that user inputted
    public ArrayList<String> inputtedSentences = new ArrayList<>(); // the previous sentences that user inputted
    public ArrayList<String> wordPredictions = new ArrayList<>();
    public int wordPredictionNum = 4;
    int mode = 0; // 0: main menu, 1: blurry input, 2: word prediction, 3: sentence prediction

    // in the order of up, left, right, down, right up, left up
    String[] blurryInputOptions = {"A, B, C, D, E, F", "G, H, I, J, K, L, M", "N, O, P, Q, R, S, T", "U, V, W, X, Y, Z", "Right up: Switch to word prediction", "Left Up: Delete last blurry input", "Blurry Input Mode"};
    String[] wordModeOptions = {"", "", "", "", "Right up: Back to main menu", "Left up: Delete previous word", "Word Mode"};
    String[] mainMenuOptions = {"Up: Blurry Input Prediction", "Left: Word Prediction", "Right: Sentence Prediction", "", "Right up: Speak text", "Left up: Clear text", "Text-entry Main Menu"};
    String[] sentenceModeOptions = {"", "", "", "", "Right up: Back to main menu", "Left up: Delete sentence", "Sentence Mode"};
    String[] blurryInputText = {"A-F", "G-M", "N-T", "U-Z"};
    KeyboardData keyboardData = new KeyboardData();
    Context mContext;
    openAIManager openAIManager = new openAIManager();
    AudioManager audioManager = new AudioManager();
    String[] sentencePredictions = new String[4];

    private void updateDisplays() {
        switch (mode) {
            case 0 -> keyboardData.setOptions(mainMenuOptions);
            case 1 -> keyboardData.setOptions(blurryInputOptions);
            case 2 -> {
                for (int i = 0; i < wordPredictions.size(); i++) {
                    wordModeOptions[i] = wordPredictions.get(i);
                }
                keyboardData.setOptions(wordModeOptions);
            }
            case 3 -> {
                System.arraycopy(sentencePredictions, 0, sentenceModeOptions, 0, 4);
                keyboardData.setOptions(sentenceModeOptions);
            }
        }

        textBox = new StringBuilder(); // creating the text box for final output
        for (int i = 0; i < inputtedSentences.size(); i++) { // adding the sentences into the text box
            textBox.append(inputtedSentences.get(i));
        }
        textBox.append(' ');
        for (int i = 0; i < inputtedWords.size(); i++) { // adding the words into the text box
            textBox.append(inputtedWords.get(i));
            textBox.append(' ');
        }
        for (int i = 0; i < currentBlurryInput.length(); i++) { // appending the current blurry input
            int input = (int) currentBlurryInput.charAt(i) - '0';
            textBox.append(blurryInputText[input]);
            textBox.append(' ');
        }
        keyboardData.setTextInput(textBox.toString());
    }

    public KeyboardData getDisplays() {
        return this.keyboardData;
    }

    public void sentencePrediction() {
        textBox = new StringBuilder(); // string to combine all words
        for (int i = 0; i < inputtedWords.size(); i++) { // adding the words into the text box
            textBox.append(inputtedWords.get(i));
            textBox.append(' ');
        }
        String finalString = textBox.toString();
        openAIManager.generateText(
                "You are a motor neuron disease patient who wishes to communicate with keywords." +
                        "Generate a coherent and simple sentence with the keywords:" + finalString +
                        "For example, 'want, food' becomes 'I want to eat food'. 'give, book' becomes 'give me the book.'");
    }

    private final BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            sentencePredictions[0] = intent.getStringExtra("message");
            Log.d("TextGeneration","Got message: " + sentencePredictions[0]);
            sentenceModeOptions[0] = sentencePredictions[0];
            //audioManager.speakText(sentencePrediction);
        }
    };

    private void mode0Input(int selection) { // input for main menu mode

        switch (selection) {
            case 0 -> mode = 1; // switch to blurry input mode
            case 1 -> { // switch to word mode
                mode = 2;
                blurryWordPrediction(currentBlurryInput, wordPredictionNum);
            }
            case 2 -> { // switch to sentence mode
                mode = 3;
                for (int i = 0; i < 4; i++) {
                    sentencePredictions[i] = ""; // clear sentence predictions
                }
                if (inputtedWords.size() >= 2) { // if more than 2 keywords, do sentence prediction
                    sentencePrediction();
                } else { // if less, there is no sentence prediction!!
                    for (int i = 0; i < 4; i++) {
                        sentenceModeOptions[i] = "Needs at least 2 words";
                    }
                }
            }
        }
    }

    private void mode1Input(int selection) { // input for blurry input
        switch (selection) {
            case 5 -> { // switch to word prediction
                mode = 2;
                blurryWordPrediction(currentBlurryInput, wordPredictionNum);
            }
            case 4 -> { // delete previous blurry input
                if (currentBlurryInput.length() != 0) {
                    currentBlurryInput = currentBlurryInput.substring(0, currentBlurryInput.length() - 1); // cut by one blurry input
                }
            }
            default -> currentBlurryInput += selection;
        }
    }

    private void mode2Input(int selection) { // input for word input
        switch (selection) {
            case 5 -> mode = 0; // back to main menu
            case 4 -> { // delete previous word
                if (inputtedWords.size() > 0) {
                    inputtedWords.remove(inputtedWords.size() - 1); // remove last word
                }
            }
            default -> { // add the word and clear blurry input
                if (wordPredictions.size() > selection) { // input is valid
                    inputtedWords.add(wordPredictions.get(selection));
                    mode = 1;
                    currentBlurryInput = "";
                }
            }
        }
    }

    private void mode3Input(int selection) {
        switch (selection) {
            case 5 -> mode = 0; // back to main menu
            case 4 -> { // removing sentences
                if (inputtedSentences.size() > 0) {
                    inputtedSentences.remove(inputtedSentences.size() - 1);
                }
            }
            default -> { // TODO: select sentence prediction
                if (!Objects.equals(sentencePredictions[selection], "")) {
                    inputtedSentences.add(sentencePredictions[selection]); // select and put the sentence in
                    for (int i = 0; i < 4; i++) {
                        sentencePredictions[i] = ""; // clear sentence predictions
                    }
                    inputtedWords.clear();
                }
            }
        }
    }

    public void manageUserInput(int selection) {
        // left gaze = 1, right gaze = 2, up gaze = 0, down gaze = -1, closed = 3, left-up gaze = 4, right-up gaze = 5
        final int[] keyboardInputMat = {-1, 1, 2, 0, -1, 3, 4, 5}; // convert gaze type into keyboard input
        int input = keyboardInputMat[selection];

        if (input != -1) {
            switch (mode) { // different inputs for different modes
                case 0 -> mode0Input(input);
                case 1 -> mode1Input(input);
                case 2 -> mode2Input(input);
                case 3 -> mode3Input(input);
            }
            Log.d("TextGeneration", "Current Mode: " + mode);
        }
        updateDisplays();
    }

    private String processWord(String word) { // converts original word to processed word for blurry input
        StringBuilder processed = new StringBuilder();
        for (int i = 0; i < word.length(); i++) {
            if (word.charAt(i) <= 'f') { processed.append("0"); } // group 1
            else if (word.charAt(i) <= 'm') { processed.append("1"); } // group 2
            else if (word.charAt(i) <= 't') { processed.append("2"); } // group 3
            else if (word.charAt(i) <= 'z') { processed.append("3"); } // group 4
        }
        return processed.toString();
    }

    void initialize(Context mContext) throws IOException {
        //AssetFileDescriptor textFile = mContext.getAssets().openFd("5000-words.txt");
        this.mContext = mContext;
        openAIManager.initialize(mContext);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(mMessageReceiver, new IntentFilter("textGenerationEvent"));
        try {
            InputStream inputStream = mContext.getAssets().open("5000-words.txt");
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                wordList.add(line);
                blurryInputProcessed.add(processWord(line)); // process word
            }
            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        inputtedWords.add("want");
        inputtedWords.add("sleep");
        inputtedWords.add("now");

        Log.d("TextPrediction", "First word in the word list: " +  wordList.get(0));
        Log.d("TextPrediction", "First processed word in the word list: " +  blurryInputProcessed.get(0));
    }

    public void blurryWordPrediction(String codeA, int num) { // predict the word based on a blurry input

        wordPredictions = new ArrayList<>();
        if (codeA.length() == 0) { // the string is empty, nothing to compare
            return;
        }

        for (int i = 0; i < blurryInputProcessed.size(); i++) {
            String codeB = blurryInputProcessed.get(i);
            if (codeA.length() <= codeB.length()) {
                String subString = codeB.substring(0, codeA.length());
                //Log.d("TextPrediction", "Code: " + codeA + ", substring from word: " + subString);
                if (subString.equals(codeA)) {
                    wordPredictions.add(wordList.get(i));
                }
            }
            if (wordPredictions.size() == num) {break;} // enough word predictions
        }
        Log.d("TextPrediction", "Word detected: " + wordPredictions.size());
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mMessageReceiver);
        super.onDestroy();
    }
}
