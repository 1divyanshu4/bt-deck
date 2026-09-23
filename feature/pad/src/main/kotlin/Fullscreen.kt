package dev.keyset.feature.pad

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dev.keyset.core.designsystem.theme.KeySetShapes
import dev.keyset.core.designsystem.theme.Palette
import kotlinx.coroutines.delay

/** Immersive mode: hides status and navigation bars; an edge swipe shows them briefly. */
@Composable
fun SystemBarsHidden(hidden: Boolean) {
    val view = LocalView.current
    DisposableEffect(hidden) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        if (hidden) {
            controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller?.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
        onDispose { }
    }
}

/** Shown briefly on entering fullscreen. Not touchable, so it never steals a key press. */
@Composable
fun BoxScope.FullscreenHint() {
    var visible by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(2500)
        visible = false
    }
    if (!visible) return
    Text(
        "Swipe edge, then Back to exit",
        color = Color.White,
        fontSize = 13.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 24.dp, start = 16.dp, end = 16.dp)
            .clip(KeySetShapes.Small)
            .background(Color(0xE6202026))
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

/** Fullscreen hides the status bar, so a dropped connection gets its own strip above the pad. */
@Composable
fun DisconnectedStrip(onExit: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(KeySetShapes.Medium)
            .background(Palette[4])
            .clickable(onClick = onExit)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Not connected", color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text("Exit fullscreen", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
