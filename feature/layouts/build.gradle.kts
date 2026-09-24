plugins {
    alias(libs.plugins.keyset.android.feature)
}

android {
    namespace = "dev.keyset.feature.layouts"
}

dependencies {
    implementation(projects.core.data)
    implementation(libs.androidx.core.ktx) // FileProvider for sharing
}
