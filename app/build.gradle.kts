plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "org.akanework.serendipity"
    compileSdk = 33

    defaultConfig {
        applicationId = "org.akanework.serendipity"
        minSdk = 33
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
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
}

dependencies {

    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.media3:media3-exoplayer:1.1.0")
    implementation("androidx.media3:media3-exoplayer-dash:1.1.0")
    implementation("androidx.media3:media3-exoplayer-hls:1.1.0")
    implementation("androidx.media3:media3-ui:1.1.0")
    implementation("androidx.media3:media3-session:1.1.0")

}