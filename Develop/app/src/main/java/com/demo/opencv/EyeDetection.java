package com.demo.opencv;

import static org.opencv.core.Core.normalize;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
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
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;

public class EyeDetection extends AppCompatActivity {
    private CascadeClassifier faceDetector, eyeDetector;
    private Interpreter gazeClassifier;
    public Context mContext;
    public Mat eyeROI, faceROI, originalImage, squaredROI, rightFaceROI, leftFaceROI;
    public GazeInput gazeInput;
    Mat[] templates;
    String[] tags;
    Rect[] eyesArray, facesArray;
    int IMAGE_SIZE = 96;
    double GESTURE_DETECT_THRESHOLD = 30;
    public Mat rotateMat(Mat mat, double angle) {

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

    public void loadTensorModel() throws IOException {
        ByteBuffer model = loadModelFile();
        Interpreter.Options options = new Interpreter.Options();
        gazeClassifier = new Interpreter(model, options);
        Log.d("TensorModel", "loaded tensorflow lite model");
    }

    private void loadTemplates(int index, int id) {
        Bitmap bm = BitmapFactory.decodeResource(mContext.getResources(), id);
        templates[index] = new Mat(bm.getWidth(), bm.getHeight(), CvType.CV_8UC4);
        Utils.bitmapToMat(bm, templates[index]);
        Imgproc.cvtColor(templates[index], templates[index], Imgproc.COLOR_BGR2GRAY);
        Imgproc.resize(templates[index], templates[index], new Size(IMAGE_SIZE,IMAGE_SIZE), Imgproc.INTER_AREA);
    }

    public void loadOpenCVModels(int[] files) { // use input output stream to write CascadeClassifier

        templates = new Mat[6];
        int[] id = {R.drawable.left, R.drawable.straight, R.drawable.right,
                R.drawable.left2, R.drawable.straight2, R.drawable.right2};
        for (int i = 0; i < 6; i++) {
            loadTemplates(i, id[i]);
        }
        tags = new String[]{"Left", "Straight", "Right", "Left", "Straight", "Right"};

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
        double sum = 0;
        Mat difMat = new Mat(h, w, CvType.CV_8UC4);
        Imgproc.cvtColor(difMat, difMat, Imgproc.COLOR_BGR2GRAY);
       // Log.d("EyeDetection", a.channels() + " " + b.channels() + " " + difMat.channels());
        Core.subtract(a, b, difMat);
        Mat destSquared = difMat.mul(difMat);
        Scalar s = Core.sumElems(destSquared);
        double d = (float) h * w;
        sum = s.val[0] / d;
        return sum;
    }
    private void runModel() {
        // create mat resized with black background
        Mat squareMat = new Mat(eyeROI.cols(), eyeROI.cols(), Imgproc.COLOR_BGR2GRAY);
        squareMat.convertTo(squareMat,CvType.CV_8UC4);

        // create ROI for resized and add the eye in the middle
        int startY = eyeROI.cols() / 2 - eyeROI.rows() / 2;
        Rect ROI = new Rect(0,startY, eyeROI.cols(), eyeROI.rows());
        eyeROI.copyTo(squareMat.submat(ROI));

        // resize the mat to the correct image size
        Mat tensorMat = new Mat(IMAGE_SIZE, IMAGE_SIZE, CvType.CV_8UC4);
        Imgproc.resize(squareMat, tensorMat, new Size(IMAGE_SIZE, IMAGE_SIZE), Imgproc.INTER_AREA);

        // MSE
        double minError = 10000000;
        int index = 0;
        for (int i = 0; i < 3; i++) {
            double sum = mse(templates[i], tensorMat, IMAGE_SIZE, IMAGE_SIZE);
            if (sum < minError) {
                minError = sum;
                index = i;
            }
        }
        squaredROI = squareMat;
        gazeInput.gazeType = tags[index];
        gazeInput.gazeLength = 1;
        gazeInput.gazeProbability = (float)minError;
        if (minError <= GESTURE_DETECT_THRESHOLD) {
            gazeInput.Success = true;
        } else {
            gazeInput.Success = false;
        }

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
    }
    private Point add(Point a, Point b) {
        Point z = new Point();
        z.x = a.x + b.x;
        z.y = a.y + b.y;
        return z;
    }
    private MatOfPoint find_max_contour(Mat targetMat, Mat drawMat) {
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
    public void eyeball_detection(Rect eye) {

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

        eyeROI = new Mat(faceROI, newEye);
    }

    private void eye_detection(Rect face) {
        faceROI = new Mat(originalImage, face); // roi with only the face
        MatOfRect eyesDetected = new MatOfRect();
        int minEyeSize = Math.round(faceROI.rows() * 0.03f); // TODO: adjust

        eyeDetector.detectMultiScale(faceROI,
                eyesDetected,
                1.1,
                3,
                1,
                new Size(minEyeSize, minEyeSize),
                new Size()
        );

        eyesArray = eyesDetected.toArray();
    }
    public GazeInput inputDetection(Mat inputImage) {
        if (faceDetector == null || eyeDetector == null) {
            Log.d("EyeDetectionCheck", "Waiting for Cascade to load");
            return null;
        }
        gazeInput = new GazeInput();
        gazeInput.Success = false;
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

            //split face into half and detect eyes separately
            eye_detection(face);

            int eyeCount = eyesArray.length;
            if (eyeCount < 1) {
                Log.d("EyeDetectionCheck","Insufficient Eyes Detected");
            } else if (eyeCount == 1 || eyeCount == 2) {
                Log.d("EyeDetectionCheck","1 or 2 eyes detected, proceeding...");
                eyeball_detection(eyesArray[0]);
            } else {
                Log.d("EyeDetectionCheck","More than two eyes detected: " + eyeCount);
            }
            squaredROI = new Mat();
            runModel();

            // draw rectangles last
            Imgproc.rectangle(originalImage, add(eyesArray[0].tl(), face.tl()), add(eyesArray[0].br(), face.tl()), new Scalar(255,0,0), 3);
            Imgproc.rectangle(originalImage, face.tl(), face.br(), new Scalar(0, 0, 255), 3);
        } else {
            Log.d("EyeDetectionCheck","More than one face detected: " + faceCount);
        }
        return gazeInput;
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
