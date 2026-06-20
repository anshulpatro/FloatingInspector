import com.vanniktech.maven.publish.SonatypeHost

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.vanniktech.maven.publish")
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

    kotlinOptions {
        jvmTarget = "1.8"
    }

    resourcePrefix = "debug_overlay_"
}

dependencies {
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
    if (providers.gradleProperty("signingInMemoryKey").isPresent ||
        providers.gradleProperty("signing.keyId").isPresent
    ) {
        signAllPublications()
    }

    coordinates(
        providers.gradleProperty("GROUP").get(),
        providers.gradleProperty("ARTIFACT_ID").get(),
        providers.gradleProperty("VERSION_NAME").get()
    )

    pom {
        name.set("FloatingInspector")
        description.set("On-device floating overlay that shows live Firebase Analytics events.")
        inceptionYear.set("2026")
        url.set("https://github.com/anshulpatro/FloatingInspector")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
            }
        }
        developers {
            developer {
                id.set("anshulpatro")
                name.set("Anshul Patro")
                url.set("https://github.com/anshulpatro")
            }
        }
        scm {
            url.set("https://github.com/anshulpatro/FloatingInspector")
            connection.set("scm:git:git://github.com/anshulpatro/FloatingInspector.git")
            developerConnection.set("scm:git:ssh://git@github.com/anshulpatro/FloatingInspector.git")
        }
    }
}
