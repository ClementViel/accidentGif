package com.example.accidentgif

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.accidentgif.databinding.ActivityMainBinding
import com.google.common.util.concurrent.ListenableFuture
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMainBinding
    private var imageCapture: ImageCapture? = null
    private var outputPath = "/storage/emulated/0/Pictures/gif4000/gif"
    private var outPath = "/storage/emulated/0/Pictures/gif4000"
    private lateinit var cameraExecutor: ExecutorService
    val enableButton: (()->Unit) = { viewBinding.imageCaptureButton.isEnabled = true; viewBinding.progressBar.visibility = View.INVISIBLE; resetProgressBarText(); Log.e(TAG, "callback") }
    private lateinit var gifmaker: GifMaker
    private var lensFacing: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private val handler = Handler()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        // Request camera permissions
        if (allPermissionsGranted()) {
            Log.e(TAG,"creating activity")
                startCamera()
                Log.e(TAG, "camera has started")

        } else {
            Log.e(TAG, "not granted permissions")
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
            startCamera()
        }

        // Set up the listeners for take photo and video capture buttons
        viewBinding.imageCaptureButton.setOnClickListener { triggerGif() }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            viewBinding.imageShareButton.setOnClickListener { openFolder(outPath) }
        }
        viewBinding.frontCameraButton.setOnClickListener { switchCamera()}
        //Set up listener for sharing button
        cameraExecutor = Executors.newSingleThreadExecutor()
        setSupportActionBar(viewBinding.menuToolbar)
    }

    private fun switchCamera() {
        if (lensFacing == CameraSelector.DEFAULT_BACK_CAMERA) {
            lensFacing = CameraSelector.DEFAULT_FRONT_CAMERA
            viewBinding.frontCameraButton.setText("TA GUEULE")
        } else {
            lensFacing = CameraSelector.DEFAULT_BACK_CAMERA
            viewBinding.frontCameraButton.setText("MA GUEULE")
        }
        startCamera()
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun openFolder(location: String) {
        val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString()
        val uri: Uri = Uri.parse(location)
        Log.e(TAG, "opening ${uri.path.toString()}")
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.setDataAndType(uri, "*/*")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            resultLauncher.launch(intent)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            val gifUri: Uri? = it.data?.data
            Log.e(TAG, "scanning into mediastore ?")

            if (gifUri != null) {
                Log.e(TAG, "scanning into mediastore")
                MediaScannerConnection.scanFile(applicationContext,
                    arrayOf(gifUri.path.toString()),
                    arrayOf("*/*"),
                    MediaScannerConnection.OnScanCompletedListener { path, _ ->
                        // Use the FileProvider to get a content URI
                        val requestFile = File(path)
                        val fileUri: Uri? = try {
                            FileProvider.getUriForFile(
                                this@MainActivity,
                                "com.example.accidentgif",
                                requestFile)
                        } catch (e: IllegalArgumentException) {
                            Log.e("File Selector",
                                "The selected file can't be shared: $requestFile")
                            null
                        }
                        val shareIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_STREAM, gifUri )
                            type = "image/*"
                        }
                        startActivity(Intent.createChooser(shareIntent, "Partage ton GIF là !"))
                    }
                )
            }
        }
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

    fun setProgressBarProgress(step: Int) {
        Log.e(TAG, (step * viewBinding.progressBar.max / GifMaker.getPicNum()).toString() )
        handler.post(kotlinx.coroutines.Runnable {
            viewBinding.imageCaptureButton.text = (step * viewBinding.progressBar.max / GifMaker.getPicNum()).toString() + "%"
        })
    }

    fun changeProgressBarText(text: String) {
        Log.e(TAG, "changing text")
        handler.post(kotlinx.coroutines.Runnable {
            viewBinding.imageCaptureButton.text = text
        })
    }

    fun resetProgressBarText() {
        viewBinding.imageCaptureButton.text = R.string.trigger_gif.toString()
    }

     fun triggerGif() {
         viewBinding.progressBar.visibility = View.VISIBLE
         viewBinding.imageCaptureButton.isEnabled = false
         for (index in 0..GifMaker.getPicNum()) {
             gifmaker.takePhoto()
         }
     }

    private fun createGifInstance() {
        Log.e(TAG, "Gif instance created")
        gifmaker = GifMaker.getInstance(imageCapture!!, outputPath, enableButton, this)
    }

    @SuppressLint("RestrictedApi")
    private fun startCamera(): ListenableFuture<ProcessCameraProvider> {

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
            // Create GifMaker Instance after ImageCapture is built. TODO(exit gracefully)
            createGifInstance()

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, lensFacing, preview, imageCapture)
            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))

        return cameraProviderFuture
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
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }
}

