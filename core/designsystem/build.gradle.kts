plugins {
    alias(libs.plugins.keyset.android.compose)
}

android {
    namespace = "dev.keyset.core.designsystem"
}

dependencies {
    api(libs.androidx.compose.material.icons.extended)
    // ButtonIcon draws the user's custom images, which core:data stores.
    implementation(projects.core.data)
}
