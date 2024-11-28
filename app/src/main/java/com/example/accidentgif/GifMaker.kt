package com.example.accidentgif

import android.content.Context
import android.media.MediaScannerConnection
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import com.arthenica.ffmpegkit.FFmpegKit
import java.io.File
import java.io.FileOutputStream

class GifMaker private constructor(imageCapture: ImageCapture?, outputPath: String, callback: () -> Unit, context: Context) {
    var capture = imageCapture
    var outPath = outputPath
    var cb = callback
    var context = context

    companion object {
        private const val TAG = "GifMaker"
        private var photoCount = 0
        private var num_saved = 0
        private var dir = "/storage/emulated/0/Pictures/gif4000/"
        private var picNum = 29
        private var gif_num = 1
        private var framerate = 25
        private var  gif_name = "res0.gif"
        @Volatile
        private var num_gif_session = 0
        @Volatile
        private var instance: GifMaker? = null

        fun getPicNum() : Int {
            return this.picNum
        }
        fun getInstance(imageCapture: ImageCapture?, outputPath: String, callback: () -> Unit, context: Context ) =
            instance ?: synchronized(this) {
                instance ?: GifMaker(imageCapture, outputPath, callback, context).also { instance = it }
            }
        fun getInstance(): GifMaker {
                return instance!!
        }

        fun createDirectory(path: String) {
            try {
                val dir = File(path)
                if (!dir.exists()) {
                    dir.mkdirs()
                }
            } catch (e: Exception) {
                Log.w("creating file error", e.toString())
            }
        }
        fun deleteDirectory(path: String) {
            for (file in File(path).listFiles()!!) {
                if (!file.isDirectory) {
                    file.delete()
                }
            }
            num_gif_session = 0
        }

        fun creatGif(ffmpeg_command: String, callback: () -> Unit) {
           val gifpath = "${dir}/${gif_name}.gif"

            val session = FFmpegKit.execute(ffmpeg_command)
            if (session.returnCode.isValueSuccess) {
                //TODO: change this as global variables for singleton are unsusable
                MediaScannerConnection.scanFile(/* context = */ instance!!.context,
                    /* paths = */ arrayOf(gifpath),
                    /* mimeTypes = */ arrayOf("*/*")
                ) { path, _ -> Log.e(TAG, "${gifpath} scanned") }
            } else {
                Log.e(TAG, "session failed")
                num_gif_session = 0
            }
            num_saved = 0
            callback()
        }
    }

    private fun getGifNumber(path: String) : Int{
        var numFiles = 0
        try {
            val dir = File(path)
            if (dir.exists()) {
                numFiles = dir.listFiles()!!.size
            }
        } catch (e: Exception) {
            Log.w("creating file error", e.toString())
        }
        return numFiles
    }

     fun takePhoto() {
         photoCount++
         createDirectory(outPath)
        // Get a stable reference of the modifiable image capture use case
        // Create time stamped name and MediaStore entry.
        val name = "IMG-$photoCount"
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(FileOutputStream("${outPath}/${name}.jpeg")).build()

        capture!!.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }
                override fun onImageSaved(output: ImageCapture.OutputFileResults) =
                    notifySaved(cb)
            }
        )
    }

    fun notifySaved(callback: () -> Unit) {
        val activity = context as MainActivity
        var numGif = getGifNumber(dir)
        Log.e(TAG, "saved picture is ${num_saved}")
        gif_name = "res_gif${numGif++}"
        num_saved++

        activity.setProgressBarProgress(num_saved)
        // On pics_completed trigger gif
        if (num_saved == picNum+1) {
            creatGif(" -y -framerate ${framerate} -f image2 -i '/storage/emulated/0/Pictures/gif4000/gif/IMG-%d.jpeg' -vf scale=768x1020 ${dir}${gif_name}.gif", callback)
            activity.changeProgressBarText("GIF IT")
            photoCount = 0
            deleteDirectory("${dir}/gif")
        }
    }

    fun notifyPreferenceChanged(key: String, value: String) {
        when(key) {
            "gif number" -> gif_num = value.toInt()
            "picture number" -> picNum = value.toInt()
            "framerate" -> framerate = value.toInt()
            else -> Log.e(TAG, "Preference ${key} Not supported")
        }
    }
}