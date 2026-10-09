package dev.btdeck.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.btdeck.core.designsystem.theme.ZexDeckColors
import dev.btdeck.core.designsystem.theme.ZexDeckShapes

/**
 * A small badge that floats over a corner, like a button's shortcut or a layout's button
 * count. Long text ends in "…" if there isn't room.
 */
@Composable
fun CornerBadge(
    text: String,
    modifier: Modifier = Modifier,
    background: Color = ZexDeckColors.Background.copy(alpha = 0.85f),
    color: Color = Color.White,
) {
    Text(
        text,
        color = color,
        fontSize = 10.sp,
        // Tight line height: the theme's default (24sp) would make the badge twice as tall.
        lineHeight = 12.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .clip(ZexDeckShapes.XSmall)
            .background(background)
            .defaultMinSize(minWidth = 16.dp)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}
