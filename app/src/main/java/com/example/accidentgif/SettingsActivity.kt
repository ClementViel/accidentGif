package com.example.accidentgif

import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.commit
import androidx.preference.PreferenceManager
private const val TAG = "SETTINGS"
class SettingsActivity : AppCompatActivity(R.layout.activity_settings) {
    var gif = GifMaker.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                replace(R.id.fragmentContainerView,SettingsFragment())
            }
        }


        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
                    gif.notifyPreferenceChanged(key.toString(), sharedPreferences.getString(key, "").toString())


        }
        Log.e(TAG, "rregistering listener")

        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
    }




}