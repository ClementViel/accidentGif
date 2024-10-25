package com.example.accidentgif

import android.content.Context
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
            val session = FFmpegKit.execute(ffmpeg_command)
            if (session.returnCode.isValueSuccess) {
                num_gif_session++
            } else {
                num_gif_session = 0
            }
            num_saved = 0
            callback()
        }
    }
     fun takePhoto() {
         photoCount++
         createDirectory(outPath)

        // Get a stable reference of the modifiable image capture use case
        // Create time stamped name and MediaStore entry.
        val name = "IMG-$photoCount"
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(FileOutputStream("${outPath}/${name}.jpeg")).build()

        // Set up image capture listener, which is triggered after photo has
        // been taken

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
        val gif_name = "res_gif${num_gif_session}"
        num_saved++
        // On pics_completed trigger gif
        if (num_saved == picNum+1) {
            creatGif(" -y -framerate ${framerate} -f image2 -i '/storage/emulated/0/Pictures/gif4000/gif/IMG-%d.jpeg' -vf scale=768x1020 ${dir}/${gif_name}.gif", callback)
            photoCount = 0
            deleteDirectory("${dir}/gif")
        }
    }

    fun notifyPreferenceChanged(key: String, value: String) {
        if (key == "gif number") {
            gif_num = value.toInt()
        } else if (key == "picture number"){
            picNum = value.toInt()
        } else if (key == "framerate") {
            framerate = value.toInt()
        } else {
            Log.e(TAG, "Preference ${key} Not supported")
        }
    }
}