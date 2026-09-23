plugins {
    alias(libs.plugins.keyset.android.feature)
}

android {
    namespace = "dev.keyset.feature.pad"
}

dependencies {
    implementation(projects.core.bluetooth)
}
