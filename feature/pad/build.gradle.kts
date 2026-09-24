plugins {
    alias(libs.plugins.btdeck.android.feature)
}

android {
    namespace = "dev.btdeck.feature.pad"
}

dependencies {
    implementation(projects.core.bluetooth)
}
