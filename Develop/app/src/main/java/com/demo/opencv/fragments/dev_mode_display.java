package com.demo.opencv.fragments;

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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.demo.opencv.vision.DetectionOutput;
import com.demo.opencv.R;
import com.demo.opencv.UIViewModel;
import com.demo.opencv.UserDataManager;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.util.Objects;

public class dev_mode_display extends Fragment {
    private UIViewModel viewModel;
    Activity mActivity;
    ImageView[] images = new ImageView[4];
    TextView[] texts = new TextView[7];
    TextView gazeInputLog;
    Handler handler;
    Runnable updateUI;
    SeekBar sensitivitySeekBar;
    Spinner textEntrySpinner, languageSpinner;
    UserDataManager userDataManager;

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
        int sensitivity = userDataManager.getSensitivity();
        int textEntryMode = userDataManager.getTextEntryMode();
        String language = userDataManager.getLanguage();

        Log.d("DevModeDisplay", "Settings updated " + sensitivity);
        sensitivitySeekBar.setProgress(sensitivity);
        texts[4].setText(String.valueOf(sensitivity));
        textEntrySpinner.setSelection(textEntryMode);

        if (Objects.equals(language, "English")) {
            languageSpinner.setSelection(0);
        } else if (Objects.equals(language, "Spanish")) {
            languageSpinner.setSelection(1);
        } else if (Objects.equals(language, "Chinese")) {
            languageSpinner.setSelection(2);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {

        // different image templates on the dev mode display
        userDataManager = (UserDataManager) requireActivity().getApplication();

        images[0] = mActivity.findViewById(R.id.imageView1);
        texts[0] = mActivity.findViewById(R.id.textView1);
        texts[1] = mActivity.findViewById(R.id.textView2);
        texts[2] = mActivity.findViewById(R.id.textView3);
        texts[3] = mActivity.findViewById(R.id.textView4);
        texts[4] = mActivity.findViewById(R.id.textView5);
        gazeInputLog = mActivity.findViewById(R.id.gazeInputLog2);
        sensitivitySeekBar = mActivity.findViewById(R.id.seekBar);
        textEntrySpinner = mActivity.findViewById(R.id.textEntrySpinner);
        languageSpinner = mActivity.findViewById(R.id.languageSpinner);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(mActivity, R.array.language_options, R.layout.custom_spinner_item);
        adapter.setDropDownViewResource(R.layout.custom_spinner_item);
        languageSpinner.setAdapter(adapter);

        ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(mActivity, R.array.textEntry_options, R.layout.custom_spinner_item);
        adapter.setDropDownViewResource(R.layout.custom_spinner_item);
        textEntrySpinner.setAdapter(adapter2);

        updateSettings();

        // set seek bars
        SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                   if (seekBar == sensitivitySeekBar) {
                        String text = String.valueOf(progress);
                        texts[4].setText(text);
                    }
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                if (seekBar == sensitivitySeekBar) {
                    userDataManager.setSensitivity(progress);
                }
            }
        };
        sensitivitySeekBar.setOnSeekBarChangeListener(listener);


        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String language = "";
                switch (position) {
                    case 0 -> language = "English";
                    case 1 -> language = "Spanish";
                    case 2 -> language = "Chinese";
                }
                Log.d("SpinnerTesting", "language changed to " + language);
                userDataManager.setLanguage(language);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Another interface callback
            }
        });

        textEntrySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d("SpinnerTesting", "text entry mode changed to " + position);
                userDataManager.setTextEntryMode(position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Another interface callback
            }
        });

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

                    if (detection.prevInputs != null) {
                        String listString = String.join(", ", detection.prevInputs);
                        String text = "Gaze input log: " + listString;
                        gazeInputLog.setText(text);
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