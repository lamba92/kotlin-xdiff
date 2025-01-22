# [libgit2/xdiff](https://github.com/libgit2/xdiff) for Kotlin Multiplatform

Native bindings of [libgit2/xdiff](https://github.com/libgit2/xdiff) for:
- Kotlin/Native:
  - iOS (arm64, simulatorArm64, simulatorX64)
  - tvOS (arm64, simulatorArm64, simulatorX64)
  - watchOS (arm64, simulatorArm64, simulatorX64)
  - Linux (arm64, x64)
  - macOs Linux (arm64, x64)
  - Android (arm64, x64, x86, arm32)
- JVM (via JNA):
  - Android (arm64, x64, x86, arm32)
  - Linux (arm64, x64, arm32hfp)
  - macOs (arm64, x64)

The results are kotlinx.serializable!

# Usage

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.github.lamba92:kotlin-xdiff:{lavest-version}") // see GitHub Releases section
}
```

In your app:
```kotlin

val source: String = "..." 
val target: String = "..."

val textDiff: TextDiff = TextDiff.compute(source, target)
```
