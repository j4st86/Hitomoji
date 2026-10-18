package app.hitomoji.domain

import java.time.LocalDate

enum class CardState {
    NEW,
    REVIEW,
    RELEARNING,
}

enum class Grade {
    AGAIN,
    HARD,
    GOOD,
}

data class SrsCard(
    val rank: Int,
    val state: CardState,
    val ease: Double,
    val intervalDays: Int,
    val repetitions: Int,
    val lapses: Int,
    val dueDate: LocalDate?,
    val introducedOn: LocalDate?,
    val stability: Double = 0.0,
    val difficulty: Double = 0.0,
)

enum class WidgetMode {
    NEW,
    REVIEW,
    DONE,
    WAITING,
}

data class WidgetSelection(
    val rank: Int?,
    val mode: WidgetMode,
)
