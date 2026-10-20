package com.bsbeetle.bananaspaghetti.utils.diagnostics

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.bsbeetle.bananaspaghetti.BuildConfig

/**
 * Opens a new issue in the Beetle GitHub repository, filled in from the app.
 *
 * Beetle has no server of its own. The user reads the filled-in form in the browser,
 * changes it if necessary, and submits it with their GitHub account.
 */
object GitHubIssues {

    private const val NEW_ISSUE_URL = "https://github.com/9u3u3/Beetle/issues/new"

    /** GitHub refuses very long URLs, so long fields are cut to this many characters. */
    private const val MAX_FIELD_LENGTH = 4000

    /** The device line for the "Device and Android version" field of the bug form. */
    fun deviceInfo(): String =
            "${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}), " +
                    (Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown ABI")

    fun appVersion(): String = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"

    /**
     * Builds the URL of the bug report form. The keys of [fields] are the field ids in
     * `.github/ISSUE_TEMPLATE/bug_report.yml`.
     */
    fun bugReportUrl(title: String, fields: Map<String, String>): String {
        val builder = Uri.parse(NEW_ISSUE_URL).buildUpon()
                .appendQueryParameter("template", "bug_report.yml")
                .appendQueryParameter("title", title.take(200))
        fields.forEach { (id, value) ->
            if (value.isNotBlank()) builder.appendQueryParameter(id, shorten(value))
        }
        return builder.build().toString()
    }

    /** Opens [url] in the browser. Returns false when no app can open it. */
    fun open(context: Context, url: String): Boolean =
            runCatching {
                context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }.isSuccess

    private fun shorten(value: String): String =
            if (value.length <= MAX_FIELD_LENGTH) value
            else value.take(MAX_FIELD_LENGTH) + "\n... (cut to $MAX_FIELD_LENGTH characters)"
}
