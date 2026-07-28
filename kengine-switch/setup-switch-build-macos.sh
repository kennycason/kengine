#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

DEVKITPRO="${DEVKITPRO:-/opt/devkitpro}"
DEVKITA64="${DEVKITA64:-$DEVKITPRO/devkitA64}"
SHELLRC=""
YES=0
INSTALL=1
UPDATE_SHELLRC=1
RUN_BUILD_CHECKS=1

log() {
    printf '%s\n' "$*"
}

fail() {
    printf 'error: %s\n' "$*" >&2
    exit 1
}

usage() {
    cat <<'EOF'
Usage: setup-switch-build-macos.sh [options]

Installs and verifies the macOS devkitPro Switch homebrew toolchain used by
the experimental kengine-switch module.

Options:
  --yes                Run without confirmation prompts.
  --no-install         Do not install packages; only configure/verify.
  --no-shellrc         Do not update the shell startup file.
  --shellrc PATH       Shell startup file to update. Defaults to ~/.zshrc.
  --skip-build-checks  Skip Gradle verification tasks.
  --help               Show this help.
EOF
}

while [ "$#" -gt 0 ]; do
    case "$1" in
        --yes)
            YES=1
            ;;
        --no-install)
            INSTALL=0
            ;;
        --no-shellrc)
            UPDATE_SHELLRC=0
            ;;
        --shellrc)
            [ "$#" -ge 2 ] || fail "--shellrc requires a path."
            SHELLRC="$2"
            shift
            ;;
        --skip-build-checks)
            RUN_BUILD_CHECKS=0
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

if [ "$(uname -s)" != "Darwin" ]; then
    fail "This setup script is for macOS only."
fi

if [ -z "$SHELLRC" ]; then
    case "${SHELL:-}" in
        */bash)
            SHELLRC="$HOME/.bash_profile"
            ;;
        *)
            SHELLRC="$HOME/.zshrc"
            ;;
    esac
fi

run_sudo() {
    if [ "$(id -u)" -eq 0 ]; then
        "$@"
    else
        sudo "$@"
    fi
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

require_command() {
    command -v "$1" >/dev/null 2>&1 || fail "Missing required command: $1"
}

find_dkp_pacman() {
    if command -v dkp-pacman >/dev/null 2>&1; then
        command -v dkp-pacman
        return
    fi

    for candidate in \
        "$DEVKITPRO/pacman/bin/dkp-pacman" \
        "/opt/devkitpro/pacman/bin/dkp-pacman" \
        "/usr/local/bin/dkp-pacman"; do
        if [ -x "$candidate" ]; then
            printf '%s\n' "$candidate"
            return
        fi
    done

    return 1
}

ensure_xcode_tools() {
    if xcode-select -p >/dev/null 2>&1; then
        return
    fi

    log "Xcode command line tools are not installed."
    if [ "$INSTALL" -eq 0 ]; then
        fail "Run xcode-select --install, then rerun this script."
    fi

    xcode-select --install || true
    fail "Finish the Xcode command line tools installer, then rerun this script."
}

latest_pacman_pkg_url() {
    local api_url="https://api.github.com/repos/devkitPro/pacman/releases/latest"
    local fallback_base="https://github.com/devkitPro/pacman/releases/latest/download"
    local arch
    local selected

    arch="$(uname -m)"
    selected="$(
        { curl -fsSL "$api_url" 2>/dev/null || true; } |
            awk -F '"' '/browser_download_url/ && /devkitpro-pacman-installer/ && /\.pkg/ { print $4 }' |
            awk -v arch="$arch" '
                {
                    urls[++n] = $0
                    if (!found && (arch == "arm64" || arch == "aarch64") && $0 ~ /(arm64|aarch64)/) {
                        print
                        found = 1
                        exit
                    }
                    if (!found && arch != "arm64" && arch != "aarch64" && $0 ~ /(x86_64|x64)/) {
                        print
                        found = 1
                        exit
                    }
                }
                END {
                    if (!found && n > 0) {
                        print urls[1]
                    }
                }
            '
    )"

    if [ -n "$selected" ]; then
        printf '%s\n' "$selected"
        return
    fi

    for candidate in \
        "$fallback_base/devkitpro-pacman-installer.$arch.pkg" \
        "$fallback_base/devkitpro-pacman-installer.pkg" \
        "https://pkg.devkitpro.org/packages/macos-installers/devkitpro-pacman-installer.$arch.pkg" \
        "https://pkg.devkitpro.org/packages/macos-installers/devkitpro-pacman-installer.pkg"; do
        if curl -fsIL "$candidate" >/dev/null 2>&1; then
            printf '%s\n' "$candidate"
            return
        fi
    done

    fail "Could not resolve the latest devkitPro pacman macOS installer URL."
}

install_devkitpro_pacman() {
    local pacman_path

    if pacman_path="$(find_dkp_pacman)"; then
        log "dkp-pacman already installed: $pacman_path"
        return
    fi

    if [ "$INSTALL" -eq 0 ]; then
        fail "dkp-pacman is not installed. Install from https://github.com/devkitPro/pacman/releases/latest"
    fi

    require_command curl
    local tmp_dir
    local pkg_file
    local pkg_url

    tmp_dir="$(mktemp -d)"
    pkg_file="$tmp_dir/devkitpro-pacman-installer.pkg"
    pkg_url="$(latest_pacman_pkg_url)"

    log "Downloading devkitPro pacman installer:"
    log "$pkg_url"
    curl -fL "$pkg_url" -o "$pkg_file"

    log "Installing devkitPro pacman package manager. sudo may prompt."
    run_sudo installer -pkg "$pkg_file" -target /
    rm -rf "$tmp_dir"
}

install_switch_dev() {
    local pacman_path

    if [ "$INSTALL" -eq 0 ]; then
        return
    fi

    pacman_path="$(find_dkp_pacman)" || fail "dkp-pacman is not installed."
    log "Updating devkitPro package database. sudo may prompt."
    run_sudo "$pacman_path" -Syu --noconfirm

    log "Installing Switch development package group: switch-dev"
    run_sudo "$pacman_path" -S --needed --noconfirm switch-dev
}

update_shellrc() {
    if [ "$UPDATE_SHELLRC" -eq 0 ]; then
        return
    fi

    local start_marker="# >>> kengine switch build environment >>>"
    local end_marker="# <<< kengine switch build environment <<<"
    local tmp_file

    mkdir -p "$(dirname "$SHELLRC")"
    touch "$SHELLRC"
    tmp_file="$(mktemp)"

    awk -v start="$start_marker" -v end="$end_marker" '
        $0 == start { skipping = 1; next }
        $0 == end { skipping = 0; next }
        !skipping { print }
    ' "$SHELLRC" > "$tmp_file"

    {
        cat "$tmp_file"
        printf '\n%s\n' "$start_marker"
        printf 'export DEVKITPRO=%s\n' "$DEVKITPRO"
        printf 'export DEVKITA64=$DEVKITPRO/devkitA64\n'
        printf 'export PATH=$DEVKITA64/bin:$DEVKITPRO/tools/bin:$DEVKITPRO/pacman/bin:$PATH\n'
        printf '%s\n' "$end_marker"
    } > "$SHELLRC"

    rm -f "$tmp_file"
    log "Updated shell environment block in $SHELLRC"
}

verify_file() {
    if [ ! -x "$1" ]; then
        fail "Missing executable: $1"
    fi
    log "ok: $1"
}

verify_toolchain() {
    export DEVKITPRO
    export DEVKITA64
    export PATH="$DEVKITA64/bin:$DEVKITPRO/tools/bin:$DEVKITPRO/pacman/bin:$PATH"

    verify_file "$DEVKITA64/bin/aarch64-none-elf-gcc"
    verify_file "$DEVKITPRO/tools/bin/nacptool"
    verify_file "$DEVKITPRO/tools/bin/elf2nro"

    "$DEVKITA64/bin/aarch64-none-elf-gcc" --version | sed -n '1p'
}

run_gradle_checks() {
    if [ "$RUN_BUILD_CHECKS" -eq 0 ]; then
        return
    fi

    cd "$REPO_ROOT"

    if command -v jenv >/dev/null 2>&1; then
        jenv exec ./gradlew -Pkengine.switch=true :kengine-switch:switchToolchainInfo
        jenv exec ./gradlew -Pkengine.switch=true :kengine-switch:compileSwitchKotlinStatic
        jenv exec ./gradlew -Pkengine.switch=true :kengine-switch:buildSwitchCOnlyNro
    else
        ./gradlew -Pkengine.switch=true :kengine-switch:switchToolchainInfo
        ./gradlew -Pkengine.switch=true :kengine-switch:compileSwitchKotlinStatic
        ./gradlew -Pkengine.switch=true :kengine-switch:buildSwitchCOnlyNro
    fi
}

log "This script will configure the devkitPro Switch homebrew toolchain for kengine-switch."
log "DEVKITPRO=$DEVKITPRO"
log "DEVKITA64=$DEVKITA64"
log "Shell startup file: $SHELLRC"

if [ "$INSTALL" -eq 1 ]; then
    confirm "Install/update devkitPro pacman and switch-dev now?"
fi

ensure_xcode_tools
install_devkitpro_pacman
install_switch_dev
update_shellrc
verify_toolchain
run_gradle_checks

log ""
log "Switch build setup complete."
log "Open a new terminal or run: source \"$SHELLRC\""
