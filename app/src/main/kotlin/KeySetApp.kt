package dev.keyset

import android.app.Application
import dev.keyset.core.bluetooth.DeviceScanner
import dev.keyset.core.bluetooth.HidKeyboard
import dev.keyset.core.data.CustomIcons
import dev.keyset.core.data.LayoutStore

class KeySetApp : Application() {
    lateinit var layouts: LayoutStore
        private set

    override fun onCreate() {
        super.onCreate()
        layouts = LayoutStore(this)
        HidKeyboard.init(this)
        DeviceScanner.init(this)
        CustomIcons.init(this)
    }
}
