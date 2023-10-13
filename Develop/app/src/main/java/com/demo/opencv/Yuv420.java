package com.demo.opencv;

import android.annotation.TargetApi;
import android.media.Image;
import android.os.Build;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.nio.ByteBuffer;


public class Yuv420 {

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
        // yuv420图片有三个通道，按顺序下来分别对应YUV
        // 转换需要把三个通道的数据按顺序合并在一个数组里，
        // 即全部Y，随后全部U，再随后全部是V，
        // 再由此数组生成Yuv420的Mat，
        // 之后可以利用opencv将其转为其他格式
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
}