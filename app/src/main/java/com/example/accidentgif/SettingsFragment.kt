package com.example.accidentgif

import android.content.SharedPreferences
import android.os.Build
import com.example.accidentgif.R
import android.os.Bundle
import android.util.Log
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager



private const val TAG = "FRAG"

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        Log.e(TAG, "fragment")
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

}