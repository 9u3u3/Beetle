package com.bsbeetle.bananaspaghetti.utils.python

import android.content.Context
import android.util.Log
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.TerminalSession
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.regex.Pattern

/**
 * PythonExecutor manages a persistent headless Python REPL for notebook execution.
 * It uses a hidden TerminalSession with PTY to communicate with Python running in Ubuntu.
 */
class PythonExecutor(private val context: Context) {
    
    companion object {
        private const val TAG = "PythonExecutor"
        private const val EXECUTION_TIMEOUT_MS = 300000L // 5 minutes
        
        // Markers for detecting output boundaries
        private const val START_MARKER = ">>BEETLE_START<<"
        private const val END_MARKER = ">>BEETLE_END<<"
        private const val ERROR_MARKER = ">>BEETLE_ERROR<<"
        private const val PLOT_MARKER = ">>BEETLE_PLOT:"
        
        // Regex to strip ANSI escape codes and terminal control sequences
        private val ANSI_ESCAPE_REGEX = Regex("""\x1b\[[0-9;?]*[a-zA-Z]|\x1b\].*?\x07|\x1b\(B""")
        
        /**
         * Strip ANSI escape codes and terminal control sequences from output
         */
        private fun stripAnsiCodes(text: String): String {
            return ANSI_ESCAPE_REGEX.replace(text, "")
                .replace("\r\n", "\n")
                .replace("\r", "")
        }
    }
    
    private var pythonSession: TerminalSession? = null
    private var outputBuffer = StringBuilder()
    private var isExecuting = false
    private var currentCellId: String? = null
    private var executionStartTime = 0L
    
    // Callback for when execution completes
    var onExecutionComplete: ((cellId: String, output: String, outputType: String, executionTime: Double) -> Unit)? = null
    var onPlotGenerated: ((cellId: String, base64Data: String) -> Unit)? = null
    
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * Initialize the Python REPL session.
     * This creates a headless terminal session running Python.
     */
    fun initialize() {
        if (pythonSession != null) {
            Log.d(TAG, "Python session already initialized")
            return
        }
        
        Log.d(TAG, "Initializing Python REPL session...")
        
        // Create a hidden terminal session for Python
        pythonSession = TerminalSession(
            context = context,
            name = "PythonREPL-Headless",
            initialDir = null
        )
        
        // Set up output capture
        pythonSession?.onOutput = { text ->
            handleOutput(text)
        }
        
        // Start Python after a delay to let the shell initialize
        scope.launch {
            delay(2000) // Wait for shell to be ready
            startPythonRepl()
        }
    }
    
    private fun startPythonRepl() {
        // Start an interactive Python REPL that will persist for all cell executions
        // We start python3 -i and then feed the initialization code
        
        // 1. Disable echo to prevent command duplication in output
        pythonSession?.insertText("stty -echo 2>/dev/null\n")
        
        // 2. Start Python in interactive mode
        pythonSession?.insertText("python3 -i\n")
        
        // 3. Define helper functions and setup environment
        // We use a single block execution to define everything
        val pythonSetup = """
            |import sys
            |import os
            |import subprocess
            |
            |# Configure matplotlib to use non-GUI backend  
            |try:
            |    import matplotlib
            |    matplotlib.use('Agg')
            |    import matplotlib.pyplot as plt
            |    import base64
            |    import io
            |    
            |    # Store original functions before overriding
            |    _original_savefig = plt.savefig
            |    _original_show = plt.show
            |    
            |    # Helper to emit plot as base64 for display (uses original savefig to avoid recursion)
            |    def _beetle_emit_plot():
            |        buf = io.BytesIO()
            |        _original_savefig(buf, format='png', dpi=100, bbox_inches='tight', facecolor='#1e1e1e')
            |        buf.seek(0)
            |        b64 = base64.b64encode(buf.read()).decode('utf-8')
            |        print(f'>>BEETLE_PLOT:{b64}')
            |        return b64
            |    
            |    # Override plt.show to display plot
            |    def _beetle_show(*args, **kwargs):
            |        _beetle_emit_plot()
            |        plt.close('all')
            |    plt.show = _beetle_show
            |    
            |    # Override plt.savefig to also display the plot after saving
            |    def _beetle_savefig(*args, **kwargs):
            |        # First, do the actual save
            |        result = _original_savefig(*args, **kwargs)
            |        # Then emit the plot for display (don't close figures here, let user do that)
            |        _beetle_emit_plot()
            |        return result
            |    plt.savefig = _beetle_savefig
            |except ImportError:
            |    pass  # matplotlib not available
            |
            |# Function to execute cell code with markers
            |def _beetle_exec(code_b64):
            |    import base64
            |    import subprocess
            |    code = base64.b64decode(code_b64).decode('utf-8')
            |    print('>>BEETLE_START<<')
            |    
            |    # Pre-process code for shell commands
            |    lines = code.split('\n')
            |    processed_lines = []
            |    for line in lines:
            |        stripped = line.strip()
            |        if stripped.startswith('!'):
            |            cmd = stripped[1:]
            |            # Escape quotes for python string
            |            cmd_escaped = cmd.replace("'", "\\'")
            |            # Use subprocess.Popen to execute and stream output
            |            processed_lines.append(f"print('!{cmd_escaped}')")
            |            processed_lines.append(f"process = subprocess.Popen('{cmd_escaped}', shell=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, bufsize=1)")
            |            processed_lines.append("for line in process.stdout:")
            |            processed_lines.append("    print(line, end='')")
            |            processed_lines.append("process.wait()")
            |        else:
            |            processed_lines.append(line)
            |            
            |    final_code = '\n'.join(processed_lines)
            |
            |    try:
            |        exec(final_code, globals())
            |    except Exception as e:
            |        print('>>BEETLE_ERROR<<')
            |        import traceback
            |        traceback.print_exc()
            |    print('>>BEETLE_END<<')
            |
            |print('>>BEETLE_READY<<')
            |
        """.trimMargin()
        
        // Send setup code
        // We wrap it in an exec() call to ensure it runs as a block, or just send it line by line?
        // Sending it as a block is safer if we can.
        // But since we are in a REPL, indentation matters.
        // Let's send it via a temporary file to be safe and clean, or just paste it carefully.
        // Actually, using exec() on a base64 string is the most robust way to define the setup code too!
        
        val setupBase64 = android.util.Base64.encodeToString(
            pythonSetup.toByteArray(), 
            android.util.Base64.NO_WRAP
        )
        
        // We use a small bootstrap command to load the setup code
        val bootstrap = "import base64; exec(base64.b64decode('$setupBase64').decode('utf-8'))\n"
        
        // Give python a moment to start
        scope.launch {
            delay(1000)
            pythonSession?.insertText(bootstrap)
        }
        
        // Mark as ready after Python starts
        scope.launch {
            delay(3000) // Give Python time to initialize
            _isReady.value = true
            Log.d(TAG, "Python REPL ready")
        }
    }
    
    /**
     * Execute Python code in a specific cell.
     */
    fun executeCode(code: String, cellId: String) {
        if (!_isReady.value) {
            Log.w(TAG, "Python REPL not ready yet")
            onExecutionComplete?.invoke(cellId, "Python REPL not ready. Please wait...", "stderr", 0.0)
            return
        }
        
        if (isExecuting) {
            Log.w(TAG, "Already executing code")
            onExecutionComplete?.invoke(cellId, "Already executing another cell", "stderr", 0.0)
            return
        }
        
        Log.d(TAG, "Executing code in cell $cellId")
        isExecuting = true
        currentCellId = cellId
        outputBuffer.clear()
        executionStartTime = System.currentTimeMillis()
        
        // Wrap code with markers for output detection
        val wrappedCode = buildExecutionCommand(code)
        pythonSession?.insertText(wrappedCode)
        
        // Set timeout for execution
        scope.launch {
            delay(EXECUTION_TIMEOUT_MS)
            if (isExecuting && currentCellId == cellId) {
                finishExecution("Execution timed out", "stderr")
            }
        }
    }
    
    private fun buildExecutionCommand(code: String): String {
        // Use base64 encoding to avoid quote escaping issues
        val base64Code = android.util.Base64.encodeToString(
            code.toByteArray(), 
            android.util.Base64.NO_WRAP
        )
        
        // Call the _beetle_exec function in the running Python REPL
        // This preserves context between cells (variables persist)
        return "_beetle_exec('$base64Code')\n"
    }
    
    private fun handleOutput(text: String) {
        if (!isExecuting) return
        
        outputBuffer.append(text)
        val fullOutput = outputBuffer.toString()
        
        // Check if we have started execution
        val startIdx = fullOutput.indexOf(START_MARKER)
        if (startIdx != -1) {
            // We have started, check if we have ended
            val endIdx = fullOutput.indexOf(END_MARKER)
            
            if (endIdx != -1) {
                // Execution complete
                processCompletedOutput(fullOutput)
            } else {
                // Still executing, stream partial output
                // Only stream what's after the start marker
                val partialOutput = fullOutput.substring(startIdx + START_MARKER.length)
                // Don't stream if it looks like we're just buffering a plot or error marker
                if (partialOutput.isNotEmpty() && !partialOutput.contains(PLOT_MARKER)) {
                     val cleanOutput = stripAnsiCodes(partialOutput).trim()
                     if (cleanOutput.isNotEmpty()) {
                         currentCellId?.let { cellId ->
                             // Send partial update - we use a special output type or just update stdout
                             // For now, let's just update stdout. The UI should handle appending or replacing.
                             // Actually, the UI replaces content. So we send the full accumulated output so far.
                             onExecutionComplete?.invoke(cellId, cleanOutput, "stdout", 0.0)
                         }
                     }
                }
            }
        }
    }
    
    private fun processCompletedOutput(fullOutput: String) {
        val executionTime = (System.currentTimeMillis() - executionStartTime) / 1000.0
        
        // Extract output between markers
        val startIdx = fullOutput.indexOf(START_MARKER)
        val endIdx = fullOutput.indexOf(END_MARKER)
        
        if (startIdx == -1 || endIdx == -1) {
            finishExecution(fullOutput, "stdout")
            return
        }
        
        var output = fullOutput.substring(startIdx + START_MARKER.length, endIdx).trim()
        var outputType = "stdout"
        
        // Check for errors
        if (output.contains(ERROR_MARKER)) {
            output = output.substringAfter(ERROR_MARKER).trim()
            outputType = "stderr"
        }
        
        // Check for plots
        val plotPattern = Pattern.compile("$PLOT_MARKER([A-Za-z0-9+/=]+)")
        val plotMatcher = plotPattern.matcher(output)
        
        if (plotMatcher.find()) {
            val base64Data = plotMatcher.group(1)
            // Remove plot marker from text output
            output = output.replace("$PLOT_MARKER$base64Data", "").trim()
            
            // Send plot to callback
            currentCellId?.let { cellId ->
                onPlotGenerated?.invoke(cellId, base64Data ?: "")
            }
            outputType = "image"
        }
        
        finishExecution(output, outputType, executionTime)
    }
    
    private fun finishExecution(output: String, outputType: String, executionTime: Double = 0.0) {
        val cellId = currentCellId ?: return
        
        isExecuting = false
        currentCellId = null
        
        // Clean the output by stripping ANSI escape codes
        val cleanOutput = stripAnsiCodes(output).trim()
        
        Log.d(TAG, "Execution finished for cell $cellId: ${cleanOutput.take(50)}...")
        
        onExecutionComplete?.invoke(cellId, cleanOutput, outputType, executionTime)
    }
    
    /**
     * Get the plot directory for saving matplotlib output.
     */
    fun getPlotDirectory(): File {
        val plotDir = File(context.cacheDir, "notebook_plots")
        if (!plotDir.exists()) {
            plotDir.mkdirs()
        }
        return plotDir
    }
    
    /**
     * Clean up resources.
     */
    fun destroy() {
        Log.d(TAG, "Destroying Python executor")
        scope.cancel()
        pythonSession?.destroy()
        pythonSession = null
        _isReady.value = false
    }
}
