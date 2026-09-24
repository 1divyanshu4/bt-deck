package dev.btdeck.feature.settings

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
import dev.btdeck.core.designsystem.component.PageTitle

/** When this policy last changed. Update it with every change to [policy]. */
private const val EFFECTIVE_DATE = "24 September 2026"

/**
 * BTDeck's privacy policy. Every statement here must stay true of the code: no INTERNET
 * permission, data only in the app's own storage, keys only to the connected device.
 */
private val policy = listOf(
    "In short" to
        "BTDeck doesn't collect, send or sell any of your data. It has no internet access, no " +
        "accounts, no ads and no analytics.",
    "What stays on your phone" to
        "Your layouts and buttons, your settings, images you add as icons, and the last tablet or " +
        "laptop you connected to, so BTDeck can reconnect. They're kept in BTDeck's own storage " +
        "and removed when you uninstall BTDeck or clear its storage.",
    "Bluetooth" to
        "BTDeck works as a Bluetooth keyboard. The keys you press go only to the device you " +
        "connected to. When you scan, BTDeck shows the names of nearby devices so you can pair " +
        "with one; it doesn't keep that list.",
    "Permissions" to
        "Nearby devices: to act as a keyboard, and to find and connect to your tablet or laptop.\n" +
        "Notifications: for the notification that keeps the connection alive in the background.\n" +
        "Location (Android 11 and older only): Android requires it to scan for Bluetooth devices. " +
        "BTDeck never reads your location.",
    "Sharing layouts" to
        "When you share or save a layout, BTDeck makes a .keyset file and hands it to the app you " +
        "choose; what happens next is up to that app. Opening a .keyset file only reads that file.",
    "Backups" to
        "If Android backup is on, Android may include BTDeck's layouts and settings in your device " +
        "backup, like it does for other apps. That's handled by Android, not by BTDeck.",
    "Children" to "BTDeck collects no data from anyone, including children.",
    "Changes" to "If this policy changes, the new version will be in the app with a new date.",
    "Contact" to "Questions about privacy? Open an issue on BTDeck's GitHub page.",
)

@Composable
fun PrivacyScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PageTitle("Privacy policy", onBack)
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Text("Effective $EFFECTIVE_DATE", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
            for ((title, text) in policy) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 20.dp))
                Text(
                    text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
