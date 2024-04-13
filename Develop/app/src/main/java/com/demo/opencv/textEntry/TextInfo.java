package com.demo.opencv.textEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

class TextInfo {
    String[] blurryInputText = {"A-F", "G-M", "N-T", "U-Z"};
    String BlurryInput = ""; // inputted blurry inputs
    public List<String> Words = new ArrayList<>(); // inputted words
    String Sentence = ""; // inputted sentence
    void addBlurryInput(String blurryInput) {
        BlurryInput += blurryInput;
    }
    void deleteBlurryInput() {
        if (!Objects.equals(BlurryInput, "")) {
            BlurryInput = BlurryInput.substring(0, BlurryInput.length() - 1);
        }
    }
    void deleteWord() {
        if (Words.size() > 0) {
            Words.remove(Words.size() - 1);
        }
    }
    void clearBlurryInputs() {
        BlurryInput = "";
    }
    void addWord(String word) {
        Words.add(word);
    }
    void clearWords() {
        Words.clear();
    }
    void clearAll() {
        clearBlurryInputs();
        clearWords();
        Sentence = "";
    }
    String getWords() {
        StringBuilder stringBuilder = new StringBuilder();
        for (String i: Words) { // combining words
            stringBuilder.append(i);
            stringBuilder.append(' ');
        }
        return stringBuilder.toString();
    }
    StringBuilder buildText() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Text: ");
        for (String i: Words) { // adding the words into the text box
            stringBuilder.append(i);
            stringBuilder.append(' ');
        }
        for (int i = 0; i < BlurryInput.length(); i++) { // appending the current blurry input
            int input = (int) BlurryInput.charAt(i) - '0';
            stringBuilder.append(blurryInputText[input]);
            stringBuilder.append(' ');
        }
        return stringBuilder;
    }
}