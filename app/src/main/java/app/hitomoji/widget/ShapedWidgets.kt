package app.hitomoji.widget

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.provideContent
import androidx.glance.action.clickable
import androidx.glance.background
import androidx.glance.color.DynamicThemeColorProviders
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.size
import androidx.glance.text.Text
import app.hitomoji.HitomojiApp
import app.hitomoji.R
import app.hitomoji.data.repo.WidgetModel
import app.hitomoji.ui.label
import java.time.LocalDate
import kotlin.math.min

enum class KanjiOutline(
    @DrawableRes val drawable: Int,
    val kanjiScale: Float,
) {
    CIRCLE(R.drawable.widget_shape_circle, 0.52f),
    COOKIE(R.drawable.widget_shape_cookie, 0.46f),
    ARCH(R.drawable.widget_shape_arch, 0.42f),
    CLOVER(R.drawable.widget_shape_clover, 0.34f),
}

open class ShapedKanjiWidget(
    private val outline: KanjiOutline,
) : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as HitomojiApp
        val model = app.container.repository.widgetModel(LocalDate.now())
        provideContent {
            GlanceTheme(colors = DynamicThemeColorProviders) {
                ShapedFace(context, outline, model)
            }
        }
    }
}

class CircleWidget : ShapedKanjiWidget(KanjiOutline.CIRCLE)

class CookieWidget : ShapedKanjiWidget(KanjiOutline.COOKIE)

class ArchWidget : ShapedKanjiWidget(KanjiOutline.ARCH)

class CloverWidget : ShapedKanjiWidget(KanjiOutline.CLOVER)

class CircleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CircleWidget()
}

class CookieWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CookieWidget()
}

class ArchWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ArchWidget()
}

class CloverWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CloverWidget()
}

@Composable
private fun ShapedFace(context: Context, outline: KanjiOutline, model: WidgetModel) {
    val bounds = LocalSize.current
    val edge = min(bounds.width.value, bounds.height.value)
    val kanjiSize = (edge * outline.kanjiScale).coerceIn(28f, 96f)
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(actionStartActivity(widgetIntent(context, model))),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = GlanceModifier
                .size(edge.dp)
                .appWidgetBackground()
                .background(
                    imageProvider = ImageProvider(outline.drawable),
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.widgetBackground),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = model.character.ifBlank { model.mode.label(context) },
                style = kanjiStyle(kanjiSize.sp, GlanceTheme.colors.onSurface),
                maxLines = 2,
            )
        }
    }
}
