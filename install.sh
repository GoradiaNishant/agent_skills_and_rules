#!/usr/bin/env bash
# ==============================================================================
# Agent Skills & Rules Installer
# Installs Antigravity / Gemini rules and skills (.agents/) into any project.
#
# Usage:
#   bash install.sh android
#   bash install.sh flutter
#   bash install.sh android --symlink   (symlinks to local repo for live edits)
#
# Remote Usage via curl:
#   curl -sSL https://raw.githubusercontent.com/<USER>/agent_skills_and_rules/main/install.sh | bash -s -- android
# ==============================================================================

set -e

# Default GitHub Repo (Override via GITHUB_REPO env var if needed)
REPO_URL="${GITHUB_REPO:-https://github.com/firdos/agent_skills_and_rules.git}"
PLATFORM=""
USE_SYMLINK=false

for arg in "$@"; do
    case "$arg" in
        android|Android)
            PLATFORM="android"
            ;;
        flutter|Flutter)
            PLATFORM="flutter"
            ;;
        --symlink|-s)
            USE_SYMLINK=true
            ;;
        *)
            ;;
    esac
done

if [ -z "$PLATFORM" ]; then
    echo "❌ Error: Please specify a platform target: 'android' or 'flutter'"
    echo "Usage: $0 [android|flutter] [--symlink]"
    exit 1
fi

PLATFORM_UPPER=$(echo "$PLATFORM" | tr '[:lower:]' '[:upper:]')
echo "🚀 Installing Antigravity Skills & Rules for: $PLATFORM_UPPER..."
TARGET_DIR="$(pwd)/.agents"

# Check if script is running inside the source repo
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]:-$0}")" 2>/dev/null && pwd)"
SOURCE_PATH=""

if [ "$PLATFORM" = "android" ]; then
    SOURCE_PATH="${SCRIPT_DIR}/android_rules_skills/.agents"
else
    SOURCE_PATH="${SCRIPT_DIR}/flutter_rules_skills/.agents"
fi

if [ -d "$SOURCE_PATH" ]; then
    # Local execution from repo
    if [ "$USE_SYMLINK" = true ]; then
        echo "🔗 Creating symlink: $TARGET_DIR -> $SOURCE_PATH"
        rm -rf "$TARGET_DIR"
        ln -s "$SOURCE_PATH" "$TARGET_DIR"
    else
        echo "📦 Copying rules and skills into $TARGET_DIR..."
        rm -rf "$TARGET_DIR"
        cp -R "$SOURCE_PATH" "$TARGET_DIR"
    fi
else
    # Remote execution via curl / outside repo
    TEMP_DIR=$(mktemp -d)
    trap 'rm -rf "$TEMP_DIR"' EXIT

    echo "📥 Fetching latest rules and skills from GitHub..."
    git clone --depth 1 "$REPO_URL" "$TEMP_DIR" >/dev/null 2>&1

    if [ "$PLATFORM" = "android" ]; then
        REMOTE_SOURCE="${TEMP_DIR}/android_rules_skills/.agents"
    else
        REMOTE_SOURCE="${TEMP_DIR}/flutter_rules_skills/.agents"
    fi

    echo "📦 Extracting .agents to $TARGET_DIR..."
    rm -rf "$TARGET_DIR"
    cp -R "$REMOTE_SOURCE" "$TARGET_DIR"
fi

echo "✅ Success! .agents/ installed in $(pwd)."
echo "   - Rules:  $(find "$TARGET_DIR/rules" -name "*.md" | wc -l | tr -d ' ') rules loaded"
echo "   - Skills: $(find "$TARGET_DIR/skills" -name "SKILL.md" | wc -l | tr -d ' ') skills loaded"
