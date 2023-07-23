package com.demo.opencv;

import static java.lang.String.format;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.util.Log;
import android.widget.ImageView;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import androidx.appcompat.app.AppCompatActivity;

public class EyeDetection extends AppCompatActivity {

    public CascadeClassifier eyeDetector;
    private Context mContext;
    private Activity mActivity;
    ImageView image1;

    public void display_mat(Mat mat) {
        if (mContext == null || mActivity == null) {
            Log.d("EyeDetectionCheck", "Waiting for context and activity to load");
            return;
        }
        image1 = mActivity.findViewById(R.id.MatDisplay);
        Bitmap bm = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat,bm);
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (image1 != null) {
                    image1.setImageBitmap(bm);
                }
            }
        });

        Log.d("EyeDetectionCheck", "Displayed image on screen");
    }

    public Mat rotate_mat(Mat mat, double angle) {

        Mat rotated_mat = new Mat();
        Point rotPoint = new Point(mat.cols() / 2.0,
                mat.rows() / 2.0);
        // Create Rotation Matrix
        Mat rotMat = Imgproc.getRotationMatrix2D(
                rotPoint, angle, 1);

        // Apply Affine Transformation
        Imgproc.warpAffine(mat, rotated_mat, rotMat, mat.size(),
                Imgproc.WARP_INVERSE_MAP);
        return rotated_mat;
    }
    public void initialize_detector(Context context, Activity activity) { // use input output stream to write CascadeClassifier

        this.mContext = context;
        this.mActivity = activity;
        try {
            OpenCVLoader.initDebug();
            Resources res = mContext.getResources();
            InputStream inputStream = res.openRawResource(R.raw.haarcascade_eye);
            File file = new File(mContext.getDir("cascade", MODE_PRIVATE), "haarcascade_eye");

            FileOutputStream fileOutputStream = new FileOutputStream(file);
            byte[] data = new byte[4096];
            int read_bytes;

            while((read_bytes = inputStream.read(data)) != -1) {
                fileOutputStream.write(data,0,read_bytes);
            }

            eyeDetector = new CascadeClassifier(file.getAbsolutePath());
            if (!eyeDetector.empty()) {
                Log.d("EyeDetectionCheck", "Cascade loaded successfully");
            } else {
                Log.d("EyeDetectionCheck", "Cascade file empty!!");
            }
            //if(eyeDetector.empty()) eyeDetector = null;


            inputStream.close();
            fileOutputStream.close();
            file.delete();

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void detect_eyes(Mat loadedImage)
    {
        if (eyeDetector == null) {
            Log.d("EyeDetectionCheck", "Waiting for Cascade to load");
        }

        Log.d("EyeDetectionCheck", "Through");
        MatOfRect eyesDetected = new MatOfRect();
        int minFaceSize = Math.round(loadedImage.rows() * 0.03f);
        eyeDetector.detectMultiScale(loadedImage,
                eyesDetected,
                1.1,
                3,
                Objdetect.CASCADE_SCALE_IMAGE,
                new Size(minFaceSize, minFaceSize),
                new Size()
        );

        Rect[] eyesArray = eyesDetected.toArray();
        int size = eyesArray.length;
        Log.d("EyeDetectionCheck","Number of eyes detected: " + size);

        for(Rect face : eyesArray) {
            Imgproc.rectangle(loadedImage, face.tl(), face.br(), new Scalar(0, 0, 255), 3);
        }
        loadedImage = rotate_mat(loadedImage, 90.00);
        display_mat(loadedImage);
    }
}
