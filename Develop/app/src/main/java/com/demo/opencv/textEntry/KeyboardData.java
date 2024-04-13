package com.demo.opencv.textEntry;

public class KeyboardData {
    public boolean finished; // checks if the user finished the text entry
    public String[] Options;
    public String TextInput;
    public String context;
    public String sentence;
    void setOptions(String[] options) {
        this.Options = options;
    }
    void setTextInput(String textInput) {
        this.TextInput = textInput;
    }

}
