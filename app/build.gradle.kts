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

    buildTypes {
        release {
            isMinifyEnabled = false
            buildConfigField("String", "UPDATE_CHANNEL", "\"stable\"")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
            buildConfigField("String", "UPDATE_CHANNEL", "\"beta\"")
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
