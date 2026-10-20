package com.bsbeetle.bananaspaghetti.utils.python

import android.content.Context
import android.util.Log
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.TerminalSession
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

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
        private const val PLOT_END_MARKER = ">>BEETLE_PLOT_END<<"
        private const val READY_MARKER = ">>BEETLE_READY<<"

        /** How long to wait for [READY_MARKER] before re-sending the bootstrap. */
        private const val BOOTSTRAP_RETRY_MS = 2000L
        private const val BOOTSTRAP_ATTEMPTS = 15
        private const val BOOT_BUFFER_LIMIT = 32_768
        
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

    /** Pre-ready output, scanned for [READY_MARKER]. */
    private val bootBuffer = StringBuilder()
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
            |    # Patch exactly once, however many times this bootstrap runs.
            |    #
            |    # The wrappers resolve _beetle_original_savefig as a global at call time, not at
            |    # definition time. So a second pass would rebind that global to the first pass's
            |    # wrapper, and the wrapper would end up calling itself: RecursionError on the first
            |    # plot. The bootstrap is re-sent until Python answers READY, so running twice is
            |    # normal and this guard is what makes that safe.
            |    if not getattr(plt, '_beetle_patched', False):
            |        _beetle_original_savefig = plt.savefig
            |        _beetle_original_show = plt.show
            |
            |        # Emits via the ORIGINAL savefig, so it never re-enters the wrapper below.
            |        def _beetle_emit_plot():
            |            buf = io.BytesIO()
            |            _beetle_original_savefig(buf, format='png', dpi=100, bbox_inches='tight', facecolor='#1e1e1e')
            |            buf.seek(0)
            |            b64 = base64.b64encode(buf.read()).decode('utf-8')
            |            print(f'>>BEETLE_PLOT:{b64}')
            |            print('>>BEETLE_PLOT_END<<')
            |            return b64
            |
            |        def _beetle_show(*args, **kwargs):
            |            _beetle_emit_plot()
            |            plt.close('all')
            |
            |        def _beetle_savefig(*args, **kwargs):
            |            result = _beetle_original_savefig(*args, **kwargs)
            |            _beetle_emit_plot()
            |            return result
            |
            |        plt.show = _beetle_show
            |        plt.savefig = _beetle_savefig
            |        plt._beetle_patched = True
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
        
        // Re-send until Python answers with READY, rather than assuming it has started.
        //
        // `python3 -i` under proot regularly takes longer than a second to come up on a phone. The
        // old code fired the bootstrap once after 1s and declared the REPL ready after 3s no matter
        // what — so on a slow start the setup line was eaten by the shell that was still in the
        // foreground, `_beetle_exec` was never defined, and every later cell produced a NameError
        // with no START marker. handleOutput only reports text between START and END, so the cell
        // showed nothing at all: no output, no error.
        scope.launch {
            repeat(BOOTSTRAP_ATTEMPTS) {
                if (_isReady.value) return@launch
                pythonSession?.insertText(bootstrap)
                delay(BOOTSTRAP_RETRY_MS)
            }
            if (!_isReady.value) {
                Log.e(TAG, "Python REPL never signalled ready after $BOOTSTRAP_ATTEMPTS attempts")
            }
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
        if (!_isReady.value) {
            // The marker can straddle two PTY reads, so match against accumulated text.
            bootBuffer.append(text)
            if (bootBuffer.contains(READY_MARKER)) {
                bootBuffer.setLength(0)
                _isReady.value = true
                Log.d(TAG, "Python REPL ready")
            } else if (bootBuffer.length > BOOT_BUFFER_LIMIT) {
                // Keep only a tail long enough to still contain a split marker.
                bootBuffer.delete(0, bootBuffer.length - READY_MARKER.length)
            }
        }

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
        
        // Check for plots.
        //
        // Delimited rather than pattern-matched: the payload is tens of thousands of base64
        // characters on one logical line, and anything the tty inserts into it — a CR, a wrap, an
        // escape sequence — would end a character-class match early and hand the WebView a
        // truncated PNG that silently fails to decode. Taking everything between the two markers
        // and discarding whitespace survives all of that.
        val plotStart = output.indexOf(PLOT_MARKER)
        val plotEnd = if (plotStart == -1) -1 else output.indexOf(PLOT_END_MARKER, plotStart)

        if (plotStart != -1 && plotEnd != -1) {
            val raw = output.substring(plotStart + PLOT_MARKER.length, plotEnd)
            val base64Data = stripAnsiCodes(raw).filterNot { it.isWhitespace() }

            output =
                    (output.substring(0, plotStart) +
                                    output.substring(plotEnd + PLOT_END_MARKER.length))
                            .trim()

            if (base64Data.isNotEmpty()) {
                currentCellId?.let { cellId -> onPlotGenerated?.invoke(cellId, base64Data) }
                outputType = "image"
            }
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
