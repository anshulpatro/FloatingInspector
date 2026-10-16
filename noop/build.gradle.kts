// floatinginspector-noop: the same public API as the real library with empty bodies, for
// release builds. No service, no SYSTEM_ALERT_WINDOW permission, no resources and no
// dependencies. The root project's checkNoopApiParity task keeps the two APIs identical.
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.vanniktech.maven.publish")
}

android {
    namespace = "com.anshulpatro.floatinginspector.noop"
    compileSdk = 36

    defaultConfig {
        minSdk = 23
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_1_8
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
}

// Coordinates and POM metadata come from gradle.properties (this module overrides the
// artifact id, name and description in noop/gradle.properties).
mavenPublishing {
    if (providers.gradleProperty("signingInMemoryKey").isPresent ||
        providers.gradleProperty("signing.keyId").isPresent
    ) {
        signAllPublications()
    }
}
