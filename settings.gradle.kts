@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven(url = "https://jitpack.io") // TODO deprecated
        jcenter() // TODO deprecated
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io") // TODO deprecated
        jcenter() // TODO deprecated
    }
}

rootProject.name = "Xmp Android"
include(":app")
