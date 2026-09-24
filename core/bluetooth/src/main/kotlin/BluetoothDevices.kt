package dev.btdeck.core.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice

// Both need BLUETOOTH_CONNECT on Android 12+. The UI only reaches them after the permission
// is granted, and a revoked permission throws SecurityException, which falls back here.

/** The device's name, or its address if the name is unknown or unreadable. */
@SuppressLint("MissingPermission")
fun BluetoothDevice.displayName(): String = runCatching { name }.getOrNull() ?: address

/** The device's class (laptop, phone, headphones…), or null if unreadable. */
@SuppressLint("MissingPermission")
fun BluetoothDevice.bluetoothClassOrNull(): BluetoothClass? = runCatching { bluetoothClass }.getOrNull()
