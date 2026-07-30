#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
DEFAULT_CHECKOUT_DIR="$(cd "$REPO_ROOT/.." && pwd)/kengine-kotlin-nintendo-switch"
CHECKOUT_DIR="${KENGINE_KOTLIN_REPO:-}"
TASK=":kotlin-native:distCompiler"
GRADLE_ARGS=("--no-configuration-cache" "--dependency-verification=off" "-Pkotlin.native.enabled=true")
UPDATE_LOCAL_PROPERTIES=1
REFRESH_SWITCH_RUNTIME=1
CUSTOM_TASK=0

log() {
    printf '%s\n' "$*"
}

fail() {
    printf 'error: %s\n' "$*" >&2
    exit 1
}

usage() {
    cat <<'EOF'
Usage: build-kotlin-native-dist.sh [options]

Builds a Kotlin/Native compiler distribution from the local Kotlin source fork.

Options:
  --checkout-dir PATH      Kotlin source checkout. Defaults to local.properties or ../kengine-kotlin-nintendo-switch.
  --task TASK              Gradle task to run. Defaults to :kotlin-native:distCompiler.
  --refresh-switch-runtime Refresh switch_arm64 runtime bitcode into the local Kotlin/Native dist. Enabled by default.
  --no-switch-runtime      Skip the switch_arm64 runtime refresh.
  --no-local-properties    Do not update kengine-kotlin/local.properties after a successful build.
  --help                   Show this help.
EOF
}

property_value() {
    local key="$1"
    local file="$SCRIPT_DIR/local.properties"

    [ -f "$file" ] || return 0
    awk -F '=' -v key="$key" '$1 == key { print substr($0, length(key) + 2); exit }' "$file"
}

fork_property_value() {
    local key="$1"
    local file="$CHECKOUT_DIR/gradle.properties"

    [ -f "$file" ] || return 0
    awk -F '=' -v key="$key" '$1 == key { print substr($0, length(key) + 2); exit }' "$file"
}

is_native_utils_jar_task() {
    [ "$TASK" = ":native:kotlin-native-utils:jar" ] || [ "$TASK" = "native:kotlin-native-utils:jar" ]
}

is_dist_output_task() {
    [ "$TASK" = ":kotlin-native:dist" ] ||
        [ "$TASK" = "kotlin-native:dist" ] ||
        [ "$TASK" = ":kotlin-native:distCompiler" ] ||
        [ "$TASK" = "kotlin-native:distCompiler" ]
}

prepare_bootstrap_native_utils_override() {
    local snapshot_version
    local bootstrap_version
    local source_jar
    local artifact_dir
    local pom_file

    snapshot_version="$(fork_property_value defaultSnapshotVersion)"
    bootstrap_version="$(fork_property_value bootstrap.kotlin.default.version)"
    [ -n "$snapshot_version" ] || fail "Could not read defaultSnapshotVersion from $CHECKOUT_DIR/gradle.properties"
    [ -n "$bootstrap_version" ] || fail "Could not read bootstrap.kotlin.default.version from $CHECKOUT_DIR/gradle.properties"

    source_jar="$CHECKOUT_DIR/native/utils/build/libs/kotlin-native-utils-$snapshot_version.jar"
    [ -f "$source_jar" ] || fail "Kotlin/Native utils jar was not built: $source_jar"

    artifact_dir="$CHECKOUT_DIR/build/kengine-bootstrap-overrides/repo/org/jetbrains/kotlin/kotlin-native-utils/$bootstrap_version"
    pom_file="$artifact_dir/kotlin-native-utils-$bootstrap_version.pom"

    mkdir -p "$artifact_dir"
    cp "$source_jar" "$artifact_dir/kotlin-native-utils-$bootstrap_version.jar"

    {
        printf '%s\n' '<?xml version="1.0" encoding="UTF-8"?>'
        printf '%s\n' '<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">'
        printf '%s\n' '  <modelVersion>4.0.0</modelVersion>'
        printf '%s\n' '  <groupId>org.jetbrains.kotlin</groupId>'
        printf '%s\n' '  <artifactId>kotlin-native-utils</artifactId>'
        printf '  <version>%s</version>\n' "$bootstrap_version"
        printf '%s\n' '  <name>Kotlin Native Utils</name>'
        printf '%s\n' '  <description>Kotlin/Native utils</description>'
        printf '%s\n' '  <dependencyManagement>'
        printf '%s\n' '    <dependencies>'
        printf '%s\n' '      <dependency>'
        printf '%s\n' '        <groupId>org.jetbrains.kotlin</groupId>'
        printf '%s\n' '        <artifactId>kotlin-gradle-plugins-bom</artifactId>'
        printf '        <version>%s</version>\n' "$bootstrap_version"
        printf '%s\n' '        <type>pom</type>'
        printf '%s\n' '        <scope>import</scope>'
        printf '%s\n' '      </dependency>'
        printf '%s\n' '    </dependencies>'
        printf '%s\n' '  </dependencyManagement>'
        printf '%s\n' '  <dependencies>'
        printf '%s\n' '    <dependency>'
        printf '%s\n' '      <groupId>org.jetbrains.kotlin</groupId>'
        printf '%s\n' '      <artifactId>kotlin-util-io</artifactId>'
        printf '      <version>%s</version>\n' "$bootstrap_version"
        printf '%s\n' '      <scope>compile</scope>'
        printf '%s\n' '    </dependency>'
        printf '%s\n' '    <dependency>'
        printf '%s\n' '      <groupId>org.jetbrains.kotlin</groupId>'
        printf '%s\n' '      <artifactId>kotlin-util-klib</artifactId>'
        printf '      <version>%s</version>\n' "$bootstrap_version"
        printf '%s\n' '      <scope>compile</scope>'
        printf '%s\n' '    </dependency>'
        printf '%s\n' '  </dependencies>'
        printf '%s\n' '</project>'
    } > "$pom_file"

    log "Prepared bootstrap kotlin-native-utils override: $artifact_dir"
}

resolve_java_home() {
    local candidates=()
    local candidate

    [ -n "${JAVA_HOME:-}" ] && candidates+=("$JAVA_HOME")

    if command -v /usr/libexec/java_home >/dev/null 2>&1; then
        candidate="$(/usr/libexec/java_home -v 17 2>/dev/null || true)"
        [ -n "$candidate" ] && candidates+=("$candidate")
    fi

    if command -v jenv >/dev/null 2>&1; then
        candidate="$(jenv prefix 17.0.18 2>/dev/null || true)"
        [ -n "$candidate" ] && candidates+=("$candidate")
        candidate="$(jenv prefix 17.0 2>/dev/null || true)"
        [ -n "$candidate" ] && candidates+=("$candidate")
    fi

    for candidate in "${candidates[@]}"; do
        if [ -x "$candidate/bin/java" ]; then
            printf '%s\n' "$candidate"
            return
        fi
    done

    fail "Could not find a valid JDK 17. Set JAVA_HOME to a JDK 17 installation."
}

while [ "$#" -gt 0 ]; do
    case "$1" in
        --checkout-dir)
            [ "$#" -ge 2 ] || fail "--checkout-dir requires a path."
            CHECKOUT_DIR="$2"
            shift
            ;;
        --task)
            [ "$#" -ge 2 ] || fail "--task requires a Gradle task."
            TASK="$2"
            REFRESH_SWITCH_RUNTIME=0
            CUSTOM_TASK=1
            shift
            ;;
        --refresh-switch-runtime)
            REFRESH_SWITCH_RUNTIME=1
            ;;
        --no-switch-runtime)
            REFRESH_SWITCH_RUNTIME=0
            ;;
        --no-local-properties)
            UPDATE_LOCAL_PROPERTIES=0
            ;;
        --help|-h)
            usage
            exit 0
            ;;
        *)
            fail "Unknown option: $1"
            ;;
    esac
    shift
done

if [ -z "$CHECKOUT_DIR" ]; then
    CHECKOUT_DIR="$(property_value kengine.kotlin.repo)"
fi

if [ -z "$CHECKOUT_DIR" ]; then
    CHECKOUT_DIR="$DEFAULT_CHECKOUT_DIR"
fi

[ -d "$CHECKOUT_DIR/.git" ] || fail "Kotlin checkout not found: $CHECKOUT_DIR"
[ -x "$CHECKOUT_DIR/gradlew" ] || fail "Gradle wrapper not found in Kotlin checkout: $CHECKOUT_DIR/gradlew"

log "Building Kotlin/Native distribution:"
log "Checkout: $CHECKOUT_DIR"
log "Task: $TASK"
if [ "$REFRESH_SWITCH_RUNTIME" -eq 1 ]; then
    log "Switch runtime refresh: enabled"
else
    log "Switch runtime refresh: disabled"
fi
log "Gradle args: ${GRADLE_ARGS[*]}"

JAVA_HOME="$(resolve_java_home)"
export JAVA_HOME
export JDK_17_0="${JDK_17_0:-$JAVA_HOME}"

log "JAVA_HOME: $JAVA_HOME"

cd "$CHECKOUT_DIR"

if ! is_native_utils_jar_task; then
    log "Preparing local Kotlin/Native utils jar for native-build-tools."
    ./gradlew "${GRADLE_ARGS[@]}" :native:kotlin-native-utils:jar
    prepare_bootstrap_native_utils_override
fi

./gradlew "${GRADLE_ARGS[@]}" "$TASK"

if [ "$REFRESH_SWITCH_RUNTIME" -eq 1 ] && ! is_native_utils_jar_task; then
    ./gradlew "${GRADLE_ARGS[@]}" :kotlin-native:switch_arm64CrossDistRuntime
fi

if is_native_utils_jar_task; then
    prepare_bootstrap_native_utils_override
fi

if [ "$CUSTOM_TASK" -eq 1 ] && [ "$REFRESH_SWITCH_RUNTIME" -eq 0 ] && ! is_dist_output_task; then
    log "Skipped Kotlin/Native dist output check for custom task: $TASK"
    exit 0
fi

NATIVE_HOME="$CHECKOUT_DIR/kotlin-native/dist"
KOTLINC_NATIVE="$NATIVE_HOME/bin/kotlinc-native"

[ -x "$KOTLINC_NATIVE" ] || fail "Build completed, but kotlinc-native was not found at $KOTLINC_NATIVE"

if [ "$UPDATE_LOCAL_PROPERTIES" -eq 1 ]; then
    BRANCH="$(git -C "$CHECKOUT_DIR" branch --show-current || true)"
    VERSION="$(awk -F '"' '/^kotlin[[:space:]]*=/ { print $2; exit }' "$REPO_ROOT/gradle/libs.versions.toml")"
    SWITCH_TARGET_AVAILABLE=0

    if "$KOTLINC_NATIVE" -list-targets 2>/dev/null | awk '{ print $1 }' | grep -qx 'switch_arm64'; then
        SWITCH_TARGET_AVAILABLE=1
    fi

    {
        printf '# Generated by build-kotlin-native-dist.sh. Machine-local; ignored by Git.\n'
        printf 'kengine.kotlin.repo=%s\n' "$CHECKOUT_DIR"
        printf 'kengine.kotlin.version=%s\n' "$VERSION"
        printf 'kengine.kotlin.branch=%s\n' "$BRANCH"
        printf 'kengine.switch.kotlinNativeHome=%s\n' "$NATIVE_HOME"
        if [ "$SWITCH_TARGET_AVAILABLE" -eq 1 ]; then
            printf 'kengine.switch.kotlinTarget=switch_arm64\n'
        else
            printf '# Use this after the compiler fork knows about the target:\n'
            printf '# kengine.switch.kotlinTarget=switch_arm64\n'
        fi
    } > "$SCRIPT_DIR/local.properties"

    log "Updated $SCRIPT_DIR/local.properties"
    if [ "$SWITCH_TARGET_AVAILABLE" -eq 1 ]; then
        log "Switch Kotlin/Native target available: switch_arm64"
    fi
fi

log "Kotlin/Native compiler ready: $KOTLINC_NATIVE"
