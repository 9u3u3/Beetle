package com.bsbeetle.bananaspaghetti

import com.bsbeetle.bananaspaghetti.directGit.utils.GitUrlConverter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GitUrlConverterTest {

    @Test
    fun `converts https urls of the known hosts to ssh`() {
        assertEquals("git@github.com:user/repo.git", GitUrlConverter.httpsToSsh("https://github.com/user/repo.git"))
        assertEquals("git@gitlab.com:group/sub/repo.git", GitUrlConverter.httpsToSsh("https://gitlab.com/group/sub/repo.git"))
        assertEquals("git@bitbucket.org:team/repo.git", GitUrlConverter.httpsToSsh("https://bitbucket.org/team/repo.git"))
    }

    @Test
    fun `converts ssh urls of the known hosts to https`() {
        assertEquals("https://github.com/user/repo.git", GitUrlConverter.sshToHttps("git@github.com:user/repo.git"))
        assertEquals("https://gitlab.com/group/repo.git", GitUrlConverter.sshToHttps("git@gitlab.com:group/repo.git"))
        assertEquals("https://bitbucket.org/team/repo.git", GitUrlConverter.sshToHttps("git@bitbucket.org:team/repo.git"))
    }

    @Test
    fun `leaves unknown hosts and already converted urls alone`() {
        assertEquals("https://example.com/user/repo.git", GitUrlConverter.httpsToSsh("https://example.com/user/repo.git"))
        assertEquals("git@github.com:user/repo.git", GitUrlConverter.httpsToSsh("git@github.com:user/repo.git"))
        assertEquals("git@example.com:user/repo.git", GitUrlConverter.sshToHttps("git@example.com:user/repo.git"))
    }

    @Test
    fun `ensureSsh only rewrites https urls`() {
        assertEquals("git@github.com:u/r.git", GitUrlConverter.ensureSsh("https://github.com/u/r.git"))
        assertEquals("git@github.com:u/r.git", GitUrlConverter.ensureSsh("git@github.com:u/r.git"))
    }

    @Test
    fun `detects the url scheme`() {
        assertTrue(GitUrlConverter.isSshUrl("git@github.com:u/r.git"))
        assertFalse(GitUrlConverter.isSshUrl("https://github.com/u/r.git"))
        assertTrue(GitUrlConverter.isHttpsUrl("https://github.com/u/r.git"))
        assertFalse(GitUrlConverter.isHttpsUrl("git@github.com:u/r.git"))
    }

    @Test
    fun `ignores whitespace around a pasted url`() {
        assertEquals("git@github.com:u/r.git", GitUrlConverter.ensureSsh("  https://github.com/u/r.git\n"))
        assertEquals("git@github.com:u/r.git", GitUrlConverter.httpsToSsh("https://github.com/u/r.git "))
        assertEquals("https://github.com/u/r.git", GitUrlConverter.sshToHttps("\tgit@github.com:u/r.git"))
        assertTrue(GitUrlConverter.isHttpsUrl(" https://github.com/u/r.git"))
        assertTrue(GitUrlConverter.isSshUrl("git@github.com:u/r.git\n"))
    }
}
