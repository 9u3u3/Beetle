package com.bsbeetle.bananaspaghetti

import android.app.Application
import com.bsbeetle.bananaspaghetti.utils.languageHandling.LanguageRegistry

// All the Global services are to be initialized here
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // Language registry initialization
        LanguageRegistry.init(this)
    }
}