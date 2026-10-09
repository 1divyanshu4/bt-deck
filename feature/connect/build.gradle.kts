plugins {
    alias(libs.plugins.zexdeck.android.feature)
}

android {
    namespace = "dev.btdeck.feature.connect"
}

dependencies {
    implementation(projects.core.bluetooth)
}
