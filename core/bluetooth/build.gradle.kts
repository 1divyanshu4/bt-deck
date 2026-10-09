plugins {
    alias(libs.plugins.zexdeck.android.library)
}

android {
    namespace = "dev.btdeck.core.bluetooth"
}

dependencies {
    implementation(projects.core.model)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
}
