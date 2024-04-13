package com.demo.opencv.textEntry;
import android.content.Context;
import android.text.TextUtils;

import com.demo.opencv.R;
import com.demo.opencv.other.OpenAIManager;

import java.util.Objects;

public class QuickChatManager {

    OpenAIManager openAIManager = new OpenAIManager();
    public QuickChatData quickChatData;
    Context mContext;
    int mode = 0; // 0 = home page, 1-5 = the 5 different categories
    int MAX_MODES = 5;
    String[] modeStrings = {
            String.valueOf(R.string.QuickChat_BackMenuButton),
            String.valueOf(R.string.QuickChat_Category1),
            String.valueOf(R.string.QuickChat_Category2),
            String.valueOf(R.string.QuickChat_Category3),
            String.valueOf(R.string.QuickChat_Category4),
            String.valueOf(R.string.QuickChat_Category5)
    }; // the title of each category
    public void initialize(Context mContext) {
        this.mContext = mContext;
        openAIManager.initialize(mContext);
        quickChatData = new QuickChatData();
    }

    public void manageUserInput(int input, boolean isGazeType) {
        // left gaze = 3, right gaze = 5, up gaze = 0, down gaze = -1, closed = 4, left-up gaze = 1, right-up gaze = 2
        int selection = input;
        if (isGazeType) { // if is gaze type, then process it (is false for touchscreen)
            final int[] keyboardInputMat = {-1, 3, 5, 0, -1, 4, 1, 2}; // convert gaze type into keyboard input
            selection = keyboardInputMat[input];
        }

        // up gaze = 0, left gaze = 1, right gaze = 2, closed = 3, left-up gaze = 4, right-up gaze = 5
        if (selection != -1) {
            if (selection == 0) { // back button
                if (mode == 0) {
                    // back to home page
                } else {
                    mode = 0; // back to main page from the categories
                }
            } else { // others
                if (mode == 0) {
                    mode = selection; // go to the category
                } else {
                    speakText(selection); // speak the text
                }
            }
        }
        updateDisplays();
    }

    public void updateDisplays() {
        if (mode == 0) { // main page
            quickChatData.texts = modeStrings;
        } else { // category page
            String[] newText = new String[6];
            newText[0] = String.valueOf(R.string.QuickChat_BackButton);
            if (MAX_MODES >= 0) System.arraycopy(quickChatStrings[mode - 1], 0, newText, 1, MAX_MODES);
            quickChatData.texts = newText;
        }
    }

    private void speakText(int number) {
        String data = quickChatStrings[mode - 1][number - 1];
        if (TextUtils.isDigitsOnly(data)) { // an id
            data = mContext.getResources().getString(Integer.parseInt(data));
        }
        openAIManager.speechGenerationService(data);
    }

    String[][] quickChatStrings = { // the content stored in the categories (two-dimensional: 5x5)
            {
                    String.valueOf(R.string.QuickChat_Category1_1),
                    String.valueOf(R.string.QuickChat_Category1_2),
                    String.valueOf(R.string.QuickChat_Category1_3),
                    String.valueOf(R.string.QuickChat_Category1_4),
                    String.valueOf(R.string.QuickChat_Category1_5)
            },
            {
                    String.valueOf(R.string.QuickChat_Category2_1),
                    String.valueOf(R.string.QuickChat_Category2_2),
                    String.valueOf(R.string.QuickChat_Category2_3),
                    String.valueOf(R.string.QuickChat_Category2_4),
                    String.valueOf(R.string.QuickChat_Category2_5)
            },
            {
                    String.valueOf(R.string.QuickChat_Category3_1),
                    String.valueOf(R.string.QuickChat_Category3_2),
                    String.valueOf(R.string.QuickChat_Category3_3),
                    String.valueOf(R.string.QuickChat_Category3_4),
                    String.valueOf(R.string.QuickChat_Category3_5)
            },
            {
                    String.valueOf(R.string.QuickChat_Category4_1),
                    String.valueOf(R.string.QuickChat_Category4_2),
                    String.valueOf(R.string.QuickChat_Category4_3),
                    String.valueOf(R.string.QuickChat_Category4_4),
                    String.valueOf(R.string.QuickChat_Category4_5)
            },
            {
                    String.valueOf(R.string.QuickChat_Category5_1),
                    String.valueOf(R.string.QuickChat_Category5_2),
                    String.valueOf(R.string.QuickChat_Category5_3),
                    String.valueOf(R.string.QuickChat_Category5_4),
                    String.valueOf(R.string.QuickChat_Category5_5)
            }
    };
}
