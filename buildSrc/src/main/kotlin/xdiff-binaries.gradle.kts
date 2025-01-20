import com.android.build.api.dsl.LibraryExtension
import com.android.build.gradle.tasks.MergeSourceSetFolders
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

val xdiffVersion = "20250120T152756Z"

val downloadXdiffBinaries by tasks.registering(DownloadTask::class) {
    val xdiffVersion = xdiffVersion
    link = getXdiffBuildLink(xdiffVersion)
}

val extractXdiffBinariesForKotlinNative by registerExtractXdiffTask(
    downloadXdiffBinaries = downloadXdiffBinaries,
    strategies = kotlinNativeRenamings,
    destinationDir = layout.buildDirectory.dir("binaries/xdiff/kotlinNative"),
)

val jvmLibs = layout.buildDirectory.dir("binaries/xdiff/jvm")
val extractXdiffBinariesForJvm by registerExtractXdiffTask(
    downloadXdiffBinaries = downloadXdiffBinaries,
    strategies = jvmRenamings,
    destinationDir = jvmLibs,
)

val androidLibs = layout.buildDirectory.dir("binaries/xdiff/android")
val extractXdiffBinariesForAndroidJvm by registerExtractXdiffTask(
    downloadXdiffBinaries = downloadXdiffBinaries,
    strategies = androidJvmRenamings,
    destinationDir = androidLibs,
)

val extractHeaders by tasks.registering(Sync::class) {
    dependsOn(downloadXdiffBinaries)
    from(zipTree(downloadXdiffBinaries.map { it.downloadFile })) {
        include("**/*.h")
        eachFile { path = path.removePrefix("headers/include") }
    }
    includeEmptyDirs = false
    into(layout.buildDirectory.dir("headers"))
}

plugins.withId("com.android.library") {
    the<LibraryExtension>().apply {
        sourceSets {
            named("main") {
                jniLibs.srcDirs(androidLibs)
            }
        }
    }

    tasks.withType<MergeSourceSetFolders> {
        dependsOn(extractXdiffBinariesForAndroidJvm)
    }
}

plugins.withId("org.jetbrains.kotlin.multiplatform") {
    the<KotlinMultiplatformExtension>().apply {
        sourceSets {
            jvmMain {
                resources.srcDir(jvmLibs)
            }
        }
    }
    tasks.withType<ProcessResources> {
        dependsOn(extractXdiffBinariesForJvm)
    }
}
