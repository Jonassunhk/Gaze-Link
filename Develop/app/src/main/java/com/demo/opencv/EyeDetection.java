package com.demo.opencv;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import org.opencv.objdetect.CascadeClassifier;
import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

import androidx.appcompat.app.AppCompatActivity;

public class EyeDetection extends AppCompatActivity {
    private CascadeClassifier faceDetector, eyeDetector;
    public Context mContext;
    public Mat faceROI, originalImage, squaredROI, rightFaceROI, leftFaceROI, rightEyeROI, leftEyeROI, finalMat;
    Mat[] leftTemplates, rightTemplates;
    Integer[] tags;
    int tempNum = 7;
    static int IMAGE_WIDTH = 40, IMAGE_HEIGHT = 14;

    public void loadTensorModel() throws IOException {
        ByteBuffer model = loadModelFile();
        Interpreter.Options options = new Interpreter.Options();
        Interpreter gazeClassifier = new Interpreter(model, options);
        Log.d("TensorModel", "loaded tensorflow lite model");
    }

    private void loadTemplates(Mat[] templates, int index, int id) {
        Bitmap bm = BitmapFactory.decodeResource(mContext.getResources(), id);
        templates[index] = new Mat(bm.getWidth(), bm.getHeight(), CvType.CV_8UC4);
        Utils.bitmapToMat(bm, templates[index]);
        Imgproc.cvtColor(templates[index], templates[index], Imgproc.COLOR_BGR2GRAY);
        Imgproc.resize(templates[index], templates[index], new Size(IMAGE_WIDTH,IMAGE_HEIGHT), Imgproc.INTER_AREA);
    }

    public void loadCalibratedTemplates(boolean left, Bitmap[] bm) {
        Mat[] templates;
        if (left) {
            templates = leftTemplates;
        } else {
            templates = rightTemplates;
        }
        for (int i = 0; i < tempNum; i++) {
            templates[i] = new Mat(bm[i].getWidth(), bm[i].getHeight(), CvType.CV_8UC4);
            Utils.bitmapToMat(bm[i], templates[i]);
            Imgproc.cvtColor(templates[i], templates[i], Imgproc.COLOR_BGR2GRAY);
            Imgproc.resize(templates[i], templates[i], new Size(IMAGE_WIDTH,IMAGE_HEIGHT), Imgproc.INTER_AREA);
        }
    }

    public void loadOpenCVModels(int[] files) { // use input output stream to write CascadeClassifier

        leftTemplates = new Mat[tempNum];
        rightTemplates = new Mat[tempNum];

//        int[] left_id = {
//                R.drawable.left_left, R.drawable.left_left2,
//                R.drawable.left_right, R.drawable.left_right2,
//                R.drawable.left_straight, R.drawable.left_straight2,
//                R.drawable.left_up, R.drawable.left_up2,
//                R.drawable.left_down, R.drawable.left_down2,
//                R.drawable.left_left_up, R.drawable.left_left_up2,
//                R.drawable.left_right_up, R.drawable.left_right_up2};
//
//        int[] right_id = {
//                R.drawable.right_left, R.drawable.right_left2,
//                R.drawable.right_right, R.drawable.right_right2,
//                R.drawable.right_straight, R.drawable.right_straight2,
//                R.drawable.right_up, R.drawable.right_up2,
//                R.drawable.right_down, R.drawable.right_down2,
//                R.drawable.right_left_up, R.drawable.right_left_up2,
//                R.drawable.right_right_up, R.drawable.right_right_up2};

//        for (int i = 0; i < tempNum; i++) {
//            loadTemplates(leftTemplates, i, left_id[i]);
//            loadTemplates(rightTemplates, i, right_id[i]);
//        }

        tags = new Integer[]{1, 2, 0, 3, 4, 6, 7}; // should be flipped because of perspective

        Resources res = mContext.getResources();
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
                else if (code == R.raw.haarcascade_lefteye_2splits) eyeDetector = new CascadeClassifier(file.getAbsolutePath());

                inputStream.close();
                fileOutputStream.close();
                file.delete();

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
    private ByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor fileDescriptor = mContext.getAssets().openFd("gazeClassifierWebb.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private double mse(Mat a, Mat b, int h, int w) { // minimum squared error calculations
        double sum;
        Mat difMat = new Mat(h, w, CvType.CV_8UC4);
        Imgproc.cvtColor(difMat, difMat, Imgproc.COLOR_BGR2GRAY);
        Mat norA = new Mat();
        Mat norB = new Mat();
        a.convertTo(norA, CvType.CV_32F, 1.0/255, 0);
        b.convertTo(norB, CvType.CV_32F, 1.0/255, 0);
        //Log.d("EyeDetection", a.channels() + " " + b.channels() + " " + difMat.channels());
        Core.subtract(norA, norB, difMat);
        Mat destSquared = difMat.mul(difMat);
        Scalar s = Core.sumElems(destSquared);
        double d = (float) h * w;
        sum = s.val[0] / d;
        return sum;
    }
    public double[] runEyeModel(DetectionOutput detectionOutput, Mat eyeROI, int type) {

        Mat[] compareTemplates;
        double[] templateError = new double[tempNum];

        // resize the eyeROI to the correct image size/format
        eyeROI.convertTo(eyeROI, CvType.CV_8UC4);
        Mat tensorMat = new Mat(IMAGE_HEIGHT, IMAGE_WIDTH, CvType.CV_8UC4);
        Imgproc.resize(eyeROI, tensorMat, new Size(IMAGE_WIDTH, IMAGE_HEIGHT), Imgproc.INTER_AREA);

        if (type == 0) {
            compareTemplates = leftTemplates;
            detectionOutput.testingMats[0] = tensorMat;
        } else {
            compareTemplates = rightTemplates;
            detectionOutput.testingMats[1] = tensorMat;
        }
        // MSE
        double minError = 10000000;
        int index = 0;
        for (int i = 0; i < tempNum; i++) {
            double sum = mse(compareTemplates[i], tensorMat, IMAGE_HEIGHT, IMAGE_WIDTH); // - DETECT_BIAS[tags[i]];
            templateError[i] = sum;
            if (sum < minError) {
                minError = sum;
                index = i;
            }
        }

        Boolean success = minError <= 0.02; // get threshold for the specific type
        detectionOutput.setEyeData(type, success, tags[index], 1, (float)minError);

        return templateError;

    }

    public Point irisDetection(Mat ROI) {
        Mat eroded = new Mat();
        Mat threshold = new Mat();
        Mat opening = new Mat();
        Mat hierarchy = new Mat();
        Point irisCenter = new Point();

        Mat erode_kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5));
        //Mat opening_kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5));
        ArrayList<MatOfPoint> contours = new ArrayList<>();

        // image processing
        Imgproc.erode(ROI, eroded, erode_kernel);
        Imgproc.blur(eroded, eroded, new Size(3,3));
       // Imgproc.adaptiveThreshold(eroded, threshold, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 5, 3);
        Imgproc.threshold(eroded, threshold, 30, 255, Imgproc.THRESH_BINARY);
        //Imgproc.morphologyEx(threshold, opening, Imgproc.MORPH_OPEN, opening_kernel);
        Imgproc.blur(threshold, opening, new Size(3,3));

        // contour detection
        Core.bitwise_not(opening, opening);
        Imgproc.findContours(opening, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        double maxVal = -1;
        int maxValIdx = -1;
        for (int i = 0; i < contours.size(); i++) {
            double contourArea = Imgproc.contourArea(contours.get(i));
            if (maxVal < contourArea) {
                maxVal = contourArea;
                maxValIdx = i;
            }
        }
        finalMat = new Mat();
        Mat draw;
        if (maxValIdx != -1) {
            MatOfPoint maxContour = contours.get(maxValIdx);
            Moments moments = Imgproc.moments(maxContour);
            irisCenter.x = moments.get_m10() / moments.get_m00();
            irisCenter.y = moments.get_m01() / moments.get_m00();
            //Imgproc.drawContours(draw, contours, maxValIdx, new Scalar(255,255,255), 2);
            draw = new Mat(ROI.rows(), ROI.cols(), Imgproc.COLOR_BGR2GRAY);
            draw.setTo(new Scalar(0,0,0));
            Imgproc.circle(draw, new Point(irisCenter.x,irisCenter.y), 2, new Scalar(255,255,255));
            draw.convertTo(finalMat, CvType.CV_8UC3);
        }
        return irisCenter;
    }

    private Mat crop_eye_ROI(Mat ROI, Rect eye) {
        //eye ROI cropping: removing the eyebrows
        double topCrop = 0.5f;
        double downCrop = 0.2f;
        double leftCrop = 0.15f;
        double rightCrop = 0.15f;

        int topAmount = (int) Math.round((float) eye.height * topCrop);
        int downAmount = (int) Math.round((float) eye.height * downCrop);
        int leftAmount = (int) Math.round((float) eye.width * leftCrop);
        int rightAmount = (int) Math.round((float) eye.width * rightCrop);

        Rect newEye = new Rect(
                new Point(eye.tl().x + leftAmount, eye.tl().y + topAmount),
                new Point(eye.br().x - rightAmount, eye.br().y - downAmount)
        );

        Mat dst;
        dst = new Mat(ROI, newEye);
        return dst;
    }

    private Rect[] eye_detection(Mat ROI) {

        MatOfRect eyesDetected = new MatOfRect();
        int minEyeSize = Math.round(ROI.rows() * 0.03f); // TODO: adjust

        eyeDetector.detectMultiScale(ROI,
                eyesDetected,
                1.1,
                3,
                1,
                new Size(minEyeSize, minEyeSize),
                new Size()
        );
        return eyesDetected.toArray();
    }
    public Mat faceEyeDetection(Mat inputImage) {

        Rect[] facesArray, leftEyesArray, rightEyesArray;
        squaredROI = new Mat();

        if (faceDetector == null || eyeDetector == null) {
            Log.d("EyeDetectionCheck", "Waiting for Cascade to load");
            return null;
        }

        //Noise Reduction
        originalImage = new Mat(inputImage.rows(), inputImage.cols(), inputImage.type());
        Imgproc.GaussianBlur(inputImage, originalImage, new Size(3,3),0);

        Log.d("EyeDetectionCheck", "Through");
        MatOfRect facesDetected = new MatOfRect();
        int minFaceSize = Math.round(originalImage.rows() * 0.1f); // 0.1f for face, check later to see which one is best
        faceDetector.detectMultiScale(originalImage,
                facesDetected,
                1.1,
                3,
                0,
                new Size(minFaceSize, minFaceSize),
                new Size()
        );

        facesArray = facesDetected.toArray();
        int faceCount = facesArray.length;

        if (faceCount < 1) {
            Log.d("EyeDetectionCheck","No faces detected...");
        } else if (faceCount == 1) {
            Log.d("EyeDetectionCheck","One face detected, proceeding...");
            Rect face = facesArray[0];
            faceROI = new Mat(originalImage, face); // roi with only the face

            //split face into half and detect eyes separately
            Rect right = new Rect(0,0,faceROI.cols()/2, faceROI.rows());
            Rect left = new Rect(faceROI.cols()/2, 0, faceROI.cols()/2, faceROI.rows());
            rightFaceROI = new Mat(faceROI, right);
            leftFaceROI = new Mat(faceROI, left);

            // detect right eye
            rightEyesArray = eye_detection(rightFaceROI);
            if (rightEyesArray.length == 1) {
                Log.d("EyeDetectionCheck","right side eye detected");
                rightEyeROI = crop_eye_ROI(rightFaceROI, rightEyesArray[0]);
            }
            // detect left eye
            leftEyesArray = eye_detection(leftFaceROI);
            if (leftEyesArray.length == 1) {
                Log.d("EyeDetectionCheck","left side eye detected");
                leftEyeROI = crop_eye_ROI(leftFaceROI, leftEyesArray[0]);
            }
            // draw rectangles last
            // Imgproc.rectangle(originalImage, add(leftEyesArray[0].tl(), face.tl()), add(leftEyesArray[0].br(), face.tl()), new Scalar(255,0,0), 3);
            Imgproc.rectangle(originalImage, face.tl(), face.br(), new Scalar(0, 0, 255), 3);
        } else {
            Log.d("EyeDetectionCheck","More than one face detected: " + faceCount);
        }
        return leftEyeROI;
    }
}

// old code for image processing:

//        // Canny + dilate processing
//        Mat eyeMat = new Mat();
//        Mat inverted = new Mat();
//        Mat pupilMat = new Mat();
//
//        Core.bitwise_not(eyeROI, inverted); // invert and then detect white part?
//        Imgproc.GaussianBlur(eyeROI, eyeROI, new Size(blurRadius,blurRadius),0);
//        Imgproc.adaptiveThreshold(eyeROI, eyeMat, 255,Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV,7,2);
//        Imgproc.adaptiveThreshold(eyeROI, pupilMat, 255,Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV,7,7);
//
//        Point eyePoint = new Point(); Point pupilPoint = new Point();
//        Rect eyeRect = new Rect(); Rect pupilRect = new Rect();
//
//        Mat draw = Mat.zeros(eyeROI.size(), CvType.CV_8UC3);
//        MatOfPoint eyeMaxContour = find_max_contour(eyeMat, draw); //find contour of the eye
//        if (eyeMaxContour != null) {
//            eyeRect = Imgproc.boundingRect(eyeMaxContour);
//            Moments moments = Imgproc.moments(eyeMaxContour);
//            eyePoint.x = moments.get_m10() / moments.get_m00();
//            eyePoint.y = moments.get_m01() / moments.get_m00();
//        }
//        Imgproc.circle(draw, eyePoint, 1, new Scalar(255,255,255));
//        Imgproc.rectangle(draw, eyeRect.tl(), eyeRect.br(), new Scalar(0, 255, 0), 1);
//
//        MatOfPoint pupilMaxContour = find_max_contour(pupilMat, draw); //find contour of the eye
//        if (pupilMaxContour != null) {
//            pupilRect = Imgproc.boundingRect(pupilMaxContour);
//            Moments moments = Imgproc.moments(pupilMaxContour);
//            pupilPoint.x = moments.get_m10() / moments.get_m00();
//            pupilPoint.y = moments.get_m01() / moments.get_m00();
//        }
//        Imgproc.circle(draw, pupilPoint, 1, new Scalar(255,255,255));
//        Imgproc.rectangle(draw, pupilRect.tl(), pupilRect.br(), new Scalar(0, 255, 0), 1);

//Features2d.drawKeypoints(eyeMat, keyPoints, eyeMat, new Scalar(0,255,0), Features2d.DrawMatchesFlags_DRAW_RICH_KEYPOINTS);
//display_mat(eyeMat, image3);
//display_mat(draw, display);

// old code for tensorflow model:
/*
        Bitmap dst = Bitmap.createBitmap(tensorMat.width(), tensorMat.height(), Bitmap.Config.ARGB_8888);
        tensorMat.convertTo(tensorMat,CvType.CV_8UC4);
        Utils.matToBitmap(tensorMat, dst);

        ByteBuffer buffer = ByteBuffer.allocateDirect(IMAGE_SIZE * IMAGE_SIZE * 4).order(ByteOrder.nativeOrder());

        int[] pixels = new int[IMAGE_SIZE*IMAGE_SIZE];
        dst.getPixels(pixels,0,IMAGE_SIZE,0,0,IMAGE_SIZE,IMAGE_SIZE);

        for (int pixel: pixels) {
            buffer.putFloat((float) (Color.red(pixel) / 255.0));
        }

        int bufferSize = 3 * java.lang.Float.SIZE / java.lang.Byte.SIZE;
        ByteBuffer modelOutput = ByteBuffer.allocateDirect(bufferSize).order(ByteOrder.nativeOrder());

        gazeClassifier.run(buffer, modelOutput);

        modelOutput.rewind();
        FloatBuffer probabilities = modelOutput.asFloatBuffer();

        float maxProbability = 0;
        int type = 0;
        String[] labels = {"left", "straight", "right"};

        for (int i = 0; i < probabilities.capacity(); i++) {
            float probability = probabilities.get(i);
            Log.d("TensorModel", "Probability for class " + i + " is " + probability);
            if (probability > maxProbability) {
                maxProbability = probability;
                type = i;
            }
        }
        */