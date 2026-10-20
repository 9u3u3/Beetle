package com.bsbeetle.bananaspaghetti.utils.preferences

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.bsbeetle.bananaspaghetti.utils.ai.AiProvider
import com.bsbeetle.bananaspaghetti.utils.ai.AiProviders

/**
 * Stores BYOK (bring-your-own-key) AI settings: which provider is active, and per-provider API keys
 * and model choices.
 *
 * Keys and models are stored **per provider**, not globally. A user testing code generation across
 * Anthropic and Gemini holds two keys at once, and switching provider should not make them re-paste
 * anything or silently carry a Claude model ID into a Gemini request.
 *
 * The API keys are billable credentials, so they live in [EncryptedSharedPreferences] backed by the
 * Android Keystore — the same scheme
 * [com.bsbeetle.bananaspaghetti.directGit.utils.GitSshKeyManager] uses for SSH private keys.
 * Non-secret settings (active provider, model) stay in plain prefs so a Keystore failure can never
 * lock the user out of the picker.
 */
class AiPreferences(context: Context) {

    /** Non-secret settings. Safe to read even if the Keystore is unavailable. */
    private val prefs: SharedPreferences =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Keystore-backed store for API keys only.
     *
     * Built lazily and defensively: EncryptedSharedPreferences can throw on devices with a damaged
     * or rotated Keystore. If it does, we fall back to null and the provider reads as
     * "not configured" rather than crashing the editor — the user is prompted for a key again,
     * which is recoverable.
     */
    private val secretPrefs: SharedPreferences? by lazy {
        try {
            val masterKey =
                    MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()

            EncryptedSharedPreferences.create(
                    context,
                    SECRET_PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "Encrypted storage unavailable; AI keys cannot be persisted", e)
            null
        }
    }

    companion object {
        private const val TAG = "AiPreferences"

        private const val PREFS_NAME = "ai_preferences"
        private const val SECRET_PREFS_NAME = "ai_secrets"

        private const val KEY_PROVIDER = "ai_provider"

        /** Pre-multi-provider key, migrated to the Anthropic slot on first read. */
        private const val LEGACY_KEY_API_KEY = "ai_api_key"
        private const val LEGACY_KEY_MODEL = "ai_model"

        private fun apiKeyPref(providerId: String) = "ai_api_key_$providerId"

        private fun modelPref(providerId: String) = "ai_model_$providerId"
    }

    init {
        migrateLegacySingleProviderSettings()
    }

    /** The provider AI requests currently go to. */
    var providerId: String
        get() = prefs.getString(KEY_PROVIDER, AiProviders.ANTHROPIC) ?: AiProviders.ANTHROPIC
        set(value) = prefs.edit().putString(KEY_PROVIDER, value).apply()

    val provider: AiProvider
        get() = AiProviders.byId(providerId)

    /** The key for the active provider. */
    var apiKey: String
        get() = apiKeyFor(providerId)
        set(value) = setApiKeyFor(providerId, value)

    fun apiKeyFor(providerId: String): String =
            secretPrefs?.getString(apiKeyPref(providerId), "") ?: ""

    fun setApiKeyFor(providerId: String, value: String) {
        val store = secretPrefs
        if (store == null) {
            Log.e(TAG, "Cannot persist API key: encrypted storage unavailable")
            return
        }
        store.edit().putString(apiKeyPref(providerId), value).apply()
    }

    /**
     * The selected model for the active provider, migrating superseded IDs forward on read. The
     * migration is persisted so a user who never opens Settings still ends up on a live model
     * instead of one that has been shut down.
     */
    var model: String
        get() = modelFor(providerId)
        set(value) = setModelFor(providerId, value)

    fun modelFor(providerId: String): String {
        val p = AiProviders.byId(providerId)
        val stored = prefs.getString(modelPref(providerId), p.defaultModel) ?: p.defaultModel

        p.supersededModels[stored]?.let { current ->
            Log.i(TAG, "Migrating stored model $stored -> $current")
            prefs.edit().putString(modelPref(providerId), current).apply()
            return current
        }

        // A model that isn't in the catalogue at all (hand-edited prefs, or a picker entry we
        // later removed) falls back rather than being sent to the API to 404.
        if (p.models.none { it.id == stored }) return p.defaultModel

        return stored
    }

    fun setModelFor(providerId: String, value: String) {
        prefs.edit().putString(modelPref(providerId), value).apply()
    }

    /** True once the active provider has a key — used to gate AI features in the UI. */
    fun isConfigured(): Boolean = apiKey.isNotBlank()

    fun isConfigured(providerId: String): Boolean = apiKeyFor(providerId).isNotBlank()

    /** Wipes the stored key for one provider. Used by the "remove key" action in Settings. */
    fun clearApiKey(providerId: String = this.providerId) {
        secretPrefs?.edit()?.remove(apiKeyPref(providerId))?.apply()
    }

    /**
     * Moves settings written before multi-provider support into the Anthropic slot.
     *
     * Without this, upgrading users silently lose the key they already pasted and are asked for it
     * again. Runs once — the legacy entries are removed after the copy.
     */
    private fun migrateLegacySingleProviderSettings() {
        val legacyModel = prefs.getString(LEGACY_KEY_MODEL, null)
        if (legacyModel != null) {
            if (!prefs.contains(modelPref(AiProviders.ANTHROPIC))) {
                prefs.edit().putString(modelPref(AiProviders.ANTHROPIC), legacyModel).apply()
            }
            prefs.edit().remove(LEGACY_KEY_MODEL).apply()
        }

        val store = secretPrefs ?: return
        val legacyKey = store.getString(LEGACY_KEY_API_KEY, null) ?: return

        if (legacyKey.isNotBlank() && apiKeyFor(AiProviders.ANTHROPIC).isBlank()) {
            store.edit().putString(apiKeyPref(AiProviders.ANTHROPIC), legacyKey).apply()
            Log.i(TAG, "Migrated legacy API key into the Anthropic slot")
        }
        store.edit().remove(LEGACY_KEY_API_KEY).apply()
    }
}
