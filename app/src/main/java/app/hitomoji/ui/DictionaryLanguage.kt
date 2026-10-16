package app.hitomoji.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.hitomoji.HitomojiApp

@Composable
fun rememberDictionaryLanguage(): String {
    val context = LocalContext.current
    val app = context.applicationContext as HitomojiApp
    val fallback = if (LocalConfiguration.current.locales[0].language == "en") "en" else "ru"
    val language by app.container.repository.dictionaryLanguage(fallback)
        .collectAsStateWithLifecycle(fallback)
    return language
}

fun meaningsFor(language: String, russian: List<String>, english: List<String>): List<String> {
    return if (language == "en") english else russian
}

fun sentenceFor(language: String, russian: String, english: String): String {
    return if (language == "en") english else russian
}
