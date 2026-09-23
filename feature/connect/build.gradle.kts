plugins {
    alias(libs.plugins.keyset.android.feature)
}

android {
    namespace = "dev.keyset.feature.connect"
}

dependencies {
    implementation(projects.core.bluetooth)
}
