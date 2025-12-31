package com.pneumasoft.multitimer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.pneumasoft.multitimer.databinding.ActivityAboutBinding
// import com.pneumasoft.multitimer.BuildConfig // Ha a buildConfig = true működik

class AboutActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Verzió megjelenítése
        try {
            // Biztonságosabb módszer, ha a BuildConfig nem elérhető
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            val versionString = "v${pInfo.versionName}"

            // Ha van 'versionText' id-jű TextView a layoutban:
            binding.versionInfo.text = "Version ${pInfo.versionName}"

            // VAGY ha nincs dedikált TextView, írjuk a Toolbar alcímébe:
            supportActionBar?.subtitle = versionString

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
