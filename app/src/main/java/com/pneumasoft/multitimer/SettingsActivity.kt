// app/src/main/java/com/pneumasoft/multitimer/SettingsActivity.kt
package com.pneumasoft.multitimer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import android.content.SharedPreferences

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, SettingsFragment())
            .commit()
    }

    class SettingsFragment : PreferenceFragmentCompat(),
        SharedPreferences.OnSharedPreferenceChangeListener {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)
        }

        override fun onResume() {
            super.onResume()
            preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onPause() {
            preferenceManager.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
            super.onPause()
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            if (key == "dark_mode") {
                val value = sharedPreferences?.getString("dark_mode", "system") ?: "system"
                applyTheme(value)
            }
        }

        private fun applyTheme(darkModeValue: String) {
            val mode = when (darkModeValue) {
                "light" -> AppCompatDelegate.MODE_NIGHT_NO
                "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                else -> {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    } else {
                        AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
                    }
                }
            }
            AppCompatDelegate.setDefaultNightMode(mode)
        }
    }
}
