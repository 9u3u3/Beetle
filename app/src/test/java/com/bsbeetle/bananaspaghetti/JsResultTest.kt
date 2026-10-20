package com.bsbeetle.bananaspaghetti

import com.bsbeetle.bananaspaghetti.utils.webViewHandling.JsResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Every editor save goes through [JsResult.decodeString]. A wrong decode writes a changed file to
 * disk with no error, so these cases cover the escapes that the old `replace` chains broke.
 */
class JsResultTest {

    @Test
    fun `plain string decodes`() {
        assertEquals("hello", JsResult.decodeString("\"hello\""))
    }

    @Test
    fun `real newlines and tabs decode`() {
        assertEquals("a\n\tb\r\n", JsResult.decodeString("\"a\\n\\tb\\r\\n\""))
    }

    @Test
    fun `escaped newline in source code stays as backslash n`() {
        // Source text: printf("hi\n");  WebView JSON: "printf(\"hi\\n\");"
        val source = "printf(\"hi\\n\");"
        assertEquals(source, JsResult.decodeString("\"printf(\\\"hi\\\\n\\\");\""))
    }

    @Test
    fun `windows path keeps single backslashes`() {
        assertEquals("C:\\temp\\new", JsResult.decodeString("\"C:\\\\temp\\\\new\""))
    }

    @Test
    fun `unicode escapes decode`() {
        assertEquals("<div>é</div>", JsResult.decodeString("\"\\u003Cdiv\\u003E\\u00e9\\u003C/div\\u003E\""))
    }

    @Test
    fun `escaped backslash before u is not a unicode escape`() {
        assertEquals("\\u0041", JsResult.decodeString("\"\\\\u0041\""))
    }

    @Test
    fun `single quote passes through`() {
        assertEquals("it's", JsResult.decodeString("\"it's\""))
    }

    @Test
    fun `null and non string results return null`() {
        assertNull(JsResult.decodeString(null))
        assertNull(JsResult.decodeString("null"))
        assertNull(JsResult.decodeString("42"))
        assertNull(JsResult.decodeString("{\"count\":1}"))
    }

    @Test
    fun `empty string decodes to empty`() {
        assertEquals("", JsResult.decodeString("\"\""))
    }
}
