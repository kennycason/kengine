# Nintendo Switch

Collection of notes for Nintendo Switch build.

Currently `libnx` only has support for SDL2. https://github.com/switchbrew/libnx

## Environment Setup

### Install Required Tools
- Compiler & SDK:
- Install devkitPro and devkitA64 (Switch toolchain).
- Download Link: https://devkitpro.org/
  - https://devkitpro.org/wiki/Getting_Started#macOS
  - To get the Xcode command line tools run `xcode-select --install` from Terminal.
  - Use the pkg installer to install devkitPro pacman.
  - Reboot your mac to get environment set.
  - Use dkp-pacman to install your chosen tools as per Unix like platforms below.
- Install `libnx` for homebrew development.
- CMake (if needed) for building dependencies.
- Install SDL3 libraries for Switch.
- Prebuilt versions are available for libnx.

### Install Emulator
- Yuzu (https://yuzu-emu.org/) or Ryujinx (https://ryujinx.org/).
- Dump keys and firmware from a Switch console (needed for running homebrew).

## Add Switch Target to Build Script

Modify Gradle Script

Add the Nintendo Switch ARM64 target to build script:

```kotlin
val switchTarget = when {
    hostOs == "Linux" && isArm64 -> linuxArm64("switch")
    else -> throw GradleException("Switch target requires Linux ARM64")
}

switchTarget.apply {
    binaries {
        executable {
            baseName = "kengine"
            linkerOpts(
                "-lnx", // link with libnx
                "other options"
            )
        }
    }
}
```

## Add SDL3 Support for Switch

Done

## Test in Emulator

Yuzu Setup
- Copy the .nro file into /switch directory in the emulator.
- Launch the emulator, load the file, and check logs for errors.

Ryujinx Setup
- Place the .nro in switch folder.
- Test homebrew support with Logs Enabled to catch graphical issues.
