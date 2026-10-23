package com.bsbeetle.bananaspaghetti

import com.bsbeetle.bananaspaghetti.utils.fileHandling.DocumentIds
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentIdsTest {

    @Test
    fun `a folder contains itself and everything below it`() {
        assertTrue(DocumentIds.isSameOrDescendant("/home/proj", "/home/proj"))
        assertTrue(DocumentIds.isSameOrDescendant("/home/proj", "/home/proj/src"))
        assertTrue(DocumentIds.isSameOrDescendant("/home/proj", "/home/proj/src/deep/er"))
        assertTrue(DocumentIds.isSameOrDescendant("/home/proj/", "/home/proj/src"))
        assertTrue(DocumentIds.isSameOrDescendant("primary:Docs/a", "primary:Docs/a/b"))
    }

    @Test
    fun `siblings with a shared name prefix are not descendants`() {
        assertFalse(DocumentIds.isSameOrDescendant("/home/proj", "/home/project"))
        assertFalse(DocumentIds.isSameOrDescendant("/home/proj", "/home"))
        assertFalse(DocumentIds.isSameOrDescendant("/home/proj/src", "/home/proj"))
        assertFalse(DocumentIds.isSameOrDescendant("primary:Docs/a", "primary:Docs/ab"))
    }

    @Test
    fun `detects direct children only`() {
        assertTrue(DocumentIds.isDirectChild("/home/proj", "/home/proj/main.py"))
        assertTrue(DocumentIds.isDirectChild("/home/proj/", "/home/proj/src/"))
        assertTrue(DocumentIds.isDirectChild("primary:Docs", "primary:Docs/a"))
        assertFalse(DocumentIds.isDirectChild("/home/proj", "/home/proj/src/util.py"))
        assertFalse(DocumentIds.isDirectChild("/home/proj", "/home/proj"))
        assertFalse(DocumentIds.isDirectChild("/home/proj", "main.py"))
    }
}
