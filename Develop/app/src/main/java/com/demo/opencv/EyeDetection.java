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
import java.util.List;
import java.util.Map;

import androidx.appcompat.app.AppCompatActivity;

public class EyeDetection extends AppCompatActivity {
    private CascadeClassifier faceDetector, eyeDetector;
    private Interpreter gazeClassifier;
    public Context mContext;
    public Mat faceROI, originalImage, squaredROI, rightFaceROI, leftFaceROI, rightEyeROI, leftEyeROI;
    Mat[] leftTemplates, rightTemplates;
    Integer[] tags;
    int tempNum = 14;
    static int IMAGE_WIDTH = 120, IMAGE_HEIGHT = 45;
    Float[] DETECT_THRESHOLDS = {65f, 65f, 65f, 75f, 55f, 0f, 75f, 75f}; // straight, left, right, up, down, closed, leftUp, rightUp (N/A)

    public void loadTensorModel() throws IOException {
        ByteBuffer model = loadModelFile();
        Interpreter.Options options = new Interpreter.Options();
        gazeClassifier = new Interpreter(model, options);
        Log.d("TensorModel", "loaded tensorflow lite model");
    }

    private void loadTemplates(Mat[] templates, int index, int id) {
        Bitmap bm = BitmapFactory.decodeResource(mContext.getResources(), id);
        templates[index] = new Mat(bm.getWidth(), bm.getHeight(), CvType.CV_8UC4);
        Utils.bitmapToMat(bm, templates[index]);
        Imgproc.cvtColor(templates[index], templates[index], Imgproc.COLOR_BGR2GRAY);
        Imgproc.resize(templates[index], templates[index], new Size(IMAGE_WIDTH,IMAGE_HEIGHT), Imgproc.INTER_AREA);
    }

    public void loadOpenCVModels(int[] files) { // use input output stream to write CascadeClassifier

        leftTemplates = new Mat[tempNum];
        rightTemplates = new Mat[tempNum];

        int[] left_id = {
                R.drawable.left_left, R.drawable.left_left2,
                R.drawable.left_right, R.drawable.left_right2,
                R.drawable.left_straight, R.drawable.left_straight2,
                R.drawable.left_up, R.drawable.left_up2,
                R.drawable.left_down, R.drawable.left_down2,
                R.drawable.left_left_up, R.drawable.left_left_up2,
                R.drawable.left_right_up, R.drawable.left_right_up2};

        int[] right_id = {
                R.drawable.right_left, R.drawable.right_left2,
                R.drawable.right_right, R.drawable.right_right2,
                R.drawable.right_straight, R.drawable.right_straight2,
                R.drawable.right_up, R.drawable.right_up2,
                R.drawable.right_down, R.drawable.right_down2,
                R.drawable.right_left_up, R.drawable.right_left_up2,
                R.drawable.right_right_up, R.drawable.right_right_up2};

        for (int i = 0; i < tempNum; i++) {
            loadTemplates(leftTemplates, i, left_id[i]);
            loadTemplates(rightTemplates, i, right_id[i]);
        }
        tags = new Integer[]{2, 2, 1, 1, 0, 0, 3, 3, 4, 4, 7, 7, 6, 6}; // should be flipped because of perspective

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
        Log.d("EyeDetection", a.channels() + " " + b.channels() + " " + difMat.channels());
        Core.subtract(a, b, difMat);
        Mat destSquared = difMat.mul(difMat);
        Scalar s = Core.sumElems(destSquared);
        double d = (float) h * w;
        sum = s.val[0] / d;
        return sum;
    }
    public DetectionOutput runEyeModel(DetectionOutput detectionOutput, Mat eyeROI, int type) {

//        // create mat resized with black background
//        Mat squareMat = new Mat(eyeROI.cols(), eyeROI.cols(), Imgproc.COLOR_BGR2GRAY);
//        squareMat.convertTo(squareMat,CvType.CV_8UC4);
        // create ROI for resized and add the eye in the middle
//        int startY = eyeROI.cols() / 2 - eyeROI.rows() / 2;
//        Rect ROI = new Rect(0,startY, eyeROI.cols(), eyeROI.rows());
//        eyeROI.copyTo(squareMat.submat(ROI));

        eyeROI.convertTo(eyeROI, CvType.CV_8UC4);

        // resize the mat to the correct image size
        Mat tensorMat = new Mat(IMAGE_HEIGHT, IMAGE_WIDTH, CvType.CV_8UC4);
        // squareMat.convertTo(squareMat, -1, 1.4,1.5); // HMC Classroom lighting settings (temporary)
        Imgproc.resize(eyeROI, tensorMat, new Size(IMAGE_WIDTH, IMAGE_HEIGHT), Imgproc.INTER_AREA);

        Mat[] compareTemplates;
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
            double sum = mse(compareTemplates[i], tensorMat, IMAGE_HEIGHT, IMAGE_WIDTH) - DETECT_THRESHOLDS[tags[i]];
            if (sum < minError) {
                minError = sum;
                index = i;
            }
        }

        Boolean success = minError <= 0; // get threshold for the specific type
        detectionOutput.setEyeData(type, success, tags[index], 1, (float)minError);

        return detectionOutput;

    }
    private Point add(Point a, Point b) {
        Point z = new Point();
        z.x = a.x + b.x;
        z.y = a.y + b.y;
        return z;
    }
    private MatOfPoint find_max_contour(Mat targetMat) {
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        MatOfPoint maxContour = new MatOfPoint();
        Imgproc.findContours(targetMat, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        // sorting
        contours.sort((c1, c2) -> (int) (Imgproc.contourArea(c1)- Imgproc.contourArea(c2)));
        // drawing and finding contour
        //Imgproc.drawContours(drawMat, contours, contours.size() - 1, new Scalar(255,0,0), 1, Imgproc.LINE_8, hierarchy, 2, new Point());
        if (contours.size() > 0) {
            maxContour = contours.get(contours.size() - 1);
        }
        return maxContour;
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
    public void faceEyeDetection(Mat inputImage) {

        Rect[] facesArray, leftEyesArray, rightEyesArray;
        squaredROI = new Mat();

        if (faceDetector == null || eyeDetector == null) {
            Log.d("EyeDetectionCheck", "Waiting for Cascade to load");
            return;
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
            if (leftEyesArray.length > 0) {
               // Imgproc.rectangle(originalImage, add(leftEyesArray[0].tl(), face.tl()), add(leftEyesArray[0].br(), face.tl()), new Scalar(255,0,0), 3);
            }
            Imgproc.rectangle(originalImage, face.tl(), face.br(), new Scalar(0, 0, 255), 3);
        } else {
            Log.d("EyeDetectionCheck","More than one face detected: " + faceCount);
        }
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