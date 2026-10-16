package app.hitomoji.data.update

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckTest {
    @Test
    fun sameVersionIsNotNewer() {
        assertFalse(isNewerRelease("0.0.1", "v0.0.1"))
    }

    @Test
    fun higherPatchIsNewer() {
        assertTrue(isNewerRelease("0.0.1", "v0.0.2"))
    }

    @Test
    fun localBuildAheadOfGitHubIsNotAnUpdate() {
        assertFalse(isNewerRelease("0.0.2", "v0.0.1"))
    }

    @Test
    fun minorBeatsPatch() {
        assertTrue(isNewerRelease("0.0.9", "v0.1.0"))
    }

    @Test
    fun checkerReadsTheLatestReleaseBody() = runBlocking {
        val checker = GithubUpdateChecker(readBody = {
            """{"tag_name":"v0.0.2","html_url":"https://github.com/j4st86/Hitomoji/releases/tag/v0.0.2"}"""
        })
        val status = checker.check("0.0.1")
        assertEquals(
            UpdateStatus.Available("0.0.2", "https://github.com/j4st86/Hitomoji/releases/tag/v0.0.2"),
            status,
        )
    }

    @Test
    fun brokenResponseFailsTheCheck() = runBlocking {
        val checker = GithubUpdateChecker(readBody = { error("offline") })
        assertEquals(UpdateStatus.Failed, checker.check("0.0.1"))
    }
}
