package app.hitomoji.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import app.hitomoji.HitomojiApp
import app.hitomoji.domain.WidgetMode
import java.time.LocalDate

class WidgetRefresher(private val context: Context) {
    suspend fun refresh() {
        val app = context.applicationContext as HitomojiApp
        val model = app.container.repository.widgetModel(LocalDate.now())
        if (model.mode != WidgetMode.DONE && model.rank != null) {
            app.container.repository.setLastShown(model.rank)
        }
        SquareWidget().updateAll(context)
        WideWidget().updateAll(context)
        LargeWidget().updateAll(context)
    }
}
