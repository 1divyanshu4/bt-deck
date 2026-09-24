plugins {
    alias(libs.plugins.btdeck.android.feature)
}

android {
    namespace = "dev.btdeck.feature.layouts"
}

dependencies {
    implementation(projects.core.data)
    implementation(libs.androidx.core.ktx) // FileProvider for sharing
}
