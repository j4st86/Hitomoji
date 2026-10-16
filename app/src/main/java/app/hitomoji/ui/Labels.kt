package app.hitomoji.ui

import android.content.Context
import app.hitomoji.R
import app.hitomoji.domain.CardState
import app.hitomoji.domain.WidgetMode

fun CardState.label(context: Context): String = when (this) {
    CardState.NEW -> context.getString(R.string.state_new)
    CardState.REVIEW -> context.getString(R.string.state_review)
    CardState.RELEARNING -> context.getString(R.string.state_relearn)
}

fun WidgetMode.label(context: Context): String = when (this) {
    WidgetMode.NEW -> context.getString(R.string.widget_mode_new)
    WidgetMode.REVIEW -> context.getString(R.string.widget_mode_review)
    WidgetMode.DONE -> context.getString(R.string.widget_mode_done)
}
