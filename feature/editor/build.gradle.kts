plugins {
    alias(libs.plugins.zexdeck.android.feature)
}

android {
    namespace = "dev.btdeck.feature.editor"
}

dependencies {
    implementation(projects.core.data)
}
