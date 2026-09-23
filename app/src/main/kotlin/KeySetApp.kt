package dev.keyset

import android.app.Application
import dev.keyset.core.bluetooth.DeviceScanner
import dev.keyset.core.bluetooth.HidKeyboard
import dev.keyset.core.data.CustomIcons
import dev.keyset.core.data.LayoutStore
import dev.keyset.core.data.SettingsStore

class KeySetApp : Application() {
    lateinit var layouts: LayoutStore
        private set
    lateinit var settings: SettingsStore
        private set

    override fun onCreate() {
        super.onCreate()
        layouts = LayoutStore(this)
        settings = SettingsStore(this)
        HidKeyboard.init(this)
        DeviceScanner.init(this)
        CustomIcons.init(this)
    }
}
