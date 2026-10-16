package app.hitomoji

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.hitomoji.ui.HitomojiNav
import app.hitomoji.ui.theme.HitomojiTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    private val navSignals = MutableStateFlow(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HitomojiTheme {
                val ready by produceState(false) {
                    (application as HitomojiApp).container.repository.ensureSeeded()
                    value = true
                }
                if (!ready) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Text("Hitomoji", style = MaterialTheme.typography.headlineMedium)
                            CircularProgressIndicator()
                        }
                    }
                } else {
                    HitomojiNav(navSignals)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        navSignals.value = navSignals.value + 1
    }

    companion object {
        const val EXTRA_RANK = "app.hitomoji.extra.RANK"
        const val EXTRA_GRADING = "app.hitomoji.extra.GRADING"
    }
}
