pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS) // Использовать репозитории из settings.gradle.kts
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "SF_New"
include(":app")
