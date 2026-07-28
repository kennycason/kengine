# Kengine Switch Prototype

Experimental Nintendo Switch homebrew build harness for proving whether Kotlin/Native can participate in a libnx application.

This module is opt-in and is not part of the normal repo build:

```bash
jenv exec ./gradlew -Pkengine.switch=true :kengine-switch:switchToolchainInfo
```

## Toolchain

The macOS setup script installs and verifies the public homebrew toolchain:

```bash
./kengine-switch/setup-switch-build-macos.sh
```

For unattended setup:

```bash
./kengine-switch/setup-switch-build-macos.sh --yes
```

Install the public homebrew toolchain first:

```bash
export DEVKITPRO=/opt/devkitpro
export DEVKITA64=$DEVKITPRO/devkitA64
export PATH=$DEVKITA64/bin:$DEVKITPRO/tools/bin:$PATH
```

Required tools:

```text
aarch64-none-elf-gcc
nacptool
elf2nro
```

## Milestones

Validate the libnx shell without Kotlin:

```bash
jenv exec ./gradlew -Pkengine.switch=true :kengine-switch:buildSwitchCOnlyNro
```

Compile the Kotlin/Native static-library probe:

```bash
jenv exec ./gradlew -Pkengine.switch=true :kengine-switch:compileSwitchKotlinStatic
```

Use a local Kotlin/Native compiler fork:

```bash
./kengine-kotlin/setup-kotlin-fork.sh
./kengine-kotlin/build-kotlin-native-dist.sh
jenv exec ./gradlew -Pkengine.switch=true :kengine-switch:switchToolchainInfo
```

Attempt the Kotlin-linked NRO:

```bash
jenv exec ./gradlew -Pkengine.switch=true :kengine-switch:buildSwitchNro
```

## Current Shape

The Kotlin probe currently uses `linux_arm64` as a staging target because Kotlin/Native does not provide a first-class Switch target. This is only a linkability experiment; `linux_arm64` is not the final Switch ABI.

The intended first successful artifact is:

```text
libnx C main()
  -> calls generated Kotlin/Native static-library API
  -> prints "Kengine Switch" and a Kotlin-generated value
  -> packages as .nro
```
