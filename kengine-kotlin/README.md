# Kengine Kotlin Fork

Local tooling for experimenting with a kengine-owned Kotlin/Native compiler fork.

The Kotlin source checkout is intentionally not vendored into this repository. Kotlin is large, has its own Gradle lifecycle, and needs to be rebased independently. The default checkout location is a sibling directory:

```text
/Users/kenny/code/kengine-kotlin-fork
```

## Bootstrap the Fork

Clone JetBrains/kotlin at the Kotlin version pinned by kengine and create a local Switch branch:

```bash
./kengine-kotlin/setup-kotlin-fork.sh
```

Unattended:

```bash
./kengine-kotlin/setup-kotlin-fork.sh --yes
```

With a personal fork remote:

```bash
./kengine-kotlin/setup-kotlin-fork.sh --fork-url git@github.com:<you>/kotlin.git
```

The script writes `kengine-kotlin/local.properties`, which is ignored by Git.

## Build Kotlin/Native

Build the local Kotlin/Native distribution from the fork:

```bash
./kengine-kotlin/build-kotlin-native-dist.sh
```

This runs the upstream Kotlin task:

```bash
./gradlew -Pkotlin.native.enabled=true :kotlin-native:dist
```

Kotlin's root `gradle.properties` disables Native by default, so the `-Pkotlin.native.enabled=true` flag is required; otherwise Gradle will report that project `:kotlin-native` does not exist.
The helper also builds `:native:kotlin-native-utils:jar` first and publishes a tiny fork-local bootstrap override under `build/kengine-bootstrap-overrides/repo`. That lets Kotlin's included `native-build-tools` build use the forked target registry while generating runtime and stdlib artifacts. Because that local jar intentionally has the upstream bootstrap coordinate with forked contents, the helper disables Gradle dependency verification for these fork-local builds.

After a successful build, `local.properties` points `kengine.kotlin.nativeHome` at the fork's generated Kotlin/Native distribution. If the forked compiler lists `switch_arm64`, the script also writes:

```properties
kengine.switch.kotlinTarget=switch_arm64
```

## Use It from kengine-switch

Inspect the configured compiler:

```bash
jenv exec ./gradlew -Pkengine.switch=true :kengine-switch:switchToolchainInfo
```

Compile the current Kotlin static probe with the configured local compiler:

```bash
jenv exec ./gradlew -Pkengine.switch=true :kengine-switch:compileSwitchKotlinStatic
```

Until the compiler fork has a real Switch target, the Switch module keeps using `linux_arm64` as the staging target. Once `switch_arm64` exists in the fork, `build-kotlin-native-dist.sh` configures that target in `kengine-kotlin/local.properties`.

This intentionally swaps only the `kotlinc-native` executable used by `kengine-switch`. Swapping the whole repository to a locally published Kotlin Gradle plugin is a separate step we should do later, after the forked compiler can compile the Switch probe.

## Gradle Helper Tasks

The support module is included in the main Gradle build:

```bash
jenv exec ./gradlew :kengine-kotlin:kotlinForkInfo
jenv exec ./gradlew :kengine-kotlin:setupKotlinFork
jenv exec ./gradlew :kengine-kotlin:buildKotlinNativeDist
jenv exec ./gradlew -Pkengine.switch=true :kengine-switch:switchToolchainInfo
jenv exec ./gradlew -Pkengine.switch=true :kengine-switch:compileSwitchKotlinStatic
```

## Why Not a Submodule?

A Git submodule would make every kengine checkout aware of a very large compiler repository and would still need local branches/remotes for rebasing. Keeping Kotlin as a sibling checkout gives us the same reproducibility with less repository friction.
