#!/bin/bash
# ═══════════════════════════════════════════════════════════════════════════
# XORGram Hook Verification Script
# ═══════════════════════════════════════════════════════════════════════════
# This script verifies that all required hook points are present in the
# Telegram source code after patching.
#
# Usage: ./scripts/verify_hooks.sh
# ═══════════════════════════════════════════════════════════════════════════

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
TELEGRAM_DIR="$PROJECT_ROOT/TMessagesProj"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Hook registry - maps hook ID to search pattern
declare -A HOOKS=(
    # Ghost Engine Hooks
    ["GP-001"]="XORBridge.onBeforeMarkRead"
    ["GP-002"]="XORBridge.onBeforeSendTyping"
    ["GP-003"]="XORBridge.onBeforeUpdateOnline"
    ["GP-004"]="XORBridge.onBeforeMarkStoryRead"
    ["GP-005"]="XORBridge.onBeforeMarkVoiceListened"
    
    # Data Vault Hooks
    ["GP-010"]="XORBridge.onBeforeDeleteMessages"
    ["GP-011"]="XORBridge.onMessageEdited"
    ["GP-012"]="XORBridge.onProcessUpdates"
    ["GP-013"]="XORBridge.onContactUpdated"
    
    # UI Hooks
    ["GP-020"]="XORBridge.init"
    ["GP-021"]="XORBridge.onCreateChatOverlay"
    ["GP-022"]="XORBridge.onCreateProfileOverlay"
    ["GP-023"]="XORBridge.onCreateDialogsOverlay"
    ["GP-024"]="XORBridge.onCreateMenu"
    
    # Media Hooks
    ["GP-030"]="XORBridge.onBeforeLoadImage"
    ["GP-031"]="XORBridge.onBeforePlayAudio"
    ["GP-032"]="XORBridge.onBeforeSendMedia"
    
    # Security Hooks
    ["GP-040"]="XORBridge.onGetDeviceInfo"
    ["GP-041"]="XORBridge.onGetClientName"
    ["GP-042"]="XORBridge.onAppGoingBackground"
)

echo "═══════════════════════════════════════════════════════════════"
echo " ${BLUE}XORGram Hook Verification Tool${NC}"
echo "═══════════════════════════════════════════════════════════════"
echo ""

TOTAL_HOOKS=${#HOOKS[@]}
FOUND=0
MISSING=0

echo "Checking $TOTAL_HOOKS hook points..."
echo ""

for hook_id in "${!HOOKS[@]}"; do
    pattern="${HOOKS[$hook_id]}"
    
    # Search for the hook in Telegram source
    if grep -r "$pattern" "$TELEGRAM_DIR/src" --include="*.java" > /dev/null 2>&1; then
        echo -e "  $hook_id: ${GREEN}✅ Found${NC} - $pattern"
        ((FOUND++))
    else
        echo -e "  $hook_id: ${RED}❌ Missing${NC} - $pattern"
        ((MISSING++))
    fi
done

echo ""
echo "═══════════════════════════════════════════════════════════════"
echo " Results:"
echo "   Total hooks:   $TOTAL_HOOKS"
echo -e "   Found:         ${GREEN}$FOUND${NC}"
echo -e "   Missing:       ${RED}$MISSING${NC}"
echo "═══════════════════════════════════════════════════════════════"

if [ "$MISSING" -gt 0 ]; then
    echo ""
    echo -e "${YELLOW}Some hooks are missing!${NC}"
    echo ""
    echo "To fix:"
    echo "  1. Check if all patches were applied: ./scripts/apply_patches.sh"
    echo "  2. Manually add missing hooks to Telegram source"
    echo "  3. Generate new patches: ./scripts/generate_patches.sh"
    echo ""
    exit 1
fi

echo ""
echo -e "${GREEN}All hooks verified successfully!${NC}"
exit 0
