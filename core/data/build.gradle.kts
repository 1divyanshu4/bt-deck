plugins {
    alias(libs.plugins.btdeck.android.library)
}

android {
    namespace = "dev.btdeck.core.data"
}

dependencies {
    api(projects.core.model)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.datastore.preferences)
    // Real org.json on the JVM; android.jar only has stubs.
    testImplementation(libs.org.json)
}
