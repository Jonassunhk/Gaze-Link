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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.demo.opencv.R;
import com.demo.opencv.socialMedia.SocialMediaData;
import com.demo.opencv.UIViewModel;
import com.demo.opencv.UserDataManager;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class social_media_display extends Fragment {

    private UIViewModel viewModel;
    Activity mActivity;
    Handler handler;
    Runnable updateUI;
    TextView textBox;
    TextView userNameBox;
    TextView cameraCountdown;
    ImageView imageBox;
    boolean logged_in = false;
    List<TextView> gazeButtons = new ArrayList<>();
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = requireActivity();
        viewModel = new ViewModelProvider(requireActivity()).get(UIViewModel.class);
    }

    public interface SocialMediaModeListener {
        void onSocialMediaButtonClicked(int input);
        void AuthenticationButtonClicked();
    }
    social_media_display.SocialMediaModeListener socialMediaModeListener;
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacks(updateUI);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Ensure that the container activity has implemented the callback interface
        try {
            socialMediaModeListener = (social_media_display.SocialMediaModeListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement socialMediaModeListener");
        }
    }

    private Bitmap matToBitmap(Mat mat) {
        Bitmap bm = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat,bm);
        return bm;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {

        UserDataManager userDataManager = (UserDataManager) requireActivity().getApplication();
        logged_in = !(Objects.equals(userDataManager.getAccessToken(), ""));
        textBox = mActivity.findViewById(R.id.socialMediaText);
        imageBox = mActivity.findViewById(R.id.socialMediaImage);
        cameraCountdown = mActivity.findViewById(R.id.socialMediaCameraText);
        userNameBox = mActivity.findViewById(R.id.socialMediaUsernameText);
        gazeButtons.clear();
        gazeButtons.add(mActivity.findViewById(R.id.socialMediaDeleteTextButton)); // up
        gazeButtons.add(mActivity.findViewById(R.id.socialMediaTakePictureButton)); // left
        gazeButtons.add(mActivity.findViewById(R.id.socialMediaGenerateImageButton)); // right
        gazeButtons.add(mActivity.findViewById(R.id.socialMediaRefineTextButton)); // closed
        gazeButtons.add(mActivity.findViewById(R.id.socialMediaBackButton)); // left up
        gazeButtons.add(mActivity.findViewById(R.id.socialMediaUploadButton)); // right up

        for (int i = 0; i < 6; i++) { // add click listeners for the eye gesture buttons
            int finalI = i;
            gazeButtons.get(i).setOnClickListener(v -> socialMediaModeListener.onSocialMediaButtonClicked(finalI));
        }
        userNameBox.setOnClickListener(v -> socialMediaModeListener.AuthenticationButtonClicked());

        handler = new Handler(Looper.getMainLooper());
        updateUI = new Runnable() {
            @Override
            public void run() {
                viewModel.getSelectedItem().observe(requireActivity(), item -> {

                    SocialMediaData socialMediaData = item.socialMediaData;
                    if (socialMediaData != null) {
                        String finalText = "Tweet Text: " + socialMediaData.textBox;
                        textBox.setText(finalText); // update the text box
                        if (socialMediaData.cameraImage != null) { // update camera frame
                            imageBox.setImageBitmap(matToBitmap(socialMediaData.cameraImage));
                        }
                        if (socialMediaData.cameraState == -1) {
                            cameraCountdown.setText("");
                            gazeButtons.get(1).setText(R.string.SocialMedia_TakePhoto);
                        } else if (socialMediaData.cameraState == 0){
                            cameraCountdown.setText("");
                            gazeButtons.get(1).setText(R.string.SocialMedia_CancelPhoto);
                        } else {
                            cameraCountdown.setText(String.valueOf(socialMediaData.cameraState));
                            gazeButtons.get(1).setText(R.string.SocialMedia_TakingPhoto);
                        }
                        if (logged_in) {
                            userNameBox.setText(R.string.SocialMedia_LoggedIn);
                        } else {
                            userNameBox.setText(R.string.SocialMedia_ClickAuthenticate);
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
        return inflater.inflate(R.layout.social_media_fragment, container, false);
    }
}