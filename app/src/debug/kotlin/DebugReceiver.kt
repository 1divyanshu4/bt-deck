package dev.keyset

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dev.keyset.core.bluetooth.DeviceScanner
import dev.keyset.core.bluetooth.HidKeyboard
import dev.keyset.core.model.comboLabel

/**
 * Debug builds only. Drive the keyboard from adb and read results from logcat (tag KeySetDebug):
 *
 *   adb shell am broadcast -n dev.keyset/.DebugReceiver --es cmd status
 *   ... --es cmd connect --es addr AA:BB:CC:DD:EE:FF
 *   ... --es cmd disconnect
 *   ... --es cmd button --es label Undo        (acts like tapping the pad key)
 *   ... --es cmd hold --es label Pan --el ms 1500
 *   ... --es cmd start | stop
 */
class DebugReceiver : BroadcastReceiver() {
    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as KeySetApp
        val out = when (val cmd = intent.getStringExtra("cmd")) {
            "status" -> "status=${HidKeyboard.status.value} host=${HidKeyboard.connectedAddress()}"
            "connect" -> {
                val addr = intent.getStringExtra("addr")!!.uppercase()
                val device = context.getSystemService(BluetoothManager::class.java).adapter.getRemoteDevice(addr)
                HidKeyboard.connect(device)
                "connect requested $addr"
            }
            "disconnect" -> {
                HidKeyboard.disconnect()
                "disconnect requested"
            }
            "pair" -> {
                val addr = intent.getStringExtra("addr")!!.uppercase()
                DeviceScanner.pair(context.getSystemService(BluetoothManager::class.java).adapter.getRemoteDevice(addr))
                "pair requested $addr"
            }
            "start" -> {
                KeyboardService.start(context)
                "start requested"
            }
            "stop" -> {
                HidKeyboard.stop()
                "stopped"
            }
            "button", "hold" -> {
                val label = intent.getStringExtra("label")
                val b = app.layouts.state.value.selected.buttons.firstOrNull { it.label == label }
                    ?: return log("no button '$label'")
                if (cmd == "hold") {
                    HidKeyboard.press(b.id, b.modifiers, b.key)
                    android.os.Handler(android.os.Looper.getMainLooper())
                        .postDelayed({ HidKeyboard.release(b.id) }, intent.getLongExtra("ms", 1000))
                } else {
                    HidKeyboard.tap(b.id, b.modifiers, b.key)
                }
                "$cmd '${b.label}' ${comboLabel(b.modifiers, b.key)}"
            }
            else -> "unknown cmd $cmd"
        }
        log(out)
    }

    private fun log(msg: String) {
        Log.i("KeySetDebug", msg)
    }
}
