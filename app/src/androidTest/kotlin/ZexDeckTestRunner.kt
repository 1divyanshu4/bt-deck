package dev.btdeck

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dev.btdeck.core.bluetooth.HidKeyboard

/** Puts the keyboard in test mode before the app starts, so UI tests never use Bluetooth. */
class ZexDeckTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, className: String?, context: Context?): Application {
        HidKeyboard.testMode = true
        return super.newApplication(cl, className, context)
    }
}
