package com.bsbeetle.bananaspaghetti.utils.notebook

/**
 * Registry for cell delimiters used in notebook mode.
 * 
 * Different programming languages use different comment syntaxes for cell delimiters.
 * This registry maps language identifiers to their respective cell delimiter patterns.
 * 
 * Cell delimiters are used to split a file into executable cells in notebook mode.
 * For example, in Python, `# %%` is commonly used to separate cells.
 */
object CellDelimiterRegistry {
    
    /**
     * Map of language identifiers to their cell delimiter patterns.
     * The delimiter is typically a comment followed by a marker (e.g., `%%`).
     */
    private val delimiters: Map<String, String> = mapOf(
        // Python uses hash comments
        "python" to "# %%",
        "py" to "# %%",
        
        // JavaScript/TypeScript use double-slash comments
        "javascript" to "// %%",
        "js" to "// %%",
        "typescript" to "// %%",
        "ts" to "// %%",
        
        // Ruby uses hash comments like Python
        "ruby" to "# %%",
        "rb" to "# %%",
        
        // R uses hash comments
        "r" to "# %%",
        
        // Shell scripts use hash comments
        "bash" to "# %%",
        "sh" to "# %%",
        "shell" to "# %%",
        
        // Julia uses hash comments
        "julia" to "# %%",
        "jl" to "# %%",
        
        // Rust uses double-slash comments
        "rust" to "// %%",
        "rs" to "// %%",
        
        // Go uses double-slash comments
        "go" to "// %%",
        
        // C/C++ use double-slash comments
        "c" to "// %%",
        "cpp" to "// %%",
        "c++" to "// %%",
        
        // Java uses double-slash comments
        "java" to "// %%",
        
        // Kotlin uses double-slash comments
        "kotlin" to "// %%",
        "kt" to "// %%"
    )
    
    /**
     * Get the cell delimiter for a given language.
     * 
     * @param language The language identifier (e.g., "python", "javascript")
     * @return The cell delimiter for the language, or the default Python delimiter if not found
     */
    fun getDelimiter(language: String): String {
        return delimiters[language.lowercase()] ?: "# %%"
    }
    
    /**
     * Get the cell delimiter based on a file extension.
     * 
     * @param extension The file extension without the dot (e.g., "py", "js")
     * @return The cell delimiter for files with that extension
     */
    fun getDelimiterByExtension(extension: String): String {
        val ext = extension.lowercase().removePrefix(".")
        return when (ext) {
            "py" -> getDelimiter("python")
            "js", "mjs", "cjs" -> getDelimiter("javascript")
            "ts", "mts" -> getDelimiter("typescript")
            "rb" -> getDelimiter("ruby")
            "r" -> getDelimiter("r")
            "sh", "bash", "zsh" -> getDelimiter("bash")
            "jl" -> getDelimiter("julia")
            "rs" -> getDelimiter("rust")
            "go" -> getDelimiter("go")
            "c", "h" -> getDelimiter("c")
            "cpp", "hpp", "cc", "cxx" -> getDelimiter("cpp")
            "java" -> getDelimiter("java")
            "kt", "kts" -> getDelimiter("kotlin")
            else -> "# %%"  // Default to Python-style
        }
    }
    
    /**
     * Check if a line is a cell delimiter for the given language.
     * 
     * @param line The line to check
     * @param language The language identifier
     * @return true if the line is a cell delimiter
     */
    fun isCellDelimiter(line: String, language: String): Boolean {
        val delimiter = getDelimiter(language)
        val trimmed = line.trim()
        // Match both "# %%" and "#%%" (without space)
        return trimmed == delimiter || 
               trimmed == delimiter.replace(" ", "") ||
               trimmed.startsWith("$delimiter ")  // Allow cell titles like "# %% My Cell"
    }
    
    /**
     * Get all supported language identifiers.
     * 
     * @return Set of all supported language identifiers
     */
    fun getSupportedLanguages(): Set<String> = delimiters.keys
    
    /**
     * Check if a file extension is supported by the notebook.
     * 
     * @param extension The file extension (with or without the dot)
     * @return true if the extension is supported
     */
    fun isExtensionSupported(extension: String): Boolean {
        val ext = extension.lowercase().removePrefix(".")
        // For now, only Python files are supported
        return ext == "py"
    }
    
    /**
     * Get a list of supported file extensions for display.
     * 
     * @return List of supported extensions
     */
    fun getSupportedExtensions(): List<String> = listOf(".py")
}
