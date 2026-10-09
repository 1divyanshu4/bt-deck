package dev.btdeck.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

/**
 * A feature module: Compose UI for one area of the app. Features depend on core modules only,
 * never on each other; :app wires them together.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("zexdeck.android.compose")
        dependencies {
            add("implementation", project(":core:model"))
            add("implementation", project(":core:designsystem"))
            add("implementation", libs.library("androidx-activity-compose"))
            add("implementation", libs.library("androidx-compose-material-icons-extended"))
        }
    }
}
