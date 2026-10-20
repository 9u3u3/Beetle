package com.bsbeetle.bananaspaghetti

import com.bsbeetle.bananaspaghetti.utils.UpdatesService
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdatesServiceTest {

    @Test
    fun newerVersionIsFound() {
        assertTrue(UpdatesService.isNewer("v1.2.0", "1.1.0"))
        assertTrue(UpdatesService.isNewer("v1.1.1", "1.1.0"))
        assertTrue(UpdatesService.isNewer("v2.0", "1.9.9"))
        assertTrue(UpdatesService.isNewer("v1.10.0", "1.9.0"))
    }

    @Test
    fun sameOrOlderVersionIsNotNewer() {
        assertFalse(UpdatesService.isNewer("v1.1.0", "1.1.0"))
        assertFalse(UpdatesService.isNewer("v1.1", "1.1.0"))
        assertFalse(UpdatesService.isNewer("v1.0.0", "1.1.0"))
    }

    @Test
    fun releaseNotesBecomePlainText() {
        val notes = "## What is new\n\n- **AI agent.** Uses `beetle.apk`.\n1. Step"
        org.junit.Assert.assertEquals(
                "What is new\n\n• AI agent. Uses beetle.apk.\n1. Step",
                UpdatesService.plainText(notes)
        )
    }
}
