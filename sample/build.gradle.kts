import com.android.build.api.artifact.SingleArtifact
import javax.xml.parsers.DocumentBuilderFactory
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.w3c.dom.Element

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.anshulpatro.floatinginspector.sample"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.anshulpatro.floatinginspector.sample"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
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
    // Depend on the library source in this repo so the sample always tests the latest code.
    // This is the setup the README recommends: the real overlay in debug builds, the no-op in
    // release builds. Building both variants also proves the same call sites compile against both.
    debugImplementation(project(":"))
    releaseImplementation(project(":noop"))
}

// Checks each variant's merged manifest: debug must contain the overlay permission and service
// (from the real library), and release must contain neither (the no-op declares nothing).
androidComponents {
    onVariants { variant ->
        val expectOverlay = variant.buildType == "debug"
        val mergedManifest = variant.artifacts.get(SingleArtifact.MERGED_MANIFEST)
        val verify = tasks.register("verify${variant.name.replaceFirstChar { it.uppercase() }}OverlayManifest") {
            group = "verification"
            description = "Checks that the ${variant.name} merged manifest " +
                (if (expectOverlay) "declares" else "does not declare") + " the debug overlay."
            inputs.file(mergedManifest)

            doLast {
                val androidNs = "http://schemas.android.com/apk/res/android"
                val doc = DocumentBuilderFactory.newInstance()
                    .apply { isNamespaceAware = true }
                    .newDocumentBuilder()
                    .parse(mergedManifest.get().asFile)
                fun names(tag: String): List<String> {
                    val nodes = doc.getElementsByTagName(tag)
                    return (0 until nodes.length).map { (nodes.item(it) as Element).getAttributeNS(androidNs, "name") }
                }

                val hasPermission = "android.permission.SYSTEM_ALERT_WINDOW" in names("uses-permission")
                val hasService = "com.anshulpatro.floatinginspector.DebugOverlayService" in names("service")
                if (hasPermission != expectOverlay || hasService != expectOverlay) {
                    throw GradleException(
                        "${variant.name} merged manifest: expected overlay permission and service to be " +
                            (if (expectOverlay) "present" else "absent") +
                            ", found permission=$hasPermission service=$hasService."
                    )
                }
            }
        }
        tasks.named("check") { dependsOn(verify) }
    }
}
