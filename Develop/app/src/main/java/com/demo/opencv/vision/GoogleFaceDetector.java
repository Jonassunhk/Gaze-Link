package com.demo.opencv.vision;


import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.Rect;
import android.media.Image;
import android.util.Log;

import androidx.annotation.OptIn;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageProxy;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;

import java.util.List;
import java.util.Objects;

public class GoogleFaceDetector {

    FaceDetectorOptions realTimeOpts;
    FaceDetector detector;

    // required public data
    public FaceContour leftEyeContour, rightEyeContour;
    public PointF leftEyePos, rightEyePos;
    public float rightEyeOpenProb, leftEyeOpenProb, rotY, rotZ;
    public Rect faceBound;

    public void initialize() {

        realTimeOpts = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build();

        detector = FaceDetection.getClient(realTimeOpts);
    }

    public void analyze(List<Face> faces) {
        if (faces.size() == 0) { // prevent the previous input from continuing to work
            leftEyeContour = null;
            rightEyeContour = null;
        }
        for (Face face : faces) {
            Log.d("googleFaceDetector", "face detected");
            //faceBound = face.getBoundingBox();
            //rotY = face.getHeadEulerAngleY();  // Head is rotated to the right rotY degrees
            //rotZ = face.getHeadEulerAngleZ();  // Head is tilted sideways rotZ degrees

            // If contour detection was enabled:
           // if (leftEyeContour == null || rightEyeContour == null) {
                leftEyeContour = face.getContour(FaceContour.LEFT_EYE);
                rightEyeContour = face.getContour(FaceContour.RIGHT_EYE);
           // }

//            FaceLandmark leftEye = face.getLandmark(FaceLandmark.LEFT_EYE);
//            if (leftEye != null) {
//                leftEyePos = leftEye.getPosition();
//            }
//
//            FaceLandmark rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE);
//            if (rightEye != null) {
//                rightEyePos = rightEye.getPosition();
//            }

            // If classification was enabled:
            if (face.getLeftEyeOpenProbability() != null) {
                leftEyeOpenProb = face.getLeftEyeOpenProbability();
            }
            if (face.getRightEyeOpenProbability() != null) {
                rightEyeOpenProb = face.getRightEyeOpenProbability();
            }
        }
    }
    @OptIn(markerClass = ExperimentalGetImage.class)
    public void detect(Bitmap bm) {

        InputImage newImage = InputImage.fromBitmap(bm, 0);
        Log.d("googleFaceDetector", "detect begins");

        Task<List<Face>> result = detector.process(newImage)
                .addOnSuccessListener(this::analyze)
                .addOnFailureListener(
                        e -> {
                            Log.d("googleFaceDetector", "detection failed " + e.getMessage());
                        });
    }
}
