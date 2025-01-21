@file:OptIn(ExperimentalPathApi::class)

import kotlin.io.path.ExperimentalPathApi
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.internal.os.OperatingSystem
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeHostTest

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("versions")
}

val currentOs: OperatingSystem = OperatingSystem.current()

kotlin {
    sourceSets.silenceOptIns()
    jvmToolchain(8)
    explicitApi()
    applyDefaultHierarchyTemplate()
}

fun NamedDomainObjectContainer<KotlinSourceSet>.silenceOptIns() =
    all {
        languageSettings {
            optIn("kotlinx.cinterop.ExperimentalForeignApi")
            optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
        }
    }

tasks {

    withType<Test> {
        useJUnitPlatform()
        systemProperty("jna.debug_load", "true")
        systemProperty("jna.debug_load.jna", "true")
        testLogging {
            exceptionFormat = TestExceptionFormat.FULL
            showStandardStreams = true
            showCauses = true
            showExceptions = true
            showStackTraces = true
        }
    }
    withType<KotlinNativeHostTest> {
        testLogging {
            exceptionFormat = TestExceptionFormat.FULL
            showStandardStreams = true
            showCauses = true
            showExceptions = true
            showStackTraces = true
        }
    }

    // in CI we only want to publish the artifacts for the current OS only
    // but when developing we want to publish all the possible artifacts to test them
    if (isCi) {

        val linuxNames = listOf("linux", "android", "jvm", "js", "kotlin", "metadata", "wasm")
        val windowsNames = listOf("mingw", "windows")
        val appleNames = listOf("macos", "ios", "watchos", "tvos")

        withType<AbstractPublishToMaven> {
            when {
                name.containsAny(linuxNames) -> onlyIf { currentOs.isLinux }
                name.containsAny(windowsNames) -> onlyIf { currentOs.isWindows }
                name.containsAny(appleNames) -> onlyIf { currentOs.isMacOsX }
            }
        }
    }
}

val isCi
    get() = System.getenv("CI") == "true"

fun String.containsAny(
    strings: List<String>,
    ignoreCase: Boolean = true,
): Boolean = strings.any { contains(it, ignoreCase) }
