package app.hitomoji

import android.app.Application
import app.hitomoji.di.AppContainer
import app.hitomoji.widget.WidgetRefresher
import app.hitomoji.widget.WidgetUpdateWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class HitomojiApp : Application() {
    lateinit var container: AppContainer
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        WidgetUpdateWorker.schedule(this)
        scope.launch {
            WidgetRefresher(this@HitomojiApp).refresh()
        }
    }
}
