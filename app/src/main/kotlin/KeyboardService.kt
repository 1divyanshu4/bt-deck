package dev.btdeck

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import dev.btdeck.core.bluetooth.HidKeyboard
import dev.btdeck.core.bluetooth.HidStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Android unregisters a Bluetooth HID app as soon as it leaves the foreground, which
 * drops the tablet connection. Running as a foreground service keeps it registered
 * while you switch to other apps.
 */
class KeyboardService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Keyboard connection", NotificationManager.IMPORTANCE_LOW),
        )
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification(HidKeyboard.status.value),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            } else {
                0
            },
        )
        scope.launch {
            HidKeyboard.status.collect { nm.notify(NOTIFICATION_ID, notification(it)) }
        }
        ContextCompat.registerReceiver(
            this,
            bluetoothState,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED),
            ContextCompat.RECEIVER_EXPORTED,
        )
    }

    /** Re-register as soon as Bluetooth comes back, e.g. after the user restarts it. */
    private val bluetoothState = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                BluetoothAdapter.STATE_OFF -> HidKeyboard.onBluetoothOff()
                BluetoothAdapter.STATE_ON -> HidKeyboard.start()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            HidKeyboard.stop()
            stopSelf()
            return START_NOT_STICKY
        }
        HidKeyboard.start()
        return START_STICKY
    }

    /**
     * Swiped away from recents while not connected: release keyboard mode. A device registered
     * as a keyboard can't accept one, so a tablet with BTDeck idling would reject the phone.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        if (HidKeyboard.connectedAddress() == null) {
            HidKeyboard.stop()
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(bluetoothState) }
        scope.cancel()
        super.onDestroy()
    }

    private fun notification(status: HidStatus) = NotificationCompat.Builder(this, CHANNEL)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(
            when (status) {
                is HidStatus.Connected -> "Connected to ${status.name}"
                is HidStatus.Connecting -> "Connecting to ${status.name}…"
                HidStatus.Ready -> "Keyboard ready — not connected"
                else -> "BTDeck keyboard"
            },
        )
        .setContentText("Tap to open the shortcut pad")
        .setOngoing(true)
        .setSilent(true)
        .setContentIntent(
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE,
            ),
        )
        .addAction(
            0,
            "Stop",
            PendingIntent.getService(
                this,
                1,
                Intent(this, KeyboardService::class.java).setAction(ACTION_STOP),
                PendingIntent.FLAG_IMMUTABLE,
            ),
        )
        .build()

    companion object {
        private const val CHANNEL = "keyboard"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_STOP = "dev.btdeck.STOP"

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, KeyboardService::class.java))
        }
    }
}
