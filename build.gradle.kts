// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    val agpVersion = "8.7.0-beta02"
    id("com.android.application") version agpVersion apply false
	id("com.android.library") version agpVersion apply false
	id("com.android.test") version agpVersion apply false
	id("androidx.baselineprofile") version "1.3.2" apply false
    val kotlinVersion = "2.0.21-RC"
	kotlin("android") version kotlinVersion apply false
    kotlin("plugin.parcelize") version kotlinVersion apply false
    id("com.mikepenz.aboutlibraries.plugin") version "11.2.1" apply false
}

tasks.withType(JavaCompile::class.java) {
    options.compilerArgs.add("-Xlint:all")
}