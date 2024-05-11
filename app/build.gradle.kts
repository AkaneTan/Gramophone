@file:Suppress("UnstableApiUsage")

import com.android.build.gradle.tasks.PackageAndroidArtifact
import org.jetbrains.kotlin.util.removeSuffixIfPresent
import java.util.Properties

val aboutLibsVersion = "11.1.4" // keep in sync with plugin version

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

    defaultConfig {
        applicationId = "org.akanework.gramophone"
        // Reasons to not support KK include me.zhanghai.android.fastscroll, WindowInsets for
        // bottom sheet padding, ExoPlayer requiring multidex, vector drawables and poor SD support
        // That said, supporting Android 5.0 costs tolerable amounts of tech debt and we plan to
        // keep support for it for a while.
        minSdk = 21
        targetSdk = 34
        versionCode = 7
        versionName = "1.0.5"
        if (releaseType != "Release") {
            versionNameSuffix = myVersionName
        }
        if (project.hasProperty("AKANE_RELEASE_KEY_ALIAS")) {
            signingConfig = signingConfigs["release"]
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

        lint {
            lintConfig = file("lint.xml")
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
        create("profiling") {
            isMinifyEnabled = false
            isProfileable = true
        }
        create("userdebug") {
            isMinifyEnabled = false
            isProfileable = true
            isJniDebuggable = true
            isPseudoLocalesEnabled = true
        }
        debug {
            isPseudoLocalesEnabled = true
            if (project.hasProperty("AKANE_RELEASE_KEY_ALIAS")) {
                signingConfig = signingConfigs["release"]
            }
        }
    }

    sourceSets {
        getByName("debug") {
            // This does NOT remove src/debug/ source sets, hence "debug" is a superset of "userdebug"
            java.srcDir("src/userdebug/java")
            kotlin.srcDir("src/userdebug/kotlin")
            resources.srcDir("src/userdebug/resources")
            res.srcDir("src/userdebug/res")
            assets.srcDir("src/userdebug/assets")
            aidl.srcDir("src/userdebug/aidl")
            renderscript.srcDir("src/userdebug/renderscript")
            baselineProfiles.srcDir("src/userdebug/baselineProfiles")
            jniLibs.srcDir("src/userdebug/jniLibs")
            shaders.srcDir("src/userdebug/shaders")
            mlModels.srcDir("src/userdebug/mlModels")
        }
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
    val media3Version = "1.4.0-alpha01"
    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.7.0-beta01")
    implementation("androidx.collection:collection-ktx:1.4.0")
    implementation("androidx.concurrent:concurrent-futures-ktx:1.1.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0-alpha13")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    //implementation("androidx.datastore:datastore-preferences:1.1.0-rc01") TODO don't abuse shared prefs
    implementation("androidx.fragment:fragment-ktx:1.8.0-alpha02")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-exoplayer-midi:$media3Version")
    implementation("androidx.media3:media3-session:$media3Version")
    //implementation("androidx.navigation:navigation-fragment-ktx:2.7.7") TODO consider it
    //implementation("androidx.paging:paging-runtime-ktx:3.2.1") TODO paged, partial, flow based library loading
    //implementation("androidx.paging:paging-guava:3.2.1") TODO do we have guava? do we need this?
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.transition:transition-ktx:1.5.0") // <-- for predictive back
    implementation("com.mikepenz:aboutlibraries:$aboutLibsVersion")
    implementation("com.google.android.material:material:1.12.0")
    implementation("me.zhanghai.android.fastscroll:library:1.3.0")
    implementation("io.coil-kt.coil3:coil:3.0.0-alpha06")
    // --- below does not apply to release builds ---
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.14")
    // Note: JAudioTagger is not compatible with Android 5, we can't ship it in app
    debugImplementation("net.jthink:jaudiotagger:3.0.1") // <-- for "SD Exploder"
    testImplementation("junit:junit:4.13.2")
    "userdebugImplementation"(kotlin("reflect")) // who thought String.invoke() is a good idea?????
    debugImplementation(kotlin("reflect"))
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