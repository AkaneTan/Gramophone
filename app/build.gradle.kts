@file:Suppress("UnstableApiUsage")

import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.parcelize")
}

android {
    val releaseType = readProperties(file("../package.properties")).getProperty("releaseType")
    val myVersionName = "." + "git rev-parse --short=6 HEAD".runCommand(workingDir = rootDir)
    if (releaseType.contains("\"")) {
        throw IllegalArgumentException("releaseType must not contain \"")
    }

    namespace = "org.akanework.gramophone"
    compileSdk = 34

    androidResources {
        generateLocaleConfig = true
    }

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "org.akanework.gramophone"
        // me.zhanghai.android.fastscroll requires 21 and its not worth the effort to change that
        minSdk = 21 // Android 5.0
        targetSdk = 34 // Android 14.0
        versionCode = 6
        versionName = "1.0.4.1"
        if (releaseType != "Release") {
            versionNameSuffix = myVersionName
        }
        buildConfigField(
            "String",
            "MY_VERSION_NAME",
            "\"$versionName$myVersionName\""
        )
        buildConfigField(
            "String",
            "RELEASE_TYPE",
            "\"$releaseType\""
        )
        setProperty("archivesBaseName", "Gramophone-$versionName${versionNameSuffix ?: ""}")
    }

    signingConfigs {
        create("release") {
            if (project.hasProperty("AKANE_RELEASE_KEY_ALIAS")) {
                storeFile = file(project.properties["AKANE_RELEASE_STORE_FILE"].toString())
                storePassword = project.properties["AKANE_RELEASE_STORE_PASSWORD"].toString()
                keyAlias = project.properties["AKANE_RELEASE_KEY_ALIAS"].toString()
                keyPassword = project.properties["AKANE_RELEASE_KEY_PASSWORD"].toString()
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (project.hasProperty("AKANE_RELEASE_KEY_ALIAS")) {
                signingConfig = signingConfigs["release"]
            }
        }
        debug {
            if (project.hasProperty("AKANE_RELEASE_KEY_ALIAS")) {
                signingConfig = signingConfigs["release"]
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-Xno-param-assertions",
            "-Xno-call-assertions",
            "-Xno-receiver-assertions",
        )
    }

    // https://gitlab.com/IzzyOnDroid/repo/-/issues/491
    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = false
    }

}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.transition:transition-ktx:1.5.0-alpha06")
    implementation("androidx.fragment:fragment-ktx:1.7.0-alpha10")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0") // <-- for predictive back
    implementation("androidx.appcompat:appcompat:1.7.0-alpha03")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0-alpha13")
    implementation("androidx.media3:media3-exoplayer:1.2.1")
    implementation("androidx.media3:media3-exoplayer-midi:1.2.1")
    implementation("androidx.media3:media3-session:1.2.1")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("com.github.bumptech.glide:glide:4.15.1")
    implementation("com.google.android.material:material:1.12.0-alpha03")
    implementation("me.zhanghai.android.fastscroll:library:1.3.0")
    ksp("com.github.bumptech.glide:ksp:4.15.1")
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.12")
    debugImplementation("net.jthink:jaudiotagger:3.0.1") // <-- for "SD Exploder"
    testImplementation("junit:junit:4.13.2")
}


fun String.runCommand(
    workingDir: File = File("."),
    timeoutAmount: Long = 60,
    timeoutUnit: TimeUnit = TimeUnit.SECONDS
): String = ProcessBuilder(split("\\s(?=(?:[^'\"`]*(['\"`])[^'\"`]*\\1)*[^'\"`]*$)".toRegex()))
    .directory(workingDir)
    .redirectOutput(ProcessBuilder.Redirect.PIPE)
    .redirectError(ProcessBuilder.Redirect.PIPE)
    .start()
    .apply { waitFor(timeoutAmount, timeoutUnit) }
    .run {
        inputStream.bufferedReader().readText().trim()
    }
fun readProperties(propertiesFile: File) = Properties().apply {
    propertiesFile.inputStream().use { fis ->
        load(fis)
    }
}