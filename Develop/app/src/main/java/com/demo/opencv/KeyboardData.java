package com.demo.opencv;

public class KeyboardData {
    boolean finished; // checks if the user finished the text entry
    String[] Options;
    String TextInput;
    String context;
    String sentence;
    void setOptions(String[] options) {
        this.Options = options;
    }
    void setTextInput(String textInput) {
        this.TextInput = textInput;
    }

}
