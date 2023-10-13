package com.demo.opencv;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.util.Log;

import androidx.annotation.OptIn;
import androidx.camera.core.ExperimentalGetImage;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.List;


public class Model implements ContractInterface.Model {
    private final EyeDetection detector = new EyeDetection(); // create eye detection object to analyze user input
    private final GoogleFaceDetector faceDetector = new GoogleFaceDetector();
    DetectionOutput detectionOutput = new DetectionOutput();
    @Override
    public void initializeModels(Context context) throws IOException {

        // check and initialize OpenCV
        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCVDebug", "cannot init debug");
        } else {
            Log.d("OpenCVDebug", "success");
        }

        detector.mContext = context;
        int[] files = {R.raw.haarcascade_frontalface_alt, R.raw.haarcascade_lefteye_2splits};
        detector.loadTensorModel(); // load TensorFlow Lite Model
        detector.loadOpenCVModels(files); // load OpenCV Cascade Models
        faceDetector.initialize(); // initialize Google ML Kit
    }

    private Rect getBoundingBox(List<PointF> points) {

        int INF = 100000;
        int maxX = -1, maxY = -1, minX = INF, minY = INF;

        for (int i = 0; i < points.size(); i++) {
            maxX = Math.max(maxX, (int) points.get(i).x);
            maxY = Math.max(maxY, (int) points.get(i).y);
            minX = Math.min(minX, (int) points.get(i).x);
            minY = Math.min(minY, (int) points.get(i).y);
        }

        Rect boundingBox;
        if (maxX != -1 && maxY != -1 && minX != INF && minY != INF) { // contour is valid
            boundingBox = new Rect(new Point(minX, minY), new Point(maxX, maxY));
            return boundingBox;
        } else {
            return null;
        }
    }

    @Override
    public DetectionOutput classifyGaze(Mat rgbMat) { @OptIn(markerClass = ExperimentalGetImage.class)

        Mat leftEye = null, rightEye = null;
        Bitmap bmp;
        // opencv detection
        //Imgproc.cvtColor(rgbMat, grayMat, Imgproc.COLOR_RGB2GRAY);
        //gazeInput = detector.faceEyeDetection(grayMat); // detect gaze

        // google ml kit detection
        Mat rgbaMat = new Mat(rgbMat.cols(), rgbMat.rows(), CvType.CV_8UC4);
        Imgproc.cvtColor(rgbMat, rgbaMat, Imgproc.COLOR_BGR2RGBA);
        bmp = Bitmap.createBitmap(rgbaMat.cols(), rgbaMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(rgbaMat, bmp);
        faceDetector.detect(bmp);

        // initialization
        detectionOutput.LeftData = new GazeData();
        detectionOutput.LeftData.Success = false;
        detectionOutput.RightData = new GazeData();
        detectionOutput.RightData.Success = false;
        detectionOutput.testingMats = new Mat[2];
        
        if (faceDetector.leftEyeContour == null && faceDetector.rightEyeContour == null) { // no eye detection
            return detectionOutput;
        
        } else if (faceDetector.leftEyeOpenProb <= 0.2 && faceDetector.rightEyeOpenProb <= 0.1) { // check if eyes are closed
            detectionOutput.setEyeData(0, true, 5, 1, faceDetector.leftEyeOpenProb);
            detectionOutput.setEyeData(1, true, 5, 1, faceDetector.rightEyeOpenProb);
        } else {
            if (faceDetector.leftEyeContour != null) { // left eye available
                List<PointF> leftEyePoints = faceDetector.leftEyeContour.getPoints();
                Rect leftEyeBound = getBoundingBox(leftEyePoints);
                if (leftEyeBound != null) {
                    Log.d("MVPModel", "Rect Dimensions: " + leftEyeBound.x + ' ' + leftEyeBound.y + ' ' + leftEyeBound.height + ' ' + leftEyeBound.width);
                    leftEye = new Mat(rgbMat, leftEyeBound);
                    Imgproc.cvtColor(leftEye, leftEye, Imgproc.COLOR_RGB2GRAY);
                    detectionOutput = detector.runEyeModel(detectionOutput, leftEye, 0);
                }
            }
            if (faceDetector.rightEyeContour != null) { // right eye available
                List<PointF> rightEyePoints = faceDetector.rightEyeContour.getPoints();
                Rect rightEyeBound = getBoundingBox(rightEyePoints);
                if (rightEyeBound != null) {
                    Log.d("MVPModel", "Rect Dimensions: " + rightEyeBound.x + ' ' + rightEyeBound.y + ' ' + rightEyeBound.height + ' ' + rightEyeBound.width);
                    rightEye = new Mat(rgbMat, rightEyeBound);
                    Imgproc.cvtColor(rightEye, rightEye, Imgproc.COLOR_RGB2GRAY);
                    detectionOutput = detector.runEyeModel(detectionOutput, rightEye, 1);
                }
            }
        }
        //testing data
        detectionOutput.testingMats[0] = leftEye;
        detectionOutput.testingMats[1] = rightEye;

        return detectionOutput;
    }
}
