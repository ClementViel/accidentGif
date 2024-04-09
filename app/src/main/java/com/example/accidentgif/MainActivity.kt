package com.example.accidentgif

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageCapture
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.accidentgif.databinding.ActivityMainBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.Preview
import androidx.camera.core.CameraSelector
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.camera.core.ImageCaptureException
import com.arthenica.ffmpegkit.FFmpegKit
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMainBinding

    private var imageCapture: ImageCapture? = null
    private var pic_num = 20
    private val gifMaker = GifMaker.getInstance()
    private var outputPath = "/storage/emulated/0/Pictures/gif"

    private lateinit var cameraExecutor: ExecutorService
    val enableButton: (()->Unit) = { viewBinding.imageCaptureButton.isEnabled = true; Log.e(TAG, "callback") }

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
        viewBinding.imageCaptureButton.setOnClickListener { triggerGif(pic_num) }
        cameraExecutor = Executors.newSingleThreadExecutor()
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
         viewBinding.imageCaptureButton.isEnabled = false
         for (index in 0..num_pic) {
             takePhoto(index)
         }
    }

    private fun takePhoto(photo_num :Int) {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return
        // Create time stamped name and MediaStore entry.
        val name = "IMG-$photo_num"
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

                override fun onImageSaved(output: ImageCapture.OutputFileResults) =
                    gifMaker.notifySaved(enableButton)
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


  class GifMaker private constructor() {
     companion object {
         private const val TAG = "GifMaker"
         private var num_saved = 0
         private var dir = "/storage/emulated/0/Pictures/gif"
         private var picNum = 20
         @Volatile
         private var instance: GifMaker? = null

         fun getInstance() =
             instance ?: synchronized(this) {
                 instance ?: GifMaker().also { instance = it }
             }

         fun deleteDirectory(directory: File): Boolean {
             var ret = true
             for (file in directory.listFiles()!!) {
                 if (!file.isDirectory && file.name != "test.gif") {
                     Log.e(TAG,"erasing $file.name")
                     ret = file.delete()
                 }
             }
             return ret
         }

        fun creatGif(ffmpeg_command: String, callback: () -> Unit) {
            val session = FFmpegKit.execute(ffmpeg_command)
            if (session.returnCode.isValueSuccess) {
                Log.d(TAG, "success")
            } else {
                Log.e(TAG, "failure")
            }

            if (!deleteDirectory(File(dir))) {
                Log.e(TAG, "failed to remove base images")
            }
            num_saved = 0
            callback()
        }
     }
      fun notifySaved(callback: () -> Unit) {
          num_saved++
          // On pics_completed trigger gif
          Log.e(TAG, "picture saved")
          if (num_saved == picNum+1) {
              creatGif("-loglevel quiet -y -framerate 25 -f image2 -i '/storage/emulated/0/Pictures/gif/IMG-%d.jpeg' -vf scale=531x299,transpose=1 ${dir}/test.gif", callback)
          }
      }

      fun notifyPreferenceChanged(key: String, value: String) {
          if (key =="GIF output path") {
              Log.e(TAG,"dir changed")
              dir = value
          } else if (key == R.string.pic_num.toString()) {
              Log.e(TAG,"dir changed")
              picNum = value.toInt()
          } else {
              Log.e(TAG, "Preference Not supported")
          }
          Log.e(TAG, "dir ${dir} pciture num ${picNum}")
      }
}