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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.demo.opencv.vision.DetectionOutput;
import com.demo.opencv.R;
import com.demo.opencv.UIViewModel;
import com.demo.opencv.UserDataManager;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

public class calibration_display extends Fragment {
    UserDataManager userDataManager;
    private UIViewModel viewModel;
    Activity mActivity;
    TextView instruction;
    Handler handler;
    Runnable updateUI;
    ImageView[] calibrationTemplateViews;
    Button calibrationButton, switchEyesButton;
    ImageView calibrationImage;
    TextView calibrationDescription;
    int calibrationTemplates = 6;
    int[] calibrationMessageID = {R.string.Calibration_LookStraight, R.string.Calibration_LookLeft, R.string.Calibration_LookRight, R.string.Calibration_LookUp,  R.string.Calibration_LookLeftUp, R.string.Calibration_LookRightUp};

    private OnButtonClickListener buttonClickListener;
    boolean leftEyesDisplayed = true;
    public calibration_display() {
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

    public void onCalibrationButtonClick() { // runs when the "continue" button is pressed
        if (buttonClickListener != null) {
            Log.d("Calibration", "button clicked");
            int calibrationState = userDataManager.getCalibrationState();

            if (calibrationState == calibrationTemplates) { // finished calibration, going to reset
                toggleInfoPage(true);
                toggleCalibrationPage(false);

            } else if (calibrationState == -1) { // other states
                toggleInfoPage(false);
                toggleCalibrationPage(true);
            }
            buttonClickListener.onCalibrationButtonClick();
        }
    }

    private void toggleInfoPage(boolean state) {
        int visibility;
        if (state) {
            visibility = View.VISIBLE; // true = visible
        } else {
            visibility = View.INVISIBLE; // false = invisible
        }
        calibrationDescription.setVisibility(visibility);
        calibrationImage.setVisibility(visibility);
    }

    private void toggleCalibrationPage(boolean state) {
        int visibility;
        if (state) {
            visibility = View.VISIBLE; // true = visible
        } else {
            visibility = View.INVISIBLE; // false = invisible
        }
        for (int i = 0; i < calibrationTemplates; i++) {
            calibrationTemplateViews[i].setVisibility(visibility);
        }
        switchEyesButton.setVisibility(visibility);
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

        userDataManager = (UserDataManager) requireActivity().getApplication();
        calibrationDescription = mActivity.findViewById(R.id.CalibrationDescription);
        calibrationImage = mActivity.findViewById(R.id.CalibrationImage);
        instruction = mActivity.findViewById(R.id.calibrationInstruction);
        calibrationTemplateViews = new ImageView[calibrationTemplates];
        calibrationTemplateViews[0] = mActivity.findViewById(R.id.calibrationStraight);
        calibrationTemplateViews[1] = mActivity.findViewById(R.id.calibrationLeft);
        calibrationTemplateViews[2] = mActivity.findViewById(R.id.calibrationRight);
        calibrationTemplateViews[3] = mActivity.findViewById(R.id.calibrationUp);
        calibrationTemplateViews[4] = mActivity.findViewById(R.id.calibrationLeftUp);
        calibrationTemplateViews[5] = mActivity.findViewById(R.id.calibrationRightUp);

        calibrationButton = mActivity.findViewById(R.id.calibrationButton);
        switchEyesButton = mActivity.findViewById(R.id.switchEyesButton);
        Log.d("Calibration", "here");
        calibrationButton.setOnClickListener(v -> onCalibrationButtonClick());
        switchEyesButton.setOnClickListener(v -> leftEyesDisplayed = !leftEyesDisplayed);

        if (userDataManager.getCalibrationState() == -1) {
            instruction.setText(R.string.Calibration_Title);
            toggleInfoPage(true);
            toggleCalibrationPage(false);
        } else {
            toggleInfoPage(false);
            toggleCalibrationPage(true);
        }


        handler = new Handler(Looper.getMainLooper());
        updateUI = new Runnable(){
            @Override
            public void run() {
                viewModel.getSelectedItem().observe(requireActivity(), item -> {
                    DetectionOutput detection = item.DetectionOutput;
                    int calibrationState = item.calibrationState;
                    Log.d("CalibrationInterface", "Current State: " + calibrationState);
                    if (calibrationState == -1) { // pre-calibration page
                        instruction.setText(R.string.Calibration_Title);
                    } else { // calibration page
                        Bitmap[] images;
                        if (leftEyesDisplayed) {
                            images = item.leftTemplates;
                        } else {
                            images = item.rightTemplates;
                        }

                        if (images != null) {
                            for (int i = 0; i < calibrationTemplates; i++) {
                                if (images[i] == null || i == calibrationState) {  // show the live camera
                                    if (detection.testingMats[0] != null) {
                                        calibrationTemplateViews[i].setImageBitmap(matToBitmap(detection.testingMats[0]));
                                    }
                                } else { // show the calibration frame
                                    calibrationTemplateViews[i].setImageBitmap(images[i]);
                                }
                            }
                            if (calibrationState == 6) {
                                instruction.setText(R.string.Calibration_Finished);
                            } else {
                                instruction.setText(calibrationMessageID[calibrationState]);
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
        return inflater.inflate(R.layout.calibration_fragment, container, false);
    }
}