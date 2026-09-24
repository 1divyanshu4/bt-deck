plugins {
    alias(libs.plugins.btdeck.android.feature)
}

android {
    namespace = "dev.btdeck.feature.keyboard"
}

dependencies {
    implementation(projects.core.bluetooth)
}
