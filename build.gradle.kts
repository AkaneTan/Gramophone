// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    val agpVersion = "8.6.0-rc01"
    id("com.android.application") version agpVersion apply false
	id("com.android.library") version agpVersion apply false
	id("com.android.test") version agpVersion apply false
	id("androidx.baselineprofile") version "1.2.4" apply false
    val kotlinVersion = "2.0.20-RC2"
    id("org.jetbrains.kotlin.android") version kotlinVersion apply false
    id("org.jetbrains.kotlin.plugin.parcelize") version kotlinVersion apply false
    id("com.mikepenz.aboutlibraries.plugin") version "11.2.1" apply false
}

tasks.withType(JavaCompile::class.java) {
    options.compilerArgs.add("-Xlint:all")
}