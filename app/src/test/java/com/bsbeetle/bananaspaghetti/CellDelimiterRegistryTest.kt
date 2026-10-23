package com.bsbeetle.bananaspaghetti

import com.bsbeetle.bananaspaghetti.utils.notebook.CellDelimiterRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CellDelimiterRegistryTest {

    @Test
    fun `hash languages use the percent marker`() {
        assertEquals("# %%", CellDelimiterRegistry.getDelimiter("python"))
        assertEquals("# %%", CellDelimiterRegistry.getDelimiter("Ruby"))
        assertEquals("# %%", CellDelimiterRegistry.getDelimiter("sh"))
    }

    @Test
    fun `slash languages use the double slash marker`() {
        assertEquals("// %%", CellDelimiterRegistry.getDelimiter("javascript"))
        assertEquals("// %%", CellDelimiterRegistry.getDelimiter("rust"))
        assertEquals("// %%", CellDelimiterRegistry.getDelimiter("kotlin"))
    }

    @Test
    fun `unknown languages fall back to the python marker`() {
        assertEquals("# %%", CellDelimiterRegistry.getDelimiter("cobol"))
    }

    @Test
    fun `extensions map to the delimiter of their language`() {
        assertEquals("# %%", CellDelimiterRegistry.getDelimiterByExtension("py"))
        assertEquals("# %%", CellDelimiterRegistry.getDelimiterByExtension(".py"))
        assertEquals("// %%", CellDelimiterRegistry.getDelimiterByExtension(".mjs"))
        assertEquals("// %%", CellDelimiterRegistry.getDelimiterByExtension("hpp"))
        assertEquals("// %%", CellDelimiterRegistry.getDelimiterByExtension("KT"))
        assertEquals("# %%", CellDelimiterRegistry.getDelimiterByExtension("unknown"))
    }

    @Test
    fun `recognises delimiter lines`() {
        assertTrue(CellDelimiterRegistry.isCellDelimiter("# %%", "python"))
        assertTrue(CellDelimiterRegistry.isCellDelimiter("   # %%   ", "python"))
        assertTrue(CellDelimiterRegistry.isCellDelimiter("#%%", "python"))
        assertTrue(CellDelimiterRegistry.isCellDelimiter("# %% My Cell", "python"))
        assertTrue(CellDelimiterRegistry.isCellDelimiter("// %%", "javascript"))
    }

    @Test
    fun `rejects lines that only look similar`() {
        assertFalse(CellDelimiterRegistry.isCellDelimiter("# %%%", "python"))
        assertFalse(CellDelimiterRegistry.isCellDelimiter("x = 1  # %%", "python"))
        assertFalse(CellDelimiterRegistry.isCellDelimiter("// %%", "python"))
        assertFalse(CellDelimiterRegistry.isCellDelimiter("# comment", "python"))
    }

    @Test
    fun `only python files open as notebooks`() {
        assertTrue(CellDelimiterRegistry.isExtensionSupported("py"))
        assertTrue(CellDelimiterRegistry.isExtensionSupported(".PY"))
        assertFalse(CellDelimiterRegistry.isExtensionSupported("js"))
        assertEquals(listOf(".py"), CellDelimiterRegistry.getSupportedExtensions())
    }
}
