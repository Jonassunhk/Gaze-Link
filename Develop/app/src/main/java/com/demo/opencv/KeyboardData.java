package com.demo.opencv;

public class KeyboardData {
    String[] Options;
    String TextInput;

    void setOptions(String[] options) {
        this.Options = options;
    }
    void setOptionIndex(int index, String value) {
        if (Options != null && index < Options.length) {
            this.Options[index] = value;
        }
    }

    void setTextInput(String textInput) {
        this.TextInput = textInput;
    }
}
