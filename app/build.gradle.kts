plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}


android {
    namespace = "org.akanework.gramophone"
    compileSdk = 33

    defaultConfig {
        applicationId = "org.akanework.gramophone"
        minSdk = 31
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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

    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.activity:activity-ktx:1.7.2")
    implementation("androidx.fragment:fragment-ktx:1.6.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.media3:media3-exoplayer:1.1.0")
    implementation("androidx.media3:media3-exoplayer-dash:1.1.0")
    implementation("androidx.media3:media3-exoplayer-hls:1.1.0")
    implementation("androidx.media3:media3-ui:1.1.0")
    implementation("androidx.media3:media3-session:1.1.0")
    implementation("com.github.bumptech.glide:glide:4.15.1")
    implementation("me.zhanghai.android.fastscroll:library:1.2.0")
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.12")
    ksp("com.github.bumptech.glide:ksp:4.15.1")

}