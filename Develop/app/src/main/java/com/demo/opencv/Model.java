package com.demo.opencv;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.util.Log;

import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.ExperimentalGetImage;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;


public class Model extends AppCompatActivity implements ContractInterface.Model {
    private final EyeDetection detector = new EyeDetection(); // create eye detection object to analyze user input
    private final GoogleFaceDetector faceDetector = new GoogleFaceDetector();
    DetectionOutput detectionOutput = new DetectionOutput();
    UserDataManager userDataManager = new UserDataManager();
    private ArrayList<String> prevInputs;
    int gazeNum = 8; // number of types of gaze inputs
    int tempNum = 7; // number of gazes
    int currentGaze = -1;
    int length = 0;
    int[] gazeCount;
    static int IMAGE_WIDTH = 44, IMAGE_HEIGHT = 18;
    String[] leftEyeFileNames = {"left_left", "left_right", "left_straight", "left_up", "left_down", "left_left_up", "left_right_up"};
    String[] rightEyeFileNames = {"right_left", "right_right", "right_straight", "right_up", "right_down", "right_left_up", "right_right_up"};
    public Bitmap[] leftCalibrationData, rightCalibrationData;
    Integer[] tags = new Integer[]{1, 2, 0, 3, 4, 6, 7}; // should be flipped because of perspective
    Point[] corners = new Point[4]; // left, top, right, down
    double[] leftTemplateError = new double[tempNum];
    double[] rightTemplateError = new double[tempNum];
    public HashMap<String, String> settings = new HashMap<>();
    String[] storedSettingNames = {"Threshold", "Sensitivity", "TextEntryMode"};

    @Override
    public void initialize(Context context) throws IOException { // initialize models, libraries, and datastore

        if (!OpenCVLoader.initDebug()) { // check and initialize OpenCV
            Log.d("OpenCVDebug", "cannot init debug");
        } else {
            Log.d("OpenCVDebug", "success");
        }
        detector.mContext = context;
        prevInputs = new ArrayList<>();
        gazeCount = new int[gazeNum]; // change based on how many detections there are

        faceDetector.initialize(); // initialize Google ML Kit
        userDataManager.initialize(context); // initialize settings

        // settings
        getSettingData(); // get setting data. Will set to default if no value found
        leftCalibrationData = userDataManager.getCalibrationFiles(context, leftEyeFileNames);
        rightCalibrationData = userDataManager.getCalibrationFiles(context, rightEyeFileNames);

        if (leftCalibrationData == null || rightCalibrationData == null) {
            Log.d("Calibration", "calibration incomplete");
        } else {
            Log.d("Calibration", "calibration data found, complete");
            detector.loadCalibratedTemplates(true, leftCalibrationData);
            detector.loadCalibratedTemplates(false, rightCalibrationData);
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
        Log.d("Calibration", "Setting calibration templates");
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
            if (type == currentGaze) {
                length += 1;
                if (length == 3) { // detected
                    String gazeType = detectionOutput.AnalyzedData.getTypeString(currentGaze);
                    if (prevInputs.size() > 25) {
                        prevInputs.clear();
                    }
                    if (!Objects.equals(gazeType, "Straight")) {
                        prevInputs.add(gazeType);
                    }
                    detectionOutput.prevInputs = prevInputs;
                    detectionOutput.gestureOutput = type;
                }
            } else {
                currentGaze = type;
                length = 0;
            }
        }
    }

    private Rect getBoundingBox(List<PointF> points, Mat mat) {

        int INF = 100000;
        int maxX = -1, maxY = -1, minX = INF, minY = INF;
        int maxXIdx = -1, maxYIdx = -1, minXIdx = -1, minYIdx = -1;

        for (int i = 0; i < points.size(); i++) {
            if ((int) points.get(i).x > maxX) {
                maxX = (int) points.get(i).x;
                maxXIdx = i;
            }
            if ((int) points.get(i).y > maxY) {
                maxY = (int) points.get(i).y;
                maxYIdx = i;
            }
            if ((int) points.get(i).x < minX) {
                minX = (int) points.get(i).x;
                minXIdx = i;
            }
            if ((int) points.get(i).y < minY) {
                minY = (int) points.get(i).y;
                minYIdx = i;
            }
        }

        // slightly enlarging eye ROI
        maxX += 3;
        maxY += 3;
        minX -= 3;
        minY -= 3;
        // to fix resize issue
        float yRatio = IMAGE_HEIGHT / (float) (maxY - minY);
        float xRatio = IMAGE_WIDTH / (float) (maxX - minX);

        if (maxXIdx == -1 || maxYIdx == -1 || minXIdx == -1 || minYIdx == -1) {
            return null;
        }

        // getting all 4 corners of the eye (in relation to the ROI)
        corners[0] = new Point((maxX - points.get(minXIdx).x) * xRatio, (maxY - points.get(minXIdx).y) * yRatio);
        corners[1] = new Point((maxX - points.get(minYIdx).x) * xRatio, (maxY - points.get(minYIdx).y) * yRatio);
        corners[2] = new Point((maxX - points.get(maxXIdx).x) * xRatio, (maxY - points.get(maxXIdx).y) * yRatio);
        corners[3] = new Point((maxX - points.get(maxYIdx).x) * xRatio, (maxY - points.get(maxYIdx).y) * yRatio);

        Log.d("CornerDetection", corners[0].x + " " + corners[1].x + " " + corners[2].x + " " + corners[3].x);
        Log.d("CornerDetection", "Max: " + minX + " " + minY + " " + maxX + " " + maxY);
        Rect boundingBox;
        if (minX >= 0 && minY >= 0 && maxX < mat.cols() && maxY < mat.rows() && minX < maxX && minY < maxY) { // contour is valid
            boundingBox = new Rect(new Point(minX, minY), new Point(maxX, maxY));
            return boundingBox;
        } else {
            return null;
        }
    }

    private Point normalizeIrisCenter(Point irisCenter) { // get the Normalized iris Center with eye corners and the iris center

        double normalizedX = (irisCenter.x - corners[0].x) / (corners[2].x - corners[0].x); // normalized x coordinate
        double normalizedY = (irisCenter.y - corners[1].y) / (corners[3].y - corners[1].y); // normalized y coordinate
        return new Point(normalizedX, normalizedY);
    }

    private Point getIrisCenter(Mat eye) { // getting the normalized iris center (NIC)
        Point normalized = new Point();
        if (eye != null) {
            Point irisCenter = detector.irisDetection(eye);
            Mat irisMat = detector.finalMat;
            for (int i = 0; i < 4; i++) { // testing
                Imgproc.circle(irisMat, corners[i], 2, new Scalar(255,255,255));
            }
            normalized = normalizeIrisCenter(irisCenter);
            detectionOutput.testingMats[2] = detector.opening;
        } else {
            detectionOutput.testingMats[2] = new Mat();
        }
        return normalized;
    }

    @Override
    public DetectionOutput classifyGaze(Mat rgbMat) { @OptIn(markerClass = ExperimentalGetImage.class)

        Mat leftEye = null, rightEye = null;
        Bitmap bmp;

        // google ml kit detection
        Mat rgbaMat = new Mat(rgbMat.cols(), rgbMat.rows(), CvType.CV_8UC4);
        Imgproc.cvtColor(rgbMat, rgbaMat, Imgproc.COLOR_BGR2RGBA);
        bmp = Bitmap.createBitmap(rgbaMat.cols(), rgbaMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(rgbaMat, bmp);
        faceDetector.detect(bmp);

        // initialization
        detectionOutput.initialize(4);
        
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

                    // image processing
                    Imgproc.resize(leftEye, leftEye, new Size(IMAGE_WIDTH, IMAGE_HEIGHT), Imgproc.INTER_LINEAR);
                    Imgproc.cvtColor(leftEye, leftEye, Imgproc.COLOR_RGB2GRAY);
                    //Imgproc.equalizeHist(leftEye, leftEye);

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

                    // image processing
                    Imgproc.resize(rightEye, rightEye, new Size(IMAGE_WIDTH, IMAGE_HEIGHT), Imgproc.INTER_LINEAR);
                    Imgproc.cvtColor(rightEye, rightEye, Imgproc.COLOR_RGB2GRAY);
                    //Imgproc.equalizeHist(rightEye, rightEye);

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

        // NIC detection
        detectionOutput.leftNIC = getIrisCenter(leftEye);
        Log.d("IrisDetection", "Normalized x = " + detectionOutput.leftNIC.x + ", y = " + detectionOutput.leftNIC.y);
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

    @Override
    public void updateSettings(String valueName, String value) { // checks for change and stores the values

        settings.put(valueName, value);
        for (String i: storedSettingNames) { // check if value should be a stored value
            if (Objects.equals(i, valueName)) {
                userDataManager.setString(valueName, value);
            }
        }

        if (Objects.equals(valueName, "Threshold")) {
            detector.thresholdValue = Integer.parseInt(value);
        } else if (Objects.equals(valueName, "Sensitivity")) {
            detector.sensitivity = (float) Integer.parseInt(value) / 1000;
        }
    }

    @Override
    public HashMap<String, String> getSettings() {
        return settings;
    }

    private void getSettingData() {
        for (String i: storedSettingNames) {
            settings.put(i, userDataManager.getString(i));
        }
        settings.put("Context", "");
    }
}
