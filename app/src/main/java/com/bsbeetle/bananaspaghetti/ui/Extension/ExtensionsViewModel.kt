package com.bsbeetle.bananaspaghetti.ui.Extension

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.copyTo
import io.ktor.utils.io.jvm.javaio.toOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream

import com.bsbeetle.bananaspaghetti.utils.languageHandling.LanguageRegistry
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.TerminalSession
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout

class ExtensionsViewModel(application: Application) : AndroidViewModel(application) {

    private val _extensions = MutableStateFlow<List<ExtensionModel>>(emptyList())
    val extensions: StateFlow<List<ExtensionModel>> = _extensions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            })
        }
    }

    init {
        LanguageRegistry.init(getApplication())
        fetchExtensions()
    }

    fun fetchExtensions() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _error.value = null
            try {
                // Use production backend
                val url = "https://beetle-backend.vercel.app/api/plugin/fetch"
                
                val fetchedExtensions: List<ExtensionModel> = client.get(url).body()
                
                // Check installation status
                val updatedExtensions = fetchedExtensions.map { ext ->
                    ext.copy(installed = LanguageRegistry.isInstalled(ext.slug))
                }
                
                _extensions.value = updatedExtensions
                Log.d("ExtensionsViewModel", "Fetched ${updatedExtensions.size} extensions")
            } catch (e: Exception) {
                Log.e("ExtensionsViewModel", "Error fetching extensions", e)
                _error.value = "Failed to fetch extensions. Please check your internet connection."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun installExtension(extension: ExtensionModel) {
        viewModelScope.launch(Dispatchers.IO) {
            // Set installing state
            updateExtensionState(extension.id) { it.copy(isInstalling = true) }
            
            try {
                val context = getApplication<Application>()
                val pluginDir = File(context.filesDir, "plugins/${extension.slug}")
                if (!pluginDir.exists()) {
                    pluginDir.mkdirs()
                }

                // Download JS Module
                downloadFile(extension.jsModule, File(pluginDir, "${extension.slug}.js"))

                // Download LSP Script
                val scriptFile = File(pluginDir, "${extension.slug}.sh")
                downloadFile(extension.lspScript, scriptFile)
                scriptFile.setExecutable(true)

                // --- Installation Logic ---
                val publicDir = File(context.filesDir, "public")
                if (!publicDir.exists()) publicDir.mkdirs()

                val tempScript = File(publicDir, "install_${extension.slug}.sh")
                scriptFile.copyTo(tempScript, overwrite = true)
                tempScript.setExecutable(true)

                val wrapperFile = File(publicDir, "wrapper_${extension.slug}.sh")
                wrapperFile.writeText("""
                    #!/bin/sh
                    # Override both exec and exit to detect installation completion
                    exec() {
                        echo "INSTALLATION_COMPLETE"
                        command exit 0
                    }
                    exit() {
                        echo "INSTALLATION_COMPLETE"
                        command exit 0
                    }
                    . /public/${tempScript.name}
                    # If script doesn't call exit explicitly, mark complete anyway
                    echo "INSTALLATION_COMPLETE"
                """.trimIndent())
                wrapperFile.setExecutable(true)

                val installComplete = CompletableDeferred<Boolean>()
                val session = TerminalSession(context, "Install-${extension.slug}")
                
                session.onOutput = { output ->
                    // Log all shell output to logcat
                    Log.d("PluginInstall", "[${extension.slug}] $output")
                    
                    if (output.contains("INSTALLATION_COMPLETE")) {
                        installComplete.complete(true)
                    }
                }

                // Wait for shell init
                delay(1000)
                
                session.insertText("/public/${wrapperFile.name}\n")

                withTimeout(180000) { // 3 minutes timeout
                    installComplete.await()
                }

                // Cleanup
                tempScript.delete()
                wrapperFile.delete()
                // --------------------------

                // Update LanguageRegistry
                LanguageRegistry.setInstalled(extension.slug, extension.version, extension.id)

                // Update list to reflect installation
                updateExtensionState(extension.id) { it.copy(installed = true, isInstalling = false) }
                
                Log.d("ExtensionsViewModel", "Installed ${extension.name}")

            } catch (e: Exception) {
                Log.e("ExtensionsViewModel", "Error installing extension ${extension.name}", e)
                _error.value = "Failed to install ${extension.name}"
                // Revert installing state
                updateExtensionState(extension.id) { it.copy(isInstalling = false) }
            }
        }
    }

    private suspend fun updateExtensionState(id: Int, update: (ExtensionModel) -> ExtensionModel) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
            val currentList = _extensions.value.toMutableList()
            val index = currentList.indexOfFirst { it.id == id }
            if (index != -1) {
                currentList[index] = update(currentList[index])
                _extensions.value = currentList
            }
        }
    }
    
    fun uninstallExtension(extension: ExtensionModel) {
        viewModelScope.launch(Dispatchers.IO) {
             try {
                val context = getApplication<Application>()
                val pluginDir = File(context.filesDir, "plugins/${extension.slug}")
                if (pluginDir.exists()) {
                    pluginDir.deleteRecursively()
                }

                // Update LanguageRegistry
                LanguageRegistry.uninstall(extension.slug)

                // Update list
                val currentList = _extensions.value.toMutableList()
                val index = currentList.indexOfFirst { it.id == extension.id }
                if (index != -1) {
                    currentList[index] = currentList[index].copy(installed = false)
                    _extensions.value = currentList
                }
                 Log.d("ExtensionsViewModel", "Uninstalled ${extension.name}")
            } catch (e: Exception) {
                Log.e("ExtensionsViewModel", "Error uninstalling extension ${extension.name}", e)
                _error.value = "Failed to uninstall ${extension.name}"
            }
        }
    }

    private suspend fun downloadFile(url: String, destination: File) {
        val response = client.prepareGet(url).execute()
        val bytes = response.body<ByteArray>()
        val outputStream = FileOutputStream(destination)
        outputStream.write(bytes)
        outputStream.close()
    }
}
