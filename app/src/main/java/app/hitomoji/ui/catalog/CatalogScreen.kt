package app.hitomoji.ui.catalog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.hitomoji.HitomojiApp
import app.hitomoji.data.repo.CatalogRow
import app.hitomoji.data.repo.HitomojiRepository
import app.hitomoji.data.repo.KanjiDetail
import app.hitomoji.ui.ruLabel
import app.hitomoji.ui.study.ReadOnlyKanjiScreen
import app.hitomoji.ui.theme.JapaneseText
import kotlinx.coroutines.launch

class CatalogViewModel(
    private val repository: HitomojiRepository,
) : ViewModel() {
    var rows by mutableStateOf<List<CatalogRow>>(emptyList())
        private set

    init {
        viewModelScope.launch { rows = repository.catalog() }
    }

    companion object {
        fun factory(repository: HitomojiRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = CatalogViewModel(repository) as T
        }
    }
}

class KanjiDetailViewModel(
    private val repository: HitomojiRepository,
    private val rank: Int,
) : ViewModel() {
    var detail by mutableStateOf<KanjiDetail?>(null)
        private set

    init {
        viewModelScope.launch { detail = repository.detail(rank) }
    }

    companion object {
        fun factory(repository: HitomojiRepository, rank: Int) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return KanjiDetailViewModel(repository, rank) as T
            }
        }
    }
}

@Composable
fun CatalogScreen(onBack: () -> Unit, onOpen: (Int) -> Unit) {
    val app = LocalContext.current.applicationContext as HitomojiApp
    val viewModel: CatalogViewModel = viewModel(factory = CatalogViewModel.factory(app.container.repository))
    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TextButton(onClick = onBack, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text("Назад")
            }
            Text(
                text = "Каталог",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                items(viewModel.rows, key = { it.rank }) { row ->
                    CatalogRowItem(row, onOpen)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun CatalogRowItem(row: CatalogRow, onOpen: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen(row.rank) }
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        JapaneseText(text = row.character, fontSize = 32.sp)
        Column(Modifier.weight(1f)) {
            Text(row.meaning, style = MaterialTheme.typography.bodyLarge)
            Text(
                row.state.ruLabel(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun KanjiDetailScreen(rank: Int, onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as HitomojiApp
    val viewModel: KanjiDetailViewModel = viewModel(
        key = "detail-$rank",
        factory = KanjiDetailViewModel.factory(app.container.repository, rank),
    )
    val detail = viewModel.detail
    if (detail == null) {
        Scaffold { padding ->
            Text(
                "Hitomoji",
                modifier = Modifier.padding(padding).padding(24.dp),
                style = MaterialTheme.typography.headlineMedium,
            )
        }
    } else {
        ReadOnlyKanjiScreen(card = detail.card, onBack = onBack)
    }
}
