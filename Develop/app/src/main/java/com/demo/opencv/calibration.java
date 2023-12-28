package com.demo.opencv;


import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.Image;
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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

public class calibration extends Fragment {

    private UIViewModel viewModel;
    Activity mActivity;
    TextView instruction;
    Handler handler;
    Runnable updateUI;
    ImageView[] leftTemplateViews;
    private OnButtonClickListener buttonClickListener;
    boolean leftEyesDisplayed = true;
    public calibration() {
        // Required empty public constructor
    }

    private Bitmap matToBitmap(Mat mat) {
        Bitmap bm = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat,bm);
        return bm;
    }


    public interface OnButtonClickListener {
        void onCalibrationButtonClick();
    }

    public void onCalibrationButtonClick() {
        if (buttonClickListener != null) {
            Log.d("Calibration", "button clicked");
            buttonClickListener.onCalibrationButtonClick();
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            buttonClickListener = (OnButtonClickListener) context;
            Log.d("Calibration", "Button listener implemented");
        } catch (ClassCastException e) {
            throw new ClassCastException(context + "must implement listener");
        }
    }
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {

        instruction = mActivity.findViewById(R.id.calibrationInstruction);
        leftTemplateViews = new ImageView[7];
        leftTemplateViews[0] = mActivity.findViewById(R.id.calibrationLeft);
        leftTemplateViews[1] = mActivity.findViewById(R.id.calibrationRight);
        leftTemplateViews[2] = mActivity.findViewById(R.id.calibrationStraight);
        leftTemplateViews[3] = mActivity.findViewById(R.id.calibrationUp);
        leftTemplateViews[4] = mActivity.findViewById(R.id.calibrationDown);
        leftTemplateViews[5] = mActivity.findViewById(R.id.calibrationLeftUp);
        leftTemplateViews[6] = mActivity.findViewById(R.id.calibrationRightUp);

        Button calibrationButton = mActivity.findViewById(R.id.calibrationButton);
        Button switchEyesButton = mActivity.findViewById(R.id.switchEyesButton);
        Log.d("Calibration", "here");
        calibrationButton.setOnClickListener(v -> onCalibrationButtonClick());
        switchEyesButton.setOnClickListener(v -> leftEyesDisplayed = !leftEyesDisplayed);

        handler = new Handler(Looper.getMainLooper());
        updateUI = new Runnable(){
            @Override
            public void run() {
                viewModel.getSelectedItem().observe(requireActivity(), item -> {
                    DetectionOutput detection = item.DetectionOutput;
                    Bitmap[] images;
                    if (leftEyesDisplayed) {
                        images = item.leftTemplates;
                    } else {
                        images = item.rightTemplates;
                    }

                    if (images != null) {
                        for (int i = 0; i < 7; i++) {
                            if (images[i] != null) {
                                leftTemplateViews[i].setImageBitmap(images[i]);
                            } else if (detection.testingMats[0] != null) {
                                leftTemplateViews[i].setImageBitmap(matToBitmap(detection.testingMats[0]));
                            }
                        }
                        if (item.calibrationInstruction != null) {
                            instruction.setText(item.calibrationInstruction);
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
        return inflater.inflate(R.layout.calibration_fragment, container, false);
    }
}