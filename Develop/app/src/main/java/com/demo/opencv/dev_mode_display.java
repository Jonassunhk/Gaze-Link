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
    TextView[] texts = new TextView[4];
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
        images[2] = mActivity.findViewById(R.id.imageView3);
        texts[0] = mActivity.findViewById(R.id.textView1);
        texts[1] = mActivity.findViewById(R.id.textView2);
        texts[2] = mActivity.findViewById(R.id.textView3);
        texts[3] = mActivity.findViewById(R.id.textView4);

        handler = new Handler(Looper.getMainLooper());
        updateUI = new Runnable(){
            @Override
            public void run() {
                Log.d("devModeDisplay", "Images Displayed");
                viewModel.getSelectedItem().observe(requireActivity(), item -> {
                    for (int i = 0; i < 3; i++) {
                        if (item.testingMats[i] != null) {
                            images[i].setImageBitmap(matToBitmap(item.testingMats[i]));
                        }
                    }
                    texts[0].setText(item.gazeType);
                    texts[1].setText(String.format("%.2f", item.gazeProbability));
                    if (item.Success) {
                        texts[2].setText("INPUT DETECTED");
                    } else {
                        texts[2].setText("---------------");
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