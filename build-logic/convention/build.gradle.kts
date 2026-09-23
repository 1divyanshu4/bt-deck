plugins {
    `kotlin-dsl`
}

group = "dev.keyset.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = libs.plugins.keyset.android.application.get().pluginId
            implementationClass = "dev.keyset.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = libs.plugins.keyset.android.library.get().pluginId
            implementationClass = "dev.keyset.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = libs.plugins.keyset.android.compose.get().pluginId
            implementationClass = "dev.keyset.buildlogic.AndroidComposeConventionPlugin"
        }
        register("androidFeature") {
            id = libs.plugins.keyset.android.feature.get().pluginId
            implementationClass = "dev.keyset.buildlogic.AndroidFeatureConventionPlugin"
        }
        register("jvmLibrary") {
            id = libs.plugins.keyset.jvm.library.get().pluginId
            implementationClass = "dev.keyset.buildlogic.JvmLibraryConventionPlugin"
        }
    }
}
