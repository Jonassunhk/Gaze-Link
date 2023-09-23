package com.demo.opencv;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Range;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
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
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import com.google.common.util.concurrent.ListenableFuture;

import org.opencv.core.Mat;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements ContractInterface.View {

    // creating object of Presenter interface in Contract
    ContractInterface.Presenter presenter;
    public ImageView[] images = new ImageView[4];
    public TextView[] texts = new TextView[4];
    private final int REQUEST_CODE_PERMISSIONS = 1001;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA"};
    private final ExecutorService cameraExecutor = Executors.newSingleThreadExecutor();
    private UIViewModel viewModel;
    Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // instantiating object of Presenter Interface
        presenter = new Presenter(this, new Model());

        try {
            presenter.initialize(this); // initialize presenter
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //cameraXSetup.initializeCameraX(); // initialize cameraX (raw user input)
        Log.d("MVPView", "CameraX and Presenter initialized");

        if(allPermissionsGranted()){
            startCamera(); //start camera if permission has been granted by user
        } else{
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        viewModel = new ViewModelProvider(this).get(UIViewModel.class);

        Button devButton;
        devButton = findViewById(R.id.devModeButton);
        Button quickButton = findViewById(R.id.quickModeButton);
        FragmentManager fragmentManager = getSupportFragmentManager();

        devButton.setOnClickListener(v -> fragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, dev_mode_display.class, null)
                .setReorderingAllowed(true)
                .addToBackStack("dev_fragment") // Name can be null
                .commit());

        quickButton.setOnClickListener(v -> fragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, quick_mode_display.class, null)
                .setReorderingAllowed(true)
                .addToBackStack("quick_fragment") // Name can be null
                .commit());


//
//        texts[0] = findViewById(R.id.textView1);
//        texts[1] = findViewById(R.id.textView2);
//        texts[2] = findViewById(R.id.textView3);
//        texts[3] = findViewById(R.id.textView4);
    }

    @Override
    public void switchMode(String activityName) {

    }

    @Override
    public void showInput(int gazeCode) {

    }

    @Override
    public void displayDetectData(GazeInput gazeInput) {
        Runnable myRunnable = () -> {
            viewModel.selectItem(gazeInput);
        };
        mainHandler.post(myRunnable);
    }

    @Override
    public void displayText(int code, String text) {
        TextView view = texts[code];
        if (view != null) {
            runOnUiThread(() -> view.setText(text));
        }
        Log.d("MVPView", "Displayed text on activity");
    }

    @Override
    public void displayImage(int code, Bitmap bitmap) {
        ImageView view = images[code];
        if (view != null) {
            runOnUiThread(() -> view.setImageBitmap(bitmap));

        }
        Log.d("MVPView", "Displayed image on activity");
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
        int TARGET_FRAME_RATE = 12;
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

                //TODO: Finish setting up frame listener + image processing

                if (image.getFormat() == ImageFormat.YUV_420_888 && image.getPlanes().length == 3) {
                    Image newImage = image.getImage();
                    Mat grayMat = Yuv420.gray(newImage);
                    Log.d("MVPView", "Image Format Correct: Frame Loaded");
                    presenter.onFrame(grayMat);
                } else {
                    Log.d("MVPView", "Image format not correct");
                }
                image.close();
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
    }

}
