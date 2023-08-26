package com.demo.opencv;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.ImageAnalysis;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.core.content.ContextCompat;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.IOException;


public class MainActivity extends AppCompatActivity implements ContractInterface.View {

    // creating object of Presenter interface in Contract
    ContractInterface.Presenter presenter;
    CameraXSetup cameraXSetup = new CameraXSetup();
    public ImageView[] images;
    public TextView[] texts;
    Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_developer_mode);

        // instantiating object of Presenter Interface
        presenter = new Presenter(this, new Model());

        try {
            presenter.initialize(this); // initialize presenter
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        cameraXSetup.initializeCameraX(); // initialize cameraX (raw user input)
        Log.d("MVPView", "CameraX and Presenter initialized");

        images[0] = findViewById(R.id.imageView1);
        images[1] = findViewById(R.id.imageView2);
        images[2] = findViewById(R.id.imageView3);
        images[3] = findViewById(R.id.imageView4);

        texts[0] = findViewById(R.id.textView1);
        texts[1] = findViewById(R.id.textView2);
        texts[2] = findViewById(R.id.textView3);
        texts[3] = findViewById(R.id.textView4);
    }

    @Override
    public void switchMode(String activityName) {

    }

    @Override
    public void showInput(int gazeCode) {

    }

    @Override
    public void displayText(int code, String text) {
        TextView view = texts[code];
        if (view != null) {
            view.setText(text);
        }
        Log.d("MVPView", "Displayed text on activity");
    }

    @Override
    public void displayImage(int code, Bitmap bitmap) {
        ImageView view = images[code];
        if (view != null) {
            view.setImageBitmap(bitmap);
        }
        Log.d("MVPView", "Displayed image on activity");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        presenter.onDestroy();
    }
}
