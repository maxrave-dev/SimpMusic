#!/usr/bin/env bash
#
# Android Emulator Testing Environment Health Check
#
# Verifies that all required tools and dependencies are properly installed
# and configured for Android emulator testing.
#
# Usage: bash scripts/emu_health_check.sh [--help]

set -e

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Check flags
SHOW_HELP=false

# Parse arguments
for arg in "$@"; do
    case $arg in
        --help|-h)
            SHOW_HELP=true
            shift
            ;;
    esac
done

if [ "$SHOW_HELP" = true ]; then
    cat <<EOF
Android Emulator Testing - Environment Health Check

Verifies that your environment is properly configured for Android emulator testing.

Usage: bash scripts/emu_health_check.sh [options]

Options:
  --help, -h    Show this help message

This script checks for:
  - Android SDK availability (ANDROID_HOME)
  - ADB (Android Debug Bridge) installation
  - Emulator executable availability
  - Java Development Kit (JDK)
  - Connected Android devices/emulators
  - Python 3 installation (for scripts)

Exit codes:
  0 - All checks passed
  1 - One or more checks failed (see output for details)
EOF
    exit 0
fi

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  Android Emulator Testing - Environment Health Check${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

CHECKS_PASSED=0
CHECKS_FAILED=0

# Function to print check status
check_passed() {
    echo -e "${GREEN}✓${NC} $1"
    ((CHECKS_PASSED++))
}

check_failed() {
    echo -e "${RED}✗${NC} $1"
    ((CHECKS_FAILED++))
}

check_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

# Check 1: ANDROID_HOME
echo -e "${BLUE}[1/6]${NC} Checking ANDROID_HOME..."
if [ -n "$ANDROID_HOME" ]; then
    check_passed "ANDROID_HOME is set to $ANDROID_HOME"
else
    # Try to guess standard locations
    if [ -d "$HOME/Library/Android/sdk" ]; then
        export ANDROID_HOME="$HOME/Library/Android/sdk"
        check_warning "ANDROID_HOME not set, but found valid SDK at $ANDROID_HOME"
        echo "       Exporting for this session."
    else
        check_failed "ANDROID_HOME environment variable not set"
        echo "       Please set ANDROID_HOME to your Android SDK location."
    fi
fi
echo ""

# Check 2: ADB
echo -e "${BLUE}[2/6]${NC} Checking ADB (Android Debug Bridge)..."
if command -v adb &> /dev/null; then
    ADB_VERSION=$(adb --version | head -n 1)
    check_passed "ADB is installed ($ADB_VERSION)"
    echo "       Path: $(which adb)"
else
    # Check if inside standard path
    if [ -f "$ANDROID_HOME/platform-tools/adb" ]; then
        export PATH="$PATH:$ANDROID_HOME/platform-tools"
        check_warning "ADB found in SDK but not in PATH. Adding it temporarily."
        check_passed "ADB is installed"
    else
        check_failed "ADB command not found"
        echo "       Ensure platform-tools is in your PATH."
    fi
fi
echo ""

# Check 3: Emulator
echo -e "${BLUE}[3/6]${NC} Checking Android Emulator..."
if command -v emulator &> /dev/null; then
    EMULATOR_VERSION=$(emulator -version | head -n 1)
    check_passed "Emulator is installed ($EMULATOR_VERSION)"
else
     if [ -f "$ANDROID_HOME/emulator/emulator" ]; then
        export PATH="$PATH:$ANDROID_HOME/emulator"
        check_warning "Emulator found in SDK but not in PATH. Adding it temporarily."
        check_passed "Emulator is installed"
    else
        check_failed "Emulator command not found"
        echo "       Ensure emulator is in your PATH."
    fi
fi
echo ""

# Check 4: Java
echo -e "${BLUE}[4/6]${NC} Checking Java..."
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1)
    check_passed "Java is installed ($JAVA_VERSION)"
else
    check_failed "Java not found"
    echo "       A JDK is required for Android development."
fi
echo ""

# Check 5: Python 3
echo -e "${BLUE}[5/6]${NC} Checking Python 3..."
if command -v python3 &> /dev/null; then
    PYTHON_VERSION=$(python3 --version)
    check_passed "Python 3 is installed ($PYTHON_VERSION)"
else
    check_failed "Python 3 not found"
    echo "       Required for skill scripts."
fi
echo ""

# Check 6: Connected Devices
echo -e "${BLUE}[6/6]${NC} Checking connected devices..."
if command -v adb &> /dev/null; then
    DEVICE_COUNT=$(adb devices | grep -E "device$" | wc -l | tr -d ' ')
    
    if [ "$DEVICE_COUNT" -gt 0 ]; then
        check_passed "Found $DEVICE_COUNT connected device(s)"
        echo ""
        echo "       Connected devices:"
        adb devices | grep -E "device$" | while read -r line; do
            echo "       - $line"
        done
    else
        check_warning "No devices connected or emulators booted"
        echo "       Boot an emulator to begin testing."
        echo "       Use 'emulator -list-avds' to see available AVDs."
    fi
else
    check_failed "Cannot check devices (adb not found)"
fi
echo ""

# Summary
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  Summary${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "Checks passed: ${GREEN}$CHECKS_PASSED${NC}"
if [ "$CHECKS_FAILED" -gt 0 ]; then
    echo -e "Checks failed: ${RED}$CHECKS_FAILED${NC}"
    echo ""
    echo -e "${YELLOW}Action required:${NC} Fix the failed checks above before testing"
    exit 1
else
    echo ""
    echo -e "${GREEN}✓ Environment is ready for Android emulator testing${NC}"
    exit 0
fi
