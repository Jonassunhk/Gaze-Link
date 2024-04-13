package com.demo.opencv.textEntry;

public class KeyboardManager {

    String textInput = ""; // stores the final text input
    String[] layer1Options = {"a b c d e f", "g h i j k l", "m,n,o,p,q,r", "s,t,u,v,w,x", "y z , . ! ?", "space delete clear"};
    String[] layer2Options = {"abcdef", "ghijkl", "mnopqr", "stuvwx", "yz,.!?", " *#"};
    int layer = 1;
    int page = 0;

    public KeyboardData getDisplays() {

        KeyboardData keyboardData = new KeyboardData();
        String[] options = new String[6];

        if (layer == 1) {
            options = layer1Options;
        } else {
            String selected = layer2Options[page];
            for (int i = 0; i < selected.length(); i++) {
                if (selected.charAt(i) == ' ') { // space operation
                    options[i] = "Add space";
                } else if (selected.charAt(i) == '*') { // delete operation
                    options[i] = "Delete last letter";
                } else if (selected.charAt(i) == '#') { // clear option
                    options[i] = "Clear text";
                } else {
                    options[i] = "" + selected.charAt(i); // letter
                }
            }
        }

        keyboardData.setOptions(options);
        keyboardData.setTextInput(textInput); // set current text entry
        return keyboardData;
    }

    public void processInput(int selection) { // 0: up, 1: left, 2: down, 3: right
        // left gaze = 1, right gaze = 2, up gaze = 0, down gaze = -1, closed = 3, left-up gaze = 4, right-up gaze = 5
        final int[] keyboardInputMat = {-1, 1, 2, 0, -1, 3, 5, 4}; // convert gaze type into keyboard input
        int input = keyboardInputMat[selection];
        if (input == -1) return;

        if (layer == 1 && input < layer1Options.length) {
            layer = 2;
            page = input;
        } else if (input < layer2Options[page].length()){
            char operation = layer2Options[page].charAt(input);
            if (operation == '*' && textInput.length() > 0) {
                textInput = textInput.substring(0, textInput.length() - 1); // delete operation
            } else if (operation == '#') {
                textInput = "";
            } else {
                textInput += operation; // add the letter/space
            }
            layer = 1;
        }

    }

    public void initialize() {

    }

}
