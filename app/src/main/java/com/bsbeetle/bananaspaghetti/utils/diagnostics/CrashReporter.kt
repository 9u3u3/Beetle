package com.bsbeetle.bananaspaghetti.utils.diagnostics

import android.content.Context
import android.os.Build
import android.util.Log
import com.bsbeetle.bananaspaghetti.BuildConfig
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class CrashReport(
        val timestamp: Long,
        val versionName: String,
        val versionCode: Int,
        val androidRelease: String,
        val sdkInt: Int,
        val manufacturer: String,
        val model: String,
        val abi: String,
        val threadName: String,
        val exceptionClass: String,
        val exceptionMessage: String?,
        val stackTrace: String
)

/**
 * Captures uncaught exceptions and keeps them on the device.
 *
 * Beetle runs a real Linux environment through proot on phones we cannot test. Without this, a
 * crash on a user's phone produces only a logcat line.
 *
 * **Why crashes go to a file.** The uncaught-exception handler runs while the process stops. So the
 * handler only writes a file. On the next launch, the home screen asks the user to report the crash
 * as a GitHub issue ([pending], [GitHubIssues]). Beetle sends nothing by itself.
 *
 * The original handler always runs afterwards, so the platform still stops the process and shows
 * its normal crash UI.
 *
 * The prompt is on by default and can be turned off in Settings ([isEnabled]). Reports carry the
 * stack trace, app version and device model. They never carry file contents, project source, or
 * the API key.
 */
object CrashReporter {

    private const val TAG = "CrashReporter"
    private const val PREFS_NAME = "beetle_diagnostics"
    private const val KEY_ENABLED = "crash_reporting_enabled"
    private const val CRASH_DIR = "crashes"

    /** Stop unbounded growth through a crash loop that nobody reports. */
    private const val MAX_PENDING_REPORTS = 20

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun isEnabled(context: Context): Boolean =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .getBoolean(KEY_ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_ENABLED, enabled)
                .apply()
        if (!enabled) clearPending(context)
    }

    /** Installs the handler. Safe to call once, from [android.app.Application.onCreate]. */
    fun install(context: Context) {
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                if (isEnabled(appContext)) {
                    persist(appContext, thread, throwable)
                }
            } catch (e: Throwable) {
                // Never let reporting failure change how the app dies.
                Log.e(TAG, "Failed to persist crash report", e)
            } finally {
                previous?.uncaughtException(thread, throwable)
            }
        }
    }

    /** Returns the crash reports from earlier runs, oldest first. */
    fun pending(context: Context): List<CrashReport> {
        if (!isEnabled(context)) return emptyList()
        return crashDir(context).listFiles()?.sortedBy { it.name }?.mapNotNull { file ->
            runCatching { json.decodeFromString<CrashReport>(file.readText()) }
                    .getOrElse {
                        file.delete()
                        null
                    }
        } ?: emptyList()
    }

    /** Deletes all saved crash reports. */
    fun clearPending(context: Context) {
        crashDir(context).listFiles()?.forEach { it.delete() }
    }

    /** Builds the URL of a GitHub bug report for [report]. */
    fun issueUrl(report: CrashReport, count: Int): String {
        val summary = report.exceptionClass.substringAfterLast('.') +
                (report.exceptionMessage?.let { ": $it" } ?: "")
        val extra = if (count > 1) "\n\n$count crashes were saved. This is the latest one." else ""
        return GitHubIssues.bugReportUrl(
                title = "Crash: $summary",
                fields = mapOf(
                        "what-happened" to "Beetle stopped with an error on thread \"${report.threadName}\".$extra",
                        "steps" to "Not known. Beetle saved this report after the crash. " +
                                "Add the steps here if you remember them.",
                        "device" to "${report.manufacturer} ${report.model}, Android ${report.androidRelease} " +
                                "(API ${report.sdkInt}), ${report.abi}",
                        "app-version" to "${report.versionName} (${report.versionCode})",
                        "logs" to report.stackTrace
                )
        )
    }

    private fun persist(context: Context, thread: Thread, throwable: Throwable) {
        val dir = crashDir(context)
        val existing = dir.listFiles()?.size ?: 0
        if (existing >= MAX_PENDING_REPORTS) return

        val report =
                CrashReport(
                        timestamp = System.currentTimeMillis(),
                        versionName = BuildConfig.VERSION_NAME,
                        versionCode = BuildConfig.VERSION_CODE,
                        androidRelease = Build.VERSION.RELEASE ?: "unknown",
                        sdkInt = Build.VERSION.SDK_INT,
                        manufacturer = Build.MANUFACTURER ?: "unknown",
                        model = Build.MODEL ?: "unknown",
                        abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown",
                        threadName = thread.name,
                        exceptionClass = throwable.javaClass.name,
                        exceptionMessage = throwable.message,
                        stackTrace = stackTraceOf(throwable)
                )

        File(dir, "crash-${report.timestamp}.json")
                .writeText(json.encodeToString(CrashReport.serializer(), report))
    }

    private fun stackTraceOf(throwable: Throwable): String {
        val writer = StringWriter()
        PrintWriter(writer).use { throwable.printStackTrace(it) }
        return writer.toString()
    }

    private fun crashDir(context: Context): File =
            File(context.filesDir, CRASH_DIR).apply { if (!exists()) mkdirs() }
}
