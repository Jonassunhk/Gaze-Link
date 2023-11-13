package com.demo.opencv;

import static org.opencv.android.Utils.matToBitmap;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link dev_mode_display#newInstance} factory method to
 * create an instance of this fragment.
 */
public class dev_mode_display extends Fragment {
    private UIViewModel viewModel;
    Activity mActivity;
    ImageView[] images = new ImageView[4];
    TextView[] texts = new TextView[8];
    Handler handler;
    Runnable updateUI;
    public dev_mode_display() {
        // Required empty public constructor
    }

    private Bitmap matToBitmap(Mat mat) {
        Bitmap bm = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat,bm);
        return bm;
    }
    public static dev_mode_display newInstance(String param1, String param2) {
        dev_mode_display fragment = new dev_mode_display();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
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
    public void onViewCreated(View view, Bundle savedInstanceState) {

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
        texts[7] = mActivity.findViewById(R.id.textView8);

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
                    for (int i = 0; i < 2; i++) {
                        // check if the testing mats are empty
                        if (detection.testingMats[i] != null && detection.testingMats[i].width() > 0 && detection.testingMats[i].height() > 0) {
                            images[i].setImageBitmap(matToBitmap(detection.testingMats[i]));
                        }
                    }
                    if (detection.AnalyzedData != null) {
                        String s = "Overall Gaze Type: " + detection.AnalyzedData.getTypeString(detection.AnalyzedData.GazeType);
                        texts[1].setText(s);

                        s = "Overall Loss: " + String.format("%.2f", detection.AnalyzedData.GazeProbability);
                        texts[2].setText(s);

                        s = "Left Gaze Type: " + detection.LeftData.getTypeString(detection.LeftData.GazeType);
                        texts[4].setText(s);

                        s = "Right Gaze Type: " + detection.RightData.getTypeString(detection.RightData.GazeType);
                        texts[5].setText(s);

                        if (detection.AnalyzedData.Success) { // check if the final output is successful
                            texts[0].setText("GAZE DETECTED");
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