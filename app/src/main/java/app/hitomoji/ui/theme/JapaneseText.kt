package app.hitomoji.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text

@Composable
fun JapaneseText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 22.sp,
    textAlign: TextAlign = TextAlign.Center,
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        style = TextStyle(
            fontFamily = NotoSansJp,
            fontSize = fontSize,
            lineHeight = fontSize * 1.35f,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = textAlign,
        ),
    )
}
