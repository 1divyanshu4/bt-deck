plugins {
    alias(libs.plugins.keyset.android.library)
}

android {
    namespace = "dev.keyset.core.bluetooth"
}

dependencies {
    implementation(projects.core.model)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
}
