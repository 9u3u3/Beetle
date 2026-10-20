package com.bsbeetle.bananaspaghetti

import android.app.Application
import com.bsbeetle.bananaspaghetti.utils.diagnostics.CrashReporter
import com.bsbeetle.bananaspaghetti.utils.languageHandling.LanguageRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

// All the Global services are to be initialized here
class App : Application() {

    /**
     * Application-lifetime scope for work that must not be cancelled by navigation — currently the
     * crash-report upload, which would otherwise be dropped the moment the user leaves a screen.
     */
    private val appScope = CoroutineScope(SupervisorJob())

    override fun onCreate() {
        super.onCreate()

        // Install first, so a crash anywhere in the rest of startup is still captured.
        CrashReporter.install(this)

        // Language registry initialization
        LanguageRegistry.init(this)

        // Ship anything the previous run captured before it died.
        val email =
                getSharedPreferences("auth_prefs", MODE_PRIVATE).getString("user_email", null)
        CrashReporter.uploadPending(this, appScope, email)
    }
}
