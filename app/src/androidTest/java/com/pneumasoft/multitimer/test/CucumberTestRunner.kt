package com.pneumasoft.multitimer.test

import android.os.Bundle
import io.cucumber.android.runner.CucumberAndroidJUnitRunner
import io.cucumber.junit.CucumberOptions
import java.io.File

@CucumberOptions(
    features = ["features"],
    glue = ["com.pneumasoft.multitimer.test.steps"],
    tags = ["not @wip"]
)
class CucumberTestRunner : CucumberAndroidJUnitRunner() {

    override fun onCreate(bundle: Bundle) {
        bundle.putString("plugin", getPluginConfigurationString())
        // Replace absolute paths with relative ones if needed, or ensure the path is correct for the device
        // But usually, we don't need to manipulate "features" here if it's under assets
        super.onCreate(bundle)
    }

    private fun getPluginConfigurationString(): String {
        val cucumber = "cucumber"
        val separator = "--"
        return "junit:" + getAbsolutePath(cucumber) + separator +
                "html:" + getAbsolutePath(cucumber) + ".html"
    }

    private fun getAbsolutePath(usage: String): String {
        val context = targetContext
        val directory = context.getExternalFilesDir(null)
        return File(directory, usage).absolutePath
    }
}
