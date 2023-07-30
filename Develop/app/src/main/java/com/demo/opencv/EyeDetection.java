package com.demo.opencv;

import static java.lang.String.format;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.media.Image;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.SimpleBlobDetector;
import org.opencv.features2d.SimpleBlobDetector_Params;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;




public class EyeDetection extends AppCompatActivity {
    public CascadeClassifier faceDetector, eyeDetector;
    private Context mContext;
    private Activity mActivity;
    private SimpleBlobDetector blobDetector;
    private SimpleBlobDetector_Params params;
    int leftCounter, rightCounter;
    ImageView image1;
    ImageView image2;
    ImageView image3;
    TextView text1;
    //TODO: define number constants for variable adjustments later on
    double leftPercentage = 0.4f;
    double rightPercentage = 0.6f;

    public void display_mat(Mat mat, ImageView image) {
        if (mContext == null || mActivity == null) {
            Log.d("EyeDetectionCheck", "Waiting for context and activity to load");
            return;
        }
        //mat = rotate_mat(mat, 90.00);
        Bitmap bm = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat,bm);
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (image != null) {
                    image.setImageBitmap(bm);
                }
            }
        });
        Log.d("EyeDetectionCheck", "Displayed image on screen");
    }

    public void display_text(String text, int id) {
        if (mContext == null || mActivity == null) {
            Log.d("EyeDetectionCheck", "Waiting for context and activity to load");
            return;
        }
        text1 = mActivity.findViewById(id);

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (text1 != null) {
                    text1.setText(text);
                }
            }
        });
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

    public void set_blobDetector_params() {
        params = new SimpleBlobDetector_Params();
        params.set_minCircularity((float) 0.8);
        params.set_minArea((float) 80);
        params.set_maxArea((float) 800);
        params.set_minConvexity((float) 0.4);
        params.set_minInertiaRatio((float) 0.2);
        params.set_minThreshold((float) 10);
    }
    public void initialize_detector(Context context, Activity activity) { // use input output stream to write CascadeClassifier

        this.mContext = context;
        this.mActivity = activity;
        OpenCVLoader.initDebug();
        set_blobDetector_params();
        Resources res = mContext.getResources();
        leftCounter = 0; rightCounter = 0;

        image1 = mActivity.findViewById(R.id.MatDisplay);
        image2 = mActivity.findViewById(R.id.eye1Display);
        image3 = mActivity.findViewById(R.id.eye2Display);
        int files[] = {R.raw.haarcascade_frontalface_alt, R.raw.haarcascade_eye};

        for (int code : files) { // loop to initiate the three cascade classifiers
            try {
                InputStream inputStream = res.openRawResource(code);
                File file = new File(mContext.getDir("cascade", MODE_PRIVATE), "haarcascade_frontalface_alt");

                FileOutputStream fileOutputStream = new FileOutputStream(file);
                byte[] data = new byte[4096];
                int read_bytes;

                while ((read_bytes = inputStream.read(data)) != -1) {
                    fileOutputStream.write(data, 0, read_bytes);
                }
                if (code == R.raw.haarcascade_frontalface_alt) faceDetector = new CascadeClassifier(file.getAbsolutePath());
                else if (code == R.raw.haarcascade_eye) eyeDetector = new CascadeClassifier(file.getAbsolutePath());

                inputStream.close();
                fileOutputStream.close();
                file.delete();

            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        if (!faceDetector.empty() && !eyeDetector.empty()) {
            Log.d("EyeDetectionCheck", "Cascades loaded successfully");
        } else {
            Log.d("EyeDetectionCheck", "Some cascade files empty!!");
        }
    }

    public Point add(Point a, Point b) {
        Point z = new Point();
        z.x = a.x + b.x;
        z.y = a.y + b.y;
        return z;
    }
    public void eyeball_detection(Mat faceROI, Rect eye, ImageView display) {
        Mat eyeROI = new Mat(faceROI, eye);

        // Canny + dilate processing

        Mat threshMat = new Mat();
        Imgproc.GaussianBlur(eyeROI, eyeROI, new Size(11,11),0);

        //Imgproc.Canny(eyeROI, threshMat, 100, 200); // method 1
        //Imgproc.threshold(eyeROI, threshMat, 0,255,Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU); // method 2
        Imgproc.adaptiveThreshold(eyeROI, threshMat, 255,Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY,7,2);
        // Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3,3)); // dilute
       // Imgproc.dilate(eyeROI, eyeROI, kernel);

//        List<MatOfPoint> contours = new ArrayList<>();
//        Mat hierarchy = new Mat();
//        Imgproc.findContours(threshMat, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
//        Mat draw = Mat.zeros(threshMat.size(), CvType.CV_8UC3);
//        for (int i = 0; i < contours.size(); i++) {
//            Scalar color = new Scalar(255,0,0);
//            Imgproc.drawContours(draw, contours, i, color, 1, Imgproc.LINE_8, hierarchy, 2, new Point());
//        }
        // Blob detector parameters
        blobDetector = SimpleBlobDetector.create(params);
        MatOfKeyPoint keyPoints = new MatOfKeyPoint();
        blobDetector.detect(threshMat, keyPoints);
        Features2d.drawKeypoints(threshMat, keyPoints, threshMat, new Scalar(0,255,0), Features2d.DrawMatchesFlags_DRAW_RICH_KEYPOINTS);

//        if (points.length >= 1) {
//            double x = points[0].pt.x;
//            double y = points[0].pt.y;
//            double matX = (double) eyeROI.width();
//            double matY = (double) eyeROI.height();
//            Log.d("GazeInputCheck","Mat x percentage = " + (x / matX));
//            Log.d("GazeInputCheck","Mat y percentage = " + (y / matY));
//            if (x / matX <= leftPercentage) {
//                Log.d("GazeInputCheck","Left Detected!");
//            }
//        }
        display_mat(threshMat, display);
    }

    public Mat eye_detection(Mat loadedImage, Rect face) {

        Mat imageROI = new Mat(loadedImage, face); // roi with only the face
        MatOfRect eyesDetected = new MatOfRect();
        int minEyeSize = Math.round(imageROI.rows() * 0.03f); // TODO: adjust

        eyeDetector.detectMultiScale(imageROI,
                eyesDetected,
                1.1,
                3,
                Objdetect.CASCADE_SCALE_IMAGE,
                new Size(minEyeSize, minEyeSize),
                new Size()
        );

        Rect[] eyesArray = eyesDetected.toArray();
        int size = eyesArray.length;
        if (size < 2) {
            Log.d("EyeDetectionCheck","Insufficient Eyes Detected");
        } else if (size == 2) {
            Log.d("EyeDetectionCheck","Both eyes detected, proceeding...");
            eyeball_detection(imageROI, eyesArray[0],image2);
            eyeball_detection(imageROI, eyesArray[1],image3);

            Imgproc.rectangle(loadedImage, add(eyesArray[0].tl(), face.tl()), add(eyesArray[0].br(), face.tl()), new Scalar(255,0,0), 3);
            Imgproc.rectangle(loadedImage, add(eyesArray[1].tl(), face.tl()), add(eyesArray[1].br(), face.tl()), new Scalar(255,0,0), 3);

        } else {
            Log.d("EyeDetectionCheck","More than two eyes detected: " + size);
        }

        return loadedImage;
    }
    public void input_detection(Mat inputImage)
    {
        if (faceDetector == null || eyeDetector == null) {
            Log.d("EyeDetectionCheck", "Waiting for Cascade to load");
            return;
        }
        //Noise Reduction
        Mat loadedImage = new Mat(inputImage.rows(), inputImage.cols(), inputImage.type());
        Imgproc.GaussianBlur(inputImage, loadedImage, new Size(5,5),0);


        Log.d("EyeDetectionCheck", "Through");
        MatOfRect facesDetected = new MatOfRect();
        int minFaceSize = Math.round(loadedImage.rows() * 0.1f); // 0.1f for face, check later to see which one is best
        faceDetector.detectMultiScale(loadedImage,
                facesDetected,
                1.1,
                3,
                Objdetect.CASCADE_SCALE_IMAGE,
                new Size(minFaceSize, minFaceSize),
                new Size()
        );

        Rect[] facesArray = facesDetected.toArray();
        int size = facesArray.length;

        if (size == 0) {
            Log.d("EyeDetectionCheck","No faces detected...");
        } else if (size == 1) {
            Log.d("EyeDetectionCheck","One face detected, proceeding...");
            Rect face = facesArray[0];
            loadedImage = eye_detection(loadedImage, face);
            Imgproc.rectangle(loadedImage, face.tl(), face.br(), new Scalar(0, 0, 255), 3);
        } else {
            Log.d("EyeDetectionCheck","More than one face detected: " + size);
        }


        display_mat(loadedImage, image1);
    }
}
