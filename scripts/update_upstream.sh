#!/bin/bash
# ═══════════════════════════════════════════════════════════════════════════
# XORGram Upstream Update Script
# ═══════════════════════════════════════════════════════════════════════════
# This script updates the Telegram AOSP source to the latest upstream version
# and re-applies all XORGram patches.
#
# Usage: ./scripts/update_upstream.sh [upstream_branch]
# Example: ./scripts/update_upstream.sh upstream/master
# ═══════════════════════════════════════════════════════════════════════════

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
TELEGRAM_DIR="$PROJECT_ROOT/TMessagesProj"
PATCHES_DIR="$PROJECT_ROOT/patches"

UPSTREAM_BRANCH="${1:-upstream/master}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

echo "═══════════════════════════════════════════════════════════════"
echo " ${CYAN}XORGram Upstream Update Tool${NC}"
echo "═══════════════════════════════════════════════════════════════"
echo ""

# Step 1: Save current patches
echo "[1/6] ${BLUE}Saving current patches...${NC}"
if [ -d "$PATCHES_DIR" ] && [ "$(ls -A $PATCHES_DIR/*.patch 2>/dev/null)" ]; then
    BACKUP_DIR="$PROJECT_ROOT/patches_backup_$(date +%Y%m%d_%H%M%S)"
    mkdir -p "$BACKUP_DIR"
    cp "$PATCHES_DIR"/*.patch "$BACKUP_DIR/" 2>/dev/null || true
    echo "   Backed up to: $BACKUP_DIR"
else
    echo "   No existing patches to backup"
fi

# Step 2: Revert patches from telegram-aosp
echo ""
echo "[2/6] ${BLUE}Reverting patches from TMessagesProj...${NC}"
cd "$TELEGRAM_DIR"
if git diff --quiet HEAD 2>/dev/null; then
    echo "   No changes to revert"
else
    git checkout . 2>/dev/null || true
    echo "   Reverted local changes"
fi

# Step 3: Update from upstream
echo ""
echo "[3/6] ${BLUE}Fetching upstream Telegram...${NC}"
cd "$TELEGRAM_DIR"

# Check if upstream remote exists
if ! git remote | grep -q "^upstream$"; then
    echo "   Adding upstream remote..."
    git remote add upstream https://github.com/DrKLO/Telegram.git 2>/dev/null || true
fi

git fetch upstream 2>/dev/null || {
    echo -e "${RED}Failed to fetch upstream. Check your internet connection.${NC}"
    exit 1
}

echo ""
echo "[4/6] ${BLUE}Merging $UPSTREAM_BRANCH...${NC}"
git merge "$UPSTREAM_BRANCH" --no-edit 2>/dev/null || {
    echo -e "${YELLOW}Merge conflicts detected!${NC}"
    echo "   Please resolve conflicts manually and run:"
    echo "   ./scripts/apply_patches.sh"
    exit 1
}

# Step 5: Apply patches
echo ""
echo "[5/6] ${BLUE}Applying XORGram patches...${NC}"
cd "$PROJECT_ROOT"
./scripts/apply_patches.sh

# Step 6: Verify hooks
echo ""
echo "[6/6] ${BLUE}Verifying hooks...${NC}"
./scripts/verify_hooks.sh

echo ""
echo "═══════════════════════════════════════════════════════════════"
echo -e "${GREEN}✅ XORGram updated successfully!${NC}"
echo "═══════════════════════════════════════════════════════════════"
echo ""
echo "Next steps:"
echo "  1. Test the build: ./gradlew assembleDebug"
echo "  2. Run tests: ./gradlew test"
echo "  3. Commit changes: git commit -am 'Update to latest upstream'"
