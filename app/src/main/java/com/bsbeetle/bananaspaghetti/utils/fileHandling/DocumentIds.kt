package com.bsbeetle.bananaspaghetti.utils.fileHandling

/**
 * Helpers on document IDs, which both Beetle's own storage provider (absolute paths) and the system
 * one ("primary:folder/file") build as slash-separated paths.
 */
object DocumentIds {

    /** True when [candidateId] is [ancestorId] itself or lies anywhere below it. */
    fun isSameOrDescendant(ancestorId: String, candidateId: String): Boolean {
        val ancestor = ancestorId.trimEnd('/')
        val candidate = candidateId.trimEnd('/')
        return candidate == ancestor || candidate.startsWith("$ancestor/")
    }

    /** True when [childId] sits directly inside [parentId]. */
    fun isDirectChild(parentId: String, childId: String): Boolean {
        val child = childId.trimEnd('/')
        val slash = child.lastIndexOf('/')
        if (slash < 0) return false
        return child.substring(0, slash) == parentId.trimEnd('/')
    }
}
