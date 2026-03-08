#!/bin/bash
# ═══════════════════════════════════════════════════════════════════════════
# XORGram Patch Application Script
# ═══════════════════════════════════════════════════════════════════════════
# This script applies all XORGram patches to the Telegram AOSP source.
# Each patch adds a single hook point for the XORBridge.
#
# Usage: ./scripts/apply_patches.sh
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
echo " ${BLUE}XORGram Patch Application Tool${NC}"
echo "═══════════════════════════════════════════════════════════════"
echo ""

# Check if patches directory exists
if [ ! -d "$PATCHES_DIR" ]; then
    echo -e "${YELLOW}Warning: No patches directory found at $PATCHES_DIR${NC}"
    echo "Creating patches directory..."
    mkdir -p "$PATCHES_DIR"
    exit 0
fi

# Count patches
PATCH_COUNT=$(find "$PATCHES_DIR" -name "*.patch" | wc -l)
echo "Found $PATCH_COUNT patches to apply"
echo ""

if [ "$PATCH_COUNT" -eq 0 ]; then
    echo -e "${YELLOW}No patches found. Nothing to apply.${NC}"
    exit 0
fi

# Apply patches
APPLIED=0
FAILED=0
SKIPPED=0

for patch_file in "$PATCHES_DIR"/*.patch; do
    patch_name=$(basename "$patch_file")
    
    echo -n "Applying $patch_name... "
    
    # Try to apply the patch
    if patch -p1 -d "$TELEGRAM_DIR" --dry-run < "$patch_file" > /dev/null 2>&1; then
        # Apply for real
        if patch -p1 -d "$TELEGRAM_DIR" < "$patch_file" > /dev/null 2>&1; then
            echo -e "${GREEN}✅ OK${NC}"
            ((APPLIED++))
        else
            echo -e "${RED}❌ FAILED${NC}"
            ((FAILED++))
        fi
    else
        # Check if already applied
        if patch -p1 -d "$TELEGRAM_DIR" -R --dry-run < "$patch_file" > /dev/null 2>&1; then
            echo -e "${BLUE}⏭️ ALREADY APPLIED${NC}"
            ((SKIPPED++))
        else
            echo -e "${RED}⚠️ CONFLICT${NC}"
            ((FAILED++))
        fi
    fi
done

echo ""
echo "═══════════════════════════════════════════════════════════════"
echo " Results:"
echo "   Applied:  $APPLIED"
echo "   Skipped:  $SKIPPED"
echo "   Failed:   $FAILED"
echo "═══════════════════════════════════════════════════════════════"

if [ "$FAILED" -gt 0 ]; then
    echo ""
    echo -e "${RED}Some patches failed to apply. Manual intervention required.${NC}"
    echo ""
    echo "To fix conflicts:"
    echo "  1. Check the failed patches above"
    echo "  2. Update the patch files in patches/ directory"
    echo "  3. Run this script again"
    echo ""
    exit 1
fi

echo ""
echo -e "${GREEN}All patches applied successfully!${NC}"
exit 0
