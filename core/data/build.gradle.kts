plugins {
    alias(libs.plugins.keyset.android.library)
}

android {
    namespace = "dev.keyset.core.data"
}

dependencies {
    api(projects.core.model)
    implementation(libs.kotlinx.coroutines.android)
    // Real org.json on the JVM; android.jar only has stubs.
    testImplementation(libs.org.json)
}
