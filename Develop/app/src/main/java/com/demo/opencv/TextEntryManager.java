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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class TextEntryManager extends AppCompatActivity {
    public boolean LLMEnabled = false;
    private final List<String> wordList = new ArrayList<>(); // top 5000 word frequency list
    private final List<String> blurryInputProcessed = new ArrayList<>(); // processed string with 1, 2, 3, and 4 for blurry input
    private String currentBlurryInput = ""; // in blurry input format
    StringBuilder textBox = new StringBuilder(); // the actual text that user inputted
    List<String> inputtedWords = new ArrayList<>(); // the previous words that user inputted
    List<String> inputtedSentences = new ArrayList<>(); // the previous sentences that user inputted
    List<String> wordPredictions = new ArrayList<>(); // the word predictions shown in word mode
    List<String> extendedWordList = new ArrayList<>(); // extra words provided by OpenAI for context-based prediction
    int wordsPerPage = 3;
    int NWPWords = 6;
    int currentPage = 1;
    int mode = 0; // 0: main menu, 1: blurry input, 2: word prediction, 3: sentence prediction

    // in the order of up, left, right, down, right up, left up
    String[] blurryInputOptions = {" A, B, C, D, E, F", "G, H, I, J, K, L, M", "N, O, P, Q, R, S, T", "U, V, W, X, Y, Z", "switch to word mode", "Delete last letter", "LETTER MODE"};
    String[] wordModeOptions = {"", "", "", "next page", "back to main menu", "Delete previous word", "WORD MODE"};
    String[] mainMenuOptions = {"switch to letter mode", "switch to word mode", "switch to sentence mode", "", "Speak current text", "Clear current text", "MAIN MENU"};
    String[] sentenceModeOptions = {"", "", "", "", "Back to main menu", "Delete sentence", "SENTENCE MODE"};
    String[] blurryInputText = {"A-F", "G-M", "N-T", "U-Z"};
    String context = ""; // the context content for context-based predictions
    KeyboardData keyboardData = new KeyboardData(); // data structure for UI display
    Context mContext;
    openAIManager openAIManager = new openAIManager(); // openAI services
    AudioManager audioManager = new AudioManager(); // text-to-speech
    String[] sentencePredictions = new String[4]; // sentence predictions

    public void updateDisplays() {
        switch (mode) {
            case 0 -> keyboardData.setOptions(mainMenuOptions);
            case 1 -> keyboardData.setOptions(blurryInputOptions);
            case 2 -> {
                String[] displayedWords = getWordPage(currentPage, wordsPerPage); // get the current word page
                if (displayedWords != null) {
                    System.arraycopy(displayedWords, 0, wordModeOptions, 0, wordsPerPage);
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
        keyboardData.context = context;
    }

    public KeyboardData getDisplays() {
        return this.keyboardData;
    }

    private String combineWords(List<String> words) {
        StringBuilder text = new StringBuilder(); // string to combine all words
        for (int i = 0; i < words.size(); i++) { // adding the words
            text.append(words.get(i));
            text.append(' ');
        }
        return text.toString();
    }

    public void sentencePrediction() { // predicting the sentences based on keywords
        if (!LLMEnabled) return;
        String finalString = combineWords(inputtedWords);
        openAIManager.generateText("SP",
                "You are helping a motor neuron disease patient who wishes to communicate with keywords." +
                        "Generate a coherent and simple sentence with the keywords:" + finalString +
                        "For example, 'want, food' becomes 'I want to eat food.' 'give, book' becomes 'give me the book.'");
    }

    public void nextWordPrediction(int num) { // predicting the next word based on current keywords
        if (!LLMEnabled) return;
        if (Objects.equals(currentBlurryInput, "") && inputtedWords.size() > 0) { // no blurry input, at least one word
            String finalString = combineWords(inputtedWords);
            openAIManager.generateText("NWP",
                "Give the keywords: " + finalString + ". Predict " + num + " most frequent words" +
                        "that follows the keywords in a sentence and relate to the context: " + context +
                        "For example, for the keywords 'want, sleep', the output can be 'now, soon, later, peacefully.'" +
                        "For the keyword 'thank', the output can be 'you, him, the'" +
                        "Only output in the format:" +
                        "word, word, word, word, word, word. Don't output anything else!"
            );
        }
    }

    public void contextWordPrediction(int number) {
        if (!LLMEnabled) return;
        if (!Objects.equals(context, "")) { // if there is context
            openAIManager.generateText("CWP",
                    "You are predicting words for a motor neuron disease patient. " +
                            "Give me " + number + " most frequent words that might be part of a response to the phrase:" + context +
                            "during a conversation. ONLY output in the format: word, word, word."
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
                    sentencePredictions[0] = content;

                } else if (Objects.equals(tag, "NWP")) { // next word prediction: NWP
                    wordPredictions.clear();
                    String[] wordChoices = content.split(", "); // there should be n words, separated by comma
                    for (int i = 0; i < wordChoices.length; i++) {
                        wordPredictions.add(i, wordChoices[i]);
                    }

                } else if (Objects.equals(tag, "CWP")) { // context word prediction
                    String[] wordChoices = content.split(", ");
                    extendedWordList.clear();
                    extendedWordList.addAll(Arrays.asList(wordChoices));
                }
                updateDisplays();
            } else {
                Log.d("TextGeneration", "Fail to get data in text entry manager");
            }

        }
    };

    private void mode0Input(int selection) { // input for main menu mode
        switch (selection) {
            case 0 -> mode = 1; // switch to blurry input mode
            case 1 -> { // switch to word mode
                mode = 2;
                currentPage = 1;
                blurryWordPrediction(currentBlurryInput);
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
            case 4 -> inputtedSentences.clear(); // clear text
            case 5 -> { // speak text
                StringBuilder speakText = new StringBuilder();
                for (int i = 0; i < inputtedSentences.size(); i++) {
                    speakText.append(inputtedSentences.get(i));
                    speakText.append(' ');
                }
                speakText.append(combineWords(inputtedWords)); // add the words as well
                audioManager.speakText(speakText.toString());

            }
        }
    }

    private void mode1Input(int selection) { // input for blurry input
        switch (selection) {
            case 5 -> { // switch to word prediction
                mode = 2;
                currentPage = 1;
                blurryWordPrediction(currentBlurryInput);
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
            case 3 -> { // next page
                String[] words = getWordPage(currentPage + 1, wordsPerPage);
                if (words == null) { // page doesn't exist
                    Log.d("TextMode", "Max page reached");
                } else {
                    currentPage += 1; // add the page
                }
            }
            default -> { // add the word and clear blurry input
                int wordIndex = selection + (currentPage - 1) * wordsPerPage;
                if (wordPredictions.size() > wordIndex) { // input is valid
                    inputtedWords.add(wordPredictions.get(wordIndex));
                    currentBlurryInput = "";
                }
                currentPage = 1;
                nextWordPrediction(NWPWords); // predict the next word
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
        audioManager.initialize(mContext);
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

        context = "What food do you like to eat?";
        contextWordPrediction(50);
    }

    public void blurryWordPrediction(String codeA) { // predict the word based on a blurry input
        wordPredictions = new ArrayList<>();
        if (codeA.length() == 0) { // the string is empty, nothing to compare
            return;
        }

        // normal 5000 word bank prediction
        for (int i = 0; i < blurryInputProcessed.size(); i++) {
            String codeB = blurryInputProcessed.get(i);
            boolean match1 = codeA.length() == codeB.length(); // the two words are the same length
            boolean match2 = (codeA.length() < codeB.length()) && codeA.length() > 5; // A is less than B, but A is more than 5 characters long
            if (match1 || match2) {
                String subString = codeB.substring(0, codeA.length());
                //Log.d("TextPrediction", "Code: " + codeA + ", substring from word: " + subString);
                if (subString.equals(codeA)) {
                    wordPredictions.add(wordList.get(i));
                }
            }
        }

        // extended word bank prediction (smaller, maybe around 500 words max)
        for (int i = 0; i < extendedWordList.size(); i++) {
            String codeB = processWord(extendedWordList.get(i));
            boolean match1 = codeA.length() == codeB.length(); // the two words are the same length
            boolean match2 = (codeA.length() < codeB.length()) && codeA.length() > 5; // A is less than B, but A is more than 5 characters long
            if (match1 || match2) {
                String subString = codeB.substring(0, codeA.length());
                //Log.d("TextPrediction", "Code: " + codeA + ", substring from word: " + subString);
                if (subString.equals(codeA)) {
                    wordPredictions.add(extendedWordList.get(i));
                }
            }
        }
    }

    public String[] getWordPage(int page, int wordsPerPage) { // gets the word prediction of a page with a certain number of words
        // page begins from 1
        int wordsPredicted = wordPredictions.size(); //
        if ((page - 1) * wordsPerPage >= wordsPredicted) { // if the page doesn't exist (page 3, 3 words per page, 7 words detected)
            return null;
        } else {
            String[] wordArray = new String[wordsPerPage];
            for (int i = 1; i <= wordsPerPage; i++) {
                if ((page - 1) * wordsPerPage + i <= wordsPredicted) {
                    wordArray[i - 1] = wordPredictions.get((page - 1) * wordsPerPage + i - 1);
                }
            }
            return wordArray;
        }
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mMessageReceiver);
        super.onDestroy();
    }
}
