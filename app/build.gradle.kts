@file:Suppress("UnstableApiUsage")

import com.android.build.gradle.tasks.PackageAndroidArtifact
import org.jetbrains.kotlin.util.removeSuffixIfPresent
import java.util.Properties

val aboutLibsVersion = "11.1.0" // keep in sync with plugin version

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.parcelize")
    id("com.mikepenz.aboutlibraries.plugin")
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

    packaging {
        dex {
            useLegacyPackaging = false
        }
        jniLibs {
            useLegacyPackaging = false
        }
        resources {
            excludes += "META-INF/*.version"
        }
    }

    defaultConfig {
        applicationId = "org.akanework.gramophone"
        // Reasons to not support KK include me.zhanghai.android.fastscroll, WindowInsets for
        // bottom sheet padding, ExoPlayer requiring multidex for KK and poor SD card support
        // That said, supporting Android 5.0 barely costs any tech debt and we plan to keep support
        // for it for a while.
        minSdk = 21
        targetSdk = 34
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
            if (releaseType != "Profile") {
                isMinifyEnabled = true
                isShrinkResources = true
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro",
                )
            } else {
                isMinifyEnabled = false
                isProfileable = true
            }
            if (project.hasProperty("AKANE_RELEASE_KEY_ALIAS")) {
                signingConfig = signingConfigs["release"]
            }
        }
        debug {
            if (project.hasProperty("AKANE_RELEASE_KEY_ALIAS")) {
                signingConfig = signingConfigs["release"]
            }
            isPseudoLocalesEnabled = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-Xno-param-assertions",
            "-Xno-call-assertions",
            "-Xno-receiver-assertions",
        )
    }

    lint {
        lintConfig = file("lint.xml")
    }

    // https://gitlab.com/IzzyOnDroid/repo/-/issues/491
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    // https://stackoverflow.com/a/77745844
    tasks.withType<PackageAndroidArtifact> {
        doFirst { appMetadata.asFile.orNull?.writeText("") }
    }
}

aboutLibraries {
    configPath = "app/config"
}

dependencies {
    val glideVersion = "5.0.0-rc01"
    val media3Version = "1.3.0"
    implementation("androidx.core:core-ktx:1.13.0-alpha05")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.concurrent:concurrent-futures-ktx:1.1.0")
    implementation("androidx.transition:transition-ktx:1.5.0-alpha06") // <-- for predictive back
    implementation("androidx.fragment:fragment-ktx:1.7.0-alpha10")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.appcompat:appcompat:1.7.0-alpha03")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0-alpha13")
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-exoplayer-midi:$media3Version")
    implementation("androidx.media3:media3-session:$media3Version")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("com.github.bumptech.glide:glide:$glideVersion")
    implementation("com.google.android.material:material:1.12.0-alpha03")
    implementation("me.zhanghai.android.fastscroll:library:1.3.0")
    implementation("com.mikepenz:aboutlibraries:$aboutLibsVersion")
    ksp("com.github.bumptech.glide:ksp:$glideVersion")
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.13")
    // Note: JAudioTagger is not compatible with Android 5, we can't ship it in app
    debugImplementation("net.jthink:jaudiotagger:3.0.1") // <-- for "SD Exploder"
    testImplementation("junit:junit:4.13.2")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs_nio:2.0.4")
}

fun String.runCommand(
    workingDir: File = File(".")
): String = providers.exec {
    setWorkingDir(workingDir)
    commandLine(split(' '))
}.standardOutput.asText.get().removeSuffixIfPresent("\n")

fun readProperties(propertiesFile: File) = Properties().apply {
    propertiesFile.inputStream().use { fis ->
        load(fis)
    }
}