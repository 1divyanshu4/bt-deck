plugins {
    alias(libs.plugins.zexdeck.android.feature)
}

android {
    namespace = "dev.btdeck.feature.pad"
}

dependencies {
    implementation(projects.core.bluetooth)
}
