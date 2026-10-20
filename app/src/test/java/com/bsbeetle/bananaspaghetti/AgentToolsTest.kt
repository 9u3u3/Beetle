package com.bsbeetle.bananaspaghetti

import com.bsbeetle.bananaspaghetti.utils.ai.agent.AnthropicAgent
import com.bsbeetle.bananaspaghetti.utils.ai.agent.CommandRunner
import com.bsbeetle.bananaspaghetti.utils.ai.agent.DocPaths
import com.bsbeetle.bananaspaghetti.utils.ai.agent.FileChange
import com.bsbeetle.bananaspaghetti.utils.ai.agent.ToolCall
import com.bsbeetle.bananaspaghetti.utils.ai.agent.WorkspaceTools
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AgentToolsTest {

    private lateinit var root: File
    private val changes = mutableListOf<FileChange>()
    private lateinit var tools: WorkspaceTools

    @Before
    fun setUp() {
        root = Files.createTempDirectory("agent-test").toFile()
        File(root, "src").mkdirs()
        File(root, "src/main.c").writeText("int main() {\n    return 0;\n}\n")
        File(root, "README.md").writeText("demo\nTODO: one\nTODO: two\n")
        File(root, "node_modules/x").mkdirs()
        File(root, "node_modules/x/index.js").writeText("TODO: hidden\n")
        tools = WorkspaceTools(root, onFileChanged = { changes += it })
    }

    @After
    fun tearDown() {
        root.deleteRecursively()
    }

    private fun call(name: String, json: String): ToolCall =
            ToolCall("id", name, Json.parseToJsonElement(json).jsonObject, json)

    private fun run(name: String, json: String) = runBlocking { tools.execute(call(name, json)) }

    @Test
    fun listSkipsBuildFolders() {
        val out = run("list_files", "{}")
        assertFalse(out.isError)
        assertTrue(out.content.contains("src/"))
        assertTrue(out.content.contains("README.md"))
        assertFalse(out.content.contains("node_modules"))
    }

    @Test
    fun readShowsLineNumbersAndRange() {
        val out = run("read_file", """{"path":"src/main.c","start_line":2,"end_line":2}""")
        assertEquals("2|     return 0;\n", out.content)
    }

    @Test
    fun readUsesLiveEditorText() {
        val live = File(root, "src/main.c").canonicalPath
        val withLive = WorkspaceTools(root, liveContent = { if (it == live) "unsaved\n" else null })
        val out = runBlocking { withLive.execute(call("read_file", """{"path":"src/main.c"}""")) }
        assertTrue(out.content.contains("unsaved"))
    }

    @Test
    fun searchFindsMatchesAndSkipsNodeModules() {
        val out = run("search_files", """{"pattern":"todo"}""")
        assertTrue(out.content.contains("README.md:2"))
        assertTrue(out.content.contains("README.md:3"))
        assertFalse(out.content.contains("hidden"))
    }

    @Test
    fun editReplacesUniqueText() {
        val out = run("edit_file", """{"path":"src/main.c","old_text":"return 0;","new_text":"return 1;"}""")
        assertFalse(out.content, out.isError)
        assertEquals("int main() {\n    return 1;\n}\n", File(root, "src/main.c").readText())
        assertEquals(1, changes.size)
        assertEquals("src/main.c", changes[0].relativePath)
        assertEquals("int main() {\n    return 0;\n}\n", changes[0].before)
    }

    @Test
    fun editRefusesAmbiguousText() {
        val out = run("edit_file", """{"path":"README.md","old_text":"TODO","new_text":"DONE"}""")
        assertTrue(out.isError)
        assertTrue(out.content.contains("2 times"))
        assertTrue(changes.isEmpty())
    }

    @Test
    fun editReplaceAll() {
        val out = run("edit_file", """{"path":"README.md","old_text":"TODO","new_text":"DONE","replace_all":true}""")
        assertFalse(out.isError)
        assertEquals("demo\nDONE: one\nDONE: two\n", File(root, "README.md").readText())
    }

    @Test
    fun editMatchesWindowsLineEnds() {
        File(root, "win.txt").writeText("a\r\nb\r\nc\r\n")
        val out = run("edit_file", """{"path":"win.txt","old_text":"a\nb","new_text":"x\ny"}""")
        assertFalse(out.content, out.isError)
        assertEquals("x\r\ny\r\nc\r\n", File(root, "win.txt").readText())
    }

    @Test
    fun editReportsMissingText() {
        val out = run("edit_file", """{"path":"src/main.c","old_text":"nope","new_text":"x"}""")
        assertTrue(out.isError)
        assertTrue(out.content.contains("not found"))
    }

    @Test
    fun writeCreatesFileAndFolders() {
        val out = run("write_file", """{"path":"lib/util.h","content":"#pragma once\n"}""")
        assertFalse(out.isError)
        assertEquals("#pragma once\n", File(root, "lib/util.h").readText())
        assertNull(changes.single().before)
    }

    @Test
    fun pathsOutsideTheProjectAreRefused() {
        assertTrue(run("read_file", """{"path":"../outside.txt"}""").isError)
        assertTrue(run("read_file", """{"path":"/etc/hosts"}""").isError)
        assertTrue(run("write_file", """{"path":"src/../../evil","content":"x"}""").isError)
        assertFalse(File(root.parentFile, "evil").exists())
    }

    @Test
    fun absolutePathInsideTheProjectWorks() {
        val abs = File(root, "README.md").canonicalPath
        assertFalse(run("read_file", """{"path":"$abs"}""").isError)
    }

    @Test
    fun invalidJsonIsAnErrorResult() {
        val out = runBlocking { tools.execute(ToolCall("id", "read_file", null, "{\"path\": \"a")) }
        assertTrue(out.isError)
        assertTrue(out.content.contains("not valid JSON"))
    }

    @Test
    fun commandsAreNotOfferedWithoutRunner() {
        assertFalse(tools.specs.any { it.name == "run_command" })
        val withRunner = WorkspaceTools(root, runCommand = { _, _, _ -> error("unused") })
        assertTrue(withRunner.specs.any { it.name == "run_command" })
    }

    @Test
    fun deniedCommandDoesNotRun() {
        var ran = false
        val withRunner =
                WorkspaceTools(
                        root,
                        runCommand = { _, _, _ ->
                            ran = true
                            error("must not run")
                        },
                        approveCommand = { false }
                )
        val out = runBlocking { withRunner.execute(call("run_command", """{"command":"ls"}""")) }
        assertTrue(out.isError)
        assertFalse(ran)
    }

    // --- Anthropic stream assembly ---

    private fun events(vararg lines: String): List<JsonObject> = lines.map { Json.parseToJsonElement(it).jsonObject }

    @Test
    fun assemblerBuildsTextThinkingAndToolUse() {
        val streamed = StringBuilder()
        val asm = AnthropicAgent.TurnAssembler { streamed.append(it) }
        events(
                        """{"type":"content_block_start","index":0,"content_block":{"type":"thinking","thinking":""}}""",
                        """{"type":"content_block_delta","index":0,"delta":{"type":"thinking_delta","thinking":"hmm"}}""",
                        """{"type":"content_block_delta","index":0,"delta":{"type":"signature_delta","signature":"sig"}}""",
                        """{"type":"content_block_start","index":1,"content_block":{"type":"text","text":""}}""",
                        """{"type":"content_block_delta","index":1,"delta":{"type":"text_delta","text":"Hel"}}""",
                        """{"type":"content_block_delta","index":1,"delta":{"type":"text_delta","text":"lo"}}""",
                        """{"type":"content_block_start","index":2,"content_block":{"type":"tool_use","id":"t1","name":"read_file","input":{}}}""",
                        """{"type":"content_block_delta","index":2,"delta":{"type":"input_json_delta","partial_json":"{\"path\":"}}""",
                        """{"type":"content_block_delta","index":2,"delta":{"type":"input_json_delta","partial_json":"\"a.c\"}"}}""",
                        """{"type":"message_delta","delta":{"stop_reason":"tool_use"}}"""
                )
                .forEach(asm::accept)
        val result = asm.finish()
        assertEquals("Hello", streamed.toString())
        assertEquals("tool_use", result.stopReason)
        assertEquals(3, result.content.size)
        assertEquals("hmm", result.content[0]["thinking"]!!.jsonPrimitive.content)
        assertEquals("sig", result.content[0]["signature"]!!.jsonPrimitive.content)
        assertEquals("a.c", result.toolCalls.single().input!!["path"]!!.jsonPrimitive.content)
        assertEquals("a.c", result.content[2]["input"]!!.jsonObject["path"]!!.jsonPrimitive.content)
    }

    @Test
    fun assemblerKeepsInvalidToolJsonAsRaw() {
        val asm = AnthropicAgent.TurnAssembler()
        events(
                        """{"type":"content_block_start","index":0,"content_block":{"type":"tool_use","id":"t1","name":"write_file","input":{}}}""",
                        """{"type":"content_block_delta","index":0,"delta":{"type":"input_json_delta","partial_json":"{\"path\": \"a"}}"""
                )
                .forEach(asm::accept)
        val call = asm.finish().toolCalls.single()
        assertNull(call.input)
        assertEquals("{\"path\": \"a", call.rawInput)
    }

    @Test
    fun assemblerDropsDeclinedBlocksBeforeFallback() {
        val asm = AnthropicAgent.TurnAssembler()
        events(
                        """{"type":"content_block_start","index":0,"content_block":{"type":"thinking","thinking":""}}""",
                        """{"type":"content_block_start","index":1,"content_block":{"type":"text","text":"partial "}}""",
                        """{"type":"content_block_start","index":2,"content_block":{"type":"tool_use","id":"t0","name":"list_files","input":{}}}""",
                        """{"type":"content_block_start","index":3,"content_block":{"type":"fallback","from":{"model":"a"},"to":{"model":"b"}}}""",
                        """{"type":"content_block_start","index":4,"content_block":{"type":"text","text":"done"}}"""
                )
                .forEach(asm::accept)
        val result = asm.finish()
        assertEquals(listOf("text", "fallback", "text"), result.content.map { it["type"]!!.jsonPrimitive.content })
        assertTrue(result.toolCalls.isEmpty())
    }

    // --- Paths and commands ---

    @Test
    fun documentIdsMapToPaths() {
        assertEquals("/storage/emulated/0/Projects/demo", DocPaths.idToPath("primary:Projects/demo"))
        assertEquals("/storage/1A2B-3C4D/code", DocPaths.idToPath("1A2B-3C4D:code"))
        assertEquals("/data/user/0/app/files/ubuntu/home/a", DocPaths.idToPath("/data/user/0/app/files/ubuntu/home/a/"))
        assertEquals("/storage/emulated/0/My Folder", DocPaths.idToPath("primary%3AMy%20Folder"))
        assertNull(DocPaths.idToPath("msf:123"))
    }

    @Test
    fun shellQuoteAndClean() {
        assertEquals("'it'\\''s'", CommandRunner.quote("it's"))
        assertEquals("red\nok", CommandRunner.clean("\u001B[31mred\u001B[0m\r\nok"))
    }
}
