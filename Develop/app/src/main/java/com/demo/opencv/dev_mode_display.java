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

import org.opencv.android.Utils;
import org.opencv.core.Mat;

public class dev_mode_display extends Fragment {
    private UIViewModel viewModel;
    Activity mActivity;
    ImageView[] images = new ImageView[4];
    TextView[] texts = new TextView[7];
    Handler handler;
    Runnable updateUI;
    public dev_mode_display() {
        // Required empty public constructor
    }

    public interface OnSeekBarChangeListener {
        void onSeekBarValueChanged(String valueName, int value);
    }
    private OnSeekBarChangeListener callback;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Ensure that the container activity has implemented the callback interface
        try {
            callback = (OnSeekBarChangeListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement OnSeekBarChangeListener");
        }
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

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {

        // different image templates on the dev mode display
        images[0] = mActivity.findViewById(R.id.imageView1);
        images[1] = mActivity.findViewById(R.id.imageView2);
        texts[0] = mActivity.findViewById(R.id.textView1);
        texts[1] = mActivity.findViewById(R.id.textView2);
        texts[2] = mActivity.findViewById(R.id.textView3);
        texts[3] = mActivity.findViewById(R.id.textView4);
        texts[4] = mActivity.findViewById(R.id.textView5);
        texts[5] = mActivity.findViewById(R.id.textView6);
        texts[6] = mActivity.findViewById(R.id.textView7);

        SeekBar lightingSeekBar = mActivity.findViewById(R.id.seekBar);
        SeekBar sensitivitySeekBar = mActivity.findViewById(R.id.seekBar2);
        SeekBar textEntryModeSeekBar = mActivity.findViewById(R.id.seekBar3);

        SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    if (seekBar == lightingSeekBar) {
                        String text = "Iris Lighting Threshold: " + progress;
                        texts[4].setText(text);
                        callback.onSeekBarValueChanged("Threshold", progress);
                    } else if (seekBar == sensitivitySeekBar) {
                        String text = "Gaze Sensitivity: " + progress;
                        texts[5].setText(text);
                        callback.onSeekBarValueChanged("Sensitivity", progress);
                    } else if (seekBar == textEntryModeSeekBar) {
                        String label = "";
                        if (progress == 0) {label = "letter-by-letter"; }
                        else if (progress == 1) {label = "ambiguous keyboard only";}
                        else if (progress == 2) {label = "ambiguous keyboard + LLM";}
                        String text = "Current text-entry mode: " + label;
                        texts[6].setText(text);
                        callback.onSeekBarValueChanged("TextEntryMode", progress);
                    }
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
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

                    if (detection.testingMats[2] != null && detection.testingMats[2].width() > 0 && detection.testingMats[2].height() > 0) {
                        images[1].setImageBitmap(matToBitmap(detection.testingMats[2]));
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
        View newView = inflater.inflate(R.layout.dev_mode_fragment, container, false);

        return newView;
    }
}