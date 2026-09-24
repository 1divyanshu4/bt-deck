plugins {
    `kotlin-dsl`
}

group = "dev.btdeck.buildlogic"

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
            id = libs.plugins.btdeck.android.application.get().pluginId
            implementationClass = "dev.btdeck.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = libs.plugins.btdeck.android.library.get().pluginId
            implementationClass = "dev.btdeck.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = libs.plugins.btdeck.android.compose.get().pluginId
            implementationClass = "dev.btdeck.buildlogic.AndroidComposeConventionPlugin"
        }
        register("androidFeature") {
            id = libs.plugins.btdeck.android.feature.get().pluginId
            implementationClass = "dev.btdeck.buildlogic.AndroidFeatureConventionPlugin"
        }
        register("jvmLibrary") {
            id = libs.plugins.btdeck.jvm.library.get().pluginId
            implementationClass = "dev.btdeck.buildlogic.JvmLibraryConventionPlugin"
        }
    }
}
