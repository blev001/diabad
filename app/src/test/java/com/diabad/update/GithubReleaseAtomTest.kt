package com.diabad.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class GithubReleaseAtomTest {

    @Test
    fun parsesFirstReleaseFromGithubAtom() {
        val feed = """
            <?xml version="1.0" encoding="UTF-8"?>
            <feed xmlns="http://www.w3.org/2005/Atom">
              <entry>
                <id>tag:github.com,2008:Repository/1367560752/v20</id>
                <link rel="alternate" href="https://github.com/blev001/diabad/releases/tag/v20"/>
                <title>0.5.13</title>
                <content type="html">&lt;p&gt;DiaBAD 0.5.13 (versionCode 20)&lt;/p&gt;</content>
              </entry>
              <entry>
                <id>tag:github.com,2008:Repository/1367560752/v19</id>
                <link rel="alternate" href="https://github.com/blev001/diabad/releases/tag/v19"/>
                <title>0.5.12</title>
              </entry>
            </feed>
        """.trimIndent()

        val release = GithubReleaseAtom.firstRelease(feed)
        assertNotNull(release)
        assertEquals("v20", release!!.tag)
        assertEquals("0.5.13", release.title)
        assertEquals("DiaBAD 0.5.13 (versionCode 20)", release.notes)
    }

    @Test
    fun parseVersionCodeFromTag() {
        assertEquals(20, GithubReleaseAtom.parseVersionCode("v20"))
        assertEquals(19, GithubReleaseAtom.parseVersionCode("V19"))
        assertEquals(21, GithubReleaseAtom.parseVersionCode("v21-beta"))
        assertNull(GithubReleaseAtom.parseVersionCode("latest"))
    }

    @Test
    fun emptyFeedYieldsNull() {
        assertNull(GithubReleaseAtom.firstRelease("<feed></feed>"))
    }
}
