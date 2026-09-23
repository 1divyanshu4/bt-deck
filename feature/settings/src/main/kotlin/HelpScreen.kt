package dev.keyset.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val tips = listOf(
    "First time connecting" to
        "Open Bluetooth settings on your tablet or laptop, then in KeySet tap the status button, " +
        "Scan, and Pair. If it doesn't connect, connect once from the tablet or laptop's own " +
        "Bluetooth settings. After that, KeySet can connect by itself.",
    "One device at a time" to
        "KeySet types on one tablet or laptop at a time. Connecting to another one disconnects " +
        "the first.",
    "Don't run KeySet on the tablet" to
        "A device that's acting as a keyboard can't receive keys from one. Only install and open " +
        "KeySet on the phone.",
    "It says \"Restart Bluetooth\"" to
        "The phone is still holding an old connection, usually after KeySet was closed or updated " +
        "while connected. Turn Bluetooth off and on. KeySet reconnects by itself.",
    "Typed text looks wrong" to
        "Text is typed as if the tablet or laptop uses a US keyboard layout. Switch its layout " +
        "to US, or use the keys directly.",
)

/** Short answers to the most common connection problems. */
@Composable
fun HelpScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PageTitle("Connection help", onBack)
        for ((title, text) in tips) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(text, style = MaterialTheme.typography.bodyMedium, color = Color.LightGray)
            }
        }
    }
}
