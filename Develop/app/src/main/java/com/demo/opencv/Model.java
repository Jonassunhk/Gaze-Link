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
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
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
    int templateNum = 8;
    int templateSuccessRate = 7;
    int[] gazeCount;
    static int IMAGE_WIDTH = 44, IMAGE_HEIGHT = 18;
    String[] leftEyeFileNames = {"left_left", "left_right", "left_straight", "left_up", "left_down", "left_left_up", "left_right_up"};
    String[] rightEyeFileNames = {"right_left", "right_right", "right_straight", "right_up", "right_down", "right_left_up", "right_right_up"};
    public Bitmap[] leftCalibrationData, rightCalibrationData;
    Integer[] tags = new Integer[]{1, 2, 0, 3, 4, 6, 7}; // should be flipped because of perspective
    double[] leftTemplateError = new double[tempNum];
    double[] rightTemplateError = new double[tempNum];
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

        // calibration process
        leftCalibrationData = userDataManager.getCalibrationFiles(context, leftEyeFileNames);
        rightCalibrationData = userDataManager.getCalibrationFiles(context, rightEyeFileNames);

        if (leftCalibrationData == null || rightCalibrationData == null) {
            Log.d("Calibration", "calibration incomplete");
        } else {
            detector.loadCalibratedTemplates(true, leftCalibrationData);
            detector.loadCalibratedTemplates(false, rightCalibrationData);
            Log.d("Calibration", "calibration data found, complete");
        }
    }

    private boolean gazingLeft(GazeData gaze) {
        return gaze.GazeType == 1 || gaze.GazeType == 6;
    }

    private boolean gazingRight(GazeData gaze) {
        return gaze.GazeType == 2 || gaze.GazeType == 7;
    }

    @Override
    public void setCalibrationTemplates(Context context, Bitmap[] leftEye, Bitmap[] rightEye) { // import calibration images
        leftCalibrationData = leftEye;
        rightCalibrationData = rightEye;
        userDataManager.storeCalibrationFiles(context, leftEyeFileNames, leftEye); // data store
        userDataManager.storeCalibrationFiles(context, rightEyeFileNames, rightEye); // data store
        detector.loadCalibratedTemplates(true, leftEye);
        detector.loadCalibratedTemplates(false, rightEye);
    }

    @Override
    public void analyzeGazeOutput() { // determine whether input is valid or not based on raw data of both eyes

        if (!detectionOutput.LeftData.Success && !detectionOutput.RightData.Success) { // both eyes not available
            detectionOutput.AnalyzedData = detectionOutput.LeftData;

        } else if (detectionOutput.LeftData.Success && !detectionOutput.RightData.Success) { // only left eye available
            detectionOutput.AnalyzedData = detectionOutput.LeftData;

        } else if (!detectionOutput.LeftData.Success) { // only right eye available
            detectionOutput.AnalyzedData = detectionOutput.RightData;

        } else { // if both eyes are successful, add the two loss and take the lowest

            GazeData leftGazeData = detectionOutput.LeftData;
            GazeData rightGazeData = detectionOutput.RightData;

            if (leftGazeData.GazeType == 5 && rightGazeData.GazeType == 5) { // eyes closed
                detectionOutput.AnalyzedData = rightGazeData;

            } else if (gazingLeft(leftGazeData) && gazingLeft(rightGazeData)) { // eyes looking left, take left eye
                detectionOutput.AnalyzedData = leftGazeData;

            } else if (gazingRight(leftGazeData) && gazingRight(rightGazeData)) { // eyes looking right, take right eye
                detectionOutput.AnalyzedData = rightGazeData;

            } else { // combine the two losses
                int index = -1;
                double minError = 1000000f;
                for (int i = 0; i < tempNum; i++) {
                    double error = leftTemplateError[i] + rightTemplateError[i];
                    if (error < minError) {
                        index = i;
                        minError = error;
                    }
                }
                boolean success = minError <= 0;
                detectionOutput.setEyeData(2, success, tags[index], 1, (float)minError);
            }

        }

        detectionOutput.gestureOutput = 0;
        if (detectionOutput.AnalyzedData.Success) { // add the analyzed data into queue
            int type = detectionOutput.AnalyzedData.GazeType;
            window.add(type);
            gazeCount[type] += 1;
        }
        if (window.size() == templateNum) { // enough frames to determine input
            int index = 0, max = -1;
            for (int i = 0; i < gazeCount.length; i++) { // get max in array
                if (gazeCount[i] > max) {
                    max = gazeCount[i];
                    index = i;
                }
            }
            //Log.d("MVPModel", Arrays.toString(gazeCount));
            String gazeType = detectionOutput.AnalyzedData.getTypeString(index);
            if (max >= templateSuccessRate && !Objects.equals(gazeType, "Straight")) { // more than half of the inputs, not straight, counts
                if (prevInputs.size() > 20) {
                    prevInputs.clear();
                }
                prevInputs.add(gazeType);
                detectionOutput.prevInputs = prevInputs;
                detectionOutput.gestureOutput = index;

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

        maxX += 3;
        maxY += 3;
        minX -= 3;
        minY -= 3;
//        maxX = (int) points.get(8).x;
//        maxY = (int) points.get(12).y;
//        minX = (int) points.get(0).x;
//        minY = (int) points.get(4).y;

        Rect boundingBox;
        if (minX >= 0 && minY >= 0 && maxX < mat.cols() && maxY < mat.rows() && minX < maxX && minY < maxY) { // contour is valid
            boundingBox = new Rect(new Point(minX, minY), new Point(maxX, maxY));
            return boundingBox;
        } else {
            return null;
        }
    }

    private Rect getSurroundBox(PointF point, Mat mat) {
        Rect boundingBox;
        int minX = (int) point.x - IMAGE_WIDTH / 2;
        int maxX = (int) point.x + IMAGE_WIDTH / 2;
        int minY = (int) point.y - IMAGE_HEIGHT / 2;
        int maxY = (int) point.y + IMAGE_HEIGHT / 2;
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
        //Mat grayMat = new Mat();
        //Imgproc.cvtColor(rgbMat, grayMat, Imgproc.COLOR_RGB2GRAY);
        //Mat openCVLeft = detector.faceEyeDetection(grayMat); // detect gaze

        // google ml kit detection
        Mat rgbaMat = new Mat(rgbMat.cols(), rgbMat.rows(), CvType.CV_8UC4);
        Imgproc.cvtColor(rgbMat, rgbaMat, Imgproc.COLOR_BGR2RGBA);
        bmp = Bitmap.createBitmap(rgbaMat.cols(), rgbaMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(rgbaMat, bmp);
        faceDetector.detect(bmp);

        // initialization
        detectionOutput.initialize(2);
        
        if (faceDetector.leftEyeContour == null && faceDetector.rightEyeContour == null) { // no eye detection
            return detectionOutput;
        
        } else if (faceDetector.leftEyeOpenProb <= 0.1 && faceDetector.rightEyeOpenProb <= 0.1) { // check if eyes are closed
            detectionOutput.setEyeData(0, true, 5, 1, faceDetector.leftEyeOpenProb);
            detectionOutput.setEyeData(1, true, 5, 1, faceDetector.rightEyeOpenProb);
        } else {
            if (faceDetector.leftEyeContour != null) { // left eye available

                List<PointF> leftEyePoints = faceDetector.leftEyeContour.getPoints();
                Rect leftEyeBound = getBoundingBox(leftEyePoints, rgbMat);

                //Rect leftEyeBound = getSurroundBox(faceDetector.leftEyePos, rgbMat);
                if (leftEyeBound != null) {
                  //  Log.d("MVPModel", "Rect Dimensions: " + leftEyeBound.x + ' ' + leftEyeBound.y + ' ' + leftEyeBound.height + ' ' + leftEyeBound.width);
                    leftEye = new Mat(rgbMat, leftEyeBound);
                    Log.d("MVPModel", "Image Dimensions: " + leftEye.cols() + " " + leftEye.rows());
                    Imgproc.resize(leftEye, leftEye, new Size(IMAGE_WIDTH, IMAGE_HEIGHT), Imgproc.INTER_LINEAR);
                    Imgproc.cvtColor(leftEye, leftEye, Imgproc.COLOR_RGB2GRAY);
                    //Imgproc.blur(leftEye, leftEye, new Size(3,3));

                    if (leftCalibrationData != null) { // if there are calibration images
                        leftTemplateError = detector.runEyeModel(detectionOutput, leftEye, 0);
                    }
                }
            }
            if (faceDetector.rightEyeContour != null) { // right eye available

                List<PointF> rightEyePoints = faceDetector.rightEyeContour.getPoints();
                Rect rightEyeBound = getBoundingBox(rightEyePoints, rgbMat);

                //Rect rightEyeBound = getSurroundBox(faceDetector.rightEyePos, rgbMat);
                if (rightEyeBound != null) {
                  //  Log.d("MVPModel", "Rect Dimensions: " + rightEyeBound.x + ' ' + rightEyeBound.y + ' ' + rightEyeBound.height + ' ' + rightEyeBound.width);
                    rightEye = new Mat(rgbMat, rightEyeBound);
                    Imgproc.resize(rightEye, rightEye, new Size(IMAGE_WIDTH, IMAGE_HEIGHT), Imgproc.INTER_LINEAR);
                    Imgproc.cvtColor(rightEye, rightEye, Imgproc.COLOR_RGB2GRAY);
                    //Imgproc.blur(rightEye, rightEye, new Size(3,3));

                    if (rightCalibrationData != null) { // if there are calibration images
                        rightTemplateError = detector.runEyeModel(detectionOutput, rightEye, 1);
                    }
                }
            }
        }


        //testing data
        detectionOutput.testingMats[0] = leftEye;
        detectionOutput.testingMats[1] = rightEye;
        analyzeGazeOutput(); // analyze the output before returning to the presenter
        return detectionOutput;
    }

    @Override
    public Bitmap[] getLeftCalibrationData() {
        return leftCalibrationData;
    }

    @Override
    public Bitmap[] getRightCalibrationData() {
        return rightCalibrationData;
    }
}
