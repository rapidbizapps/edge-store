val objectboxVersion = "5.0.1"

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("maven-publish")
    id("io.objectbox") version "5.1.0" apply false
}

android {
    namespace = "com.github.rapidbizapps.edgestore"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
        targetSdk = 35
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
        // Fix for Java 17+ module access issues with kapt
        freeCompilerArgs += listOf(
            "-Xjvm-default=all"
        )
    }

    publishing {
        singleVariant("release")
    }
}

dependencies {
    // Manually add objectbox-android-objectbrowser only for debug builds,
    // and objectbox-android for release builds.
    debugImplementation("io.objectbox:objectbox-android-objectbrowser:$objectboxVersion")
    releaseImplementation("io.objectbox:objectbox-android:$objectboxVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

}

apply(plugin = "io.objectbox")

afterEvaluate {
    publishing {
        publications {
            register<MavenPublication>("release") {
                from(components["release"])
                groupId = project.findProperty("group")?.toString() ?: "com.github.rapidbizapps"
                artifactId = "edge-store"
                version = project.findProperty("version")?.toString() ?: "local"
            }
        }
    }
}
