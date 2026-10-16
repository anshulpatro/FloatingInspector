import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.vanniktech.maven.publish")
    // Applied once here; it also covers :noop. `apiCheck` runs as part of `check`.
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
}

android {
    namespace = "com.anshulpatro.floatinginspector"
    compileSdk = 36

    defaultConfig {
        minSdk = 23
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    resourcePrefix = "debug_overlay_"

    testOptions {
        unitTests.isIncludeAndroidResources = true
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

// Coordinates, POM metadata and the Central Portal target come from gradle.properties.
mavenPublishing {
    if (providers.gradleProperty("signingInMemoryKey").isPresent ||
        providers.gradleProperty("signing.keyId").isPresent
    ) {
        signAllPublications()
    }
}

apiValidation {
    ignoredProjects += "sample"
}

// floatinginspector-noop must be a drop-in replacement: same classes, same signatures.
// apiCheck keeps each committed api/*.api dump in sync with its module's code; this task
// then requires the two dumps to match, so a public API change to one module without the
// other fails `check`. Synthetic members are ignored: they are compiler-generated (e.g. the
// accessor the companion uses to reach the real library's private constructor) and cannot
// be called from Kotlin or Java source.
val checkNoopApiParity by tasks.registering {
    group = "verification"
    description = "Fails if the no-op artifact's public API differs from the real library's."

    val realApi = layout.projectDirectory.file("api/FloatingInspector.api")
    val noopApi = layout.projectDirectory.file("noop/api/noop.api")
    inputs.files(realApi, noopApi)
    // When run alongside apiDump, compare the fresh dumps rather than the stale ones.
    mustRunAfter("apiDump", ":noop:apiDump")

    doLast {
        fun callableApi(file: File) = file.readLines().filterNot { " synthetic " in it }

        val real = callableApi(realApi.asFile)
        val noop = callableApi(noopApi.asFile)
        if (real != noop) {
            val differences = (real - noop.toSet()).map { "  - $it" } + (noop - real.toSet()).map { "  + $it" }
            throw GradleException(
                "Public API mismatch between floatinginspector (-) and floatinginspector-noop (+):\n" +
                    differences.joinToString("\n") + "\n" +
                    "Update the module that is behind, then run ./gradlew apiDump."
            )
        }
    }
}

tasks.named("check") {
    dependsOn(checkNoopApiParity)
}
