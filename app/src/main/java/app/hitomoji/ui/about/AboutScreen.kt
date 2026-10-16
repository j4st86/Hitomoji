package app.hitomoji.ui.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp

private const val KANJIDIC_URL = "https://www.edrdg.org/wiki/index.php/KANJIDIC_Project"
private const val JMDICT_URL = "https://www.edrdg.org/wiki/index.php/JMdict-EDICT_Dictionary_Project"
private const val CC_BY_SA_URL = "https://creativecommons.org/licenses/by-sa/4.0/"

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val links = LocalUriHandler.current
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TextButton(onClick = onBack) { Text("Назад") }
            Text("О приложении", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Hitomoji — офлайн-тренажёр японских кандзи. Каждый календарный день вводится один новый иероглиф из пятисот самых частых. Повторы приходят отдельно, по тому, как вы вспомнили знак.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                "Иероглифы, чтения и число черт взяты из KANJIDIC2. Русские значения — перевод английских глосс KANJIDIC2. Эти глоссы восходят к JMdict, поэтому в данных есть производное от JMdict. Примеры предложений написаны для Hitomoji.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                "KANJIDIC2 и JMdict, Electronic Dictionary Research and Development Group. Лицензия CC BY-SA 4.0.",
                style = MaterialTheme.typography.bodyLarge,
            )
            TextButton(onClick = { links.openUri(KANJIDIC_URL) }) { Text("KANJIDIC2") }
            TextButton(onClick = { links.openUri(JMDICT_URL) }) { Text("JMdict") }
            TextButton(onClick = { links.openUri(CC_BY_SA_URL) }) { Text("CC BY-SA 4.0") }
        }
    }
}
