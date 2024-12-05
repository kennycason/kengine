plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

group = "kengine.playdate"
version = "1.0.0"

repositories {
    mavenCentral()
}

kotlin {
    linuxArm32Hfp("playdate") {
        compilations["main"].cinterops {
            val playdate by creating {
                definitionFile = file("src/nativeInterop/cinterop/playdate.def")
                compilerOpts(
                    "-I${System.getenv("PLAYDATE_SDK_PATH")}/C_API",
                    "-I${project.file("src/nativeInterop/cinterop").absolutePath}",
                    "-DTARGET_PLAYDATE=1",
                    "-DTARGET_EXTENSION=1",
                    "-march=armv7e-m",
                    "-mcpu=cortex-m7",
                    "-mthumb",
                    "-mfpu=fpv5-sp-d16",
                    "-mfloat-abi=hard",
                    "-nostartfiles",
                    "-nostdlib++",
                    "-fno-exceptions",
                    "-fno-rtti",
                    "-specs=nosys.specs"
                )
            }
        }
        binaries {
            staticLib("kenginePlaydate") {
                baseName = "kengine_playdate"
                outputDirectory = file("${layout.buildDirectory.get()}/bin/playdate")
                linkerOpts(
                    "-T${System.getenv("PLAYDATE_SDK_PATH")}/C_API/buildsupport/link_map.ld",
                    "-march=armv7e-m",
                    "-mcpu=cortex-m7",
                    "-mthumb",
                    "-mfpu=fpv5-sp-d16",
                    "-mfloat-abi=hard",
                    "-nostartfiles",
                    "-nostdlib++",
                    "-fno-exceptions",
                    "-fno-rtti",
                    "-specs=nosys.specs",
                    "-target arm-none-eabi"
                )
            }
        }
    }
}

tasks.register("checkBridgeFile") {
    doLast {
        val bridgeFile = project.file("src/nativeInterop/cinterop/playdate_bridge.h")
        println("Bridge file exists: ${bridgeFile.exists()}")
        println("Bridge file path: ${bridgeFile.absolutePath}")
    }
}