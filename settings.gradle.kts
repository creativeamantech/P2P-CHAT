pluginManagement {
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

rootProject.name = "P2PChatApp"
include(":app")
include(":core:model")
include(":core:crypto")
include(":core:storage")
include(":core:network")
include(":core:ui")
include(":feature:conversations")
include(":feature:messaging")
include(":feature:topics")
include(":feature:peers")
include(":feature:settings")
include(":test-utils")
