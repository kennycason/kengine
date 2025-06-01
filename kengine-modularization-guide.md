# Kengine Modularization Guide

## Overview
This guide documents the process for extracting functionality from the main Kengine module into separate, specialized modules. It's based on successful migrations of the `hooks` package to `kengine-reactive` and the `network` package to `kengine-network`.

## Modularization Strategy
The strategy is to extract cohesive, self-contained functionality into dedicated modules to:
- Improve maintainability
- Allow independent versioning
- Reduce compile-time dependencies
- Enable selective inclusion of features

## Step-by-Step Modularization Process

### 1. Identify a Candidate Package
Look for packages that:
- Have well-defined boundaries
- Minimal dependencies on other parts of Kengine
- Provide a cohesive set of functionality

Examples: `hooks`, `network`, potentially `physics`, `sound`, etc.

### 2. Create Module Directory Structure
```
mkdir -p kengine-[module]/src/nativeMain/kotlin/com/kengine/[package]
mkdir -p kengine-[module]/src/nativeTest/kotlin/com/kengine/[package]
```

If the module requires native interop:
```
mkdir -p kengine-[module]/src/nativeInterop/cinterop
```

### 3. Copy/Move Native Interop Definition Files
If the module requires native libraries:
```
cp kengine/src/nativeInterop/cinterop/[relevant_def_files].def kengine-[module]/src/nativeInterop/cinterop/
```

### 4. Create build.gradle.kts for the New Module
Create a `build.gradle.kts` file in the new module directory with:
- Kotlin Multiplatform plugin
- Serialization plugin if needed
- Proper group and version
- Native target configuration
- C interop definitions if needed
- Dependencies on other modules

Example template:
```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinxSerialization) // If needed
}

group = "kengine.[module]"
version = "1.0.0"

repositories {
    mavenCentral()
}

kotlin {
    jvm()
    js(IR) {
        browser()
        nodejs()
    }

    val hostOs = System.getProperty("os.name")
    val isArm64 = System.getProperty("os.arch") == "aarch64"
    val nativeTarget = when {
        hostOs == "Mac OS X" && isArm64 -> macosArm64("native")
        hostOs == "Mac OS X" && !isArm64 -> macosX64("native")
        hostOs == "Linux" && isArm64 -> linuxArm64("native")
        hostOs == "Linux" && !isArm64 -> linuxX64("native")
        hostOs.startsWith("Windows") -> mingwX64("native")
        else -> throw GradleException("Host OS [$hostOs] is not supported in Kotlin/Native.")
    }

    // Add C interop if needed
    nativeTarget.apply {
        compilations["main"].cinterops {
            val libraryName by creating {
                defFile = file("src/nativeInterop/cinterop/library_name.def")
                compilerOpts("-I/usr/local/include")
            }
        }

        compilations["main"].compileTaskProvider.configure {
            kotlinOptions {
                freeCompilerArgs += listOf(
                    "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
                    "-opt-in=kotlin.ExperimentalStdlibApi"
                )
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinxSerializationJson)
                implementation(libs.kotlinxCoroutinesCore)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        nativeMain {
            dependsOn(commonMain)
            dependencies {
                implementation(project(":kengine-test"))
                // Add other module dependencies as needed
                api(project(":kengine")) // If this module needs kengine
            }
        }

        nativeTest {
            dependsOn(commonTest)
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
```

### 5. Move Source Files
Move all relevant source files from the main Kengine module to the new module:
```
mv kengine/src/nativeMain/kotlin/com/kengine/[package]/* kengine-[module]/src/nativeMain/kotlin/com/kengine/[package]/
```

### 6. Move Test Files
Move all relevant test files:
```
mv kengine/src/nativeTest/kotlin/com/kengine/[package]/* kengine-[module]/src/nativeTest/kotlin/com/kengine/[package]/
```

### 7. Update settings.gradle.kts
Add the new module to the project's settings.gradle.kts:
```kotlin
val modules = mutableListOf(
    "kengine",
    "kengine-test",
    "kengine-reactive",
    "kengine-network",
    "kengine-[module]" // Add the new module here
)
```

### 8. Add Dependency in Kengine's build.gradle.kts
Add a dependency to the new module in the main Kengine module:
```kotlin
nativeMain {
    dependsOn(commonMain)
    dependencies {
        // ...existing dependencies
        implementation(project(":kengine-[module]"))
    }
}
```

### 9. Build and Test
Run a build to verify everything works:
```
./gradlew build
```

Fix any compilation errors that arise, which typically involve:
- Circular dependencies
- Missing imports
- Visibility issues

## Common Pitfalls and Solutions

### Circular Dependencies
If module A depends on module B and vice versa, you have a circular dependency.

**Solution:**
- Extract the shared code to a third module that both depend on
- Or refactor to remove the circular dependency

### Missing Imports
After moving code, you might encounter missing import errors.

**Solution:**
- Add the necessary dependencies in the build.gradle.kts file
- Make sure the dependency direction is correct (which module should depend on which)

### Native Interop Issues
If your module uses native libraries, you might encounter linking issues.

**Solution:**
- Ensure the .def files are correctly copied and configured
- Add the necessary linker options in the build.gradle.kts file

## Examples from Previous Migrations

### kengine-reactive Migration
1. Identified the `hooks` package as a candidate for extraction
2. Created directory structure for kengine-reactive
3. Moved all files from `kengine/src/nativeMain/kotlin/com/kengine/hooks` to `kengine-reactive/src/nativeMain/kotlin/com/kengine/hooks`
4. Moved all test files
5. Created build.gradle.kts for kengine-reactive
6. Updated settings.gradle.kts to include kengine-reactive
7. Added dependency to kengine-reactive in kengine's build.gradle.kts

### kengine-network Migration
1. Identified the `network` package as a candidate for extraction
2. Created directory structure for kengine-network
3. Copied SDL3_net.def to kengine-network's nativeInterop directory
4. Moved all files from `kengine/src/nativeMain/kotlin/com/kengine/network` to `kengine-network/src/nativeMain/kotlin/com/kengine/network`
5. Moved all test files
6. Created build.gradle.kts for kengine-network with SDL3_net interop
7. Updated settings.gradle.kts to include kengine-network
8. Added dependency to kengine-network in kengine's build.gradle.kts

## Conclusion
Following this guide should help streamline the process of extracting functionality from the main Kengine module into separate, specialized modules. This modularization approach improves maintainability, allows for independent versioning, and enables selective inclusion of features.
