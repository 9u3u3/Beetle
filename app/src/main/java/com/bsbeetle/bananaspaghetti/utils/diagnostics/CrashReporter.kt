package com.bsbeetle.bananaspaghetti.utils.diagnostics

import android.content.Context
import android.os.Build
import android.util.Log
import com.bsbeetle.bananaspaghetti.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
        val stackTrace: String,
        val email: String? = null
)

/**
 * Captures uncaught exceptions and forwards them to the Beetle backend.
 *
 * Beetle runs a real Linux environment through proot on hardware we don't own and can't predict.
 * Without this, a crash on a user's phone produces a
 * logcat line nobody will ever read. It exists so the first wave of users generates signal instead
 * of silence.
 *
 * **Why crashes are written to disk before they are sent.** The uncaught-exception handler runs
 * while the process is being torn down: starting a network request there usually loses the race. So
 * the handler does the one thing it can finish synchronously — write a file — and the upload happens
 * on the next launch, when there is a healthy process to do it in.
 *
 * The original handler is always invoked afterwards, so the platform still terminates the process
 * and shows its normal crash UI. Swallowing it would leave the app wedged in an unusable state.
 *
 * Reporting is on by default and can be turned off in Settings ([isEnabled]). Reports carry the
 * stack trace, app version and device model — never file contents, project source, or the API key.
 */
object CrashReporter {

    private const val TAG = "CrashReporter"
    private const val PREFS_NAME = "beetle_diagnostics"
    private const val KEY_ENABLED = "crash_reporting_enabled"
    private const val CRASH_DIR = "crashes"

    private const val ENDPOINT = "https://beetle-backend.vercel.app/api/reports/crash"

    /** Stop unbounded growth if the device is offline through a crash loop. */
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

    /**
     * Uploads anything captured by a previous run. Call after [install], from a scope tied to the
     * application rather than a screen — a report is not worth cancelling because the user navigated.
     */
    fun uploadPending(context: Context, scope: CoroutineScope, email: String? = null) {
        val appContext = context.applicationContext
        if (!isEnabled(appContext)) return

        scope.launch(Dispatchers.IO) {
            val pending = crashDir(appContext).listFiles()?.sortedBy { it.name } ?: return@launch
            if (pending.isEmpty()) return@launch

            val client =
                    HttpClient(OkHttp) {
                        install(HttpTimeout) {
                            requestTimeoutMillis = 20_000
                            connectTimeoutMillis = 10_000
                        }
                    }

            try {
                for (file in pending) {
                    val body = runCatching { file.readText() }.getOrNull()
                    if (body.isNullOrBlank()) {
                        file.delete()
                        continue
                    }

                    val payload = if (email.isNullOrBlank()) body else withEmail(body, email)

                    val sent =
                            runCatching {
                                        client.post(ENDPOINT) {
                                                    contentType(ContentType.Application.Json)
                                                    setBody(payload)
                                                }
                                                .status
                                                .isSuccess()
                                    }
                                    .getOrElse { e ->
                                        Log.w(TAG, "Crash upload failed; will retry next launch", e)
                                        false
                                    }

                    // Only delete on success, so an offline launch doesn't discard the report.
                    if (sent) file.delete()
                }
            } finally {
                client.close()
            }
        }
    }

    private fun withEmail(body: String, email: String): String =
            runCatching {
                        val report = json.decodeFromString<CrashReport>(body)
                        json.encodeToString(CrashReport.serializer(), report.copy(email = email))
                    }
                    .getOrDefault(body)

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

    private fun clearPending(context: Context) {
        crashDir(context).listFiles()?.forEach { it.delete() }
    }
}
