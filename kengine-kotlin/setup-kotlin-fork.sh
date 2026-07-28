#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
DEFAULT_CHECKOUT_DIR="$(cd "$REPO_ROOT/.." && pwd)/kengine-kotlin-fork"

UPSTREAM_URL="${KENGINE_KOTLIN_UPSTREAM_URL:-https://github.com/JetBrains/kotlin.git}"
CHECKOUT_DIR="${KENGINE_KOTLIN_REPO:-$DEFAULT_CHECKOUT_DIR}"
BRANCH="${KENGINE_KOTLIN_BRANCH:-kengine/switch-arm64}"
FORK_URL=""
VERSION=""
TAG=""
YES=0
BUILD_NATIVE_DIST=0
UPDATE_LOCAL_PROPERTIES=1

log() {
    printf '%s\n' "$*"
}

fail() {
    printf 'error: %s\n' "$*" >&2
    exit 1
}

usage() {
    cat <<'EOF'
Usage: setup-kotlin-fork.sh [options]

Clones JetBrains/kotlin at this repo's pinned Kotlin version and creates a
local Switch target branch for Kotlin/Native compiler work.

Options:
  --checkout-dir PATH      Kotlin source checkout. Defaults to ../kengine-kotlin-fork.
  --upstream-url URL       Kotlin upstream URL. Defaults to https://github.com/JetBrains/kotlin.git.
  --fork-url URL           Optional personal fork remote to add as "fork".
  --branch NAME            Local branch to create/use. Defaults to kengine/switch-arm64.
  --version VERSION        Kotlin version. Defaults to gradle/libs.versions.toml.
  --tag TAG                Exact Kotlin tag. Defaults to v<VERSION>, then VERSION.
  --build-native-dist      Run build-kotlin-native-dist.sh after checkout.
  --no-local-properties    Do not write kengine-kotlin/local.properties.
  --yes                    Run without confirmation prompts.
  --help                   Show this help.
EOF
}

while [ "$#" -gt 0 ]; do
    case "$1" in
        --checkout-dir)
            [ "$#" -ge 2 ] || fail "--checkout-dir requires a path."
            CHECKOUT_DIR="$2"
            shift
            ;;
        --upstream-url)
            [ "$#" -ge 2 ] || fail "--upstream-url requires a URL."
            UPSTREAM_URL="$2"
            shift
            ;;
        --fork-url)
            [ "$#" -ge 2 ] || fail "--fork-url requires a URL."
            FORK_URL="$2"
            shift
            ;;
        --branch)
            [ "$#" -ge 2 ] || fail "--branch requires a branch name."
            BRANCH="$2"
            shift
            ;;
        --version)
            [ "$#" -ge 2 ] || fail "--version requires a version."
            VERSION="$2"
            shift
            ;;
        --tag)
            [ "$#" -ge 2 ] || fail "--tag requires a tag."
            TAG="$2"
            shift
            ;;
        --build-native-dist)
            BUILD_NATIVE_DIST=1
            ;;
        --no-local-properties)
            UPDATE_LOCAL_PROPERTIES=0
            ;;
        --yes)
            YES=1
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

require_command() {
    command -v "$1" >/dev/null 2>&1 || fail "Missing required command: $1"
}

git_no_lfs() {
    git \
        -c filter.lfs.smudge= \
        -c filter.lfs.process= \
        -c filter.lfs.required=false \
        "$@"
}

current_kotlin_version() {
    awk -F '"' '/^kotlin[[:space:]]*=/ { print $2; exit }' "$REPO_ROOT/gradle/libs.versions.toml"
}

confirm() {
    if [ "$YES" -eq 1 ]; then
        return
    fi

    printf '%s [y/N] ' "$1"
    read -r reply
    case "$reply" in
        y|Y|yes|YES)
            ;;
        *)
            fail "Cancelled."
            ;;
    esac
}

resolve_tag() {
    if [ -n "$TAG" ]; then
        return
    fi

    [ -n "$VERSION" ] || VERSION="$(current_kotlin_version)"
    [ -n "$VERSION" ] || fail "Could not read Kotlin version from gradle/libs.versions.toml."

    for candidate in "v$VERSION" "$VERSION"; do
        if git ls-remote --exit-code --tags "$UPSTREAM_URL" "refs/tags/$candidate" >/dev/null 2>&1; then
            TAG="$candidate"
            return
        fi
    done

    fail "Could not find Kotlin tag for version $VERSION at $UPSTREAM_URL."
}

ensure_clean_if_switching_branch() {
    local current_branch

    current_branch="$(git -C "$CHECKOUT_DIR" branch --show-current || true)"
    if [ "$current_branch" = "$BRANCH" ]; then
        return
    fi

    if ! find "$CHECKOUT_DIR" -mindepth 1 -maxdepth 1 ! -name .git | grep -q .; then
        return
    fi

    git -C "$CHECKOUT_DIR" diff --quiet || fail "$CHECKOUT_DIR has unstaged changes."
    git -C "$CHECKOUT_DIR" diff --cached --quiet || fail "$CHECKOUT_DIR has staged changes."
}

ensure_checkout() {
    if [ -d "$CHECKOUT_DIR/.git" ]; then
        log "Using existing Kotlin checkout: $CHECKOUT_DIR"
        return
    fi

    if [ -e "$CHECKOUT_DIR" ]; then
        fail "$CHECKOUT_DIR exists but is not a Git checkout."
    fi

    confirm "Clone Kotlin into $CHECKOUT_DIR?"
    git_no_lfs clone --filter=blob:none --no-checkout "$UPSTREAM_URL" "$CHECKOUT_DIR"
}

ensure_tag() {
    if git -C "$CHECKOUT_DIR" rev-parse -q --verify "refs/tags/$TAG" >/dev/null; then
        return
    fi

    log "Fetching Kotlin tag $TAG"
    git_no_lfs -C "$CHECKOUT_DIR" fetch --filter=blob:none origin "refs/tags/$TAG:refs/tags/$TAG"
}

ensure_branch() {
    ensure_clean_if_switching_branch

    if git -C "$CHECKOUT_DIR" rev-parse -q --verify "refs/heads/$BRANCH" >/dev/null; then
        git_no_lfs -C "$CHECKOUT_DIR" checkout "$BRANCH"
    else
        git_no_lfs -C "$CHECKOUT_DIR" checkout -B "$BRANCH" "$TAG"
    fi
}

ensure_fork_remote() {
    if [ -z "$FORK_URL" ]; then
        return
    fi

    if git -C "$CHECKOUT_DIR" remote get-url fork >/dev/null 2>&1; then
        git -C "$CHECKOUT_DIR" remote set-url fork "$FORK_URL"
    else
        git -C "$CHECKOUT_DIR" remote add fork "$FORK_URL"
    fi
}

write_local_properties() {
    if [ "$UPDATE_LOCAL_PROPERTIES" -eq 0 ]; then
        return
    fi

    local props_file="$SCRIPT_DIR/local.properties"
    local native_home="$CHECKOUT_DIR/kotlin-native/dist"

    {
        printf '# Generated by setup-kotlin-fork.sh. Machine-local; ignored by Git.\n'
        printf 'kengine.kotlin.repo=%s\n' "$CHECKOUT_DIR"
        printf 'kengine.kotlin.version=%s\n' "$VERSION"
        printf 'kengine.kotlin.branch=%s\n' "$BRANCH"
        if [ -x "$native_home/bin/kotlinc-native" ]; then
            printf 'kengine.kotlin.nativeHome=%s\n' "$native_home"
        else
            printf '# Build first, then uncomment or rerun build-kotlin-native-dist.sh:\n'
            printf '# kengine.kotlin.nativeHome=%s\n' "$native_home"
        fi
        printf '# Use this after the compiler fork knows about the target:\n'
        printf '# kengine.switch.kotlinTarget=switch_arm64\n'
    } > "$props_file"

    log "Updated $props_file"
}

require_command git
resolve_tag

log "Kotlin upstream: $UPSTREAM_URL"
log "Kotlin version: $VERSION"
log "Kotlin tag: $TAG"
log "Checkout: $CHECKOUT_DIR"
log "Branch: $BRANCH"

ensure_checkout
ensure_tag
ensure_branch
ensure_fork_remote
write_local_properties

if [ "$BUILD_NATIVE_DIST" -eq 1 ]; then
    "$SCRIPT_DIR/build-kotlin-native-dist.sh" --checkout-dir "$CHECKOUT_DIR"
fi

log ""
log "Kotlin fork checkout is ready."
