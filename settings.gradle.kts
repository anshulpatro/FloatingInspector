pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.library") version "8.13.2"
        id("com.android.application") version "8.13.2"
        id("org.jetbrains.kotlin.android") version "2.1.21"
        id("com.vanniktech.maven.publish") version "0.30.0"
        id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.17.0"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "FloatingInspector"
include(":noop")
include(":sample")
