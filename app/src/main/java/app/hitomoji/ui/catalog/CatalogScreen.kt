package app.hitomoji.ui.catalog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import app.hitomoji.ui.AppTab
import app.hitomoji.ui.HitomojiNavigationBar
import app.hitomoji.ui.HitomojiTopBar
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.hitomoji.HitomojiApp
import app.hitomoji.R
import app.hitomoji.ui.label
import app.hitomoji.ui.rememberDictionaryLanguage
import app.hitomoji.data.repo.CatalogRow
import app.hitomoji.data.repo.HitomojiRepository
import app.hitomoji.data.repo.KanjiDetail
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
fun CatalogScreen(onOpen: (Int) -> Unit, onSelectTab: (AppTab) -> Unit) {
    val language = rememberDictionaryLanguage()
    val app = LocalContext.current.applicationContext as HitomojiApp
    val viewModel: CatalogViewModel = viewModel(factory = CatalogViewModel.factory(app.container.repository))
    Scaffold(
        topBar = { HitomojiTopBar(title = stringResource(R.string.catalog)) },
        bottomBar = { HitomojiNavigationBar(AppTab.Catalog, onSelectTab) },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = padding) {
            items(viewModel.rows, key = { it.rank }) { row ->
                CatalogRowItem(row, language, onOpen)
            }
        }
    }
}

@Composable
private fun CatalogRowItem(row: CatalogRow, language: String, onOpen: (Int) -> Unit) {
    ListItem(
        modifier = Modifier.clickable { onOpen(row.rank) },
        leadingContent = { JapaneseText(text = row.character, fontSize = 32.sp) },
        headlineContent = {
            Text(if (language == "en") row.meaningEn else row.meaningRu)
        },
        supportingContent = {
            Text(row.state.label(LocalContext.current))
        },
    )
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
        Scaffold(
            topBar = { HitomojiTopBar(title = "", onUp = onBack) },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    } else {
        ReadOnlyKanjiScreen(card = detail.card, onBack = onBack)
    }
}
