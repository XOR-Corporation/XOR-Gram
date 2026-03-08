#!/bin/bash
# ═══════════════════════════════════════════════════════════════════════════
# XORGram Patch Generation Script
# ═══════════════════════════════════════════════════════════════════════════
# This script generates patch files from current modifications to Telegram AOSP.
# Use this after making changes to the Telegram source to create new patches.
#
# Usage: ./scripts/generate_patches.sh
# ═══════════════════════════════════════════════════════════════════════════

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
PATCHES_DIR="$PROJECT_ROOT/patches"
TELEGRAM_DIR="$PROJECT_ROOT/TMessagesProj"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo "═══════════════════════════════════════════════════════════════"
echo " ${BLUE}XORGram Patch Generation Tool${NC}"
echo "═══════════════════════════════════════════════════════════════"
echo ""

# Ensure patches directory exists
mkdir -p "$PATCHES_DIR"

# Get list of modified files
cd "$TELEGRAM_DIR"
MODIFIED_FILES=$(git diff --name-only HEAD 2>/dev/null || echo "")

if [ -z "$MODIFIED_FILES" ]; then
    echo -e "${YELLOW}No modified files found in TMessagesProj${NC}"
    echo "Make changes to Telegram source first, then run this script."
    exit 0
fi

echo "Modified files:"
echo "$MODIFIED_FILES"
echo ""

# Generate patches for each modified file
PATCH_NUM=$(ls "$PATCHES_DIR"/*.patch 2>/dev/null | wc -l)
PATCH_NUM=$((PATCH_NUM + 1))

for file in $MODIFIED_FILES; do
    # Generate patch name from file and hook point
    FILENAME=$(basename "$file" .java)
    PATCH_NAME=$(printf "%03d_%s.patch" $PATCH_NUM "$(echo $FILENAME | sed 's/\([A-Z]\)/_\L\1/g')")
    
    echo -n "Generating patch for $file... "
    
    # Generate the patch
    if git diff HEAD -- "$file" > "$PATCHES_DIR/$PATCH_NAME" 2>/dev/null; then
        if [ -s "$PATCHES_DIR/$PATCH_NAME" ]; then
            echo -e "${GREEN}✅ $PATCH_NAME${NC}"
            ((PATCH_NUM++))
        else
            rm -f "$PATCHES_DIR/$PATCH_NAME"
            echo -e "${YELLOW}⏭️ Empty diff, skipped${NC}"
        fi
    else
        echo -e "${RED}❌ Failed${NC}"
    fi
done

echo ""
echo "═══════════════════════════════════════════════════════════════"
echo -e "${GREEN}Patch generation complete!${NC}"
echo "Patches saved to: $PATCHES_DIR"
echo ""
echo "Next steps:"
echo "  1. Review generated patches"
echo "  2. Rename patches to include hook point (e.g., 001_ghost_mark_read.patch)"
echo "  3. Test patches with: ./scripts/apply_patches.sh"
echo "═══════════════════════════════════════════════════════════════"
