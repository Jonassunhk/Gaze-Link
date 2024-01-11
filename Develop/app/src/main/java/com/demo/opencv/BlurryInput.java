package com.demo.opencv;

import android.content.Context;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class BlurryInput {
    private final List<String> wordList = new ArrayList<>(); // top 5000 word frequency list
    private final List<String> blurryInputProcessed = new ArrayList<>(); // processed string with 1, 2, 3, and 4 for blurry input
    List<String> extendedWordList = new ArrayList<>(); // extra words provided by OpenAI for context-based prediction

    public void initialize(Context mContext) {
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
    }

    private String processWord(String word) { // converts original word to processed word for blurry input
        StringBuilder processed = new StringBuilder();
        word = word.toLowerCase();
        for (int i = 0; i < word.length(); i++) {
            if (word.charAt(i) <= 'f') { processed.append("0"); } // group 1
            else if (word.charAt(i) <= 'm') { processed.append("1"); } // group 2
            else if (word.charAt(i) <= 't') { processed.append("2"); } // group 3
            else if (word.charAt(i) <= 'z') { processed.append("3"); } // group 4
        }
        return processed.toString();
    }

    public List<String> getMatchingWords(String codeA) { // predict the word based on a blurry input
        List<String> wordPredictions = new ArrayList<>();
        if (codeA.length() == 0) { // the string is empty, nothing to compare
            return null;
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
        return wordPredictions;
    }

    public String[] getWordPage(List<String> wordPredictions, int page, int wordsPerPage) { // gets the word prediction of a page with a certain number of words
        // page begins from 1
        if (wordPredictions == null) return null;
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
}
