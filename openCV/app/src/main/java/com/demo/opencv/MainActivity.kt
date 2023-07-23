package com.demo.opencv

// android and java imports

// opencv imports

// other class imports

import android.Manifest
import android.R.attr.angle
import android.R.attr.src
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.media.Image
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.demo.opencv.databinding.ActivityMainBinding
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.OpenCVLoader
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.imgproc.Imgproc
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


typealias LumaListener = (luma: Double) -> Unit

open class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMainBinding
    private var imageCapture: ImageCapture? = null
    var eyeDetection = EyeDetection()

    private lateinit var cameraExecutor: ExecutorService

    private val mLoaderCallback: BaseLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                SUCCESS -> {
                    Log.i("OpenCVDebug", "OpenCV loaded successfully")
                }
                else -> {
                    super.onManagerConnected(status)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // check and initialize OpenCV
        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCVDebug", "cannot init debug")
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback)
        } else {
            Log.d("OpenCVDebug", "success")
        }

        // Request camera permissions
        if (allPermissionsGranted()) {
            val newContext = this.applicationContext
            eyeDetection.initialize_detector(newContext, this)
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        // Set up the listeners for take photo and video capture buttons
        //viewBinding.imageCaptureButton.setOnClickListener { takePhoto() }
        //viewBinding.videoCaptureButton.setOnClickListener { captureVideo() }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    inner class ImageAnalyzer(private val listener: LumaListener) : ImageAnalysis.Analyzer {

        private fun ByteBuffer.toByteArray(): ByteArray {
            rewind()    // Rewind the buffer to zero
            val data = ByteArray(remaining())
            get(data)   // Copy the buffer into a byte array
            return data // Return the byte array
        }

        @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
        override fun analyze(image: ImageProxy) {
             fun jpeg(image: Image): Mat {
                val plan = image.planes[0]
                val data = plan.buffer
                data.position(0)
                // 从通道读取的图片为JPEG，并不能直接使用，
                // 将其保存在一维数组里
                return Mat(1, data.remaining(), CvType.CV_8U, data)
            }
            try {
                image.image?.let {
                    // ImageProxy uses an ImageReader under the hood:
                    // https://developer.android.com/reference/androidx/camera/core/ImageProxy.html
                    // That has a default format of YUV_420_888 if not changed that's the default
                    // Android camera format.
                    // https://developer.android.com/reference/android/graphics/ImageFormat.html#YUV_420_888
                    // https://developer.android.com/reference/android/media/ImageReader.html

                    // Sanity check
                    if (it.format == ImageFormat.YUV_420_888
                        && it.planes.size == 3
                    ) {
                        val newImage: Image? = image.getImage()
                        if (newImage != null) {
                            Log.d("ImageProcessing", "imageheight: " +  newImage.height)
                        }
                        var rgbaMat = Yuv420.gray(newImage)
                        if (rgbaMat != null) {
                            Log.d("ImageProcessing", "matimageheight: " +  rgbaMat.height())
                        }
                        eyeDetection.detect_eyes(rgbaMat)
                    } else {
                        // Manage other image formats
                        // TODO - https://developer.android.com/reference/android/media/Image.html
                    }
                }
            } catch (ise: IllegalStateException) {
                ise.printStackTrace()
            }

            image.close()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .build()

            val builder = ImageAnalysis.Builder()
            val imageAnalyzer = builder.build()
                .also {
                    it.setAnalyzer(cameraExecutor, ImageAnalyzer { luma ->
                       // Log.d("Luminosity!", "Average luminosity: $luma")
                    })
                }

            // Select front camera as a default
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, imageAnalyzer)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }
}