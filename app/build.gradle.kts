@file:Suppress("UnstableApiUsage")

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {

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

    namespace = "org.akanework.gramophone"
    compileSdk = 34
    android.buildFeatures.buildConfig = true

    androidResources {
        generateLocaleConfig = true
    }

    defaultConfig {
        applicationId = "org.akanework.gramophone"
        minSdk = 21
        targetSdk = 34
        versionCode = 3
        versionName = "1.0.2" + "." + "git rev-parse --short=6 HEAD".runCommand(workingDir = rootDir)
        setProperty("archivesBaseName", "Gramophone-$versionName")
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
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.activity:activity-ktx:1.8.0")
    implementation("androidx.transition:transition-ktx:1.5.0-alpha04")
    implementation("androidx.fragment:fragment-ktx:1.7.0-alpha06")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0-beta01")
    implementation("androidx.appcompat:appcompat:1.7.0-alpha03")
    implementation("com.google.android.material:material:1.11.0-beta01")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0-alpha13")
    implementation("androidx.media3:media3-exoplayer:1.2.0-beta01")
    implementation("androidx.media3:media3-session:1.2.0-beta01")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("com.github.bumptech.glide:glide:4.15.1")
    implementation("me.zhanghai.android.fastscroll:library:1.2.0")
    // debugImplementation("com.squareup.leakcanary:leakcanary-android:2.12")
    ksp("com.github.bumptech.glide:ksp:4.15.1")
}