package com.demo.opencv;

import android.app.Activity;
import android.os.Bundle;

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
 * Use the {@link quick_mode_display#newInstance} factory method to
 * create an instance of this fragment.
 */
public class quick_mode_display extends Fragment {


    /*
    Problem 1: Left and Right eye detection not consistent
    Problem 2: How to adjust to different lighting settings? (maybe without callibration?)
    Problem 3: Is there an efficient way to compare similarity between two images (currently use MSE)
     */
    private UIViewModel viewModel;
    Activity mActivity;
    Handler handler;
    Runnable updateUI;
    TextView textView;
    public quick_mode_display() {
        // Required empty public constructor
    }

    public static quick_mode_display newInstance() {
        quick_mode_display fragment = new quick_mode_display();
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
    public void onViewCreated(View view, Bundle savedInstanceState) {
        textView = mActivity.findViewById(R.id.gazeInputLog);

        handler = new Handler(Looper.getMainLooper());
        updateUI = new Runnable(){
            @Override
            public void run() {

                viewModel.getSelectedItem().observe(requireActivity(), item -> {
                    if (item.prevInputs != null) {
                        String listString = String.join(", ", item.prevInputs);
                        Log.d("quickModeDisplay", "prevInputs");
                        textView.setText(listString);
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
        return inflater.inflate(R.layout.quick_mode_fragment, container, false);
    }
}