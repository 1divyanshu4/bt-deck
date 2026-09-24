plugins {
    alias(libs.plugins.btdeck.android.feature)
}

android {
    namespace = "dev.btdeck.feature.editor"
}

dependencies {
    implementation(projects.core.data)
}
