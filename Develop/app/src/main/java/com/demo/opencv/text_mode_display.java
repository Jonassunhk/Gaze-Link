package com.demo.opencv;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

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
    TextView gazeInputLog, textInput, sentenceBox;
    EditText contextBox;
    final int[] keyboardInputMat = {-1, 1, 2, 0, -1, 3, 4, 5}; // convert gaze type into keyboard input

    List<TextView> keyboardOptions = new ArrayList<>();
    private boolean useFirstLayout = false;
    int prevIndex = 0;
    public text_mode_display() {
        // Required empty public constructor
    }

    public static text_mode_display newInstance(String context) {
        text_mode_display fragment = new text_mode_display();
        Bundle args = new Bundle();
        args.putString("Context", context);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacks(updateUI);
    }

    public interface TextModeListener {
        void onSettingValueChanged(String valueName, String value);
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

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {

        if (useFirstLayout) {
            gazeInputLog = mActivity.findViewById(R.id.gazeInputLog);
            textInput = mActivity.findViewById(R.id.textInput);
            contextBox = mActivity.findViewById(R.id.contextBox);
            keyboardOptions.add(mActivity.findViewById(R.id.upGazeText)); // 0: up
            keyboardOptions.add(mActivity.findViewById(R.id.leftGazeText)); // 1: left
            keyboardOptions.add(mActivity.findViewById(R.id.rightGazeText)); // 2: right
            keyboardOptions.add(mActivity.findViewById(R.id.closedGazeText)); // 3: closed gaze
            keyboardOptions.add(mActivity.findViewById(R.id.leftUpGazeText)); // 4: left up
            keyboardOptions.add(mActivity.findViewById(R.id.rightUpGazeText)); // 5: right up
            keyboardOptions.add(mActivity.findViewById(R.id.textModeTitle)); // 6: title
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
        }
        assert getArguments() != null;
        String context = getArguments().getString("Context");
        contextBox.setText(context);
        contextBox.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                textModeListener.onSettingValueChanged("Context", v.getText().toString());
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
                        for (int i = 0; i < keyboardOptions.size(); i++) {
                            if (keyboardData.Options.length > i) {
                                keyboardOptions.get(i).setText(keyboardData.Options[i]); // display letter labels for keyboard
                            }
                        }
                        //contextBox.setText(keyboardData.context);
                        textInput.setText(keyboardData.TextInput);
                        if (!useFirstLayout) {
                            sentenceBox.setText(keyboardData.sentence);
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