import org.gradle.internal.extensions.stdlib.capitalized
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    `kotlin-multiplatform-with-android-convention`
    `xdiff-binaries`
    id(libs.plugins.ktlint)
}

group = "io.github.lamba92"
version = "1.0-SNAPSHOT"

kotlin {
    jvm()

    androidTarget()

    androidNativeX64 {
        registerXdiffCinterop("android-x64")
    }

    androidNativeArm64 {
        registerXdiffCinterop("android-arm64")
    }

    androidNativeArm32 {
        registerXdiffCinterop("android-arm32")
    }
    androidNativeX86 {
        registerXdiffCinterop("android-x86")
    }

    macosX64 {
        registerXdiffCinterop("macos-x64")
    }
    macosArm64 {
        registerXdiffCinterop("macos-arm64")
    }

    linuxX64 {
        registerXdiffCinterop("linux-x64")
    }
    linuxArm64 {
        registerXdiffCinterop("linux-arm64")
    }

    iosArm64 {
        registerXdiffCinterop("ios-arm64")
    }
    iosX64 {
        registerXdiffCinterop("ios-simulator-x64")
    }
    iosSimulatorArm64 {
        registerXdiffCinterop("ios-simulator-arm64")
    }

    tvosArm64 {
        registerXdiffCinterop("tvos-arm64")
    }
    tvosX64 {
        registerXdiffCinterop("tvos-simulator-x64")
    }
    tvosSimulatorArm64 {
        registerXdiffCinterop("tvos-simulator-arm64")
    }

    watchosArm64 {
        registerXdiffCinterop("watchos-arm64")
    }
    watchosX64 {
        registerXdiffCinterop("watchos-simulator-x64")
    }
    watchosSimulatorArm64 {
        registerXdiffCinterop("watchos-simulator-arm64")
    }

    sourceSets {

        commonMain {
            dependencies {
                api(libs.kotlinx.serialization.core)
                api(libs.kotlinx.coroutines.core)
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.serialization.json)
            }
        }

        val jvmCommonMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                compileOnly(libs.jna)
            }
        }

        val jvmCommonTest by creating {
            dependsOn(commonTest.get())
        }

        androidMain {
            dependsOn(jvmCommonMain)
            dependencies {
                //noinspection UseTomlInstead
                api("net.java.dev.jna:jna:5.16.0@aar")
            }
        }

        jvmMain {
            dependsOn(jvmCommonMain)
            dependencies {
                api(libs.jna)
            }
        }

        jvmTest {
            dependsOn(jvmCommonTest)
        }

        androidUnitTest {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }

        androidInstrumentedTest {
            dependsOn(jvmCommonTest)
        }
    }
}

fun KotlinNativeTarget.registerXdiffCinterop(
    platformName: String,
    packageName: String = "xdiff",
    generateDefTaskName: String = "generate${platformName.toCamelCase().capitalized()}XdiffDefFile",
    defFileName: String = "${platformName.toCamelCase()}.def",
) {
    val generateDefTask =
        tasks.register<CreateDefFileTask>(generateDefTaskName) {
            dependsOn(tasks.extractXdiffBinariesForKotlinNative, tasks.extractHeaders)
            // the order of the headers matters! put xtypes.h first
            // because it's the one that is used by other headers
            headers = listOf(
                "xtypes.h",
                "git-xdiff.h",
                "xdiff.h",
                "xdiffi.h",
                "xemit.h",
                "xinclude.h",
                "xmacros.h",
                "xprepare.h",
                "xutils.h"
            )
            staticLibs.add("libxdiff.a")
            defFile = layout.buildDirectory.file("generated/cinterop/$defFileName")
            compilerOpts.add(
                tasks
                    .extractHeaders
                    .map { "-I${it.destinationDir.absolutePath}" },
            )
            libraryPaths.add(
                tasks
                    .extractXdiffBinariesForKotlinNative
                    .map { it.destinationDir.resolve(platformName).absolutePath },
            )
        }

    val compilation = compilations.getByName("main")

    compilation.compileTaskProvider {
        dependsOn(generateDefTask)
    }

    compilation.cinterops.register("libleveldb") {
        tasks.all {
            if (name == interopProcessingTaskName) {
                dependsOn(generateDefTask)
            }
        }
        this.packageName = packageName
        definitionFile = generateDefTask.flatMap { it.defFile }
    }
}