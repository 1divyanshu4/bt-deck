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
            id = libs.plugins.zexdeck.android.application.get().pluginId
            implementationClass = "dev.btdeck.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = libs.plugins.zexdeck.android.library.get().pluginId
            implementationClass = "dev.btdeck.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = libs.plugins.zexdeck.android.compose.get().pluginId
            implementationClass = "dev.btdeck.buildlogic.AndroidComposeConventionPlugin"
        }
        register("androidFeature") {
            id = libs.plugins.zexdeck.android.feature.get().pluginId
            implementationClass = "dev.btdeck.buildlogic.AndroidFeatureConventionPlugin"
        }
        register("jvmLibrary") {
            id = libs.plugins.zexdeck.jvm.library.get().pluginId
            implementationClass = "dev.btdeck.buildlogic.JvmLibraryConventionPlugin"
        }
    }
}
