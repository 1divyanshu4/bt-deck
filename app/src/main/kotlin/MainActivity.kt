package dev.keyset

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import dev.keyset.core.bluetooth.DeviceScanner
import dev.keyset.core.bluetooth.HidKeyboard
import dev.keyset.core.bluetooth.HidStatus
import dev.keyset.core.designsystem.theme.KeySetTheme

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
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as KeySetApp
        setContent {
            KeySetTheme {
                AppShell(
                    layouts = app.layouts,
                    settingsStore = app.settings,
                    onFixStatus = ::fixStatus,
                    onMakeDiscoverable = ::makeDiscoverable,
                    onScan = ::scanForDevices,
                )
            }
        }
        if (hasPermissions()) startKeyboard() else permissionLauncher.launch(requested)
    }

    override fun onResume() {
        super.onResume()
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
            HidStatus.NeedsBluetoothRestart -> runCatching {
                startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            }
            HidStatus.BluetoothOff -> runCatching {
                enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            }
            else -> startKeyboard()
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
