package dev.keyset.buildlogic

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** Adds Compose to an Android library (apply after keyset.android.library). */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("keyset.android.library")
        extensions.configure<LibraryExtension> { configureCompose(this) }
    }
}
