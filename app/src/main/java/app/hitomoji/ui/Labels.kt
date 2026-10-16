package app.hitomoji.ui

import app.hitomoji.domain.CardState
import app.hitomoji.domain.WidgetMode

fun CardState.ruLabel(): String = when (this) {
    CardState.NEW -> "новый"
    CardState.REVIEW -> "помню"
    CardState.RELEARNING -> "учу заново"
}

fun WidgetMode.ruLabel(): String = when (this) {
    WidgetMode.NEW -> "новый"
    WidgetMode.REVIEW -> "повтор"
    WidgetMode.DONE -> "на сегодня всё"
}
