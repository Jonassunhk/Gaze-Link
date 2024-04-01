package com.demo.opencv;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
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
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.firebase.firestore.auth.User;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.util.HashMap;
import java.util.Objects;

public class dev_mode_display extends Fragment {
    private UIViewModel viewModel;
    Activity mActivity;
    ImageView[] images = new ImageView[4];
    TextView[] texts = new TextView[7];
    TextView gazeInputLog;
    Handler handler;
    Runnable updateUI;
    SeekBar lightingSeekBar, sensitivitySeekBar, textEntryModeSeekBar;

    public dev_mode_display() {
        // Required empty public constructor
    }

    public static dev_mode_display newInstance() {
        dev_mode_display fragment = new dev_mode_display();
        Bundle args = new Bundle();
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Ensure that the container activity has implemented the callback interface
    }

    private Bitmap matToBitmap(Mat mat) {
        Bitmap bm = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat,bm);
        return bm;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = requireActivity();
        viewModel = new ViewModelProvider(requireActivity()).get(UIViewModel.class);

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacks(updateUI);
    }

    public void updateSettings() {
        UserDataManager userDataManager = (UserDataManager) requireActivity().getApplication();
        int sensitivity = userDataManager.getSensitivity();
        int textEntryMode = userDataManager.getTextEntryMode();
        int irisLighting = userDataManager.getLightingThreshold();

        Log.d("DevModeDisplay", "Settings updated " + sensitivity + ' ' + textEntryMode);

        String text = "Iris Lighting Threshold: " + irisLighting;
        texts[4].setText(text);
        lightingSeekBar.setProgress(irisLighting);

        text = "Gaze Sensitivity: " + sensitivity;
        texts[5].setText(text);
        sensitivitySeekBar.setProgress(sensitivity);

        String label = "";
        if (textEntryMode == 0) {label = "letter-by-letter"; }
        else if (textEntryMode == 1) {label = "ambiguous keyboard only";}
        else if (textEntryMode == 2) {label = "ambiguous keyboard + LLM";}
        text = "Current text-entry mode: " + label;
        texts[6].setText(text);
        textEntryModeSeekBar.setProgress(textEntryMode);
    }

    @Override
    public void onResume() {
        super.onResume();
    }
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {

        // different image templates on the dev mode display
        images[0] = mActivity.findViewById(R.id.imageView1);
        texts[0] = mActivity.findViewById(R.id.textView1);
        texts[1] = mActivity.findViewById(R.id.textView2);
        texts[2] = mActivity.findViewById(R.id.textView3);
        texts[3] = mActivity.findViewById(R.id.textView4);
        texts[4] = mActivity.findViewById(R.id.textView5);
        texts[5] = mActivity.findViewById(R.id.textView6);
        texts[6] = mActivity.findViewById(R.id.textView7);
        gazeInputLog = mActivity.findViewById(R.id.gazeInputLog2);
        lightingSeekBar = mActivity.findViewById(R.id.seekBar);
        sensitivitySeekBar = mActivity.findViewById(R.id.seekBar2);
        textEntryModeSeekBar = mActivity.findViewById(R.id.seekBar3);
        updateSettings();

        SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    if (seekBar == lightingSeekBar) {
                        String text = "Iris Lighting Threshold: " + progress;
                        texts[4].setText(text);
                    } else if (seekBar == sensitivitySeekBar) {
                        String text = "Gaze Sensitivity: " + progress;
                        texts[5].setText(text);
                    } else if (seekBar == textEntryModeSeekBar) {
                        String label = "";
                        if (progress == 0) {label = "letter-by-letter"; }
                        else if (progress == 1) {label = "ambiguous keyboard only";}
                        else if (progress == 2) {label = "ambiguous keyboard + LLM";}
                        String text = "Current text-entry mode: " + label;
                        texts[6].setText(text);
                    }
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();

                UserDataManager userDataManager = (UserDataManager) requireActivity().getApplication();

                if (seekBar == lightingSeekBar) {
                    userDataManager.setLightingThreshold(progress);
                } else if (seekBar == sensitivitySeekBar) {
                    userDataManager.setSensitivity(progress);
                } else if (seekBar == textEntryModeSeekBar) {
                    userDataManager.setTextEntryMode(progress);
                }
            }
        };

        lightingSeekBar.setOnSeekBarChangeListener(listener);
        sensitivitySeekBar.setOnSeekBarChangeListener(listener);
        textEntryModeSeekBar.setOnSeekBarChangeListener(listener);

        handler = new Handler(Looper.getMainLooper());
        updateUI = new Runnable(){
            @Override
            public void run() {
                Log.d("devModeDisplay", "Images Displayed");
                viewModel.getSelectedItem().observe(requireActivity(), item -> {
                    DetectionOutput detection = item.DetectionOutput;
                    if (detection == null) {
                        return;
                    }
                    // set testing mat 1:
                    if (detection.testingMats[0] != null && detection.testingMats[0].width() > 0 && detection.testingMats[0].height() > 0) {
                        images[0].setImageBitmap(matToBitmap(detection.testingMats[0]));
                    }

//                    if (detection.testingMats[2] != null && detection.testingMats[2].width() > 0 && detection.testingMats[2].height() > 0) {
//                        //images[1].setImageBitmap(matToBitmap(detection.testingMats[2]));
//                    }
                    if (detection.prevInputs != null) {
                        String listString = String.join(", ", detection.prevInputs);
                        gazeInputLog.setText("Gaze input log: " + listString);
                    }
                    if (detection.AnalyzedData != null) {
                        String s = "Overall Gaze Type: " + detection.AnalyzedData.getTypeString(detection.AnalyzedData.GazeType);
                        texts[1].setText(s);

                        s = "Overall Loss: " + String.format("%.2f", detection.AnalyzedData.GazeProbability);
                        texts[2].setText(s);

                        if (detection.AnalyzedData.Success) { // check if the final output is successful
                            String text = "GAZE DETECTED";
                            texts[0].setText(text);
                        } else {
                            texts[0].setText("---------------");
                        }
                    }
                });
            }
        };
        handler.post(updateUI);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        Log.d("devModeDisplay", "View Created");
        return inflater.inflate(R.layout.dev_mode_fragment, container, false);
    }
}