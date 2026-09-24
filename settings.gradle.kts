pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "BTDeck"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":app")
include(":core:model")
include(":core:data")
include(":core:bluetooth")
include(":core:designsystem")
include(":feature:pad")
include(":feature:editor")
include(":feature:connect")
include(":feature:keyboard")
include(":feature:layouts")
include(":feature:settings")
