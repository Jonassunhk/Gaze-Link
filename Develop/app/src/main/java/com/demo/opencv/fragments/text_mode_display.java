package com.demo.opencv.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.demo.opencv.textEntry.KeyboardData;
import com.demo.opencv.R;
import com.demo.opencv.UIViewModel;
import com.demo.opencv.UserDataManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class text_mode_display extends Fragment {
    /*
    Problem 1: Left and Right eye detection not consistent
    Problem 2: How to adjust to different lighting settings? (maybe without callibration?)
    Problem 3: Is there an efficient way to compare similarity between two images (currently use MSE)
     */
    private UIViewModel viewModel;
    Activity mActivity;
    Handler handler;
    Runnable updateUI;
    TextView textInput, sentenceBox;
    TextView contextBox;
    ImageButton recordButton;
    final int[] keyboardInputMat = {-1, 1, 2, 0, -1, 3, 4, 5}; // convert gaze type into keyboard input
    List<TextView> keyboardOptions = new ArrayList<>();
    List<TextView> finishedScreenTexts = new ArrayList<>();
    List<ImageView> finishedScreenImages = new ArrayList<>();
    private boolean useFirstLayout = false;
    private boolean finishedScreenDisplayed = false;
    public text_mode_display() {
        // Required empty public constructor
    }

    public static text_mode_display newInstance() {
        text_mode_display fragment = new text_mode_display();
        return fragment;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacks(updateUI);
    }

    public interface TextModeListener {
        void onTextModeButtonClicked(int input);
        void recordButtonClicked();
    }
    TextModeListener textModeListener;

    public void switchLayout() {
        // Toggle the boolean flag
        useFirstLayout = !useFirstLayout;

        // Re-inflate the layout
        if (getView() != null) {
            ViewGroup parent = (ViewGroup) getView().getParent();
            parent.removeAllViews();
            View newView = onCreateView(getLayoutInflater(), parent, null);
            parent.addView(newView);
        }
    }

    private void setBackground(int index, int originalID, int newID, long delayMillis) {
        TextView target = keyboardOptions.get(index);
        int left = target.getPaddingLeft();
        int top = target.getPaddingTop();
        int right = target.getPaddingRight();
        int bottom = target.getPaddingBottom();
        target.setBackgroundResource(newID);
        target.setPadding(left, top, right, bottom);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                target.setBackgroundResource(originalID);
                target.setPadding(left, top, right, bottom);
            }
        }, delayMillis); // delayMillis is the time after which the color will change back
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Ensure that the container activity has implemented the callback interface
        try {
            textModeListener = (text_mode_display.TextModeListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement TextModeListener");
        }
    }

    private void toggleFinishedScreen(boolean state) {
        int visibility;
        if (state) {
            visibility = View.VISIBLE;
        } else {
            visibility = View.INVISIBLE;
        }
        for (TextView text: finishedScreenTexts) {
            text.setVisibility(visibility);
        }
        for (ImageView image: finishedScreenImages) {
            image.setVisibility(visibility);
        }
    }

    private void setText(TextView textView, String string) {
        if (string != null && !Objects.equals(string, "") && TextUtils.isDigitsOnly(string)) {
            textView.setText(Integer.parseInt(string)); // set string resource
        } else {
            textView.setText(string); // set the original string
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {

        UserDataManager userDataManager = (UserDataManager) requireActivity().getApplication();

        finishedScreenTexts.add(mActivity.findViewById(R.id.speakButton));
        finishedScreenTexts.add(mActivity.findViewById(R.id.uploadButton));
        finishedScreenTexts.add(mActivity.findViewById(R.id.backButton));
        finishedScreenTexts.add(mActivity.findViewById(R.id.finishBox));
        finishedScreenTexts.add(mActivity.findViewById(R.id.darkenLayer));

        finishedScreenImages.add(mActivity.findViewById(R.id.speakButtonImage));
        finishedScreenImages.add(mActivity.findViewById(R.id.uploadButtonImage));
        finishedScreenImages.add(mActivity.findViewById(R.id.backButtonImage));
        toggleFinishedScreen(false); // closed finished screen

        if (useFirstLayout) {
            textInput = mActivity.findViewById(R.id.textInput);
            contextBox = mActivity.findViewById(R.id.contextBox);
            sentenceBox = mActivity.findViewById(R.id.sentenceBox);
            keyboardOptions.add(mActivity.findViewById(R.id.upGazeText)); // 0: up
            keyboardOptions.add(mActivity.findViewById(R.id.leftGazeText)); // 1: left
            keyboardOptions.add(mActivity.findViewById(R.id.rightGazeText)); // 2: right
            keyboardOptions.add(mActivity.findViewById(R.id.closedGazeText)); // 3: closed gaze
            keyboardOptions.add(mActivity.findViewById(R.id.leftUpGazeText)); // 4: left up
            keyboardOptions.add(mActivity.findViewById(R.id.rightUpGazeText)); // 5: right up
        } else {
            textInput = mActivity.findViewById(R.id.textInput2);
            contextBox = mActivity.findViewById(R.id.contextBox2);
            sentenceBox = mActivity.findViewById(R.id.sentenceBox2);
            keyboardOptions.add(mActivity.findViewById(R.id.upGazeText2)); // 0: up
            keyboardOptions.add(mActivity.findViewById(R.id.leftGazeText2)); // 1: left
            keyboardOptions.add(mActivity.findViewById(R.id.rightGazeText2)); // 2: right
            keyboardOptions.add(mActivity.findViewById(R.id.closedGazeText2)); // 3: closed gaze
            keyboardOptions.add(mActivity.findViewById(R.id.leftUpGazeText2)); // 4: left up
            keyboardOptions.add(mActivity.findViewById(R.id.rightUpGazeText2)); // 5: right up
            keyboardOptions.add(mActivity.findViewById(R.id.leftGazeSubText)); // 6. left sub text
            keyboardOptions.add(mActivity.findViewById(R.id.rightGazeSubText)); // 7. right sub text
            keyboardOptions.add(mActivity.findViewById(R.id.leftUpGazeSubText)); // 8. left up sub text
            keyboardOptions.add(mActivity.findViewById(R.id.rightUpGazeSubText)); // 9. right up sub text
            recordButton = mActivity.findViewById(R.id.recordButton);
            recordButton.setOnClickListener(v -> textModeListener.recordButtonClicked());
        }

        // add click listeners for finished screen
        finishedScreenTexts.get(0).setOnClickListener(v -> textModeListener.onTextModeButtonClicked(1));
        finishedScreenTexts.get(1).setOnClickListener(v -> textModeListener.onTextModeButtonClicked(2));
        finishedScreenTexts.get(2).setOnClickListener(v -> textModeListener.onTextModeButtonClicked(5));

        for (int i = 0; i < 6; i++) { // add click listeners for the eye gesture buttons
            int finalI = i;
            keyboardOptions.get(i).setOnClickListener(v -> textModeListener.onTextModeButtonClicked(finalI));
        }

        String context = userDataManager.getTextEntryContext();
        setText(contextBox, context);
        contextBox.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                userDataManager.setTextEntryContext(v.getText().toString());
            }
            return false;
        });

        handler = new Handler(Looper.getMainLooper());
        updateUI = new Runnable(){
            @Override
            public void run() {
                viewModel.getSelectedItem().observe(requireActivity(), item -> {

                    KeyboardData keyboardData = item.KeyboardData;

                    if (keyboardData != null) {
                        if (finishedScreenDisplayed != keyboardData.finished) { // check if finished screen should be displayed
                            setText(finishedScreenTexts.get(3), keyboardData.sentence);
                            finishedScreenDisplayed = keyboardData.finished;
                            toggleFinishedScreen(keyboardData.finished);
                        }
                        for (int i = 0; i < keyboardOptions.size(); i++) {
                            if (keyboardData.Options.length > i) {
                                setText(keyboardOptions.get(i), keyboardData.Options[i]); // set string resource
                            }
                        }
                        setText(contextBox, keyboardData.context); // set context
                        setText(textInput, keyboardData.TextInput);
                        setText(sentenceBox, keyboardData.sentence);

                        if (!useFirstLayout) {
                            if (item.isRecording) {
                                recordButton.setBackgroundResource(R.drawable.record_button_recording);
                            } else {
                                recordButton.setBackgroundResource(R.drawable.record_button_idle);
                            }
                        }
                    }

                    if (!useFirstLayout && item.DetectionOutput != null) {
                        //int detection = keyboardInputMat[item.DetectionOutput.AnalyzedData.GazeType]; // the determination for that frame
                        int actual = keyboardInputMat[item.DetectionOutput.gestureOutput]; // the actual input by the user

                        if (actual != -1) {
                            if (actual == 0 || actual == 3) {
                                setBackground(actual, R.drawable.gaze_option_bg_2, R.drawable.gaze_option_bg_selected, 300);
                            } else {
                                setBackground(actual, R.drawable.gaze_option_bg_1, R.drawable.gaze_option_bg_selected, 300);
                            }
                        }
                    }
                });
            }
        };
        handler.post(updateUI);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = requireActivity();
        viewModel = new ViewModelProvider(requireActivity()).get(UIViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        int layoutId = useFirstLayout ? R.layout.text_mode_fragment : R.layout.text_mode_fragment2;
        return inflater.inflate(layoutId, container, false);
    }
}