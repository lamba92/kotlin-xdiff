@file:Suppress("OPT_IN_USAGE", "DEPRECATION")

plugins {
    `kotlin-multiplatform-with-android-convention`
    id(libs.plugins.ktlint)
    `publishing-convention`
}

group = "com.github.lamba92"
version = "1.0-SNAPSHOT"

kotlin {

    jvmToolchain(8)
    jvm()

    wasmJs {
        browser()
        nodejs()
    }
    js {
        browser()
        nodejs()
    }

    androidTarget()

    androidNativeX64()
    androidNativeArm64()
    androidNativeArm32()
    androidNativeX86()

    macosX64()
    macosArm64()

    linuxX64()
    linuxArm64()
    linuxArm32Hfp()

    iosArm64()
    iosX64()
    iosSimulatorArm64()

    tvosArm64()
    tvosX64()
    tvosSimulatorArm64()

    watchosArm64()
    watchosX64()
    watchosSimulatorArm64()

    sourceSets {

        commonMain {
            dependencies {
                api(libs.kotlinx.serialization.core)
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.serialization.json)
            }
        }

        androidInstrumentedTest {
            dependencies {
                implementation(libs.androidx.test.runner)
                implementation(libs.androidx.test.core)
                implementation(libs.android.test.junit)
                implementation(kotlin("test-junit"))
            }
        }
    }
}

tasks {
    clean {
        delete(".kotlin")
    }
}
