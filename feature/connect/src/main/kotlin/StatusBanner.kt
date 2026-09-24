package dev.keyset.feature.connect

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.keyset.core.bluetooth.HidStatus
import dev.keyset.core.designsystem.component.HelpButton
import dev.keyset.core.designsystem.theme.KeySetColors
import dev.keyset.core.designsystem.theme.KeySetShapes
import dev.keyset.core.designsystem.theme.Palette

/** Compact connection indicator for the top bar. */
@Composable
fun StatusChip(status: HidStatus, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val (text, color) = chipText(status)
    Row(
        modifier
            .clip(KeySetShapes.Small)
            .background(KeySetColors.Surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).clip(KeySetShapes.Small).background(color))
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 14.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** What the chip says for each state, and its dot colour (green, amber or red). */
private fun chipText(status: HidStatus): Pair<String, Color> = when (status) {
    is HidStatus.Connected -> "Connected to ${status.name}" to Palette[2]
    is HidStatus.Connecting -> "Connecting to ${status.name}…" to Palette[3]
    HidStatus.Starting -> "Starting…" to Palette[3]
    HidStatus.Ready, is HidStatus.ConnectFailed -> "Not connected" to Palette[3]
    HidStatus.Stopped -> "Keyboard off" to Palette[4]
    HidStatus.BluetoothOff -> "Bluetooth off" to Palette[4]
    HidStatus.NoBluetooth -> "No Bluetooth" to Palette[4]
    HidStatus.NeedsPermission -> "Needs permission" to Palette[4]
    HidStatus.NeedsBluetoothRestart -> "Restart Bluetooth" to Palette[4]
    is HidStatus.Unsupported -> "Unavailable" to Palette[4]
}

/** One-line status with an action (and a "?" for details) whenever not connected. */
@Composable
fun StatusBanner(
    status: HidStatus,
    onFix: (HidStatus) -> Unit,
    onOpenConnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    data class Banner(val message: String, val help: String?, val action: String?)
    val b = when (status) {
        HidStatus.NoBluetooth -> Banner("No Bluetooth", "This phone has no Bluetooth adapter.", null)
        HidStatus.BluetoothOff -> Banner("Bluetooth is off", null, "Turn on")
        HidStatus.NeedsPermission ->
            Banner("Permission needed", "KeySet needs Bluetooth permission to act as a keyboard.", "Allow")
        is HidStatus.Unsupported -> Banner(status.title, status.detail, "Retry")
        is HidStatus.ConnectFailed -> Banner(
            "Connect from ${status.name} once",
            "${status.name} hasn't accepted this device as a keyboard yet. Connect once from " +
                "${status.name}'s side: open its Bluetooth settings and tap this device (Linux: " +
                "Bluetooth menu, or bluetoothctl connect). After that, Connect here works.",
            "Connect",
        )
        HidStatus.Stopped -> Banner("Keyboard stopped", null, "Start")
        HidStatus.NeedsBluetoothRestart -> Banner(
            "Restart Bluetooth",
            "The phone's Bluetooth is still holding an old keyboard session (this happens after " +
                "KeySet is updated or closed while connected). Restart turns Bluetooth off and on; " +
                "KeySet reconnects by itself.",
            "Restart",
        )
        else -> return
    }
    Row(
        modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .clip(KeySetShapes.Medium)
            .background(KeySetColors.Surface)
            .padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(
                b.message,
                Modifier.weight(1f, fill = false).padding(vertical = 8.dp),
                fontSize = 14.sp,
                color = Color(0xFFCCCCD4),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (b.help != null) HelpButton(b.help)
        }
        if (b.action != null) {
            TextButton(onClick = {
                when (status) {
                    HidStatus.Ready, is HidStatus.ConnectFailed -> onOpenConnect()
                    else -> onFix(status)
                }
            }) { Text(b.action) }
        }
    }
}
