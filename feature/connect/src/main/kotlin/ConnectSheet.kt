package dev.keyset.feature.connect

import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.TabletAndroid
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.keyset.core.bluetooth.DeviceScanner
import dev.keyset.core.bluetooth.HidKeyboard
import dev.keyset.core.bluetooth.HidStatus
import dev.keyset.core.bluetooth.ScanState
import dev.keyset.core.bluetooth.bluetoothClassOrNull
import dev.keyset.core.bluetooth.displayName
import dev.keyset.core.designsystem.component.HelpButton
import dev.keyset.core.designsystem.theme.KeySetColors
import dev.keyset.core.designsystem.theme.KeySetShapes
import dev.keyset.core.designsystem.theme.Palette

/** Bottom sheet for pairing and connecting to a host (tablet or laptop). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectSheet(status: HidStatus, onMakeDiscoverable: () -> Unit, onScan: () -> Unit, onDismiss: () -> Unit) {
    val scan by DeviceScanner.state.collectAsState()

    // Paired list is fresh on open; discovery must not keep running once the sheet is gone.
    DisposableEffect(Unit) {
        DeviceScanner.refreshPaired()
        onDispose { DeviceScanner.stopScan() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = KeySetColors.Surface,
        // Half-open hides Scan/Pair on landscape tablets.
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            SheetTitle()
            PairedDevices(scan.paired, status)
            NearbyDevices(scan, onScan)
            DiscoverableFallback(onMakeDiscoverable)
        }
    }
}

@Composable
private fun SheetTitle() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Connect a device", style = MaterialTheme.typography.titleLarge)
        HelpButton(
            "Works with tablets and laptops (Android, Windows, Mac, Linux, ChromeOS). " +
                "Open Bluetooth settings on that device so it can be found, then Scan and tap Pair.",
        )
    }
}

/** Already-paired devices: tap to connect, or to disconnect the current host. */
@Composable
private fun PairedDevices(paired: List<BluetoothDevice>, status: HidStatus) {
    SheetHeader("Paired devices") {
        IconButton(onClick = { DeviceScanner.refreshPaired() }) {
            Icon(Icons.Default.Refresh, contentDescription = "Refresh paired devices", tint = Color.Gray)
        }
    }
    if (paired.isEmpty()) EmptyHint("None yet")

    val connected = HidKeyboard.connectedAddress()
    for (device in paired) {
        val name = device.displayName()
        val isConnected = device.address == connected
        val failed = status is HidStatus.ConnectFailed && status.name == name
        val action = when {
            isConnected -> "Disconnect"
            status is HidStatus.Connecting && status.name == name -> "Connecting…"
            failed -> "Failed · Retry"
            else -> "Connect"
        }
        DeviceRow(
            icon = deviceIcon(device.bluetoothClassOrNull()),
            name = name,
            action = action,
            actionColor = if (isConnected || failed) Palette[4] else MaterialTheme.colorScheme.primary,
            highlighted = isConnected,
            onClick = { if (isConnected) HidKeyboard.disconnect() else HidKeyboard.connect(device) },
        )
    }
}

/** Discovery results: devices found by Scan that can be paired from here. */
@Composable
private fun NearbyDevices(scan: ScanState, onScan: () -> Unit) {
    SheetHeader(
        "Nearby devices",
        help = "The other device only shows up while its Bluetooth settings screen is open.",
    ) {
        if (scan.scanning) {
            CircularProgressIndicator(Modifier.padding(end = 12.dp).size(18.dp), strokeWidth = 2.dp)
            TextButton(onClick = { DeviceScanner.stopScan() }) { Text("Stop") }
        } else {
            TextButton(onClick = onScan) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Scan")
            }
        }
    }
    if (scan.nearby.isEmpty()) EmptyHint(if (scan.scanning) "Searching…" else "Nothing found yet")

    for (found in scan.nearby) {
        val pairing = scan.pairing == found.device.address
        DeviceRow(
            icon = deviceIcon(found.device.bluetoothClassOrNull()),
            name = found.name ?: found.device.address,
            dim = found.name == null,
            action = if (pairing) "Pairing…" else "Pair",
            actionColor = MaterialTheme.colorScheme.primary,
            onClick = { if (!pairing) DeviceScanner.pair(found.device) },
        )
    }
}

/** For hosts that can't be found by scanning: let them find this phone instead. */
@Composable
private fun DiscoverableFallback(onMakeDiscoverable: () -> Unit) {
    Spacer(Modifier.size(16.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = onMakeDiscoverable, shape = KeySetShapes.Small, modifier = Modifier.weight(1f)) {
            Text("Make phone discoverable")
        }
        HelpButton(
            "Can't find the device? Make this phone visible, then pair from the other device's Bluetooth settings.",
        )
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(text, color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
private fun SheetHeader(title: String, help: String? = null, actions: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 16.dp).heightIn(min = 40.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title.uppercase(), fontSize = 12.sp, letterSpacing = 1.sp, color = Color.Gray)
        if (help != null) HelpButton(help)
        Spacer(Modifier.weight(1f))
        actions()
    }
}

@Composable
private fun DeviceRow(
    icon: ImageVector,
    name: String,
    action: String,
    actionColor: Color,
    onClick: () -> Unit,
    highlighted: Boolean = false,
    dim: Boolean = false,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(KeySetShapes.Small)
            .background(if (highlighted) Palette[2].copy(alpha = 0.18f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            name,
            Modifier.weight(1f),
            color = if (dim) Color.Gray else Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(action, color = actionColor, fontSize = 14.sp)
    }
}

private fun deviceIcon(cls: BluetoothClass?): ImageVector = when (cls?.majorDeviceClass) {
    BluetoothClass.Device.Major.COMPUTER -> when (cls.deviceClass) {
        BluetoothClass.Device.COMPUTER_DESKTOP, BluetoothClass.Device.COMPUTER_SERVER -> Icons.Filled.Computer
        BluetoothClass.Device.COMPUTER_HANDHELD_PC_PDA,
        BluetoothClass.Device.COMPUTER_PALM_SIZE_PC_PDA,
        -> Icons.Filled.TabletAndroid
        else -> Icons.Filled.Laptop
    }
    BluetoothClass.Device.Major.PHONE -> Icons.Filled.Smartphone
    BluetoothClass.Device.Major.AUDIO_VIDEO -> Icons.Filled.Headphones
    BluetoothClass.Device.Major.WEARABLE -> Icons.Filled.Watch
    else -> Icons.Filled.Devices
}
