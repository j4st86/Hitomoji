package app.hitomoji.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.hitomoji.HitomojiApp
import app.hitomoji.data.repo.HomeSnapshot
import app.hitomoji.data.repo.HitomojiRepository
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDate
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: HitomojiRepository,
) : ViewModel() {
    var snapshot by mutableStateOf(HomeSnapshot(newCount = 0, reviewCount = 0, startRank = null))
        private set
    var ready by mutableStateOf(false)
        private set

    fun refresh() {
        viewModelScope.launch {
            snapshot = repository.homeSnapshot(LocalDate.now())
            ready = true
        }
    }

    companion object {
        fun factory(repository: HitomojiRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = HomeViewModel(repository) as T
        }
    }
}

@Composable
fun HomeScreen(
    onStart: (Int) -> Unit,
    onCatalog: () -> Unit,
    onAbout: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as HitomojiApp
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(app.container.repository))
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val snapshot = viewModel.snapshot
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Hitomoji",
                style = MaterialTheme.typography.displayMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(36.dp))
            Text(
                text = "Новых сегодня: ${snapshot.newCount}",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Повторов: ${snapshot.reviewCount}",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(40.dp))
            val startRank = snapshot.startRank
            if (startRank != null) {
                Button(onClick = { onStart(startRank) }) {
                    Text("Начать занятие")
                }
            } else if (viewModel.ready) {
                Text(
                    text = "На сегодня всё",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(48.dp))
            TextButton(onClick = onCatalog) { Text("Каталог") }
            TextButton(onClick = onAbout) { Text("О приложении") }
        }
    }
}
