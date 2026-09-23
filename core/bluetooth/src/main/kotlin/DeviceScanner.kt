package dev.keyset.core.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NearbyDevice(val device: BluetoothDevice, val name: String?, val rssi: Int, val isComputer: Boolean)

data class ScanState(
    val paired: List<BluetoothDevice> = emptyList(),
    val nearby: List<NearbyDevice> = emptyList(),
    val scanning: Boolean = false,
    /** Address we asked to pair with; connect to it as soon as the bond completes. */
    val pairing: String? = null,
)

/**
 * Finds hosts (tablets, laptops) so they can be paired from the phone instead of from the
 * host's settings. The host must be discoverable, which usually means its Bluetooth
 * settings screen is open.
 */
@SuppressLint("MissingPermission") // The activity checks scan/connect permissions before calling in.
object DeviceScanner {
    private val _state = MutableStateFlow(ScanState())
    val state: StateFlow<ScanState> = _state.asStateFlow()

    private lateinit var appContext: Context
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var watchdog: Job? = null
    private val adapter: BluetoothAdapter?
        get() = appContext.getSystemService(BluetoothManager::class.java)?.adapter

    fun init(context: Context) {
        appContext = context.applicationContext
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothDevice.ACTION_NAME_CHANGED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        // Exported: these broadcasts come from the Bluetooth process, not the system server,
        // so a not-exported receiver misses some of them. They're all protected broadcasts.
        ContextCompat.registerReceiver(appContext, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
    }

    fun refreshPaired() {
        val paired = runCatching { adapter?.bondedDevices?.toList().orEmpty() }.getOrDefault(emptyList())
            .sortedBy { it.displayName().lowercase() }
        _state.value = _state.value.copy(
            paired = paired,
            nearby = _state.value.nearby.filterNot { n -> paired.any { it.address == n.device.address } },
        )
    }

    /** Clears the nearby list and runs a fresh ~12 s discovery. */
    fun scan() {
        val a = adapter ?: return
        refreshPaired()
        _state.value = _state.value.copy(nearby = emptyList())
        runCatching {
            if (a.isDiscovering) a.cancelDiscovery()
            if (a.startDiscovery()) {
                _state.value = _state.value.copy(scanning = true)
                watchDiscovery(a)
            }
        }
    }

    /** Backup for a missed DISCOVERY_FINISHED broadcast, so the spinner never gets stuck. */
    private fun watchDiscovery(a: BluetoothAdapter) {
        watchdog?.cancel()
        watchdog = scope.launch {
            delay(4_000)
            while (runCatching { a.isDiscovering }.getOrDefault(false)) delay(1_000)
            _state.value = _state.value.copy(scanning = false)
        }
    }

    /** Discovery slows down other Bluetooth traffic, including key reports; stop when done. */
    fun stopScan() {
        watchdog?.cancel()
        runCatching { adapter?.takeIf { it.isDiscovering }?.cancelDiscovery() }
        _state.value = _state.value.copy(scanning = false)
    }

    fun pair(device: BluetoothDevice) {
        stopScan()
        _state.value = _state.value.copy(pairing = device.address)
        if (runCatching { device.createBond() }.getOrDefault(false) != true) {
            _state.value = _state.value.copy(pairing = null)
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothAdapter.ACTION_DISCOVERY_STARTED ->
                    _state.value = _state.value.copy(scanning = true)
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED ->
                    _state.value = _state.value.copy(scanning = false)
                BluetoothDevice.ACTION_FOUND, BluetoothDevice.ACTION_NAME_CHANGED -> {
                    val device = intent.deviceExtra() ?: return
                    if (device.bondState == BluetoothDevice.BOND_BONDED) return
                    val found = NearbyDevice(
                        device = device,
                        name =
                        intent.getStringExtra(BluetoothDevice.EXTRA_NAME) ?: runCatching {
                            device.name
                        }.getOrNull(),
                        rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt(),
                        isComputer = device.bluetoothClass?.majorDeviceClass == BluetoothClass.Device.Major.COMPUTER,
                    )
                    val list = _state.value.nearby.filterNot { it.device.address == device.address } + found
                    // Named devices first, then strongest signal.
                    _state.value = _state.value.copy(
                        nearby = list.sortedWith(
                            compareBy<NearbyDevice> {
                                it.name == null
                            }.thenByDescending { it.rssi },
                        ),
                    )
                }
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val device = intent.deviceExtra() ?: return
                    val bond = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE)
                    val wasPairing = _state.value.pairing == device.address
                    if (bond != BluetoothDevice.BOND_BONDING && wasPairing) {
                        _state.value = _state.value.copy(pairing = null)
                    }
                    refreshPaired()
                    if (bond == BluetoothDevice.BOND_BONDED &&
                        wasPairing
                    ) {
                        HidKeyboard.connect(device, afterPairing = true)
                    }
                }
            }
        }
    }

    private fun Intent.deviceExtra(): BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
    }
}
