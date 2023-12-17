package com.demo.opencv;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link text_mode_display#newInstance} factory method to
 * create an instance of this fragment.
 */
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
    TextView gazeInputLog;
    TextView textInput;
    TextView[] keyboardOptions = new TextView[7];

    public text_mode_display() {
        // Required empty public constructor
    }

    public static text_mode_display newInstance() {
        text_mode_display fragment = new text_mode_display();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacks(updateUI);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {

        gazeInputLog = mActivity.findViewById(R.id.gazeInputLog);
        textInput = mActivity.findViewById(R.id.textInput);
        keyboardOptions[0] = mActivity.findViewById(R.id.keyboardUpText); // 0: up
        keyboardOptions[1] = mActivity.findViewById(R.id.keyboardLeftText); // 1: left
        keyboardOptions[2] = mActivity.findViewById(R.id.keyboardRightText); // 2: right
        keyboardOptions[3] = mActivity.findViewById(R.id.keyboardDownText); // 3: down
        keyboardOptions[4] = mActivity.findViewById(R.id.switchButton); // 4: switch mode button
        keyboardOptions[5] = mActivity.findViewById(R.id.deleteButton); // 5: delete button
        keyboardOptions[6] = mActivity.findViewById(R.id.keyboardClosedText); // 6: close

        handler = new Handler(Looper.getMainLooper());
        updateUI = new Runnable(){
            @Override
            public void run() {
                viewModel.getSelectedItem().observe(requireActivity(), item -> {
                    DetectionOutput detection = item.DetectionOutput;
                    if (detection.prevInputs != null) {
                        String listString = String.join(", ", detection.prevInputs);
                        Log.d("quickModeDisplay", listString + " " + detection.prevInputs.size());
                        gazeInputLog.setText(listString);
                    }

                    KeyboardData keyboardData = item.KeyboardData;
                    if (keyboardData != null) {
                        for (int i = 0; i < 7; i++) {
                            if (keyboardData.Options.length > i) {
                                keyboardOptions[i].setText(keyboardData.Options[i]); // display letter labels for keyboard
                            }
                        }
                        textInput.setText("Current Text: " + keyboardData.TextInput);
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
        return inflater.inflate(R.layout.text_mode_fragment, container, false);
    }
}