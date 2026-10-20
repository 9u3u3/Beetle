package com.bsbeetle.bananaspaghetti.ui.home

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bsbeetle.bananaspaghetti.ui.theme.AppColors
import com.bsbeetle.bananaspaghetti.utils.ai.AiProviders
import com.bsbeetle.bananaspaghetti.utils.diagnostics.CrashReporter
import com.bsbeetle.bananaspaghetti.utils.preferences.AiPreferences

/**
 * The AI category in Settings: provider, API key, model, and diagnostics.
 *
 * Before this existed the key could only be entered from inside the editor's AI sheet, so there was
 * no way to rotate a key, switch model, or check what was configured without opening a file first.
 *
 * Keys and models are per provider, so switching between Anthropic and Gemini keeps both set up —
 * which is what makes comparing their code generation practical rather than a re-paste each time.
 *
 * Colors come from [AppColors] rather than local literals — the rest of this screen still uses
 * file-private values, which is theming debt still to clear.
 */
@Composable
fun AiSettingsContent() {
    val context = LocalContext.current
    val prefs = remember { AiPreferences(context) }

    var providerId by remember { mutableStateOf(prefs.providerId) }
    val provider = remember(providerId) { AiProviders.byId(providerId) }

    // Re-keyed on providerId so switching provider reloads that provider's saved key and model
    // rather than showing the previous one's.
    var keyInput by remember(providerId) { mutableStateOf(prefs.apiKeyFor(providerId)) }
    var selectedModel by remember(providerId) { mutableStateOf(prefs.modelFor(providerId)) }
    var savedNotice by remember(providerId) { mutableStateOf<String?>(null) }

    var keyRevealed by remember { mutableStateOf(false) }
    var crashReportingEnabled by remember { mutableStateOf(CrashReporter.isEnabled(context)) }

    Column(modifier = Modifier.fillMaxWidth()) {

        // ---------------- Provider ----------------
        AiSettingsCard(title = "Provider") {
            Column {
                AiProviders.all.forEach { option ->
                    val configured = prefs.isConfigured(option.id)
                    Row(
                            modifier =
                                    Modifier.fillMaxWidth()
                                            .clickable {
                                                providerId = option.id
                                                prefs.providerId = option.id
                                            }
                                            .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                                selected = providerId == option.id,
                                onClick = {
                                    providerId = option.id
                                    prefs.providerId = option.id
                                },
                                colors =
                                        RadioButtonDefaults.colors(
                                                selectedColor = AppColors.AccentBlue,
                                                unselectedColor = AppColors.TextSecondary
                                        )
                        )

                        Column(modifier = Modifier.padding(start = 8.dp).weight(1f)) {
                            Text(
                                    text = option.displayName,
                                    color = AppColors.TextPrimary,
                                    fontSize = 16.sp
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                    text = option.keyNote,
                                    color = AppColors.TextSecondary,
                                    fontSize = 13.sp
                            )
                        }

                        // Tells the user at a glance which providers are ready to use, so an empty
                        // response later isn't a mystery.
                        Text(
                                text = if (configured) "Key set" else "No key",
                                color =
                                        if (configured) AppColors.AccentGreen
                                        else AppColors.TextMuted,
                                fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ---------------- API key ----------------
        AiSettingsCard(title = "${provider.displayName} API key") {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                        text =
                                "Beetle uses your own key, so you pay ${provider.displayName} " +
                                        "directly and we never bill you for tokens. The key is stored " +
                                        "encrypted on this device and is excluded from cloud backups.",
                        color = AppColors.TextSecondary,
                        fontSize = 13.sp
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                        value = keyInput,
                        onValueChange = {
                            keyInput = it
                            savedNotice = null
                        },
                        singleLine = true,
                        placeholder = {
                            Text(provider.keyHint, color = AppColors.TextMuted, fontSize = 14.sp)
                        },
                        visualTransformation =
                                if (keyRevealed) VisualTransformation.None
                                else PasswordVisualTransformation(),
                        colors =
                                OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = AppColors.TextPrimary,
                                        unfocusedTextColor = AppColors.TextPrimary,
                                        focusedBorderColor = AppColors.AccentBlue,
                                        unfocusedBorderColor = AppColors.Border,
                                        cursorColor = AppColors.AccentBlue
                                ),
                        modifier = Modifier.fillMaxWidth()
                )

                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { keyRevealed = !keyRevealed }) {
                            Text(
                                    text = if (keyRevealed) "Hide" else "Show",
                                    color = AppColors.TextSecondary,
                                    fontSize = 13.sp
                            )
                        }
                        TextButton(
                                onClick = {
                                    context.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse(provider.consoleUrl))
                                    )
                                }
                        ) {
                            Text("Get a key", color = AppColors.TextSecondary, fontSize = 13.sp)
                        }
                    }

                    Row {
                        if (prefs.isConfigured(providerId)) {
                            TextButton(
                                    onClick = {
                                        prefs.clearApiKey(providerId)
                                        keyInput = ""
                                        savedNotice = "Key removed."
                                    }
                            ) { Text("Remove", color = AppColors.AccentRed, fontSize = 13.sp) }
                        }

                        TextButton(
                                onClick = {
                                    prefs.setApiKeyFor(providerId, keyInput.trim())
                                    savedNotice =
                                            if (keyInput.isBlank()) "Key cleared." else "Key saved."
                                }
                        ) { Text("Save", color = AppColors.AccentBlue, fontSize = 13.sp) }
                    }
                }

                savedNotice?.let {
                    Text(text = it, color = AppColors.AccentGreen, fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ---------------- Model ----------------
        AiSettingsCard(title = "Model") {
            Column {
                provider.models.forEach { option ->
                    Row(
                            modifier =
                                    Modifier.fillMaxWidth()
                                            .clickable {
                                                selectedModel = option.id
                                                prefs.setModelFor(providerId, option.id)
                                            }
                                            .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                                selected = selectedModel == option.id,
                                onClick = {
                                    selectedModel = option.id
                                    prefs.setModelFor(providerId, option.id)
                                },
                                colors =
                                        RadioButtonDefaults.colors(
                                                selectedColor = AppColors.AccentBlue,
                                                unselectedColor = AppColors.TextSecondary
                                        )
                        )

                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(
                                    text = option.displayName,
                                    color = AppColors.TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Normal
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                    text = option.hint,
                                    color = AppColors.TextSecondary,
                                    fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ---------------- Diagnostics ----------------
        AiSettingsCard(title = "Diagnostics") {
            Row(
                    modifier =
                            Modifier.fillMaxWidth()
                                    .clickable {
                                        crashReportingEnabled = !crashReportingEnabled
                                        CrashReporter.setEnabled(context, crashReportingEnabled)
                                    }
                                    .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                            text = "Send crash reports",
                            color = AppColors.TextPrimary,
                            fontSize = 16.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                            text =
                                    "Sends the stack trace, app version and device model when " +
                                            "Beetle crashes. Never your code, files, or API keys.",
                            color = AppColors.TextSecondary,
                            fontSize = 13.sp
                    )
                }

                Spacer(Modifier.width(12.dp))

                Checkbox(
                        checked = crashReportingEnabled,
                        onCheckedChange = {
                            crashReportingEnabled = it
                            CrashReporter.setEnabled(context, it)
                        },
                        colors =
                                CheckboxDefaults.colors(
                                        checkedColor = AppColors.AccentBlue,
                                        uncheckedColor = AppColors.TextSecondary,
                                        checkmarkColor = AppColors.TextPrimary
                                )
                )
            }
        }
    }
}

@Composable
private fun AiSettingsCard(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
                text = title,
                color = AppColors.TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )

        Column(
                modifier =
                        Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(AppColors.BgTertiary)
        ) { content() }
    }
}
