package app.hitomoji.ui.about

import android.app.LocaleManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.LocaleList
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import app.hitomoji.ui.AppTab
import app.hitomoji.ui.HitomojiNavigationBar
import app.hitomoji.ui.HitomojiTopBar
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
fun AboutScreen(onSelectTab: (AppTab) -> Unit) {
    val context = LocalContext.current
    val links = LocalUriHandler.current
    val viewModel: AboutViewModel = viewModel()
    val scope = rememberCoroutineScope()
    val app = context.applicationContext as HitomojiApp
    val language = selectedLanguage(context)
    val dictionaryLanguage = rememberDictionaryLanguage()
    Scaffold(
        topBar = { HitomojiTopBar(title = stringResource(R.string.about)) },
        bottomBar = { HitomojiNavigationBar(AppTab.About, onSelectTab) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionLabel(stringResource(R.string.language))
            TwoChoice(
                left = stringResource(R.string.language_russian),
                right = stringResource(R.string.language_english),
                leftSelected = language == "ru",
                onLeft = { applyLanguage(context, "ru") },
                onRight = { applyLanguage(context, "en") },
            )
            SectionLabel(stringResource(R.string.dictionary_language))
            TwoChoice(
                left = stringResource(R.string.language_russian),
                right = stringResource(R.string.language_english),
                leftSelected = dictionaryLanguage == "ru",
                onLeft = { scope.launch { app.container.repository.setDictionaryLanguage("ru") } },
                onRight = { scope.launch { app.container.repository.setDictionaryLanguage("en") } },
            )
            FilledTonalButton(
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
            Text(
                stringResource(R.string.about_sources),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(R.string.about_license),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LinkRow("KANJIDIC2") { links.openUri(KANJIDIC_URL) }
            LinkRow("JMdict") { links.openUri(JMDICT_URL) }
            LinkRow("CC BY-SA 4.0") { links.openUri(CC_BY_SA_URL) }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TwoChoice(
    left: String,
    right: String,
    leftSelected: Boolean,
    onLeft: () -> Unit,
    onRight: () -> Unit,
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        SegmentedButton(
            selected = leftSelected,
            onClick = onLeft,
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
        ) { Text(left) }
        SegmentedButton(
            selected = !leftSelected,
            onClick = onRight,
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
        ) { Text(right) }
    }
}

@Composable
private fun LinkRow(label: String, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(label) },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
            )
        },
    )
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
            Button(onClick = { onOpen(status.pageUrl) }, modifier = Modifier.fillMaxWidth()) {
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
