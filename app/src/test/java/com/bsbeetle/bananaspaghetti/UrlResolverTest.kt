package com.bsbeetle.bananaspaghetti

import com.bsbeetle.bananaspaghetti.utils.UrlResolver
import org.junit.Assert.assertEquals
import org.junit.Test

class UrlResolverTest {

    @Test
    fun `keeps urls that already have a scheme`() {
        assertEquals("http://example.com/a", UrlResolver.resolve("http://example.com/a"))
        assertEquals("https://example.com", UrlResolver.resolve("https://example.com"))
        assertEquals("about:blank", UrlResolver.resolve("about:blank"))
    }

    @Test
    fun `local addresses use plain http`() {
        assertEquals("http://localhost:5500/index.html", UrlResolver.resolve("localhost:5500/index.html"))
        assertEquals("http://localhost", UrlResolver.resolve("localhost"))
        assertEquals("http://127.0.0.1:3000", UrlResolver.resolve("127.0.0.1:3000"))
        assertEquals("http://192.168.1.20:8080/x", UrlResolver.resolve("192.168.1.20:8080/x"))
        assertEquals("http://10.0.0.5", UrlResolver.resolve("10.0.0.5"))
        assertEquals("http://172.16.0.1", UrlResolver.resolve("172.16.0.1"))
        assertEquals("http://mybox.local:8000", UrlResolver.resolve("mybox.local:8000"))
    }

    @Test
    fun `public hosts use https`() {
        assertEquals("https://example.com", UrlResolver.resolve("example.com"))
        assertEquals("https://example.com/path?q=1", UrlResolver.resolve("example.com/path?q=1"))
        assertEquals("https://172.32.0.1", UrlResolver.resolve("172.32.0.1"))
        assertEquals("https://8.8.8.8", UrlResolver.resolve("8.8.8.8"))
    }

    @Test
    fun `anything else is a search`() {
        assertEquals("https://www.google.com/search?q=hello", UrlResolver.resolve("hello"))
        assertEquals(
                "https://www.google.com/search?q=kotlin+coroutines",
                UrlResolver.resolve("kotlin coroutines")
        )
        assertEquals(
                "https://www.google.com/search?q=what+is+example.com",
                UrlResolver.resolve("what is example.com")
        )
    }

    @Test
    fun `blank input loads a blank page`() {
        assertEquals("about:blank", UrlResolver.resolve("   "))
    }
}
