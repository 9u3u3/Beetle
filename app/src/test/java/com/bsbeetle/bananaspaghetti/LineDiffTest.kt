package com.bsbeetle.bananaspaghetti

import com.bsbeetle.bananaspaghetti.utils.ai.LineDiff
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The `+n −m` an AI edit reports comes from here, and a wrong count is worse than none — it tells
 * the user a file changed less (or more) than it did, right as they decide whether to undo.
 */
class LineDiffTest {

    @Test
    fun `identical input reports no change`() {
        val text = "int main() {\n    return 0;\n}"
        val result = LineDiff.of(text, text)

        assertEquals(0, result.added)
        assertEquals(0, result.removed)
        assertTrue(result.preview.isEmpty())
    }

    @Test
    fun `counts an added line without touching the rest`() {
        val before = "#include <stdio.h>\n\nint main() {\n    return 0;\n}"
        val after = "#include <stdio.h>\n#include <stdlib.h>\n\nint main() {\n    return 0;\n}"

        val result = LineDiff.of(before, after)

        assertEquals(1, result.added)
        assertEquals(0, result.removed)
    }

    @Test
    fun `a modified line counts as one add and one remove`() {
        val before = "printf(\"Hello\");"
        val after = "printf(\"Hello, World!\");"

        val result = LineDiff.of(before, after)

        assertEquals(1, result.added)
        assertEquals(1, result.removed)
    }

    @Test
    fun `preview elides unchanged regions instead of reprinting the file`() {
        val before = (1..200).joinToString("\n") { "line $it" }
        val after = before.replace("line 100", "line 100 changed")

        val result = LineDiff.of(before, after)

        assertEquals(1, result.added)
        assertEquals(1, result.removed)
        // Two changed lines, a little context either side, and gap markers — not 200 lines.
        assertTrue("preview was ${result.preview.size} lines", result.preview.size < 12)
        assertTrue(result.preview.any { it.text == "…" })
    }

    @Test
    fun `preview is capped so a full rewrite cannot flood the panel`() {
        val before = (1..500).joinToString("\n") { "old $it" }
        val after = (1..500).joinToString("\n") { "new $it" }

        val result = LineDiff.of(before, after, previewLimit = 60)

        assertEquals(500, result.added)
        assertEquals(500, result.removed)
        assertTrue(result.preview.size <= 60)
    }

    @Test
    fun `additions and removals are labelled with the side they came from`() {
        val result = LineDiff.of("keep\ndrop", "keep\nadd")

        assertEquals(listOf("keep", "drop", "add"), result.preview.map { it.text })
        assertEquals(
                listOf(LineDiff.Type.CONTEXT, LineDiff.Type.REMOVE, LineDiff.Type.ADD),
                result.preview.map { it.type }
        )
    }
}
