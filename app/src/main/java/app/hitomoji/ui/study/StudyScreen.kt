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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.hitomoji.HitomojiApp
import app.hitomoji.data.repo.HitomojiRepository
import app.hitomoji.data.repo.StudyCard
import app.hitomoji.domain.Grade
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
    val meanings: List<String> = emptyList(),
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
            meanings = card.meaningsRu,
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
    StudyContent(ui = viewModel.ui, onReveal = viewModel::reveal, onGrade = viewModel::grade, onHome = onFinishedHome)
}

internal suspend fun HitomojiApp.hitomojiRefreshWidgets() {
    app.hitomoji.widget.WidgetRefresher(this).refresh()
}

@Composable
fun ReadOnlyKanjiScreen(card: StudyCard, onBack: () -> Unit) {
    StudyContent(
        ui = StudyUi(
            loading = false,
            gradingAllowed = false,
            revealed = true,
            character = card.character,
            sentenceJp = card.sentenceJp,
            sentenceKana = card.sentenceKana,
            sentenceRu = card.sentenceRu,
            meanings = card.meaningsRu,
            onReadings = card.onReadings,
            kunReadings = card.kunReadings,
        ),
        onReveal = {},
        onGrade = {},
        onHome = {},
        onBack = onBack,
    )
}

@Composable
private fun StudyContent(
    ui: StudyUi,
    onReveal: () -> Unit,
    onGrade: (Grade) -> Unit,
    onHome: () -> Unit,
    onBack: (() -> Unit)? = null,
) {
    Scaffold(
        bottomBar = {
            if (!ui.loading && !ui.done && ui.gradingAllowed) {
                GradeBar(ui = ui, onReveal = onReveal, onGrade = onGrade)
            }
        },
    ) { padding ->
        when {
            ui.loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Hitomoji", style = MaterialTheme.typography.headlineMedium)
            }
            ui.done -> Box(Modifier.fillMaxSize().padding(padding).padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "На сегодня всё",
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = onHome) { Text("На главную") }
                }
            }
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (onBack != null) {
                    TextButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) {
                        Text("Назад")
                    }
                }
                Spacer(Modifier.height(12.dp))
                JapaneseText(text = ui.character, fontSize = 112.sp)
                Spacer(Modifier.height(28.dp))
                JapaneseText(text = ui.sentenceJp, fontSize = 22.sp)
                AnimatedVisibility(
                    visible = ui.revealed,
                    enter = fadeIn(tween(240)),
                    exit = fadeOut(tween(120)),
                ) {
                    RevealedBlock(ui)
                }
            }
        }
    }
}

@Composable
private fun RevealedBlock(ui: StudyUi) {
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
            text = ui.meanings.joinToString(", "),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        if (ui.onReadings.isNotEmpty()) {
            ReadingLine("он", ui.onReadings.joinToString("、"))
        }
        if (ui.kunReadings.isNotEmpty()) {
            ReadingLine("кун", ui.kunReadings.joinToString("、"))
        }
        Text(
            text = ui.sentenceRu,
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "${ui.position} из ${ui.total}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().heightIn(min = 56.dp), contentAlignment = Alignment.Center) {
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
                        Text("Показать перевод")
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        GradeButton("Снова", Modifier.weight(1f), ui.saving) { onGrade(Grade.AGAIN) }
                        GradeButton("Сложно", Modifier.weight(1f), ui.saving) { onGrade(Grade.HARD) }
                        GradeButton("Хорошо", Modifier.weight(1f), ui.saving) { onGrade(Grade.GOOD) }
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
    onClick: () -> Unit,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp),
        enabled = !saving,
    ) {
        Text(label, textAlign = TextAlign.Center)
    }
}
