package com.bsbeetle.bananaspaghetti.liveserver

import android.content.Context
import android.util.Log
import java.io.File

/** Project type detection for Live Server */
enum class ProjectType {
    VANILLA_HTML, // Has index.html
    REACT_VITE, // Has package.json with vite
    REACT_CRA, // Has package.json with react-scripts
    UNKNOWN
}

/**
 * Singleton manager for the Live Server feature. Handles starting/stopping the server, detecting
 * project types, and triggering reloads.
 */
object LiveServerManager {
    private const val TAG = "LiveServerManager"
    private const val DEFAULT_PORT = 5500

    private var server: LiveHTTPServer? = null
    private var currentPort: Int = DEFAULT_PORT
    private var projectRootPath: String? = null
    private var projectType: ProjectType = ProjectType.UNKNOWN

    /** Check if the live server is currently running */
    fun isRunning(): Boolean = server?.isAlive == true

    /** Get the current server URL */
    fun getServerUrl(): String? {
        return if (isRunning()) "http://localhost:$currentPort" else null
    }

    /** Get the current project root path */
    fun getProjectRoot(): String? = projectRootPath

    /**
     * Convert a content URI to a real file path. Works with SAF (Storage Access Framework) URIs.
     */
    fun getPathFromUri(context: Context, uri: android.net.Uri): String? {
        Log.d(TAG, "getPathFromUri: $uri")

        // If it's already a file path
        if (uri.scheme == "file") {
            return uri.path
        }

        // For content URIs, try to extract the path from the document ID
        if (uri.scheme == "content") {
            // Check if it's a document URI
            val docId =
                    try {
                        android.provider.DocumentsContract.getDocumentId(uri)
                    } catch (e: Exception) {
                        try {
                            android.provider.DocumentsContract.getTreeDocumentId(uri)
                        } catch (e2: Exception) {
                            null
                        }
                    }

            Log.d(TAG, "Document ID: $docId")

            if (docId != null) {
                // Common format: "primary:path/to/folder" or "raw:/storage/emulated/0/path"
                val split = docId.split(":")
                if (split.size >= 2) {
                    val type = split[0]
                    val path = split.subList(1, split.size).joinToString(":")

                    return when {
                        type == "primary" -> "/storage/emulated/0/$path"
                        type == "raw" -> path
                        type.startsWith("/") -> docId // Already a path
                        else -> {
                            // Try to find the volume
                            "/storage/$type/$path"
                        }
                    }
                } else if (docId.startsWith("/")) {
                    return docId
                }
            }

            // Fallback: try to get the path directly
            uri.path?.let { path ->
                // SAF URIs often have format: /tree/primary:xxx/document/primary:xxx
                val primaryMatch = Regex("primary:(.+)").find(path)
                if (primaryMatch != null) {
                    return "/storage/emulated/0/${primaryMatch.groupValues[1]}"
                }
            }
        }

        Log.w(TAG, "Could not extract path from URI: $uri")
        return null
    }

    /** Detect project type from the given directory */
    fun detectProjectType(projectPath: String): ProjectType {
        val projectDir = File(projectPath)
        if (!projectDir.exists() || !projectDir.isDirectory) {
            return ProjectType.UNKNOWN
        }

        // Check for package.json (React/Node projects)
        val packageJson = File(projectDir, "package.json")
        if (packageJson.exists()) {
            try {
                val content = packageJson.readText()
                return when {
                    content.contains("\"vite\"") -> ProjectType.REACT_VITE
                    content.contains("\"react-scripts\"") -> ProjectType.REACT_CRA
                    content.contains("\"react\"") ->
                            ProjectType.REACT_VITE // Default to Vite for React
                    else -> ProjectType.UNKNOWN
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading package.json", e)
            }
        }

        // Check for index.html (vanilla HTML)
        val indexHtml = File(projectDir, "index.html")
        if (indexHtml.exists()) {
            return ProjectType.VANILLA_HTML
        }

        // Check in common subfolders
        val publicIndexHtml = File(projectDir, "public/index.html")
        if (publicIndexHtml.exists()) {
            return ProjectType.VANILLA_HTML
        }

        return ProjectType.UNKNOWN
    }

    /**
     * Start the live server for the given project
     * @param context Application context
     * @param projectPath Path to the project root directory
     * @param entryFile Optional specific file to serve (if not index.html)
     * @param port Port to run the server on (default 5500)
     * @return ServerResult with success status and message
     */
    fun startServer(
            context: Context,
            projectPath: String,
            entryFile: String? = null,
            port: Int = DEFAULT_PORT
    ): ServerResult {
        // Stop existing server first
        stopServer()

        val projectDir = File(projectPath)
        if (!projectDir.exists()) {
            Log.e(TAG, "Project directory not found: $projectPath")
            return ServerResult(false, "Project directory not found: $projectPath")
        }

        projectType = detectProjectType(projectPath)
        projectRootPath = projectPath

        return when (projectType) {
            ProjectType.VANILLA_HTML -> startVanillaServer(context, projectDir, entryFile, port)
            ProjectType.REACT_VITE ->
                    ServerResult(
                            true,
                            "React (Vite) project detected. Run 'npm run dev' in terminal, then open http://localhost:5173",
                            reactDevPort = 5173
                    )
            ProjectType.REACT_CRA ->
                    ServerResult(
                            true,
                            "React (CRA) project detected. Run 'npm start' in terminal, then open http://localhost:3000",
                            reactDevPort = 3000
                    )
            ProjectType.UNKNOWN -> {
                // If we have an entry file, serve it directly
                if (entryFile != null) {
                    startVanillaServer(context, projectDir, entryFile, port)
                } else {
                    // Try to find index.html
                    val indexHtml = findIndexHtml(projectDir)
                    if (indexHtml != null) {
                        startVanillaServer(
                                context,
                                indexHtml.parentFile ?: projectDir,
                                indexHtml.name,
                                port
                        )
                    } else {
                        ServerResult(false, "No HTML file found to serve")
                    }
                }
            }
        }
    }

    /** Start vanilla HTML server */
    private fun startVanillaServer(
            context: Context,
            projectDir: File,
            entryFile: String? = null,
            port: Int
    ): ServerResult {
        return try {
            currentPort = findAvailablePort(port)
            server = LiveHTTPServer(context, projectDir, currentPort, entryFile)
            server?.start()

            val entryPath = entryFile ?: "index.html"
            Log.d(
                    TAG,
                    "Live server started on port $currentPort for ${projectDir.absolutePath}/$entryPath"
            )
            ServerResult(
                    true,
                    "Server started on http://localhost:$currentPort",
                    serverUrl = "http://localhost:$currentPort"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start server", e)
            ServerResult(false, "Failed to start server: ${e.message}")
        }
    }

    /** Stop the live server */
    fun stopServer() {
        try {
            server?.stop()
            server = null
            projectRootPath = null
            projectType = ProjectType.UNKNOWN
            Log.d(TAG, "Live server stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping server", e)
        }
    }

    /** Trigger a reload for all connected clients */
    fun triggerReload() {
        server?.triggerReload()
    }

    /** Find index.html in the project directory or common subfolders */
    private fun findIndexHtml(projectDir: File): File? {
        // Check root
        val rootIndex = File(projectDir, "index.html")
        if (rootIndex.exists()) return rootIndex

        // Check common folders
        val commonFolders = listOf("public", "src", "dist", "build", "www")
        for (folder in commonFolders) {
            val index = File(projectDir, "$folder/index.html")
            if (index.exists()) return index
        }

        return null
    }

    /** Find an available port starting from the given port */
    private fun findAvailablePort(startPort: Int): Int {
        var port = startPort
        var maxAttempts = 10

        while (maxAttempts > 0) {
            try {
                java.net.ServerSocket(port).use {
                    return port
                }
            } catch (e: Exception) {
                port++
                maxAttempts--
            }
        }

        return startPort // Fallback
    }
}

/** Result of a server operation */
data class ServerResult(
        val success: Boolean,
        val message: String,
        val serverUrl: String? = null,
        val reactDevPort: Int? = null
)
