package app.hitomoji.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import app.hitomoji.HitomojiApp
import java.time.LocalDate

class WidgetRefresher(private val context: Context) {
    suspend fun refresh() {
        val app = context.applicationContext as HitomojiApp
        app.container.repository.widgetModel(LocalDate.now())
        SquareWidget().updateAll(context)
        WideWidget().updateAll(context)
        LargeWidget().updateAll(context)
        CircleWidget().updateAll(context)
        CookieWidget().updateAll(context)
        ArchWidget().updateAll(context)
        CloverWidget().updateAll(context)
    }
}
