plugins {
    alias(libs.plugins.keyset.android.application)
    // Generates the open-source licenses list shown in Settings.
    alias(libs.plugins.aboutlibraries)
}

android {
    namespace = "dev.keyset"

    defaultConfig {
        applicationId = "dev.keyset"
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "dev.keyset.KeySetTestRunner"
    }

    buildTypes {
        release {
            // Shrinks the icon library down to the icons actually used.
            isMinifyEnabled = true
            isShrinkResources = true
            // Signed with the debug key so it installs over test builds. Use a real
            // keystore (see docs/RELEASING.md) before publishing anywhere.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.data)
    implementation(projects.core.bluetooth)
    implementation(projects.core.designsystem)
    implementation(projects.feature.pad)
    implementation(projects.feature.editor)
    implementation(projects.feature.connect)
    implementation(projects.feature.keyboard)
    implementation(projects.feature.layouts)
    implementation(projects.feature.settings)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.aboutlibraries.compose.m3)

    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4.accessibility)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.espresso.core)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
