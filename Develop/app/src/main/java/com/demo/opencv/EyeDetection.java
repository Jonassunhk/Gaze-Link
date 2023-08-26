package com.demo.opencv;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import org.opencv.android.Utils;
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
import java.io.FileNotFoundException;
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
    public Mat eyeROI, faceROI, loadedImage, resized;
    public GazeInput gazeInput;

    //TODO: define number constants for variable adjustments later on
    public double heightCropPercentage = 1.0f;
    int IMAGE_SIZE = 96;
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
        if (gazeClassifier != null) {
            Log.d("TensorModel", "loaded tensorflow lite model");
        }
    }

    public void loadOpenCVModels(int[] files) throws IOException { // use input output stream to write CascadeClassifier

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
    private ByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor fileDescriptor = mContext.getAssets().openFd("gazeClassifier.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }
    private void runModel() {
        Imgproc.resize(eyeROI, resized, new Size(IMAGE_SIZE, IMAGE_SIZE), Imgproc.INTER_AREA);
        resized = rotateMat(resized, 90.00);
        Bitmap dst = Bitmap.createBitmap(resized.width(), resized.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(resized, dst);

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
        gazeInput.gazeType = labels[type];
        gazeInput.gazeLength = 1;
        gazeInput.Success = true;
        //display_text("Decision: " + labels[type], text1);
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
        int amount = (int) Math.round((float) eye.height * heightCropPercentage);
        Rect newEye = new Rect(new Point(eye.tl().x, eye.tl().y + amount), new Point(eye.br().x, eye.br().y - amount));
        eyeROI = new Mat(faceROI, newEye);
        runModel();
        //display_mat(eyeROI, display);
    }

    private Mat eye_detection(Rect face) {
        faceROI = new Mat(loadedImage, face); // roi with only the face
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

        Rect[] eyesArray = eyesDetected.toArray();
        int size = eyesArray.length;
        if (size < 2) {
            Log.d("EyeDetectionCheck","Insufficient Eyes Detected");
        } else if (size == 2) {
            Log.d("EyeDetectionCheck","Both eyes detected, proceeding...");
            eyeball_detection(eyesArray[0]);
           // eyeball_detection(imageROI, eyesArray[1],image3);
            Imgproc.rectangle(loadedImage, add(eyesArray[0].tl(), face.tl()), add(eyesArray[0].br(), face.tl()), new Scalar(255,0,0), 3);
            Imgproc.rectangle(loadedImage, add(eyesArray[1].tl(), face.tl()), add(eyesArray[1].br(), face.tl()), new Scalar(255,0,0), 3);

        } else {
            Log.d("EyeDetectionCheck","More than two eyes detected: " + size);
        }

        return loadedImage;
    }
    public GazeInput inputDetection(Mat inputImage) {
        if (faceDetector == null || eyeDetector == null) {
            Log.d("EyeDetectionCheck", "Waiting for Cascade to load");
            return null;
        }
        //Noise Reduction
        loadedImage = new Mat(inputImage.rows(), inputImage.cols(), inputImage.type());
        Imgproc.GaussianBlur(inputImage, loadedImage, new Size(3,3),0);

        Log.d("EyeDetectionCheck", "Through");
        MatOfRect facesDetected = new MatOfRect();
        int minFaceSize = Math.round(loadedImage.rows() * 0.1f); // 0.1f for face, check later to see which one is best
        faceDetector.detectMultiScale(loadedImage,
                facesDetected,
                1.1,
                3,
                0,
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
            loadedImage = eye_detection(face);
            Imgproc.rectangle(loadedImage, face.tl(), face.br(), new Scalar(0, 0, 255), 3);
        } else {
            Log.d("EyeDetectionCheck","More than one face detected: " + size);
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
