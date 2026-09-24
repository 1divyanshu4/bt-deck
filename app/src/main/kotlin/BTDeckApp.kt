package dev.btdeck

import android.app.Application
import dev.btdeck.core.bluetooth.DeviceScanner
import dev.btdeck.core.bluetooth.HidKeyboard
import dev.btdeck.core.data.CustomIcons
import dev.btdeck.core.data.LayoutStore
import dev.btdeck.core.data.LayoutTransfer
import dev.btdeck.core.data.SettingsStore

class BTDeckApp : Application() {
    lateinit var layouts: LayoutStore
        private set
    lateinit var settings: SettingsStore
        private set
    lateinit var transfer: LayoutTransfer
        private set

    override fun onCreate() {
        super.onCreate()
        layouts = LayoutStore(this)
        settings = SettingsStore(this)
        transfer = LayoutTransfer(this, layouts)
        HidKeyboard.init(this)
        DeviceScanner.init(this)
        CustomIcons.init(this)
    }
}
