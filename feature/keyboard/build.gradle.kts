plugins {
    alias(libs.plugins.keyset.android.feature)
}

android {
    namespace = "dev.keyset.feature.keyboard"
}

dependencies {
    implementation(projects.core.bluetooth)
}
