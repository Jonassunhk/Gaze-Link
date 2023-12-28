package com.demo.opencv;

import java.util.Stack;

public class KeyboardManager {

    static class PageInfo {
        char startLetter; // letter to start
        int[] letters; // letters displayed for each option
        int[] nodes;
        int prevPage; // how many letters

        void set(char startLetter, int[] letters, int prevPage, int[] nodes) {
            this.startLetter = startLetter;
            this.letters = letters;
            this.prevPage = prevPage;
            this.nodes = nodes;
        }
    }
    String textInput = ""; // stores the final text input
    String alphabet = "abcdefghijklmnopqrstuvwxyz";
    Stack<Integer> pagePath = new Stack<>();
    PageInfo[] pageStructure = new PageInfo[36];

    KeyboardData getDisplays() {

        KeyboardData keyboardData = new KeyboardData();
        String[] options = new String[4];

        PageInfo pageInfo = pageStructure[pagePath.peek()];
        for (int i = 0; i < 4; i++) {
            options[i] = "Back";
        }
        int pos = pageInfo.startLetter - 'a'; // get the starting index
        for (int i = 0; i < pageInfo.letters.length; i++) {
            options[i] = alphabet.substring(pos, pos + pageInfo.letters[i]);
            pos += pageInfo.letters[i];
        }

        keyboardData.setOptions(options);
        keyboardData.setTextInput(textInput); // set current text entry
        return keyboardData;
    }

    void processInput(int selection) { // 0: up, 1: left, 2: down, 3: right
        // left gaze = 1, right gaze = 2, up gaze = 0, down gaze = -1, closed = 3, left-up gaze = 4, right-up gaze = 5
        final int[] keyboardInputMat = {-1, 1, 2, 0, -1, 3, 4, 5}; // convert gaze type into keyboard input
        int input = keyboardInputMat[selection];
        if (input == -1) return;
        switch (input) {
            case 4 -> { // delete previous input
                if (textInput.length() > 0) {
                    textInput = textInput.substring(0, textInput.length() - 1); //
                }
            }
            case 5 -> textInput += ' '; // add space
            default -> {
                PageInfo currentPage = pageStructure[pagePath.peek()];
                if (input >= currentPage.letters.length) { // choice invalid, meaning its going back to prev page
                    pagePath.pop(); // back to previous page

                } else { // there is a choice, going to next page
                    if (currentPage.letters[input] == 1) { // only one letter left, final page
                        char finalLetter = (char) (currentPage.startLetter + input);
                        textInput += finalLetter;
                        pagePath.empty();
                        pagePath.push(0); // reset the stack to store current page
                    } else {
                        pagePath.push(currentPage.nodes[input]);
                    }
                }
            }
        }
    }

    void initialize() {
        for (int i = 0; i < 36; i++) {
            pageStructure[i] = new PageInfo();
        }
        // first layer
        pageStructure[0].set('a', new int[]{6,7,7,6}, 0, new int[]{1,2,3,4});

        // second layer
        pageStructure[1].set('a', new int[]{2,2,2}, 0, new int[]{5,6,7});
        pageStructure[2].set('g', new int[]{2,3,2}, 0, new int[]{8,9,10});
        pageStructure[3].set('n', new int[]{2,3,2}, 0, new int[]{11,12,13});
        pageStructure[4].set('u', new int[]{2,2,2}, 0, new int[]{14,15,16});

        // third layer
        pageStructure[5].set('a', new int[]{1,1}, 1, new int[]{});
        pageStructure[6].set('c', new int[]{1,1}, 1, new int[]{});
        pageStructure[7].set('e', new int[]{1,1}, 1, new int[]{});
        pageStructure[8].set('g', new int[]{1,1}, 2, new int[]{});
        pageStructure[9].set('i', new int[]{1,1,1}, 2, new int[]{});
        pageStructure[10].set('l', new int[]{1,1}, 2, new int[]{});
        pageStructure[11].set('n', new int[]{1,1}, 3, new int[]{});
        pageStructure[12].set('p', new int[]{1,1,1}, 3, new int[]{});
        pageStructure[13].set('s', new int[]{1,1}, 3, new int[]{});
        pageStructure[14].set('u', new int[]{1,1}, 4, new int[]{});
        pageStructure[15].set('w', new int[]{1,1}, 4, new int[]{});
        pageStructure[16].set('y', new int[]{1,1}, 4, new int[]{});

        pagePath.push(0); // main page
    }

}
