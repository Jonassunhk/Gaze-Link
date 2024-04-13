package com.demo.opencv.vision;

import android.graphics.Bitmap;
import android.media.Image;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import android.content.Context;


public class ImageFormatUtils {

    // 复制uv通道数据
    private static void copy(ByteBuffer data, ByteBuffer uv, int yl) {
        int l = uv.remaining();
        int quarter = yl / 4;
        int half = yl / 2;
        if (l == quarter) {
            // 魅族M8，长度刚好是y的四分之一，直接写入。
            data.put(uv);
        } else if (quarter < l && l <= half) {
            // 华为荣耀，实际读取到的长度是y的(1 / 2 - 1)
            for (int i = 0; i < l; i++) {
                byte b = uv.get();
                if (i % 2 == 0) {
                    data.put(b);
                }
            }
        } else if (l > half) {
            // 未发现此种情况，先预留着
            for (int i = 0; i < l; i++) {
                byte b = uv.get();
                if (i % 4 == 0) {
                    data.put(b);
                }
            }
        }
    }

    public static Mat yuv420(Image image) {
        Image.Plane[] plans = image.getPlanes();
        ByteBuffer y = plans[0].getBuffer();
        ByteBuffer u = plans[1].getBuffer();
        ByteBuffer v = plans[2].getBuffer();
        // 此处需要把postition移到0才能读取
        y.position(0);
        u.position(0);
        v.position(0);
        int yl = y.remaining();
        // yuv420即4个Y对应1个U和一个V，即4:1:1的关系，长度刚好是Y的1.5倍
        ByteBuffer data = ByteBuffer.allocateDirect(yl * 3 / 2);
        // y通道直接全部插入
        data.put(y);
        copy(data, u, yl);
        copy(data, v, yl);
        // 生成Yuv420格式的Mat
        int rows = image.getHeight();
        int cols = image.getWidth();
        return new Mat(rows * 3 / 2, cols, CvType.CV_8UC1, data);
    }

    public static Mat rgb(Image image) {
        Mat yuvMat = yuv420(image);
        int rows = image.getHeight();
        int cols = image.getWidth();
        // RGB三通道，保存采用CV_8UC3
        Mat rgbMat = new Mat(rows, cols, CvType.CV_8UC3);
        // 通过cv::cvtColor将yuv420转换为rgb格式
        Imgproc.cvtColor(yuvMat, rgbMat, Imgproc.COLOR_YUV2RGB_I420);
        // Mat是jni本地对象，释放对象是良好的习惯
        yuvMat.release();
        return rgbMat;
    }

    public static Mat gray(Image image) {
        Mat yuvMat = yuv420(image);
        int rows = image.getHeight();
        int cols = image.getWidth();
        // 灰色只有一个通道，保存采用CV_8UC1
        Mat grayMat = new Mat(rows, cols, CvType.CV_8UC1);
        // 通过cv::cvtColor将yuv420转换为灰色图片
        Imgproc.cvtColor(yuvMat, grayMat, Imgproc.COLOR_YUV2GRAY_I420);
        yuvMat.release();
        return grayMat;
    }

    public static Bitmap matToBitmap(Mat mat) {
            Bitmap bm = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mat,bm);
            return bm;
    }

    public static File saveBitmapAsPng(Context context, Bitmap bitmap, String fileName) {
            // Use the app's internal storage
            File file = new File(context.getFilesDir(), fileName + ".png");
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out); // PNG is a lossless format, the compression factor (100) is ignored
                return file;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            } finally {
                try {
                    if (out != null) out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
    }
}