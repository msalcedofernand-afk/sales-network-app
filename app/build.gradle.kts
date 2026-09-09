plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

import java.util.Properties

val versionPropsFile = rootProject.file("version.properties")
val versionProps = Properties()
if (versionPropsFile.exists()) {
    versionPropsFile.inputStream().use { versionProps.load(it) }
}
val buildCode = versionProps.getProperty("versionCode")?.toIntOrNull() ?: 1
val baseName = versionProps.getProperty("versionName") ?: "1.0.0"

tasks.matching { it.name.startsWith("assemble") }.configureEach {
    doLast {
        val newCode = (versionProps.getProperty("versionCode")?.toIntOrNull() ?: 1) + 1
        versionProps.setProperty("versionCode", newCode.toString())
        versionPropsFile.outputStream().use { versionProps.store(it, "Auto-incremented build code") }

        // Auto-update version JSON manifests
        val channel = when {
            name.contains("stable", ignoreCase = true) -> "stable"
            name.contains("beta", ignoreCase = true) -> "beta"
            else -> "stable"
        }
        val webUpdatesDir = rootProject.file("web/public/updates")
        val apkUrls = mapOf(
            "stable" to "https://raw.githubusercontent.com/msalcedofernand-afk/sales-network-app-releases/main/releases/sales-network-stable.apk",
            "beta" to "https://raw.githubusercontent.com/msalcedofernand-afk/sales-network-app-releases/beta/releases/sales-network-beta.apk"
        )
        val releaseNotes = mapOf(
            "stable" to "Versión estable actualizada. Mejoras de rendimiento y correcciones.",
            "beta" to "Versión beta con cambios experimentales."
        )
        try {
            val jsonContent = """
                {
                    "versionCode": $newCode,
                    "versionName": "${versionProps.getProperty("versionName", "1.0.0")}",
                    "channel": "$channel",
                    "apkUrl": "${apkUrls[channel]}",
                    "releaseNotes": "${releaseNotes[channel]}",
                    "mandatory": true
                }
            """.trimIndent()
            val jsonFile = File(webUpdatesDir, "$channel.json")
            jsonFile.parentFile?.mkdirs()
            jsonFile.writeText(jsonContent)
            logger.lifecycle("Updated version manifest: ${jsonFile.absolutePath} (code=$newCode)")
        } catch (e: Exception) {
            logger.warn("Failed to update version manifest: ${e.message}")
        }
    }
}

android {
    namespace = "com.salesnetwork.avon.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.salesnetwork.avon.app"
        minSdk = 26
        targetSdk = 35
        versionCode = buildCode
        versionName = baseName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += "channel"
    productFlavors {
        create("stable") {
            dimension = "channel"
            buildConfigField("String", "UPDATE_CHANNEL", "\"stable\"")
        }
        create("beta") {
            dimension = "channel"
            applicationIdSuffix = ".beta"
            versionNameSuffix = "-beta"
            buildConfigField("String", "UPDATE_CHANNEL", "\"beta\"")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Jsoup for catalog web scraping
    implementation("org.jsoup:jsoup:1.16.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("org.json:json:20230227")
    testImplementation("org.jsoup:jsoup:1.16.1")
}
