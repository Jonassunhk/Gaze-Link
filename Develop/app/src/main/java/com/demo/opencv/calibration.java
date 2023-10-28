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
    ImageView calibrationLeft, calibrationRight;
    private OnButtonClickListener buttonClickListener;
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
        } catch (ClassCastException e) {
            throw new ClassCastException(context + "must implement listener");
        }
    }
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {

        instruction = mActivity.findViewById(R.id.calibrationInstruction);
        calibrationLeft = mActivity.findViewById(R.id.calibrationLeft);
        calibrationRight = mActivity.findViewById(R.id.calibrationRight);
        Button calibrationButton = mActivity.findViewById(R.id.calibrateButton);

        calibrationButton.setOnClickListener(v -> onCalibrationButtonClick());

        handler = new Handler(Looper.getMainLooper());
        updateUI = new Runnable(){
            @Override
            public void run() {
                viewModel.getSelectedItem().observe(requireActivity(), item -> {
                    DetectionOutput detection = item.DetectionOutput;
                    if (detection.testingMats[0] != null) {
                        calibrationLeft.setImageBitmap(matToBitmap(detection.testingMats[0]));
                    }
                    if (detection.testingMats[1] != null) {
                        calibrationRight.setImageBitmap(matToBitmap(detection.testingMats[1]));
                    }
                    if (item.calibrationInstruction != null) {
                        instruction.setText(item.calibrationInstruction);
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