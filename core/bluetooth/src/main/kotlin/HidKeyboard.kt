package dev.keyset.core.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppQosSettings
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import dev.keyset.core.model.Mod
import dev.keyset.core.model.buildReport
import dev.keyset.core.model.keystrokeFor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

sealed interface HidStatus {
    data object NoBluetooth : HidStatus
    data object BluetoothOff : HidStatus
    data object NeedsPermission : HidStatus

    /** Phone vendor disabled the HID Device profile, or registration was released. */
    data class Unsupported(val title: String, val detail: String) : HidStatus
    data object Starting : HidStatus

    /** Stopped from the notification. */
    data object Stopped : HidStatus
    data object Ready : HidStatus
    data class Connecting(val name: String) : HidStatus

    /** A connect the user asked for didn't complete. Behaves like Ready. */
    data class ConnectFailed(val name: String) : HidStatus

    /**
     * The stack is stuck holding an old keyboard session (e.g. KeySet was killed while
     * connected and the phone never finished releasing it). Only a Bluetooth restart clears it.
     */
    data object NeedsBluetoothRestart : HidStatus
    data class Connected(val name: String) : HidStatus
}

/**
 * Makes the phone appear as a Bluetooth keyboard (HID Device profile) so any host,
 * such as a tablet or laptop, can pair with it like a real keyboard. The host needs no app.
 *
 * Every pressed pad button contributes to one combined report, so a held modifier
 * (e.g. Space to pan) can stay down while other buttons are tapped.
 *
 * Follows the platform contract and Google's HID Device sample (ginkage/wearmouse):
 * - registration state comes only from onAppStatusChanged; registerApp()'s return value only
 *   says whether the request was sent (some builds return false and then succeed);
 * - GET_REPORT / SET_REPORT from the host are always answered;
 * - callbacks run on the main thread, so all state here is main-thread only;
 * - the connection is reconciled against the service, since callbacks can be missed.
 */
@SuppressLint("MissingPermission") // Callers check BLUETOOTH_CONNECT before start().
object HidKeyboard {
    private const val TAG = "HidKeyboard"
    private const val REPORT_ID = 1
    private const val PREFS = "hid"
    private const val PREF_LAST_HOST = "last_host"
    private const val PREF_AUTO_RECONNECT = "auto_reconnect"

    /** Source id for keys pressed by [typeText]. */
    private const val TYPING = "typing"
    private const val KEY_GAP_MS = 12L

    /** Caps Lock bit in the LED byte the host sends. */
    private const val LED_CAPS_LOCK = 0x02

    // Standard boot keyboard: 1 byte modifiers, 1 reserved, 6 key slots, plus the LED byte
    // the host sends back (so the keyboard knows when Caps Lock is on).
    private val DESCRIPTOR = byteArrayOf(
        0x05, 0x01, // Usage Page (Generic Desktop)
        0x09, 0x06, // Usage (Keyboard)
        0xA1.toByte(), 0x01, // Collection (Application)
        0x85.toByte(), REPORT_ID.toByte(),
        0x05, 0x07, //   Usage Page (Key Codes)
        0x19, 0xE0.toByte(), //   Usage Minimum (224)
        0x29, 0xE7.toByte(), //   Usage Maximum (231)
        0x15, 0x00, //   Logical Minimum (0)
        0x25, 0x01, //   Logical Maximum (1)
        0x75, 0x01, //   Report Size (1)
        0x95.toByte(), 0x08, //   Report Count (8)
        0x81.toByte(), 0x02, //   Input (Data, Variable, Absolute) — modifiers
        0x95.toByte(), 0x01, //   Report Count (1)
        0x75, 0x08, //   Report Size (8)
        0x81.toByte(), 0x01, //   Input (Constant) — reserved
        0x95.toByte(), 0x06, //   Report Count (6)
        0x75, 0x08, //   Report Size (8)
        0x15, 0x00, //   Logical Minimum (0)
        0x25, 0x65, //   Logical Maximum (101)
        0x05, 0x07, //   Usage Page (Key Codes)
        0x19, 0x00, //   Usage Minimum (0)
        0x29, 0x65, //   Usage Maximum (101)
        0x81.toByte(), 0x00, //   Input (Data, Array) — keys
        0x95.toByte(), 0x05, //   Report Count (5)
        0x75, 0x01, //   Report Size (1)
        0x05, 0x08, //   Usage Page (LEDs)
        0x19, 0x01, //   Usage Minimum (Num Lock)
        0x29, 0x05, //   Usage Maximum (Kana)
        0x91.toByte(), 0x02, //   Output (Data, Variable, Absolute) — LEDs
        0x95.toByte(), 0x01, //   Report Count (1)
        0x75, 0x03, //   Report Size (3)
        0x91.toByte(), 0x01, //   Output (Constant) — padding
        0xC0.toByte(), // End Collection
    )

    private val SDP = BluetoothHidDeviceAppSdpSettings(
        "KeySet",
        "KeySet shortcut pad",
        "KeySet",
        BluetoothHidDevice.SUBCLASS1_KEYBOARD,
        DESCRIPTOR,
    )

    // Outgoing QoS as in Google's sample: best effort, small reports, ~11 ms latency.
    private val QOS_OUT = BluetoothHidDeviceAppQosSettings(
        BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
        800,
        9,
        0,
        11_250,
        BluetoothHidDeviceAppQosSettings.MAX,
    )

    private val _status = MutableStateFlow<HidStatus>(HidStatus.Starting)
    val status: StateFlow<HidStatus> = _status.asStateFlow()

    private lateinit var appContext: Context
    private var adapter: BluetoothAdapter? = null
    private var hid: BluetoothHidDevice? = null
    private var host: BluetoothDevice? = null
    private var registered = false

    /** A profile proxy request is in flight; onCreate and onResume both call start(). */
    private var proxyRequested = false

    /** registerApp() was sent and onAppStatusChanged hasn't answered yet. */
    private var registerPending = false
    private var registerAttempts = 0

    /** The running connect attempt(s); the status belongs to it while it's active. */
    private var connectJob: Job? = null

    /** Result slot for the single attempt in flight: address to "did it connect". */
    private var pending: Pair<String, CompletableDeferred<Boolean>>? = null

    /** Host the user just disconnected; Android hosts may reconnect a known keyboard on their own. */
    private var userDisconnected: Pair<String, Long>? = null

    /** Address whose DISCONNECTED we're waiting for after the user pressed Disconnect. */
    private var awaitingDisconnect: String? = null

    /** Unregistering on purpose to reset a stuck stack; re-register when it's done. */
    private var reRegistering = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** Currently held keys, by source (a button id). */
    private val held = LinkedHashMap<String, Pair<Int, Int>>()
    private var lastReport = ByteArray(8)
    private val _heldIds = MutableStateFlow<Set<String>>(emptySet())

    /** Button ids whose keys are currently down, for lighting up held/toggled buttons. */
    val heldIds: StateFlow<Set<String>> = _heldIds.asStateFlow()

    private val _capsLock = MutableStateFlow(false)

    /**
     * Whether Caps Lock is on. The host reports it through the LED byte; [toggleCapsLock]
     * guesses ahead for hosts that don't (ones that paired before KeySet sent LEDs).
     */
    val capsLock: StateFlow<Boolean> = _capsLock.asStateFlow()

    /** Called when the Caps Lock key is pressed. The host's own LED report, if any, wins. */
    fun toggleCapsLock() {
        _capsLock.value = !_capsLock.value
    }

    /**
     * Set by the instrumentation runner: Bluetooth is never touched, so UI tests can't type
     * into whatever the test device is paired with. Reports still reach [reportListener].
     */
    @Volatile var testMode = false

    /** Observes every report built (tests). */
    @Volatile var reportListener: ((ByteArray) -> Unit)? = null

    /** Tests only: show a given connection state. */
    fun setStatusForTest(status: HidStatus) {
        check(testMode)
        _status.value = status
    }

    fun init(context: Context) {
        appContext = context.applicationContext
        if (testMode) return
        adapter = appContext.getSystemService(BluetoothManager::class.java)?.adapter
        if (adapter == null) _status.value = HidStatus.NoBluetooth
    }

    /** Registers the keyboard with the Bluetooth stack. Safe to call repeatedly. */
    fun start() {
        if (testMode) {
            if (_status.value == HidStatus.Starting) _status.value = HidStatus.Ready
            return
        }
        val a = adapter ?: return run { _status.value = HidStatus.NoBluetooth }
        if (!a.isEnabled) return run { _status.value = HidStatus.BluetoothOff }
        val h = hid
        if (h != null) {
            if (registered) reconcile() else register()
            return
        }
        if (proxyRequested) return
        proxyRequested = true
        _status.value = HidStatus.Starting
        val ok = a.getProfileProxy(
            appContext,
            object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    proxyRequested = false
                    hid = proxy as BluetoothHidDevice
                    register()
                }

                override fun onServiceDisconnected(profile: Int) {
                    resetState()
                    _status.value = HidStatus.Starting
                }
            },
            BluetoothProfile.HID_DEVICE,
        )
        if (!ok) {
            proxyRequested = false
            _status.value = HidStatus.Unsupported(
                "Keyboard mode not supported",
                "This phone doesn't offer Bluetooth keyboard mode (the HID Device profile). " +
                    "Some phone makers switch it off.",
            )
        }
    }

    fun stop() {
        connectJob?.cancel()
        releaseAll()
        hid?.let { h ->
            // Disconnect hosts before unregistering, as the platform sample does.
            runCatching { h.connectedDevices }.getOrDefault(emptyList()).forEach { h.disconnect(it) }
            if (registered) h.unregisterApp()
            adapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, h)
        }
        resetState()
        _status.value = HidStatus.Stopped
    }

    /** Bluetooth turned off: everything registered with the stack is gone. */
    fun onBluetoothOff() {
        connectJob?.cancel()
        resetState()
        _status.value = HidStatus.BluetoothOff
    }

    private fun resetState() {
        synchronized(held) { held.clear() }
        _heldIds.value = emptySet()
        hid = null
        host = null
        registered = false
        registerPending = false
        proxyRequested = false
        registerAttempts = 0
    }

    fun onPermissionMissing() {
        _status.value = HidStatus.NeedsPermission
    }

    private fun register() {
        val h = hid ?: return
        if (registered || registerPending) return
        registerPending = true
        _status.value = HidStatus.Starting
        val sent = h.registerApp(SDP, null, QOS_OUT, appContext.mainExecutor, callback)
        Log.d(TAG, "registerApp sent=$sent (attempt $registerAttempts)")
        // Only the callback says whether it worked. Give it time, then recover if it never came.
        scope.launch {
            delay(3_000)
            if (registered || !registerPending || hid !== h) return@launch
            registerPending = false
            recoverFailedRegistration(h)
        }
    }

    private fun recoverFailedRegistration(h: BluetoothHidDevice) {
        // Android allows one keyboard app at a time. The usual holder is our own stale
        // registration from before an update or crash; it has our uid, so unregistering
        // clears it. (It can't touch another app's registration.) Then retry.
        if (registerAttempts < 3) {
            registerAttempts++
            runCatching { h.unregisterApp() }
            scope.launch {
                delay(700L * registerAttempts)
                register()
            }
            return
        }
        registerAttempts = 0
        // Usually our own old session the stack never released, occasionally another
        // keyboard app. A Bluetooth restart fixes the first and KeySet re-registers itself.
        _status.value = HidStatus.NeedsBluetoothRestart
    }

    // Delivered on the main thread (registerApp's executor).
    private val callback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, isRegistered: Boolean) {
            Log.d(TAG, "app registered=$isRegistered plugged=${pluggedDevice?.address}")
            registerPending = false
            registered = isRegistered
            if (!isRegistered && reRegistering) {
                reRegistering = false
                host = null
                register()
                return
            }
            if (!isRegistered) {
                host = null
                connectJob?.cancel()
                _status.value = HidStatus.Unsupported(
                    "Keyboard mode stopped",
                    "Android released the keyboard registration. Tap Retry.",
                )
                return
            }
            registerAttempts = 0
            _status.value = HidStatus.Ready
            // Reconnect to the host we used last time, unless the user disconnected on purpose.
            val target = pluggedDevice ?: lastHost()
            // Not while a connect/switch is running: it re-registers on purpose and owns the flow.
            if (target != null && autoReconnect() && connectJob?.isActive != true) {
                connect(target, userInitiated = false)
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            Log.d(TAG, "connection ${device.address} -> $state")
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> onHostConnected(device)
                BluetoothProfile.STATE_CONNECTING ->
                    if (host == null) _status.value = HidStatus.Connecting(device.displayName())
                // Some builds (Xiaomi/MIUI) report DISCONNECTING and then never DISCONNECTED,
                // so treat either as the end of the connection.
                BluetoothProfile.STATE_DISCONNECTING, BluetoothProfile.STATE_DISCONNECTED -> {
                    if (state == BluetoothProfile.STATE_DISCONNECTED && awaitingDisconnect == device.address) {
                        awaitingDisconnect = null
                    }
                    pending?.takeIf { it.first == device.address }?.second?.complete(false)
                    if (host == null || host?.address == device.address) dropHost()
                }
            }
        }

        // Hosts query the input report (e.g. on connect); an unanswered GET_REPORT can stall them.
        override fun onGetReport(device: BluetoothDevice, type: Byte, id: Byte, bufferSize: Int) {
            val h = hid ?: return
            if (type == BluetoothHidDevice.REPORT_TYPE_INPUT) {
                h.replyReport(device, type, REPORT_ID.toByte(), lastReport)
            } else {
                h.reportError(device, BluetoothHidDevice.ERROR_RSP_UNSUPPORTED_REQ)
            }
        }

        // Caps/Num Lock LED state, sent over the control channel. It must be acknowledged.
        override fun onSetReport(device: BluetoothDevice, type: Byte, id: Byte, data: ByteArray) {
            if (type == BluetoothHidDevice.REPORT_TYPE_OUTPUT) onLeds(data)
            hid?.reportError(device, BluetoothHidDevice.ERROR_RSP_SUCCESS)
        }

        // The same LED state, sent over the interrupt channel (Linux and most hosts).
        override fun onInterruptData(device: BluetoothDevice, reportId: Byte, data: ByteArray) {
            onLeds(data)
        }

        // The host removed this keyboard (unpaired it): forget it as the auto-reconnect target.
        override fun onVirtualCableUnplug(device: BluetoothDevice) {
            Log.d(TAG, "virtual cable unplug ${device.address}")
            if (lastHostAddress() == device.address) saveLastHost(null)
            if (host?.address == device.address) dropHost()
        }
    }

    /** The stack strips the report id, so the LED byte comes first. */
    private fun onLeds(data: ByteArray) {
        val leds = data.firstOrNull()?.toInt() ?: return
        _capsLock.value = leds and LED_CAPS_LOCK != 0
    }

    private fun onHostConnected(device: BluetoothDevice) {
        val recent = userDisconnected
        if (recent != null &&
            recent.first == device.address &&
            System.currentTimeMillis() - recent.second < 15_000 &&
            connectJob?.isActive != true
        ) {
            // The host reconnected by itself right after the user pressed Disconnect.
            hid?.disconnect(device)
            return
        }
        host = device
        saveLastHost(device)
        setAutoReconnect(true)
        pending?.takeIf { it.first == device.address }?.second?.complete(true)
        _status.value = HidStatus.Connected(device.displayName())
    }

    /**
     * Brings [host] in line with what the service reports. Callbacks can be missed (some
     * builds never send DISCONNECTED), so this runs whenever the app comes to the front.
     */
    private fun reconcile() {
        val h = hid ?: return
        if (!registered || connectJob?.isActive == true) return
        val connected = runCatching { h.connectedDevices }.getOrDefault(emptyList())
        val current = host
        when {
            current != null && connected.none { it.address == current.address } -> dropHost()
            current == null && connected.isNotEmpty() -> onHostConnected(connected.first())
        }
    }

    fun pairedDevices(): List<BluetoothDevice> =
        runCatching { adapter?.bondedDevices?.toList().orEmpty() }.getOrDefault(emptyList())
            .sortedBy { it.displayName().lowercase() }

    /**
     * Connects to [device] as its keyboard, retrying a little. [userInitiated] failures are
     * shown in the UI; the automatic reconnect on startup quietly falls back to Ready.
     *
     * [afterPairing]: hosts refuse a keyboard connection until they've finished reading the
     * new device's services (BlueZ logs "unknown device"), so wait before the first try.
     */
    fun connect(device: BluetoothDevice, userInitiated: Boolean = true, afterPairing: Boolean = false) {
        val h = hid
        if (h == null || !registered) {
            // Not a keyboard yet — nothing to connect with. Try registering again instead.
            start()
            return
        }
        reconcile()
        if (host?.address == device.address) return // already connected to it
        userDisconnected = null
        val name = device.displayName()
        connectJob?.cancel()
        connectJob = scope.launch {
            _status.value = HidStatus.Connecting(name)
            // A keyboard serves one host: release the current one fully before switching.
            host?.takeIf { it.address != device.address }?.let { releaseHost(h, it) }
            val waits = if (afterPairing) listOf(2_500L, 4_000L, 6_000L) else listOf(0L, 2_000L)
            for (wait in waits) {
                delay(wait)
                // The host may have connected on its own in the meantime.
                if (host?.address == device.address) return@launch
                if (attempt(h, device)) return@launch
            }
            _status.value = if (userInitiated || afterPairing) HidStatus.ConnectFailed(name) else HidStatus.Ready
        }
    }

    /** One connect attempt; true once the host accepts, false if it refuses or times out. */
    private suspend fun attempt(h: BluetoothHidDevice, device: BluetoothDevice): Boolean {
        val result = CompletableDeferred<Boolean>()
        pending = device.address to result
        try {
            if (!h.connect(device)) return false
            val ok = withTimeoutOrNull(10_000) { result.await() } ?: false
            if (!ok && host?.address != device.address) runCatching { h.disconnect(device) }
            return ok || host?.address == device.address
        } finally {
            pending = null
        }
    }

    fun disconnect() {
        val h = hid ?: return
        connectJob?.cancel()
        val target = host
        saveLastHost(null)
        // Stay disconnected until the user connects again (the stack still remembers the
        // host as "plugged" and would otherwise hand it back on the next registration).
        setAutoReconnect(false)
        dropHost()
        if (target != null) {
            userDisconnected = target.address to System.currentTimeMillis()
            connectJob = scope.launch {
                releaseHost(h, target)
                if (registered) _status.value = HidStatus.Ready
            }
        }
    }

    /**
     * Disconnects [target] and makes sure the stack actually let go of it.
     *
     * Some stacks (Xiaomi/MIUI) never finish a keyboard-side disconnect: no DISCONNECTED
     * arrives and their HID layer stays "disconnecting", refusing every host until reset
     * ("hidd_l2cif_connect_ind: incoming CTRL in invalid state (5)"). unregisterApp() is the
     * documented way to drop the connection and reset that state, so re-register if needed.
     */
    private suspend fun releaseHost(h: BluetoothHidDevice, target: BluetoothDevice) {
        awaitingDisconnect = target.address
        h.disconnect(target)
        if (host?.address == target.address) {
            host = null
            synchronized(held) { held.clear() }
            _heldIds.value = emptySet()
        }
        val clean = withTimeoutOrNull(2_500) { while (awaitingDisconnect != null) delay(100) } != null
        if (clean || hid !== h) return
        Log.d(TAG, "disconnect never completed; re-registering to reset the stack")
        awaitingDisconnect = null
        reRegistering = true
        h.unregisterApp()
        withTimeoutOrNull(6_000) { while (reRegistering || !registered) delay(100) }
    }

    private fun dropHost() {
        host = null
        synchronized(held) { held.clear() }
        _heldIds.value = emptySet()
        lastReport = ByteArray(8)
        // While a connect job runs it decides the status (it may retry).
        if (registered && connectJob?.isActive != true) _status.value = HidStatus.Ready
    }

    fun connectedAddress(): String? = host?.address

    // ---- key reports ----

    fun press(source: String, modifiers: Int, key: Int) {
        synchronized(held) { held[source] = modifiers to key }
        sendHeld()
    }

    fun release(source: String) {
        val removed = synchronized(held) { held.remove(source) }
        if (removed != null) sendHeld()
    }

    fun isHeld(source: String): Boolean = synchronized(held) { source in held }

    /** Press and release, keeping any other held keys down. */
    fun tap(source: String, modifiers: Int, key: Int) {
        press(source, modifiers, key)
        scope.launch {
            delay(35)
            release(source)
        }
    }

    /**
     * Types [text] as key presses, one character at a time, using the US keyboard layout.
     * Returns how many characters were skipped because they have no key (e.g. emoji).
     */
    suspend fun typeText(text: String): Int {
        var skipped = 0
        for (c in text) {
            val keystroke = keystrokeFor(c)
            if (keystroke == null) {
                skipped++
                continue
            }
            // With Caps Lock on, the host flips letters; flip Shift to get the case asked for.
            val (shift, key) = keystroke
            val modifiers = if (_capsLock.value && c.isLetter()) shift xor Mod.SHIFT else shift
            press(TYPING, modifiers, key)
            delay(KEY_GAP_MS)
            release(TYPING)
            // Hosts can merge a repeated letter if press and release come too close together.
            delay(KEY_GAP_MS)
        }
        return skipped
    }

    fun releaseAll() {
        synchronized(held) { held.clear() }
        sendHeld()
    }

    private fun sendHeld() {
        val report = synchronized(held) {
            _heldIds.value = held.keys.toSet()
            buildReport(held.values)
        }
        lastReport = report
        reportListener?.invoke(report)
        if (testMode) return
        val h = hid ?: return
        val device = host ?: return
        if (!h.sendReport(device, REPORT_ID, report)) Log.w(TAG, "sendReport refused")
    }

    // ---- persistence ----

    private fun prefs() = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun lastHostAddress(): String? = prefs().getString(PREF_LAST_HOST, null)

    private fun lastHost(): BluetoothDevice? {
        val addr = lastHostAddress() ?: return null
        return pairedDevices().firstOrNull { it.address == addr }
    }

    private fun autoReconnect(): Boolean = prefs().getBoolean(PREF_AUTO_RECONNECT, true)

    private fun setAutoReconnect(on: Boolean) {
        prefs().edit().putBoolean(PREF_AUTO_RECONNECT, on).apply()
    }

    private fun saveLastHost(device: BluetoothDevice?) {
        prefs().edit().putString(PREF_LAST_HOST, device?.address).apply()
    }
}
