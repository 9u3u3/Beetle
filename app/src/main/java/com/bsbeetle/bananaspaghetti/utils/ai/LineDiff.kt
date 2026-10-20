package com.bsbeetle.bananaspaghetti.utils.ai

/**
 * Line-level diff, used to report what an AI edit actually changed.
 *
 * The assistant rewrites whole files, so without this the user is told "applied" and left to spot
 * the difference themselves. A `+12 −3` summary and the changed lines turn an opaque overwrite into
 * a reviewable one.
 */
object LineDiff {

    enum class Type {
        CONTEXT,
        ADD,
        REMOVE
    }

    data class Line(val type: Type, val text: String)

    data class Result(val added: Int, val removed: Int, val preview: List<Line>)

    /**
     * Diffs [before] against [after].
     *
     * [previewLimit] caps the returned lines — a full rewrite of a large file would otherwise
     * produce a preview longer than the file, on a phone screen.
     *
     * The LCS table is O(n·m) in both time and memory, which is fine for source files but not for
     * a minified bundle or a large data file. Past [MAX_LCS_LINES] the counts are computed with a
     * multiset comparison instead, so the summary stays honest about totals while giving up on
     * showing precisely which lines moved.
     */
    fun of(before: String, after: String, previewLimit: Int = 60): Result {
        val old = before.lines()
        val new = after.lines()

        if (old.size.toLong() * new.size.toLong() > MAX_LCS_LINES) {
            return coarse(old, new)
        }

        // lcs[i][j] = length of the longest common subsequence of old[i..] and new[j..].
        val lcs = Array(old.size + 1) { IntArray(new.size + 1) }
        for (i in old.indices.reversed()) {
            for (j in new.indices.reversed()) {
                lcs[i][j] =
                        if (old[i] == new[j]) lcs[i + 1][j + 1] + 1
                        else maxOf(lcs[i + 1][j], lcs[i][j + 1])
            }
        }

        val lines = mutableListOf<Line>()
        var added = 0
        var removed = 0
        var i = 0
        var j = 0
        while (i < old.size && j < new.size) {
            when {
                old[i] == new[j] -> {
                    lines += Line(Type.CONTEXT, old[i])
                    i++
                    j++
                }
                lcs[i + 1][j] >= lcs[i][j + 1] -> {
                    lines += Line(Type.REMOVE, old[i])
                    removed++
                    i++
                }
                else -> {
                    lines += Line(Type.ADD, new[j])
                    added++
                    j++
                }
            }
        }
        while (i < old.size) {
            lines += Line(Type.REMOVE, old[i])
            removed++
            i++
        }
        while (j < new.size) {
            lines += Line(Type.ADD, new[j])
            added++
            j++
        }

        // No change means nothing to preview — otherwise [condense] elides the whole file into a
        // single gap marker and callers render an empty diff card around it.
        if (added == 0 && removed == 0) return Result(0, 0, emptyList())

        return Result(added, removed, condense(lines, previewLimit))
    }

    /**
     * Drops runs of unchanged lines, keeping [CONTEXT_LINES] either side of each change.
     *
     * Without this the preview of a one-line fix in a 200-line file is 200 lines, and the change is
     * exactly as hard to find as it was before.
     */
    private fun condense(lines: List<Line>, limit: Int): List<Line> {
        val keep = BooleanArray(lines.size)
        lines.forEachIndexed { index, line ->
            if (line.type != Type.CONTEXT) {
                val from = maxOf(0, index - CONTEXT_LINES)
                val to = minOf(lines.size - 1, index + CONTEXT_LINES)
                for (k in from..to) keep[k] = true
            }
        }

        val out = mutableListOf<Line>()
        var skipping = false
        for (index in lines.indices) {
            if (keep[index]) {
                out += lines[index]
                skipping = false
            } else if (!skipping) {
                out += Line(Type.CONTEXT, "…")
                skipping = true
            }
            if (out.size >= limit) break
        }
        return out
    }

    /** Counts-only fallback for files too large to diff properly. */
    private fun coarse(old: List<String>, new: List<String>): Result {
        val oldCounts = old.groupingBy { it }.eachCount().toMutableMap()
        var added = 0
        new.forEach { line ->
            val remaining = oldCounts[line] ?: 0
            if (remaining > 0) oldCounts[line] = remaining - 1 else added++
        }
        val removed = oldCounts.values.sum()
        return Result(added, removed, emptyList())
    }

    private const val CONTEXT_LINES = 2
    private const val MAX_LCS_LINES = 4_000_000L
}
