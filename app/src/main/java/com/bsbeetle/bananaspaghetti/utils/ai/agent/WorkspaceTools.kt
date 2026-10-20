package com.bsbeetle.bananaspaghetti.utils.ai.agent

import java.io.File
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/** One file the agent changed. [before] is null when the agent created the file. */
data class FileChange(val path: String, val relativePath: String, val before: String?, val after: String)

/**
 * The tools the agent uses on the open project.
 *
 * Every path is resolved against [root] and must stay inside it. Paths come from the model, so a
 * `..` or an absolute path outside the project is refused instead of followed.
 *
 * @param liveContent returns the editor's current text for a path that is open in an editor, so
 *   the agent reads and edits what the user sees, unsaved changes included.
 * @param runCommand runs a shell command in the Ubuntu environment. Null when no runner exists.
 * @param approveCommand asks the user before each command. The command runs only on true.
 * @param onFileChanged receives each write after it reaches the disk.
 */
class WorkspaceTools(
        val root: File,
        private val liveContent: (String) -> String? = { null },
        private val runCommand: (suspend (String, File, Int) -> ToolOutcome)? = null,
        private val approveCommand: suspend (String) -> Boolean = { false },
        private val onFileChanged: (FileChange) -> Unit = {}
) {

    private val rootCanonical: File = root.canonicalFile

    val specs: List<ToolSpec> = buildList {
        add(
                ToolSpec(
                        name = "list_files",
                        description =
                                "List files and folders in the project as a tree. Call this first " +
                                        "when you do not know the project layout. Build folders " +
                                        "such as .git, node_modules and build are left out.",
                        schema =
                                objectSchema(
                                        required = emptyList(),
                                        "path" to stringProp("Folder relative to the project root. Default: the root."),
                                        "depth" to intProp("How many levels to show, 1 to 4. Default: 2.")
                                )
                )
        )
        add(
                ToolSpec(
                        name = "read_file",
                        description =
                                "Read a text file with line numbers. Call this before you edit a " +
                                        "file. For a long file, read it in parts with start_line " +
                                        "and end_line. Line numbers are for reference only and are " +
                                        "not part of the file.",
                        schema =
                                objectSchema(
                                        required = listOf("path"),
                                        "path" to stringProp("File path relative to the project root."),
                                        "start_line" to intProp("First line to return, from 1."),
                                        "end_line" to intProp("Last line to return.")
                                )
                )
        )
        add(
                ToolSpec(
                        name = "search_files",
                        description =
                                "Search the text of all project files with a regular expression. " +
                                        "Use this to find where something is defined or used.",
                        schema =
                                objectSchema(
                                        required = listOf("pattern"),
                                        "pattern" to stringProp("Regular expression, for example 'fun main' or 'TODO'."),
                                        "path" to stringProp("Folder to search, relative to the root. Default: the root."),
                                        "case_sensitive" to boolProp("Default: false.")
                                )
                )
        )
        add(
                ToolSpec(
                        name = "edit_file",
                        description =
                                "Replace an exact piece of text in an existing file. old_text must " +
                                        "match the file exactly, including spaces and indentation, " +
                                        "and must occur once unless replace_all is true. Include " +
                                        "enough lines around the change to make old_text unique. " +
                                        "Prefer this to write_file for changes to existing files.",
                        schema =
                                objectSchema(
                                        required = listOf("path", "old_text", "new_text"),
                                        "path" to stringProp("File path relative to the project root."),
                                        "old_text" to stringProp("Exact text to replace."),
                                        "new_text" to stringProp("Text to put in its place."),
                                        "replace_all" to boolProp("Replace every occurrence. Default: false.")
                                )
                )
        )
        add(
                ToolSpec(
                        name = "write_file",
                        description =
                                "Create a new file, or replace all the content of a file. Use this " +
                                        "for new files. For changes to an existing file, use " +
                                        "edit_file instead.",
                        schema =
                                objectSchema(
                                        required = listOf("path", "content"),
                                        "path" to stringProp("File path relative to the project root."),
                                        "content" to stringProp("The full content of the file.")
                                )
                )
        )
        if (runCommand != null) {
            add(
                    ToolSpec(
                            name = "run_command",
                            description =
                                    "Run a shell command with bash in the project folder, in an " +
                                            "Ubuntu 24.04 environment on the user's phone. Use it " +
                                            "to build, test, run code, or use git. The user must " +
                                            "approve each command. Output is stdout and stderr " +
                                            "together. Commands that wait for input do not work.",
                            schema =
                                    objectSchema(
                                            required = listOf("command"),
                                            "command" to stringProp("The bash command."),
                                            "timeout_seconds" to intProp("Default: 120. Maximum: 600.")
                                    )
                    )
            )
        }
    }

    /** Runs one tool call. It never throws: every failure becomes an error result for the model. */
    suspend fun execute(call: ToolCall): ToolOutcome {
        val input =
                call.input
                        ?: return ToolOutcome(
                                "The arguments were not valid JSON. Send the call again with " +
                                        "valid JSON arguments. Received: ${call.rawInput.take(500)}",
                                isError = true
                        )
        return try {
            when (call.name) {
                "list_files" -> listFiles(input)
                "read_file" -> readFile(input)
                "search_files" -> searchFiles(input)
                "edit_file" -> editFile(input)
                "write_file" -> writeFile(input)
                "run_command" -> runCommand(input)
                else -> ToolOutcome("Unknown tool: ${call.name}", isError = true)
            }
        } catch (e: ToolError) {
            ToolOutcome(e.message ?: "Tool failed.", isError = true)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            ToolOutcome("${call.name} failed: ${e.javaClass.simpleName}: ${e.message}", isError = true)
        }
    }

    // ---------------------------------------------------------------------------------------------

    private fun listFiles(input: JsonObject): ToolOutcome {
        val dir = resolve(input.string("path") ?: ".")
        if (!dir.isDirectory) throw ToolError("Not a folder: ${relative(dir)}")
        val depth = (input.int("depth") ?: 2).coerceIn(1, 4)

        val out = StringBuilder()
        var count = 0
        fun walk(folder: File, level: Int) {
            val children =
                    (folder.listFiles() ?: return)
                            .filterNot { it.name in SKIP_DIRS && it.isDirectory }
                            .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            for (child in children) {
                if (count >= MAX_LIST_ENTRIES) return
                count++
                out.append("  ".repeat(level))
                        .append(child.name)
                        .append(if (child.isDirectory) "/" else "")
                        .append('\n')
                if (child.isDirectory && level + 1 < depth) walk(child, level + 1)
            }
        }
        walk(dir, 0)
        if (count >= MAX_LIST_ENTRIES) out.append("… (stopped after $MAX_LIST_ENTRIES entries)\n")
        val header = "${relative(dir)}/\n"
        return ToolOutcome(if (count == 0) header + "(empty)" else header + out)
    }

    private fun readFile(input: JsonObject): ToolOutcome {
        val file = resolve(input.requireString("path"))
        val text = currentText(file) ?: throw ToolError("File not found: ${relative(file)}")
        val lines = text.lines()
        val start = (input.int("start_line") ?: 1).coerceAtLeast(1)
        val requestedEnd = input.int("end_line") ?: lines.size
        val end = minOf(requestedEnd, lines.size, start + MAX_READ_LINES - 1)
        if (start > lines.size) {
            return ToolOutcome("${relative(file)} has ${lines.size} lines. start_line $start is past the end.")
        }

        val width = end.toString().length
        val body = StringBuilder()
        for (i in start..end) {
            body.append(i.toString().padStart(width)).append("| ").append(lines[i - 1]).append('\n')
        }
        val note =
                if (end < minOf(requestedEnd, lines.size)) {
                    "\n(showing lines $start-$end of ${lines.size}; read the rest with start_line=${end + 1})"
                } else ""
        return ToolOutcome(clip(body.toString()) + note)
    }

    private fun searchFiles(input: JsonObject): ToolOutcome {
        val pattern = input.requireString("pattern")
        val dir = resolve(input.string("path") ?: ".")
        val options =
                if (input.bool("case_sensitive") == true) emptySet() else setOf(RegexOption.IGNORE_CASE)
        val regex =
                runCatching { Regex(pattern, options) }.getOrElse { Regex(Regex.escape(pattern), options) }

        val out = StringBuilder()
        var matches = 0
        dir.walkTopDown()
                .onEnter { it == dir || it.name !in SKIP_DIRS }
                .filter { it.isFile && it.length() <= MAX_SEARCH_FILE_BYTES }
                .forEach { file ->
                    if (matches >= MAX_SEARCH_MATCHES) return@forEach
                    val text = currentText(file) ?: return@forEach
                    if (text.take(4000).contains('\u0000')) return@forEach
                    text.lineSequence().forEachIndexed { index, line ->
                        if (matches < MAX_SEARCH_MATCHES && regex.containsMatchIn(line)) {
                            matches++
                            out.append(relative(file))
                                    .append(':')
                                    .append(index + 1)
                                    .append(": ")
                                    .append(line.trim().take(200))
                                    .append('\n')
                        }
                    }
                }
        if (matches == 0) return ToolOutcome("No matches for /$pattern/ in ${relative(dir)}.")
        if (matches >= MAX_SEARCH_MATCHES) out.append("… (stopped after $MAX_SEARCH_MATCHES matches)\n")
        return ToolOutcome(clip(out.toString()))
    }

    private fun editFile(input: JsonObject): ToolOutcome {
        val file = resolve(input.requireString("path"))
        val before = currentText(file) ?: throw ToolError("File not found: ${relative(file)}. Use write_file to create it.")
        var oldText = input.requireString("old_text", allowEmpty = false)
        var newText = input.requireString("new_text")
        val replaceAll = input.bool("replace_all") == true

        // The model writes "\n". A file saved on Windows uses "\r\n", so try that form too.
        if (!before.contains(oldText) && before.contains("\r\n")) {
            val crlfOld = oldText.replace("\r\n", "\n").replace("\n", "\r\n")
            if (before.contains(crlfOld)) {
                oldText = crlfOld
                newText = newText.replace("\r\n", "\n").replace("\n", "\r\n")
            }
        }

        val count = before.windowedCount(oldText)
        if (count == 0) {
            throw ToolError(
                    "old_text was not found in ${relative(file)}. Read the file again and copy " +
                            "the text exactly, including indentation."
            )
        }
        if (count > 1 && !replaceAll) {
            throw ToolError(
                    "old_text occurs $count times in ${relative(file)}. Add more lines around it " +
                            "to make it unique, or set replace_all to true."
            )
        }
        val after =
                if (replaceAll) before.replace(oldText, newText)
                else before.replaceFirst(oldText, newText)
        save(file, before, after)
        return ToolOutcome("Edited ${relative(file)}: replaced $count occurrence${if (count == 1) "" else "s"}.")
    }

    private fun writeFile(input: JsonObject): ToolOutcome {
        val file = resolve(input.requireString("path"))
        if (file.isDirectory) throw ToolError("${relative(file)} is a folder.")
        val content = input.requireString("content")
        val before = currentText(file)
        save(file, before, content)
        val verb = if (before == null) "Created" else "Replaced the content of"
        return ToolOutcome("$verb ${relative(file)} (${content.lines().size} lines).")
    }

    private suspend fun runCommand(input: JsonObject): ToolOutcome {
        val runner = runCommand ?: throw ToolError("Commands are not available.")
        val command = input.requireString("command", allowEmpty = false)
        val timeout = (input.int("timeout_seconds") ?: 120).coerceIn(5, 600)
        if (!approveCommand(command)) {
            return ToolOutcome("The user did not allow this command. Do not run it again unless the user asks.", isError = true)
        }
        return runner(command, rootCanonical, timeout)
    }

    // ---------------------------------------------------------------------------------------------

    private fun save(file: File, before: String?, after: String) {
        if (before == after) return
        file.parentFile?.mkdirs()
        file.writeText(after, Charsets.UTF_8)
        onFileChanged(FileChange(file.path, relative(file), before, after))
    }

    /** The editor's text when the file is open, otherwise the disk content. Null if missing. */
    private fun currentText(file: File): String? =
            liveContent(file.path) ?: if (file.isFile) file.readText(Charsets.UTF_8) else null

    /** Resolves a model-supplied path and refuses anything outside the project root. */
    fun resolve(path: String): File {
        val cleaned = path.trim().removePrefix("./").ifEmpty { "." }
        val candidate = if (cleaned.startsWith("/")) File(cleaned) else File(rootCanonical, cleaned)
        val canonical = candidate.canonicalFile
        val inside =
                canonical == rootCanonical ||
                        canonical.path.startsWith(rootCanonical.path + File.separator)
        if (!inside) throw ToolError("Path is outside the project: $path")
        return canonical
    }

    private fun relative(file: File): String =
            if (file == rootCanonical) "." else file.relativeTo(rootCanonical).path

    private class ToolError(message: String) : Exception(message)

    private fun JsonObject.string(key: String): String? =
            (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull

    private fun JsonObject.requireString(key: String, allowEmpty: Boolean = true): String {
        val value = string(key) ?: throw ToolError("Missing required argument: $key")
        if (!allowEmpty && value.isEmpty()) throw ToolError("$key must not be empty.")
        return value
    }

    private fun JsonObject.int(key: String): Int? = (this[key] as? JsonPrimitive)?.let {
        it.intOrNull ?: it.contentOrNull?.toDoubleOrNull()?.toInt()
    }

    private fun JsonObject.bool(key: String): Boolean? = (this[key] as? JsonPrimitive)?.let {
        it.booleanOrNull ?: it.contentOrNull?.toBooleanStrictOrNull()
    }

    private fun String.windowedCount(part: String): Int {
        var count = 0
        var index = indexOf(part)
        while (index >= 0) {
            count++
            index = indexOf(part, index + part.length)
        }
        return count
    }

    private fun clip(text: String): String =
            if (text.length <= MAX_OUTPUT_CHARS) text
            else text.take(MAX_OUTPUT_CHARS) + "\n… (output cut at $MAX_OUTPUT_CHARS characters)"

    companion object {
        private val SKIP_DIRS =
                setOf(".git", "node_modules", "build", ".gradle", ".idea", "dist", "__pycache__", ".venv", "venv", "target", ".cxx")
        private const val MAX_LIST_ENTRIES = 400
        private const val MAX_READ_LINES = 1500
        private const val MAX_SEARCH_MATCHES = 150
        private const val MAX_SEARCH_FILE_BYTES = 1_000_000L
        const val MAX_OUTPUT_CHARS = 40_000

        private fun stringProp(description: String) = buildJsonObject {
            put("type", "string")
            put("description", description)
        }

        private fun intProp(description: String) = buildJsonObject {
            put("type", "integer")
            put("description", description)
        }

        private fun boolProp(description: String) = buildJsonObject {
            put("type", "boolean")
            put("description", description)
        }

        private fun objectSchema(required: List<String>, vararg props: Pair<String, JsonObject>) =
                buildJsonObject {
                    put("type", "object")
                    putJsonObject("properties") { props.forEach { (name, schema) -> put(name, schema) } }
                    put("required", buildJsonArray { required.forEach { add(JsonPrimitive(it)) } })
                }
    }
}
