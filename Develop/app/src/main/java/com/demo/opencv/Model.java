package com.demo.opencv;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;


public class Model implements ContractInterface.Model {
    private final EyeDetection detector = new EyeDetection(); // create eye detection object to analyze user input
    private final GoogleFaceDetector faceDetector = new GoogleFaceDetector();
    DetectionOutput detectionOutput = new DetectionOutput();
    UserDataManager userDataManager = new UserDataManager();
    private ArrayList<String> prevInputs;
    private Queue<Integer> window;
    int gazeNum = 8; // number of types of gaze inputs
    int tempNum = 7;
    int[] gazeCount;
    String[] leftEyeFileNames = {"left_left", "left_right", "left_straight", "left_up", "left_down", "left_left_up", "left_right_up"};
    String[] rightEyeFileNames = {"right_left", "right_right", "right_straight", "right_up", "right_down", "right_left_up", "right_right_up"};
    Bitmap[] calibrationImagesLeft, calibrationImagesRight;
    @Override
    public void initialize(Context context) throws IOException { // initialize models, libraries, and datastore

        if (!OpenCVLoader.initDebug()) { // check and initialize OpenCV
            Log.d("OpenCVDebug", "cannot init debug");
        } else {
            Log.d("OpenCVDebug", "success");
        }
        detector.mContext = context;
        prevInputs = new ArrayList<>();
        window = new LinkedList<>();
        gazeCount = new int[gazeNum]; // change based on how many detections there are
        int[] files = {R.raw.haarcascade_frontalface_alt, R.raw.haarcascade_lefteye_2splits};

        detector.loadTensorModel(); // load TensorFlow Lite Model
        detector.loadOpenCVModels(files); // load OpenCV Cascade Models
        faceDetector.initialize(); // initialize Google ML Kit
        userDataManager.initialize(context); // initialize settings

        int[] left_id = {
                R.drawable.left_left, R.drawable.left_right, R.drawable.left_straight, R.drawable.left_up,
                R.drawable.left_down, R.drawable.left_left_up, R.drawable.left_right_up};

        int[] right_id = {
                R.drawable.right_left, R.drawable.right_right, R.drawable.right_straight, R.drawable.right_up,
                R.drawable.right_down, R.drawable.right_left_up, R.drawable.right_right_up};

        // calibration process
        calibrationImagesLeft = userDataManager.getCalibrationFiles(context, leftEyeFileNames);
        calibrationImagesRight = userDataManager.getCalibrationFiles(context, rightEyeFileNames);

        if (calibrationImagesLeft == null || calibrationImagesRight == null) {
            Log.d("MVPModel", "calibration incomplete");
            // testing, get the bitmaps from the template images
            Bitmap[] calibrateLeft = new Bitmap[tempNum];
            for (int i = 0; i < left_id.length; i++) {
                calibrateLeft[i] = BitmapFactory.decodeResource(context.getResources(), left_id[i]);
            }
            Bitmap[] calibrateRight = new Bitmap[tempNum];
            for (int i = 0; i < right_id.length; i++) {
                calibrateRight[i] = BitmapFactory.decodeResource(context.getResources(), right_id[i]);
            }
            userDataManager.storeCalibrationFiles(context, leftEyeFileNames, calibrateLeft);
            userDataManager.storeCalibrationFiles(context, rightEyeFileNames, calibrateRight);
            Log.d("MVPModel", "Calibration data stored");
        } else {
            Log.d("MVPModel", "calibration data found, complete");
        }
    }

    @Override
    public void analyzeGazeOutput() { // determine whether input is valid or not based on raw data of both eyes

        if (!detectionOutput.LeftData.Success && !detectionOutput.RightData.Success) { // both eyes not available
            detectionOutput.AnalyzedData = detectionOutput.LeftData;

        } else if (detectionOutput.LeftData.Success && !detectionOutput.RightData.Success) { // only left eye available
            detectionOutput.AnalyzedData = detectionOutput.LeftData;

        } else if (!detectionOutput.LeftData.Success) { // only right eye available
            detectionOutput.AnalyzedData = detectionOutput.RightData;

        } else { // if both eyes are successful, take the gaze input with the lower loss
            if (detectionOutput.LeftData.GazeProbability <= detectionOutput.RightData.GazeProbability) {
                detectionOutput.AnalyzedData = detectionOutput.LeftData;
            } else {
                detectionOutput.AnalyzedData = detectionOutput.RightData;
            }
        }

        detectionOutput.gestureOutput = 0;
        if (detectionOutput.AnalyzedData.Success) { // add the analyzed data into queue
            int type = detectionOutput.AnalyzedData.GazeType;
            window.add(type);
            gazeCount[type] += 1;
        }
        if (window.size() == 12) { // enough frames to determine input
            int index = 0, max = -1;
            for (int i = 0; i < gazeCount.length; i++) { // get max in array
                if (gazeCount[i] > max) {
                    max = gazeCount[i];
                    index = i;
                }
            }
            //Log.d("MVPModel", Arrays.toString(gazeCount));
            String gazeType = detectionOutput.AnalyzedData.getTypeString(index);
            if (max >= 5 && !Objects.equals(gazeType, "Straight")) { // more than half of the inputs, not straight, counts
                prevInputs.add(gazeType);
                detectionOutput.prevInputs = prevInputs;
                detectionOutput.gestureOutput = index;

                if (prevInputs.size() > 10) {
                    prevInputs.clear();
                }
                window.clear(); // clear window after input detected
                gazeCount = new int[gazeNum];
            } else {
                int lastIndex = window.poll(); // get the last element out
                gazeCount[lastIndex] -= 1; // decrease number
            }
        }

    }

    private Rect getBoundingBox(List<PointF> points, Mat mat) {

        int INF = 100000;
        int maxX = -1, maxY = -1, minX = INF, minY = INF;

        for (int i = 0; i < points.size(); i++) {
            maxX = Math.max(maxX, (int) points.get(i).x);
            maxY = Math.max(maxY, (int) points.get(i).y);
            minX = Math.min(minX, (int) points.get(i).x);
            minY = Math.min(minY, (int) points.get(i).y);
        }

        Rect boundingBox;
        if (minX >= 0 && minY >= 0 && maxX < mat.cols() && maxY < mat.rows() && minX < maxX && minY < maxY) { // contour is valid
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
                Rect leftEyeBound = getBoundingBox(leftEyePoints, rgbMat);
                if (leftEyeBound != null) {
                  //  Log.d("MVPModel", "Rect Dimensions: " + leftEyeBound.x + ' ' + leftEyeBound.y + ' ' + leftEyeBound.height + ' ' + leftEyeBound.width);
                    leftEye = new Mat(rgbMat, leftEyeBound);
                    Imgproc.cvtColor(leftEye, leftEye, Imgproc.COLOR_RGB2GRAY);
                    detectionOutput = detector.runEyeModel(detectionOutput, leftEye, 0);
                }
            }
            if (faceDetector.rightEyeContour != null) { // right eye available
                List<PointF> rightEyePoints = faceDetector.rightEyeContour.getPoints();
                Rect rightEyeBound = getBoundingBox(rightEyePoints, rgbMat);
                if (rightEyeBound != null) {
                  //  Log.d("MVPModel", "Rect Dimensions: " + rightEyeBound.x + ' ' + rightEyeBound.y + ' ' + rightEyeBound.height + ' ' + rightEyeBound.width);
                    rightEye = new Mat(rgbMat, rightEyeBound);
                    Imgproc.cvtColor(rightEye, rightEye, Imgproc.COLOR_RGB2GRAY);
                    detectionOutput = detector.runEyeModel(detectionOutput, rightEye, 1);
                }
            }
        }
        //testing data
        detectionOutput.testingMats[0] = leftEye;
        detectionOutput.testingMats[1] = rightEye;
        analyzeGazeOutput(); // analyze the output before returning to the presenter
        return detectionOutput;
    }
}
