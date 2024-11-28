package com.example.accidentgif

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

private const val TAG = "IntentReceiver"

class IntentReceiver : BroadcastReceiver() {
    private val gif = GifMaker.getInstance()

    override fun onReceive(context: Context, intent: Intent) {
                Log.e(TAG, "received ${intent.action}")
                when  (intent.action){
                    "com.example.accidentgif.ACTION_SEND"-> {
                        if (intent.type == "text/plain") {
                            handleText(intent)
                            Log.e(TAG, "catched")
                        } else {
                            Log.e(TAG, "catched but not text")
                        }
                    }
                    "android.bluetooth.device.action.PAIRING_REQUEST" -> {
                        Log.e(TAG, "got a pairing request")
                    }
                    else -> { Log.e(TAG, "not a text: ${intent.action}") }
                }
    }

    private fun handleText(intent: Intent) {
        val command = intent.getStringExtra("command")
        Log.e(TAG, "this is command: $command")
        when (command) {
            "take photo" -> {
                gif.takePhoto()
            }
            "erase" -> {
                GifMaker.deleteDirectory("/storage/emulated/0/Pictures/gif4000")
            }
             else -> Log.e(TAG, "unrecognized command")
        }
    }

}
