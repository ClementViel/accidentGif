package com.example.accidentgif

import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import com.example.accidentgif.GifMaker
private const val TAG = "setting activity"

class SettingsActivity : AppCompatActivity() {

    private var gif = GifMaker.getInstance()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragmentContainerView, SettingsFragment())
            .commit()

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
                    gif.notifyPreferenceChanged(key.toString(), sharedPreferences.getString(key, "").toString())


            }
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
    }




}