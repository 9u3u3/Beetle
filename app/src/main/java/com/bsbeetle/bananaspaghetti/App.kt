package com.bsbeetle.bananaspaghetti

import android.app.Application
import com.bsbeetle.bananaspaghetti.utils.diagnostics.CrashReporter
import com.bsbeetle.bananaspaghetti.utils.languageHandling.LanguageRegistry

// All the Global services are to be initialized here
class App : Application() {

    override fun onCreate() {
        super.onCreate()

        // Install first, so a crash anywhere in the rest of startup is still captured.
        CrashReporter.install(this)

        // Language registry initialization
        LanguageRegistry.init(this)
    }
}
