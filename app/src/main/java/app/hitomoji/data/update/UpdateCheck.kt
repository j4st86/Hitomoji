package app.hitomoji.data.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

sealed interface UpdateStatus {
    data class UpToDate(val version: String) : UpdateStatus
    data class Available(val version: String, val pageUrl: String) : UpdateStatus
    data object Failed : UpdateStatus
}

internal fun versionNumbers(raw: String): List<Int> {
    val cleaned = raw.trim().removePrefix("v").removePrefix("V")
    if (cleaned.isEmpty()) return emptyList()
    return cleaned.split('.', '-', '+').map { part ->
        part.takeWhile { it.isDigit() }.toIntOrNull() ?: 0
    }
}

fun isNewerRelease(localVersion: String, remoteTag: String): Boolean {
    val local = versionNumbers(localVersion)
    val remote = versionNumbers(remoteTag)
    val size = maxOf(local.size, remote.size)
    for (index in 0 until size) {
        val left = local.getOrElse(index) { 0 }
        val right = remote.getOrElse(index) { 0 }
        if (right != left) return right > left
    }
    return false
}

class GithubUpdateChecker(
    private val latestUrl: String = LATEST_RELEASE_URL,
    private val readBody: (String) -> String = ::readUrl,
) {
    suspend fun check(localVersion: String): UpdateStatus = withContext(Dispatchers.IO) {
        try {
            val body = readBody(latestUrl)
            val release = json.decodeFromString<GithubRelease>(body)
            val version = release.tagName.trim().removePrefix("v").removePrefix("V")
            if (version.isEmpty() || release.htmlUrl.isBlank()) return@withContext UpdateStatus.Failed
            if (isNewerRelease(localVersion, release.tagName)) {
                UpdateStatus.Available(version, release.htmlUrl)
            } else {
                UpdateStatus.UpToDate(localVersion)
            }
        } catch (_: Exception) {
            UpdateStatus.Failed
        }
    }

    private companion object {
        const val LATEST_RELEASE_URL = "https://api.github.com/repos/j4st86/Hitomoji/releases/latest"
        val json = Json { ignoreUnknownKeys = true }
    }
}

@Serializable
private data class GithubRelease(
    @SerialName("tag_name") val tagName: String,
    @SerialName("html_url") val htmlUrl: String,
)

private fun readUrl(url: String): String {
    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 15_000
        readTimeout = 15_000
        setRequestProperty("Accept", "application/vnd.github+json")
        setRequestProperty("User-Agent", "Hitomoji")
    }
    try {
        val stream = if (connection.responseCode in 200..299) {
            connection.inputStream
        } else {
            error("GitHub responded ${connection.responseCode}")
        }
        return stream.bufferedReader().use { it.readText() }
    } finally {
        connection.disconnect()
    }
}
