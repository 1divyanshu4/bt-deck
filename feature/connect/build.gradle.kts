plugins {
    alias(libs.plugins.btdeck.android.feature)
}

android {
    namespace = "dev.btdeck.feature.connect"
}

dependencies {
    implementation(projects.core.bluetooth)
}
