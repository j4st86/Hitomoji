package app.hitomoji.ui.study

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import app.hitomoji.ui.HitomojiTopBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.hitomoji.HitomojiApp
import app.hitomoji.R
import app.hitomoji.data.repo.HitomojiRepository
import app.hitomoji.data.repo.StudyCard
import app.hitomoji.domain.Grade
import app.hitomoji.ui.meaningsFor
import app.hitomoji.ui.rememberDictionaryLanguage
import app.hitomoji.ui.sentenceFor
import app.hitomoji.ui.theme.JapaneseText
import androidx.compose.ui.platform.LocalContext
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class StudyUi(
    val loading: Boolean = true,
    val done: Boolean = false,
    val gradingAllowed: Boolean = false,
    val revealed: Boolean = false,
    val saving: Boolean = false,
    val character: String = "",
    val sentenceJp: String = "",
    val sentenceKana: String = "",
    val sentenceRu: String = "",
    val sentenceEn: String = "",
    val meaningsRu: List<String> = emptyList(),
    val meaningsEn: List<String> = emptyList(),
    val onReadings: List<String> = emptyList(),
    val kunReadings: List<String> = emptyList(),
    val position: Int = 0,
    val total: Int = 0,
)

class StudyViewModel(
    private val repository: HitomojiRepository,
    private val startRank: Int,
    private val gradingAllowed: Boolean,
    private val refreshWidgets: suspend () -> Unit,
) : ViewModel() {
    var ui by mutableStateOf(StudyUi(gradingAllowed = gradingAllowed))
        private set

    private var cards: List<StudyCard> = emptyList()
    private var index: Int = 0

    init {
        viewModelScope.launch {
            val session = repository.session(LocalDate.now(), startRank)
            cards = session.cards
            index = if (cards.isEmpty()) 0 else session.startIndex.coerceIn(0, cards.lastIndex)
            show(revealed = !gradingAllowed)
        }
    }

    fun reveal() {
        if (!ui.revealed) ui = ui.copy(revealed = true)
    }

    fun grade(grade: Grade) {
        if (!gradingAllowed || ui.saving || ui.done || ui.loading) return
        val card = cards.getOrNull(index) ?: return
        ui = ui.copy(saving = true)
        viewModelScope.launch {
            withContext(NonCancellable) {
                repository.grade(card.rank, grade, LocalDate.now(), Instant.now())
                refreshWidgets()
            }
            index += 1
            show(revealed = false)
        }
    }

    private fun show(revealed: Boolean) {
        val card = cards.getOrNull(index)
        if (card == null) {
            ui = StudyUi(loading = false, done = true, gradingAllowed = gradingAllowed)
            return
        }
        ui = StudyUi(
            loading = false,
            done = false,
            gradingAllowed = gradingAllowed,
            revealed = revealed || !gradingAllowed,
            character = card.character,
            sentenceJp = card.sentenceJp,
            sentenceKana = card.sentenceKana,
            sentenceRu = card.sentenceRu,
            sentenceEn = card.sentenceEn,
            meaningsRu = card.meaningsRu,
            meaningsEn = card.meaningsEn,
            onReadings = card.onReadings,
            kunReadings = card.kunReadings,
            position = index + 1,
            total = cards.size,
        )
    }

    companion object {
        fun factory(
            repository: HitomojiRepository,
            startRank: Int,
            gradingAllowed: Boolean,
            refreshWidgets: suspend () -> Unit,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return StudyViewModel(repository, startRank, gradingAllowed, refreshWidgets) as T
            }
        }
    }
}

@Composable
fun StudyScreen(
    startRank: Int,
    gradingAllowed: Boolean,
    onFinishedHome: () -> Unit,
    onUp: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as HitomojiApp
    val viewModel: StudyViewModel = viewModel(
        key = "study-$startRank-$gradingAllowed",
        factory = StudyViewModel.factory(
            repository = app.container.repository,
            startRank = startRank,
            gradingAllowed = gradingAllowed,
            refreshWidgets = { app.hitomojiRefreshWidgets() },
        ),
    )
    StudyContent(
        ui = viewModel.ui,
        dictionaryLanguage = rememberDictionaryLanguage(),
        onReveal = viewModel::reveal,
        onGrade = viewModel::grade,
        onHome = onFinishedHome,
        onUp = onUp,
    )
}

internal suspend fun HitomojiApp.hitomojiRefreshWidgets() {
    app.hitomoji.widget.WidgetRefresher(this).refresh()
}

@Composable
fun ReadOnlyKanjiScreen(card: StudyCard, onBack: () -> Unit) {
    StudyContent(
        dictionaryLanguage = rememberDictionaryLanguage(),
        ui = StudyUi(
            loading = false,
            gradingAllowed = false,
            revealed = true,
            character = card.character,
            sentenceJp = card.sentenceJp,
            sentenceKana = card.sentenceKana,
            sentenceRu = card.sentenceRu,
            sentenceEn = card.sentenceEn,
            meaningsRu = card.meaningsRu,
            meaningsEn = card.meaningsEn,
            onReadings = card.onReadings,
            kunReadings = card.kunReadings,
        ),
        onReveal = {},
        onGrade = {},
        onHome = {},
        onUp = onBack,
    )
}

@Composable
private fun StudyContent(
    ui: StudyUi,
    dictionaryLanguage: String,
    onReveal: () -> Unit,
    onGrade: (Grade) -> Unit,
    onHome: () -> Unit,
    onUp: (() -> Unit)? = null,
) {
    val title = if (ui.gradingAllowed && ui.total > 0 && !ui.done && !ui.loading) {
        stringResource(R.string.session_progress, ui.position, ui.total)
    } else {
        ""
    }
    Scaffold(
        topBar = { HitomojiTopBar(title = title, onUp = onUp) },
        bottomBar = {
            if (!ui.loading && !ui.done && ui.gradingAllowed) {
                GradeBar(ui = ui, onReveal = onReveal, onGrade = onGrade)
            }
        },
    ) { padding ->
        when {
            ui.loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            ui.done -> Box(Modifier.fillMaxSize().padding(padding).padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.done_for_today),
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = onHome) { Text(stringResource(R.string.home)) }
                }
            }
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                JapaneseText(text = ui.character, fontSize = 96.sp)
                Spacer(Modifier.height(24.dp))
                JapaneseText(text = ui.sentenceJp, fontSize = 22.sp)
                AnimatedVisibility(
                    visible = ui.revealed,
                    enter = fadeIn(tween(240)),
                    exit = fadeOut(tween(120)),
                ) {
                    RevealedBlock(ui, dictionaryLanguage)
                }
            }
        }
    }
}

@Composable
private fun RevealedBlock(ui: StudyUi, dictionaryLanguage: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (ui.sentenceKana.isNotBlank()) {
            JapaneseText(text = ui.sentenceKana, fontSize = 16.sp)
        }
        Text(
            text = meaningsFor(dictionaryLanguage, ui.meaningsRu, ui.meaningsEn).joinToString(", "),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        if (ui.onReadings.isNotEmpty()) {
            ReadingLine(stringResource(R.string.reading_on), ui.onReadings.joinToString("、"))
        }
        if (ui.kunReadings.isNotEmpty()) {
            ReadingLine(stringResource(R.string.reading_kun), ui.kunReadings.joinToString("、"))
        }
        Text(
            text = sentenceFor(dictionaryLanguage, ui.sentenceRu, ui.sentenceEn),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ReadingLine(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        JapaneseText(text = value, fontSize = 18.sp)
    }
}

@Composable
private fun GradeBar(
    ui: StudyUi,
    onReveal: () -> Unit,
    onGrade: (Grade) -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(NavigationBarDefaults.windowInsets)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (ui.total > 0) {
            LinearProgressIndicator(
                progress = { ui.position / ui.total.toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Box(Modifier.fillMaxWidth().heightIn(min = 48.dp), contentAlignment = Alignment.Center) {
            AnimatedContent(
                targetState = ui.revealed,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                label = "grade",
            ) { revealed ->
                if (!revealed) {
                    Button(
                        onClick = onReveal,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !ui.saving,
                    ) {
                        Text(stringResource(R.string.show_translation))
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        GradeButton(stringResource(R.string.grade_again), Modifier.weight(1f), ui.saving, filled = false) { onGrade(Grade.AGAIN) }
                        GradeButton(stringResource(R.string.grade_hard), Modifier.weight(1f), ui.saving, filled = false) { onGrade(Grade.HARD) }
                        GradeButton(stringResource(R.string.grade_good), Modifier.weight(1f), ui.saving, filled = true) { onGrade(Grade.GOOD) }
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun GradeButton(
    label: String,
    modifier: Modifier,
    saving: Boolean,
    filled: Boolean,
    onClick: () -> Unit,
) {
    val buttonModifier = modifier.heightIn(min = 48.dp)
    if (filled) {
        Button(onClick = onClick, modifier = buttonModifier, enabled = !saving) {
            Text(label, textAlign = TextAlign.Center)
        }
    } else {
        FilledTonalButton(onClick = onClick, modifier = buttonModifier, enabled = !saving) {
            Text(label, textAlign = TextAlign.Center)
        }
    }
}
