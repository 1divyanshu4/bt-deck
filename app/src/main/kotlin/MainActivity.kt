package dev.btdeck

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import dev.btdeck.core.bluetooth.DeviceScanner
import dev.btdeck.core.bluetooth.HidKeyboard
import dev.btdeck.core.bluetooth.HidStatus
import dev.btdeck.core.designsystem.theme.BTDeckTheme
import dev.btdeck.feature.connect.BluetoothAction
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Settings' "turn Bluetooth off?" dialog; the constant is hidden in the SDK. */
private const val ACTION_REQUEST_DISABLE = "android.bluetooth.adapter.action.REQUEST_DISABLE"

class MainActivity : ComponentActivity() {

    private val permissions =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_ADVERTISE)
        } else {
            emptyArray()
        }

    /** Optional: lets the "keyboard running" notification show on Android 13+. */
    private val requested =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions + Manifest.permission.POST_NOTIFICATIONS
        } else {
            permissions
        }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { startKeyboard() }

    private val enableBtLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { startKeyboard() }

    /** Restart asked for: turn Bluetooth back on once the "turn off" dialog has done its job. */
    private var restartingBluetooth = false

    private val disableBtLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (!restartingBluetooth) return@registerForActivityResult
            restartingBluetooth = false
            if (result.resultCode == RESULT_OK) turnBluetoothOnWhenOff()
        }

    private val scanPermissions =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    private val scanPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result.values.all { it }) {
                scanForDevices()
            } else {
                Toast.makeText(this, "Scanning needs the Nearby devices permission", Toast.LENGTH_LONG).show()
            }
        }

    private val discoverableLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Before super.onCreate: swaps the splash theme for the app's own as the first screen draws.
        // It stays up until the saved settings are read, so the first screen doesn't flicker from
        // the defaults to them; a second at most, in case reading ever stalls.
        val app = application as BTDeckApp
        val started = SystemClock.uptimeMillis()
        installSplashScreen().setKeepOnScreenCondition {
            !app.settings.loaded.value && SystemClock.uptimeMillis() - started < 1_000
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        incomingFile = layoutFileFrom(intent)
        setContent {
            BTDeckTheme {
                AppShell(
                    layouts = app.layouts,
                    settingsStore = app.settings,
                    transfer = app.transfer,
                    incomingFile = incomingFile,
                    onIncomingFileHandled = { incomingFile = null },
                    onFixStatus = ::fixStatus,
                    onBluetooth = ::controlBluetooth,
                    onMakeDiscoverable = ::makeDiscoverable,
                    onScan = ::scanForDevices,
                )
            }
        }
        // onStart starts the keyboard once permissions are in place.
        if (!hasPermissions()) permissionLauncher.launch(requested)
    }

    /** A .keyset file another app asked us to open, waiting for the user to confirm. */
    private var incomingFile by mutableStateOf<Uri?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        layoutFileFrom(intent)?.let { incomingFile = it }
    }

    /** The file in an "open with" (VIEW) or "share to" (SEND) intent, if any. */
    private fun layoutFileFrom(intent: Intent?): Uri? = when (intent?.action) {
        Intent.ACTION_VIEW -> intent.data
        Intent.ACTION_SEND -> IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
        else -> null
    }

    /**
     * Coming back to BTDeck turns the keyboard on, even after Stop in the notification.
     * onStart, not onResume: the shade pulled over the app never stops it, so Stop pressed
     * there sticks until the user leaves and returns.
     */
    override fun onStart() {
        super.onStart()
        if (hasPermissions()) startKeyboard()
    }

    override fun onPause() {
        super.onPause()
        // Never leave a key stuck down on the tablet. The connection itself stays up:
        // KeyboardService keeps it alive while other apps are open.
        HidKeyboard.releaseAll()
    }

    private fun hasPermissions() = permissions.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun startKeyboard() {
        if (hasPermissions()) KeyboardService.start(this) else HidKeyboard.onPermissionMissing()
    }

    /** Called from the status banner's action button. */
    private fun fixStatus(status: HidStatus) {
        when (status) {
            HidStatus.NeedsPermission -> permissionLauncher.launch(requested)
            HidStatus.NeedsBluetoothRestart -> controlBluetooth(BluetoothAction.Restart)
            HidStatus.BluetoothOff -> controlBluetooth(BluetoothAction.TurnOn)
            else -> startKeyboard()
        }
    }

    /**
     * Since Android 13 apps can't switch Bluetooth themselves, but they can ask: Android shows
     * a small "allow?" dialog over BTDeck. Turning off uses Settings' own request, which isn't
     * in the public SDK; phones without it get the Bluetooth settings screen instead.
     */
    private fun controlBluetooth(action: BluetoothAction) {
        when (action) {
            BluetoothAction.TurnOn -> runCatching {
                enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            }
            BluetoothAction.TurnOff -> requestBluetoothOff()
            BluetoothAction.Restart -> {
                restartingBluetooth = true
                requestBluetoothOff()
            }
        }
    }

    private fun requestBluetoothOff() {
        val asked = runCatching { disableBtLauncher.launch(Intent(ACTION_REQUEST_DISABLE)) }.isSuccess
        if (asked) return
        restartingBluetooth = false
        Toast.makeText(this, "Turn Bluetooth off and on here", Toast.LENGTH_LONG).show()
        runCatching { startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }
    }

    /** Turning off takes a moment after the dialog closes; ask to turn on once it's done. */
    private fun turnBluetoothOnWhenOff() {
        val adapter = getSystemService(BluetoothManager::class.java)?.adapter ?: return
        lifecycleScope.launch {
            val off = withTimeoutOrNull(10_000) {
                while (adapter.state != BluetoothAdapter.STATE_OFF) delay(200)
            } != null
            if (off) controlBluetooth(BluetoothAction.TurnOn)
        }
    }

    private fun scanForDevices() {
        val granted = scanPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        if (!granted) return scanPermissionLauncher.launch(scanPermissions)
        // Android 11 and below return no scan results while location services are off.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S &&
            getSystemService(LocationManager::class.java)?.isLocationEnabled == false
        ) {
            Toast.makeText(
                this,
                "Turn on Location to scan for devices (Android 11 and below)",
                Toast.LENGTH_LONG,
            ).show()
            runCatching { startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
            return
        }
        DeviceScanner.scan()
    }

    private fun makeDiscoverable() {
        runCatching {
            discoverableLauncher.launch(
                Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
                    .putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120),
            )
        }
    }
}
