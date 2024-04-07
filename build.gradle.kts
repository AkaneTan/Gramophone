// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.4.0-rc01" apply false
    val kotlinVersion = "2.0.0-Beta5"
    id("org.jetbrains.kotlin.android") version kotlinVersion apply false
    id("com.google.devtools.ksp") version "$kotlinVersion-1.0.20" apply false
    id("org.jetbrains.kotlin.plugin.parcelize") version kotlinVersion apply false
//    id("com.mikepenz.aboutlibraries.plugin") version "11.1.1" apply false
}

tasks.withType(JavaCompile::class.java) {
    options.compilerArgs.add("-Xlint:all")
}