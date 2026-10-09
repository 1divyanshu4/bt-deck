plugins {
    alias(libs.plugins.zexdeck.android.feature)
}

android {
    namespace = "dev.btdeck.feature.keyboard"
}

dependencies {
    implementation(projects.core.bluetooth)
}
