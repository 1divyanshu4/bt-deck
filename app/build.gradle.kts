plugins {
    alias(libs.plugins.btdeck.android.application)
    // Generates the open-source licenses list shown in Settings.
    alias(libs.plugins.aboutlibraries)
}

android {
    namespace = "dev.btdeck"

    defaultConfig {
        applicationId = "dev.btdeck"
        versionCode = 3
        versionName = "0.1.1"
        testInstrumentationRunner = "dev.btdeck.BTDeckTestRunner"
    }

    // The release key lives outside the repository; its location and passwords come from
    // ~/.gradle/gradle.properties (see docs/RELEASING.md). Without them, release builds are
    // left unsigned rather than signed with a key users could never update from.
    val releaseStore = providers.gradleProperty("BTDECK_RELEASE_STORE_FILE").orNull
    if (releaseStore != null) {
        signingConfigs.create("release") {
            storeFile = file(releaseStore)
            storePassword = providers.gradleProperty("BTDECK_RELEASE_STORE_PASSWORD").get()
            keyAlias = providers.gradleProperty("BTDECK_RELEASE_KEY_ALIAS").get()
            keyPassword = providers.gradleProperty("BTDECK_RELEASE_KEY_PASSWORD").get()
        }
    }

    buildTypes {
        release {
            // Shrinks the icon library down to the icons actually used.
            isMinifyEnabled = true
            isShrinkResources = true
            // Without this, R8 uses proguard-android.txt, which turns optimisation off.
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.findByName("release")
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
    implementation(libs.androidx.core.splashscreen)
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
