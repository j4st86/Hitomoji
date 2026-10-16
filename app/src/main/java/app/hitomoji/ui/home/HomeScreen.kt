package app.hitomoji.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.hitomoji.ui.AppTab
import app.hitomoji.ui.HitomojiNavigationBar
import app.hitomoji.ui.HitomojiTopBar
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.hitomoji.HitomojiApp
import app.hitomoji.R
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
    onSelectTab: (AppTab) -> Unit,
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
    Scaffold(
        topBar = { HitomojiTopBar(title = "Hitomoji") },
        bottomBar = { HitomojiNavigationBar(AppTab.Home, onSelectTab) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (!viewModel.ready) {
                CircularProgressIndicator()
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        value = snapshot.newCount,
                        label = stringResource(R.string.new_today),
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        value = snapshot.reviewCount,
                        label = stringResource(R.string.reviews_today),
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(24.dp))
                val startRank = snapshot.startRank
                if (startRank != null) {
                    Button(
                        onClick = { onStart(startRank) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.start_session))
                    }
                } else {
                    Text(
                        text = stringResource(R.string.done_for_today),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(value: Int, label: String, modifier: Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.displaySmall,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
