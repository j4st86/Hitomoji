package app.hitomoji.ui.about

import android.app.LocaleManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.LocaleList
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.hitomoji.HitomojiApp
import app.hitomoji.R
import app.hitomoji.data.update.GithubUpdateChecker
import app.hitomoji.data.update.UpdateStatus
import app.hitomoji.ui.rememberDictionaryLanguage
import kotlinx.coroutines.launch
import java.util.Locale

private const val KANJIDIC_URL = "https://www.edrdg.org/wiki/index.php/KANJIDIC_Project"
private const val JMDICT_URL = "https://www.edrdg.org/wiki/index.php/JMdict-EDICT_Dictionary_Project"
private const val CC_BY_SA_URL = "https://creativecommons.org/licenses/by-sa/4.0/"

class AboutViewModel : ViewModel() {
    private val checker = GithubUpdateChecker()
    var checking by mutableStateOf(false)
        private set
    var status by mutableStateOf<UpdateStatus?>(null)
        private set

    fun check(localVersion: String) {
        if (checking) return
        checking = true
        viewModelScope.launch {
            status = checker.check(localVersion)
            checking = false
        }
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val links = LocalUriHandler.current
    val viewModel: AboutViewModel = viewModel()
    val scope = rememberCoroutineScope()
    val app = context.applicationContext as HitomojiApp
    val language = selectedLanguage(context)
    val dictionaryLanguage = rememberDictionaryLanguage()
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
            Text(stringResource(R.string.about), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.language), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LanguageButton(
                    label = stringResource(R.string.language_russian),
                    selected = language == "ru",
                    modifier = Modifier.weight(1f),
                ) { applyLanguage(context, "ru") }
                LanguageButton(
                    label = stringResource(R.string.language_english),
                    selected = language == "en",
                    modifier = Modifier.weight(1f),
                ) { applyLanguage(context, "en") }
            }
            Text(stringResource(R.string.dictionary_language), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LanguageButton(
                    label = stringResource(R.string.language_russian),
                    selected = dictionaryLanguage == "ru",
                    modifier = Modifier.weight(1f),
                ) { scope.launch { app.container.repository.setDictionaryLanguage("ru") } }
                LanguageButton(
                    label = stringResource(R.string.language_english),
                    selected = dictionaryLanguage == "en",
                    modifier = Modifier.weight(1f),
                ) { scope.launch { app.container.repository.setDictionaryLanguage("en") } }
            }
            Button(
                onClick = { viewModel.check(installedVersion(context)) },
                enabled = !viewModel.checking,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (viewModel.checking) {
                        stringResource(R.string.update_checking)
                    } else {
                        stringResource(R.string.check_updates)
                    },
                )
            }
            UpdateResult(viewModel.status) { links.openUri(it) }
            Text(stringResource(R.string.about_summary), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.about_sources), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.about_license), style = MaterialTheme.typography.bodyLarge)
            TextButton(onClick = { links.openUri(KANJIDIC_URL) }) { Text("KANJIDIC2") }
            TextButton(onClick = { links.openUri(JMDICT_URL) }) { Text("JMdict") }
            TextButton(onClick = { links.openUri(CC_BY_SA_URL) }) { Text("CC BY-SA 4.0") }
        }
    }
}

@Composable
private fun LanguageButton(
    label: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    if (selected) {
        Button(onClick = onClick, modifier = modifier) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) { Text(label) }
    }
}

@Composable
private fun UpdateResult(status: UpdateStatus?, onOpen: (String) -> Unit) {
    when (status) {
        null -> Unit
        UpdateStatus.Failed -> Text(
            stringResource(R.string.update_failed),
            style = MaterialTheme.typography.bodyLarge,
        )
        is UpdateStatus.UpToDate -> Text(
            stringResource(R.string.update_current, status.version),
            style = MaterialTheme.typography.bodyLarge,
        )
        is UpdateStatus.Available -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.update_available, status.version),
                style = MaterialTheme.typography.bodyLarge,
            )
            TextButton(onClick = { onOpen(status.pageUrl) }) {
                Text(stringResource(R.string.open_release))
            }
        }
    }
}

private fun selectedLanguage(context: Context): String {
    val chosen = context.getSystemService(LocaleManager::class.java).applicationLocales
    val language = if (chosen.isEmpty) Locale.getDefault().language else chosen.get(0).language
    return if (language == "en") "en" else "ru"
}

private fun applyLanguage(context: Context, languageTag: String) {
    context.getSystemService(LocaleManager::class.java).applicationLocales =
        LocaleList.forLanguageTags(languageTag)
    (context.applicationContext as HitomojiApp).refreshWidgets()
}

private fun installedVersion(context: Context): String {
    return context.packageManager.getPackageInfo(
        context.packageName,
        PackageManager.PackageInfoFlags.of(0),
    ).versionName ?: "0.0.0"
}
