@file:Suppress("UnstableApiUsage")

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
    id("com.gradle.develocity") version "3.19"
}

rootProject.name = "kotlin-xdiff"

develocity {
    buildScan {
        termsOfUseUrl = "https://gradle.com/terms-of-service"
        termsOfUseAgree = "yes"
        publishing {
            onlyIf { System.getenv("CI") == "true" }
        }
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    rulesMode = RulesMode.PREFER_SETTINGS
}
