package app.hitomoji.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.color.DynamicThemeColorProviders
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import app.hitomoji.HitomojiApp
import app.hitomoji.MainActivity
import app.hitomoji.data.repo.WidgetModel
import app.hitomoji.domain.WidgetMode
import app.hitomoji.ui.label
import java.time.LocalDate
import kotlin.math.min

enum class WidgetShape { SQUARE, WIDE, LARGE }

class SquareWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provide(context, WidgetShape.SQUARE)
    }
}

class WideWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provide(context, WidgetShape.WIDE)
    }
}

class LargeWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provide(context, WidgetShape.LARGE)
    }
}

private suspend fun GlanceAppWidget.provide(context: Context, shape: WidgetShape) {
    val app = context.applicationContext as HitomojiApp
    val model = app.container.repository.widgetModel(LocalDate.now())
    provideContent {
        GlanceTheme(colors = DynamicThemeColorProviders) {
            WidgetFace(context, shape, model)
        }
    }
}

class SquareWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SquareWidget()
}

class WideWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WideWidget()
}

class LargeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LargeWidget()
}

@androidx.compose.runtime.Composable
private fun WidgetFace(context: Context, shape: WidgetShape, model: WidgetModel) {
    val size = LocalSize.current
    val shortest = min(size.width.value, size.height.value)
    val kanjiSize = when (shape) {
        WidgetShape.SQUARE -> (shortest * 0.62f).coerceIn(28f, 96f)
        WidgetShape.WIDE -> (size.height.value * 0.55f).coerceIn(28f, 72f)
        WidgetShape.LARGE -> (size.height.value * 0.38f).coerceIn(32f, 84f)
    }
    val captionSize = (shortest * 0.16f).coerceIn(13f, 20f)
    val sentenceSize = (size.width.value / 16f).coerceIn(13f, 20f)
    val intent = widgetIntent(context, model)
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(android.R.dimen.system_app_widget_background_radius)
            .clickable(actionStartActivity(intent)),
        contentAlignment = Alignment.Center,
    ) {
        when (shape) {
            WidgetShape.SQUARE -> Text(
                text = model.character.ifBlank { model.mode.label(context) },
                modifier = GlanceModifier.padding(12.dp),
                style = kanjiStyle(kanjiSize.sp, GlanceTheme.colors.onSurface),
                maxLines = 2,
            )
            WidgetShape.WIDE -> Row(
                modifier = GlanceModifier.fillMaxSize().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = model.character,
                    modifier = GlanceModifier.defaultWeight(),
                    style = kanjiStyle(kanjiSize.sp, GlanceTheme.colors.onSurface),
                    maxLines = 1,
                )
                Text(
                    text = model.mode.label(context),
                    modifier = GlanceModifier.defaultWeight(),
                    style = captionStyle(captionSize.sp),
                    maxLines = 2,
                )
            }
            WidgetShape.LARGE -> Column(
                modifier = GlanceModifier.fillMaxSize().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = model.character,
                    style = kanjiStyle(kanjiSize.sp, GlanceTheme.colors.onSurface),
                    maxLines = 1,
                )
                if (model.mode == WidgetMode.DONE || model.mode == WidgetMode.WAITING) {
                    Text(
                        text = model.mode.label(context),
                        style = captionStyle(captionSize.sp),
                        maxLines = 2,
                    )
                } else if (model.sentenceJp.isNotBlank()) {
                    Text(
                        text = model.sentenceJp,
                        style = captionStyle(sentenceSize.sp),
                        maxLines = 3,
                    )
                }
            }
        }
    }
}

internal fun kanjiStyle(size: androidx.compose.ui.unit.TextUnit, color: ColorProvider) = TextStyle(
    color = color,
    fontSize = size,
    textAlign = TextAlign.Center,
)

@androidx.compose.runtime.Composable
private fun captionStyle(size: androidx.compose.ui.unit.TextUnit) = TextStyle(
    color = GlanceTheme.colors.onSurfaceVariant,
    fontSize = size,
    textAlign = TextAlign.Center,
)

internal fun widgetIntent(context: Context, model: WidgetModel): Intent {
    val openStudy = model.rank != null &&
        model.mode != WidgetMode.DONE &&
        model.mode != WidgetMode.WAITING
    val path = if (openStudy) "study/${model.rank}" else "home"
    return Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        data = Uri.parse("hitomoji://open/$path")
        if (openStudy) {
            putExtra(MainActivity.EXTRA_RANK, model.rank)
            putExtra(MainActivity.EXTRA_GRADING, true)
        }
    }
}
