package dev.keyset.core.designsystem.component

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** Small "?" that shows [text] in a tooltip, instead of a paragraph on screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpButton(text: String, title: String? = null) {
    val state = rememberTooltipState(isPersistent = true)
    val scope = rememberCoroutineScope()
    TooltipBox(
        positionProvider = TooltipDefaults.rememberRichTooltipPositionProvider(),
        tooltip = {
            RichTooltip(title = title?.let { { Text(it) } }) { Text(text) }
        },
        state = state,
    ) {
        IconButton(onClick = { scope.launch { state.show() } }) {
            Icon(
                Icons.AutoMirrored.Outlined.HelpOutline,
                contentDescription = "Help",
                tint = Color.Gray,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
