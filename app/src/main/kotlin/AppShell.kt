package dev.btdeck

import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewQuilt
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import dev.btdeck.core.bluetooth.HidKeyboard
import dev.btdeck.core.bluetooth.HidStatus
import dev.btdeck.core.data.LayoutStore
import dev.btdeck.core.data.LayoutTransfer
import dev.btdeck.core.data.SettingsStore
import dev.btdeck.core.designsystem.theme.ZexDeckColors
import dev.btdeck.feature.connect.BluetoothAction
import dev.btdeck.feature.connect.ConnectSheet
import dev.btdeck.feature.connect.StatusBanner
import dev.btdeck.feature.connect.StatusChip
import dev.btdeck.feature.keyboard.KeyboardScreen
import dev.btdeck.feature.keyboard.sidewaysWhenTall
import dev.btdeck.feature.layouts.ImportLayoutDialog
import dev.btdeck.feature.layouts.LayoutsScreen
import dev.btdeck.feature.pad.SystemBarsHidden
import dev.btdeck.feature.settings.AboutScreen
import dev.btdeck.feature.settings.PrivacyScreen
import dev.btdeck.feature.settings.SettingsScreen
import dev.btdeck.feature.settings.TutorialsScreen
import dev.btdeck.feature.settings.WalkthroughDialog

/** The four tabs, in the order they appear in the bottom bar / side rail. */
enum class Tab(val label: String, val icon: ImageVector) {
    Pad("Pad", Icons.Default.GridView),
    Layouts("Layouts", Icons.AutoMirrored.Filled.ViewQuilt),
    Keyboard("Keyboard", Icons.Default.Keyboard),
    Settings("Settings", Icons.Default.Settings),
}

/** Pages inside the Settings tab. Tutorials, Privacy and About open from Main; licenses from About. */
private enum class SettingsPage { Main, Tutorials, Privacy, About, Licenses }

/**
 * The whole app: a bottom bar with Pad, Keyboard, Layouts and Settings. Also owns the
 * things several tabs share: fullscreen, the connect sheet, and keep-screen-on.
 */
@Composable
fun AppShell(
    layouts: LayoutStore,
    settingsStore: SettingsStore,
    transfer: LayoutTransfer,
    incomingFile: Uri?,
    onIncomingFileHandled: () -> Unit,
    onFixStatus: (HidStatus) -> Unit,
    onBluetooth: (BluetoothAction) -> Unit,
    onMakeDiscoverable: () -> Unit,
    onScan: () -> Unit,
) {
    val state by layouts.state.collectAsState()
    val settings by settingsStore.settings.collectAsState()
    val settingsLoaded by settingsStore.loaded.collectAsState()
    val status by HidKeyboard.status.collectAsState()

    var tab by rememberSaveable { mutableStateOf(Tab.Pad) }
    var settingsPage by rememberSaveable { mutableStateOf(SettingsPage.Main) }
    var fullscreen by rememberSaveable { mutableStateOf(false) }
    var showConnect by rememberSaveable { mutableStateOf(false) }
    var showWalkthrough by rememberSaveable { mutableStateOf(false) }
    val openConnect = { showConnect = true }
    val openWalkthrough = { showWalkthrough = true }

    // Picking Settings in the bar always opens its main page, not the page last left open.
    val selectTab = { picked: Tab ->
        if (picked == Tab.Settings) settingsPage = SettingsPage.Main
        tab = picked
    }

    // A .keyset file opened from another app: show it on the Layouts tab and ask first.
    LaunchedEffect(incomingFile) {
        if (incomingFile != null) {
            fullscreen = false
            tab = Tab.Layouts
        }
    }
    incomingFile?.let { ImportLayoutDialog(it, transfer, onDone = onIncomingFileHandled) }

    KeepScreenOn(settings.keepScreenOn)
    SystemBarsHidden(fullscreen)
    // Back leaves fullscreen, then closes a settings page, then returns to the pad.
    BackHandler(enabled = fullscreen || tab != Tab.Pad) {
        when {
            fullscreen -> fullscreen = false
            settingsPage == SettingsPage.Licenses -> settingsPage = SettingsPage.About
            settingsPage != SettingsPage.Main -> settingsPage = SettingsPage.Main
            else -> tab = Tab.Pad
        }
    }

    // Wide screens (landscape phones, tablets) get a side rail so screens keep their height.
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val showBottomBar = !fullscreen && !landscape
    val showRail = !fullscreen && landscape

    Scaffold(
        containerColor = ZexDeckColors.Background,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = { if (showBottomBar) BottomBar(tab, onSelect = selectTab) },
    ) { padding ->
        Row(Modifier.fillMaxSize().padding(padding)) {
            if (showRail) SideRail(tab, onSelect = selectTab)
            val content = Modifier.weight(1f).fillMaxHeight()
            when (tab) {
                Tab.Pad -> PadScreen(
                    state = state,
                    store = layouts,
                    settings = settings,
                    status = status,
                    fullscreen = fullscreen,
                    onFullscreenChange = { fullscreen = it },
                    onOpenConnect = openConnect,
                    onFixStatus = onFixStatus,
                    modifier = content,
                )
                Tab.Keyboard -> Column(content.sidewaysWhenTall()) {
                    // Shows only when something needs fixing; the chip covers "not connected".
                    StatusBanner(status, onFixStatus, openConnect, Modifier.padding(horizontal = 12.dp))
                    KeyboardScreen(vibrate = settings.vibrate, modifier = Modifier.weight(1f)) {
                        StatusChip(status, openConnect)
                    }
                }
                Tab.Layouts -> LayoutsScreen(
                    state = state,
                    store = layouts,
                    transfer = transfer,
                    onOpen = { tab = Tab.Pad },
                    modifier = content,
                    status = { StatusChip(status, openConnect) },
                )
                Tab.Settings -> when (settingsPage) {
                    SettingsPage.Main -> SettingsScreen(
                        settings = settings,
                        onChange = settingsStore::update,
                        onOpenWalkthrough = openWalkthrough,
                        onOpenTutorials = { settingsPage = SettingsPage.Tutorials },
                        onOpenPrivacy = { settingsPage = SettingsPage.Privacy },
                        onOpenAbout = { settingsPage = SettingsPage.About },
                        modifier = content,
                        status = { StatusChip(status, openConnect) },
                    )
                    SettingsPage.About -> AboutScreen(
                        appVersion = appVersion(),
                        onBack = { settingsPage = SettingsPage.Main },
                        onOpenLicenses = { settingsPage = SettingsPage.Licenses },
                        modifier = content,
                    )
                    SettingsPage.Tutorials -> TutorialsScreen(
                        onBack = { settingsPage = SettingsPage.Main },
                        onOpenWalkthrough = openWalkthrough,
                        modifier = content,
                    )
                    SettingsPage.Privacy -> PrivacyScreen(
                        onBack = { settingsPage = SettingsPage.Main },
                        modifier = content,
                    )
                    SettingsPage.Licenses -> LicensesScreen(onBack = {
                        settingsPage = SettingsPage.About
                    }, modifier = content)
                }
            }
        }
    }

    if (showConnect) {
        ConnectSheet(status, onBluetooth, onMakeDiscoverable, onScan, onDismiss = { showConnect = false })
    }

    if ((settingsLoaded && !settings.hasSeenWalkthrough) || showWalkthrough) {
        WalkthroughDialog(
            onDismiss = {
                if (!settings.hasSeenWalkthrough) {
                    settingsStore.update { it.copy(hasSeenWalkthrough = true) }
                }
                showWalkthrough = false
            },
            onConnect = {
                if (!settings.hasSeenWalkthrough) {
                    settingsStore.update { it.copy(hasSeenWalkthrough = true) }
                }
                showWalkthrough = false
                showConnect = true
            },
        )
    }
}

@Composable
private fun BottomBar(selected: Tab, onSelect: (Tab) -> Unit) {
    NavigationBar(containerColor = ZexDeckColors.Surface) {
        for (tab in Tab.entries) {
            NavigationBarItem(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(tab.label) },
            )
        }
    }
}

@Composable
private fun SideRail(selected: Tab, onSelect: (Tab) -> Unit) {
    NavigationRail(containerColor = ZexDeckColors.Surface) {
        Spacer(Modifier.weight(1f))
        for (tab in Tab.entries) {
            NavigationRailItem(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(tab.label) },
            )
        }
        Spacer(Modifier.weight(1f))
    }
}

/** Keeps the phone awake while ZexDeck is on screen, if the setting is on. */
@Composable
private fun KeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(enabled) {
        view.keepScreenOn = enabled
        onDispose { view.keepScreenOn = false }
    }
}

@Composable
private fun appVersion(): String {
    val context = LocalContext.current
    return context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
}
