package com.demo.opencv;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.ImageFormat;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Range;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.camera2.interop.Camera2Interop;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.demo.opencv.fragments.calibration_display;
import com.demo.opencv.fragments.clinician_mode_display;
import com.demo.opencv.fragments.dev_mode_display;
import com.demo.opencv.fragments.quick_chat_display;
import com.demo.opencv.fragments.social_media_display;
import com.demo.opencv.fragments.text_mode_display;
import com.demo.opencv.other.ClinicalData;
import com.demo.opencv.socialMedia.TwitterAuthenticator;
import com.demo.opencv.vision.ImageFormatUtils;
import com.demo.opencv.vision.Model;
import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONException;
import org.opencv.core.Mat;

import java.io.IOException;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity
        implements ContractInterface.View, calibration_display.OnButtonClickListener,
        text_mode_display.TextModeListener, social_media_display.SocialMediaModeListener,
        quick_chat_display.QuickChatListener

{

    // creating object of Presenter interface in Contract
    ContractInterface.Presenter presenter;
    TwitterAuthenticator twitterAuthenticator;

    public ImageView[] images = new ImageView[4];
    private final int REQUEST_CODE_PERMISSIONS = 1001;
    private final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA", "android.permission.RECORD_AUDIO"};
    private final ExecutorService cameraExecutor = Executors.newSingleThreadExecutor();
    private UIViewModel viewModel;
    FragmentManager fragmentManager;
    Handler mainHandler = new Handler(Looper.getMainLooper());
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UserDataManager userDataManager = (UserDataManager) getApplication();
        userDataManager.getSettings();
        setLocale(userDataManager.getLanguage());
        setContentView(R.layout.activity_main);
        // instantiating object of Presenter Interface
        presenter = new Presenter(this, new Model());

        Log.d("MainActivityGazeLink", "initialized");
        try {
            presenter.initialize(this, getApplication()); // initialize presenter
        } catch (IOException | JSONException e) {
            throw new RuntimeException(e);
        }

        if(allPermissionsGranted()){
            startCamera(); //start camera if permission has been granted by user
        } else{
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_RECORD_AUDIO_PERMISSION);
        }
        viewModel = new ViewModelProvider(this).get(UIViewModel.class);

        //twitter authenticator
        twitterAuthenticator = new TwitterAuthenticator(getApplication());
        Intent intent = getIntent();
        Uri uri = intent.getData();
        twitterAuthenticator.handleCallback(uri, getApplication()); // when redirected back, get access token

        // home page set up
        Button devButton, textButton, calibrateButton, clinicianButton, quickChatButton;
        devButton = findViewById(R.id.devModeButton);
        textButton = findViewById(R.id.textModeButton);
        calibrateButton =  findViewById(R.id.calibrateButton);
        clinicianButton = findViewById(R.id.clinicianModeButton);
        quickChatButton = findViewById(R.id.quickChatButton);

        fragmentManager = getSupportFragmentManager();

        openSettings();
        clinicianButton.setOnClickListener(v -> {
            if (!Objects.equals(presenter.getMode(), "Clinician")) {
                setLocale(userDataManager.getLanguage());
                openClinician();
            }
        });
        devButton.setOnClickListener(v -> {
                if (!Objects.equals(presenter.getMode(), "Dev")) {
                    setLocale(userDataManager.getLanguage());
                    openSettings();
                }
            }
        );
        textButton.setOnClickListener(v -> {
            if (!Objects.equals(presenter.getMode(), "Text")) {
                setLocale(userDataManager.getLanguage());
                openTextEntry();
            }
        });
        calibrateButton.setOnClickListener(v -> {
            if (!Objects.equals(presenter.getMode(), "Calibration")) {
                setLocale(userDataManager.getLanguage());
                openCalibration();
            }
        });
        quickChatButton.setOnClickListener(v -> {
            if (!Objects.equals(presenter.getMode(), "QuickChat")) {
                setLocale(userDataManager.getLanguage());
                openQuickChat();
            }
        });
    }

    private void setLocale(String language) {

        String languageCode;
        if (Objects.equals(language, "English")) {
            languageCode = "en-us";
        } else if (Objects.equals(language, "Spanish")) {
            languageCode = "es";
        } else if (Objects.equals(language, "Chinese")) {
            languageCode = "zh";
        } else {
            languageCode = "en-us";
        }
        Log.d("LocaleSettings", "Current Code: " + languageCode);
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        Resources resources = getResources();
        Configuration configuration = resources.getConfiguration();

        configuration.setLocale(locale);

        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
    }

    @Override
    public void onCalibrationButtonClick() {
        presenter.updateCalibration();
    }

    @Override
    public void updateLiveData(AppLiveData appLiveData) {
        Runnable myRunnable = () -> viewModel.selectItem(appLiveData);
        mainHandler.post(myRunnable);
    }

    @Override
    public void openSocialMedia() {
        presenter.setMode("Social Media");
        fragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, social_media_display.class, null)
                .setReorderingAllowed(true)
                .addToBackStack("SocialMediaFragment") // Name can be null
                .commit();
    }
    @Override
    public void openTextEntry() {
        presenter.setMode("Text");
        Fragment textFragment = text_mode_display.newInstance();
        fragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, textFragment)
                .setReorderingAllowed(true)
                .addToBackStack("text_fragment") // Name can be null
                .commit();
    }
    @Override
    public void openCalibration() {
        presenter.setMode("Calibration");
        fragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, calibration_display.class, null)
                .setReorderingAllowed(true)
                .addToBackStack("calibrate_fragment") // Name can be null
                .commit();
    }
    @Override
    public void openSettings() {
        presenter.setMode("Dev");
        Fragment devFragment = dev_mode_display.newInstance();
        fragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, devFragment)
                .setReorderingAllowed(true)
                .addToBackStack("dev_fragment") // Name can be null
                .commit();
    }

    @Override
    public void openQuickChat() {
        presenter.setMode("QuickChat");
        fragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, quick_chat_display.class, null)
                .setReorderingAllowed(true)
                .addToBackStack("quick_chat_fragment") // Name can be null
                .commit();
    }
    @Override
    public void openClinician() {
        presenter.setMode("Clinician");
        ClinicalData clinicalData = presenter.getClinicalData(); // get clinical data from the presenter
        Fragment calibrationFragment = clinician_mode_display.newInstance(clinicalData);
        fragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, calibrationFragment)
                .setReorderingAllowed(true)
                .addToBackStack("clinician_fragment") // Name can be null
                .commit();
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        presenter.onDestroy();
        cameraExecutor.shutdown();
    }

    private void startCamera() {
        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCamera(cameraProvider);

            } catch (ExecutionException | InterruptedException e) {
                // No errors need to be handled for this Future.
                // This should never be reached.
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @OptIn(markerClass = ExperimentalCamera2Interop.class)
    void bindCamera(@NonNull ProcessCameraProvider cameraProvider) {

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        ImageAnalysis.Builder newBuilder = new ImageAnalysis.Builder();

        Camera2Interop.Extender<ImageAnalysis> ext = new Camera2Interop.Extender<>(newBuilder);
        int TARGET_FRAME_RATE = 13;
        ext.setCaptureRequestOption(
                CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                new Range<>(TARGET_FRAME_RATE, TARGET_FRAME_RATE)
        );
        ImageAnalysis imageAnalysis = newBuilder
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
        imageAnalysis.setAnalyzer(cameraExecutor, new ImageAnalysis.Analyzer() {
            @Override
            @OptIn(markerClass = ExperimentalGetImage.class)
            public void analyze(@NonNull ImageProxy image) {

                if (presenter.getPresenterState()) {
                    image.close();
                } else {
                    if (image.getFormat() == ImageFormat.YUV_420_888 && image.getPlanes().length == 3) {
                        //Log.d("MVPView", "Image Format Correct: Frame Loaded");
                        Image newImage = image.getImage();
                        Mat rgbaMat = ImageFormatUtils.rgb(newImage);

                        presenter.onFrame(rgbaMat);
                        assert newImage != null;
                        newImage.close();
                    } else {
                        Log.d("MVPView", "Image format not correct");
                    }
                    image.close();
                }
            }
        });
        cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis);
    }

    private boolean allPermissionsGranted(){
        for(String permission : REQUIRED_PERMISSIONS){
            if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                this.finish();
            }
        }
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            Log.d("MVPView", "Record audio permission granted");
        }
    }

    @Override
    public void onTextModeButtonClicked(int input) {
        presenter.onGazeButtonClicked(input);
    }

    @Override
    public void recordButtonClicked() {
        presenter.onRecordButtonClicked();
    }

    @Override
    public void onSocialMediaButtonClicked(int input) { presenter.onGazeButtonClicked(input); }

    @Override
    public void AuthenticationButtonClicked() { // update twitter authentication
        twitterAuthenticator = new TwitterAuthenticator(this);
        twitterAuthenticator.startAuthentication(getApplication());
    }

    @Override
    public void onQuickChatButtonClicked(int input) {
        presenter.onGazeButtonClicked(input);
    }

    @Override
    public void onQuickChatButtonContentChanged(String newText) {

    }
}
