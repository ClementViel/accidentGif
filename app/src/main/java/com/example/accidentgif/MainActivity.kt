package com.example.accidentgif

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.net.Uri
import android.os.Environment.getExternalStorageDirectory
import android.os.FileUtils
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageCapture
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.accidentgif.databinding.ActivityMainBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.widget.Toast
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.Preview
import androidx.camera.core.CameraSelector
import android.util.Log
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.PermissionChecker
import androidx.fragment.app.commit
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegSession
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Locale

typealias LumaListener = (luma: Double) -> Unit
var name_suffix = 0
class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMainBinding

    private var imageCapture: ImageCapture? = null
    private var pic_num = 5
    private var gifMaker = GifMaker(MediaStore.Images.Media.EXTERNAL_CONTENT_URI.toString(), pic_num)
    private var outputPath = "/storage/emulated/0/Pictures/gif"
    private var recording: Recording? = null

    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        // Request camera permissions
        if (allPermissionsGranted()) {
            Log.e(TAG,"creating activity")
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        // Set up the listeners for take photo and video capture buttons
        viewBinding.imageCaptureButton.setOnClickListener { triggerGif(5) }

        cameraExecutor = Executors.newSingleThreadExecutor()
        Log.e(TAG,"create toolbar")
        setSupportActionBar(viewBinding.menuToolbar)

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean{
        Log.e(TAG,"create menu")
        viewBinding.menuToolbar.inflateMenu(R.menu.right_corner)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            // launch settings activity
            R.id.settings-> {
                Log.e(TAG,"select menu")
                val intent = Intent(this, SettingsActivity()::class.java)
                startActivity(intent)

            }
        }
        return super.onOptionsItemSelected(item)
    }
    private fun triggerGif(num_pic: Int) {
        var fullFileNameOut = "test.gif"
        for (index in 0..num_pic) {
           takePhoto()
        }
    }
    private fun takePhoto() {
            // Get a stable reference of the modifiable image capture use case
            val imageCapture = imageCapture ?: return

            // Create time stamped name and MediaStore entry.
            val name = "IMG-$name_suffix"
            name_suffix++


        val file = File("${outputPath}/${name}.jpeg").createNewFile()
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(FileOutputStream("${outputPath}/${name}.jpeg")).build()

            // Set up image capture listener, which is triggered after photo has
            // been taken
            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exc: ImageCaptureException) {
                        Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    }

                    override fun
                            onImageSaved(output: ImageCapture.OutputFileResults) {
                        gifMaker.notifySaved()

                    }
                }
            )
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
            imageCapture = ImageCapture.Builder().build()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture)


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
        private const val TAG = "Accident GIF"
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


 class GifMaker(var pathToFiles: String,var num_pics: Int) {
     private var pattern: String = "[0-9]+"
     private var num_saved = 0
     private var dir = "/storage/emulated/0/Pictures/gif"

    companion object {
        private const val TAG = "GifMaker"
    }
     fun deleteDirectory(directory: File) {
         for (file in directory.listFiles()) {
             if (!file.isDirectory) {
                 file.delete()
             }
         }
     }
    fun notifySaved() {
        num_saved++;
        // On pics_completed trigger gif
        if (num_saved == num_pics+1) {
            Log.d(TAG, "5 images in $pathToFiles")
            creatGif("-framerate 25 -f image2 -i '/storage/emulated/0/Pictures/gif/IMG-%d.jpeg' -vf scale=531x299,transpose=1 /storage/emulated/0/Pictures/test.gif")
        }
    }
    fun creatGif(ffmpeg_command: String) {
        var session = FFmpegKit.execute(ffmpeg_command)
        if (session.returnCode.isValueSuccess) {
            Log.d(TAG,"success")
        } else {
            Log.e(TAG, "failure")
        }
        deleteDirectory(File(dir))

    }

}