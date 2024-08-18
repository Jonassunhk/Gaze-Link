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
        Face largestFace = null;
        int maxSize = -1;
        for (Face face: faces) { // determine the largest face
            Rect boundingBox = face.getBoundingBox();
            if (boundingBox.height() * boundingBox.width() > maxSize) {
                largestFace = face;
                maxSize = boundingBox.height() * boundingBox.width();
            }
        }

        if (largestFace != null) {
            leftEyeContour = largestFace.getContour(FaceContour.LEFT_EYE);
            rightEyeContour = largestFace.getContour(FaceContour.RIGHT_EYE);

            // If classification was enabled:
            if (largestFace.getLeftEyeOpenProbability() != null) {
                leftEyeOpenProb = largestFace.getLeftEyeOpenProbability();
            }
            if (largestFace.getRightEyeOpenProbability() != null) {
                rightEyeOpenProb = largestFace.getRightEyeOpenProbability();
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
