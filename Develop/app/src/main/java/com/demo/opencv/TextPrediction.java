package com.demo.opencv;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class TextPrediction {
    private final List<String> wordList = new ArrayList<>(); // top 5000 word frequency list
    private final List<String> blurryInputProcessed = new ArrayList<>(); // processed string with 1, 2, 3, and 4 for blurry input
    public String currentBlurryInput = ""; // in blurry input format
    public StringBuilder textBox = new StringBuilder(); // the actual text that user inputted
    public List<String> inputtedWords = new ArrayList<>(); // the previous words that user inputted
    public ArrayList<String> wordPredictions = new ArrayList<>();
    public int wordPredictionNum = 4;
    int mode = 0; // 0: blurry input, 1: word prediction
    String[] blurryInputOptions = {"A, B, C, D, E, F", "G, H, I, J, K, L, M", "N, O, P, Q, R, S, T", "U, V, W, X, Y, Z", "Right up: Switch to word prediction", "Left Up: Delete last blurry input"};
    String[] textPredictionOptions = {"", "", "", "", "Right up: Delete previous word", "Left up: Switch to blurry input"};
    String[] blurryInputText = {"A-F", "G-M", "N-T", "U-Z"};
    KeyboardData keyboardData = new KeyboardData();

    private void updateDisplays() {
        if (mode == 0) { // blurry input
            keyboardData.setOptions(blurryInputOptions);
        } else if (mode == 1) {
            for (int i = 0; i < wordPredictions.size(); i++) {
                textPredictionOptions[i] = wordPredictions.get(i);
            }
            keyboardData.setOptions(textPredictionOptions);
        }

        textBox = new StringBuilder(); // creating the text box for final output
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

    public void manageUserInput(int selection) {
        final int[] keyboardInputMat = {-1, 1, 2, 0, -1, 3, 4, 5}; // convert gaze type into keyboard input
        int input = keyboardInputMat[selection];
        if (input != -1) {
            if (input == 4) { // left up = delete operation
                if (mode == 0 && currentBlurryInput.length() != 0) {
                    currentBlurryInput = currentBlurryInput.substring(0, currentBlurryInput.length() - 1); // cut by one blurry input
                } else if (mode == 1 && inputtedWords.size() > 0) {
                    inputtedWords.remove(inputtedWords.size() - 1); // remove last word
                }
            } else if (input == 5) { // switching modes
                if (mode == 0) {
                    blurryWordPrediction(currentBlurryInput, wordPredictionNum); //  re process word
                    mode = 1;
                }
                else if (mode == 1) {mode = 0;}
            } else {
                if (mode == 0) { // add the input to the string
                    currentBlurryInput += input;
                } else if (mode == 1 && wordPredictions.size() > input) { // add the word and clear blurry input
                    inputtedWords.add(wordPredictions.get(input));
                    currentBlurryInput = "";
                    mode = 0;
                }

            }
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
}
