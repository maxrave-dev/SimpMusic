#!/bin/bash
# Validates KMP source set structure and detects common issues

set -e

PROJECT_ROOT="${1:-.}"
cd "$PROJECT_ROOT"

echo "=== Validating KMP Structure ==="
echo

# Colors for output
RED='\033[0;31m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
NC='\033[0m' # No Color

ISSUES_FOUND=0

# Check 1: jvmAndroid defined before androidMain/jvmMain
echo "📋 Checking source set definition order..."
if [ -f "quartz/build.gradle.kts" ]; then
    jvmandroid_line=$(grep -n "val jvmAndroid = create" quartz/build.gradle.kts | cut -d: -f1)
    android_line=$(grep -n "androidMain {" quartz/build.gradle.kts | cut -d: -f1)
    jvm_line=$(grep -n "jvmMain {" quartz/build.gradle.kts | cut -d: -f1)

    if [ -n "$jvmandroid_line" ] && [ -n "$android_line" ] && [ -n "$jvm_line" ]; then
        if [ "$jvmandroid_line" -lt "$android_line" ] && [ "$jvmandroid_line" -lt "$jvm_line" ]; then
            echo -e "${GREEN}✓${NC} jvmAndroid defined before androidMain and jvmMain"
        else
            echo -e "${RED}✗${NC} jvmAndroid must be defined BEFORE androidMain and jvmMain"
            ISSUES_FOUND=$((ISSUES_FOUND + 1))
        fi
    fi
fi

# Check 2: Platform code in commonMain (Android imports)
echo
echo "📋 Checking for platform code in commonMain..."
android_imports_in_common=$(find */src/commonMain -name "*.kt" 2>/dev/null | xargs grep -l "^import android\." || true)
if [ -n "$android_imports_in_common" ]; then
    echo -e "${RED}✗${NC} Found Android imports in commonMain:"
    echo "$android_imports_in_common" | sed 's/^/  /'
    echo "  Fix: Move to androidMain or create expect/actual"
    ISSUES_FOUND=$((ISSUES_FOUND + 1))
else
    echo -e "${GREEN}✓${NC} No Android imports in commonMain"
fi

# Check 3: JVM libraries in commonMain (Jackson, OkHttp)
echo
echo "📋 Checking for JVM libraries in commonMain..."
jvm_imports_in_common=$(find */src/commonMain -name "*.kt" 2>/dev/null | xargs grep -l "^import com.fasterxml.jackson\|^import okhttp3\." || true)
if [ -n "$jvm_imports_in_common" ]; then
    echo -e "${RED}✗${NC} Found JVM library imports in commonMain:"
    echo "$jvm_imports_in_common" | sed 's/^/  /'
    echo "  Fix: Move to jvmAndroid or migrate to kotlinx.serialization/ktor"
    ISSUES_FOUND=$((ISSUES_FOUND + 1))
else
    echo -e "${GREEN}✓${NC} No JVM library imports in commonMain"
fi

# Check 4: Unmatched expect/actual declarations
echo
echo "📋 Checking expect/actual pairs..."
expect_files=$(find */src/commonMain -name "*.kt" 2>/dev/null | xargs grep -l "^expect " || true)
if [ -n "$expect_files" ]; then
    for file in $expect_files; do
        # Extract declarations
        expects=$(grep "^expect \(class\|object\|fun\|interface\)" "$file" | sed 's/expect //' | awk '{print $2}' | sed 's/[({].*$//')

        # Check for actuals in platform source sets
        for expect_name in $expects; do
            actual_count=0
            for platform in androidMain jvmMain iosMain; do
                platform_dir=$(dirname "$file" | sed "s/commonMain/$platform/")
                platform_file="${platform_dir}/$(basename "$file")"
                if [ -f "$platform_file" ] && grep -q "actual.*$expect_name" "$platform_file"; then
                    actual_count=$((actual_count + 1))
                fi
            done

            if [ "$actual_count" -eq 0 ]; then
                echo -e "${YELLOW}⚠${NC} No actual implementations found for: $expect_name in $file"
                echo "  Check: androidMain, jvmMain, iosMain"
                ISSUES_FOUND=$((ISSUES_FOUND + 1))
            fi
        done
    done
else
    echo -e "${GREEN}✓${NC} No expect declarations to validate"
fi

# Check 5: Duplicated business logic across platforms
echo
echo "📋 Checking for potential code duplication..."
# This is a heuristic check - look for similar function names in different platform source sets
common_functions=$(find */src/commonMain -name "*.kt" 2>/dev/null | xargs grep -h "^fun " | awk '{print $2}' | sed 's/[({<].*$//' | sort -u || true)
if [ -n "$common_functions" ]; then
    for func in $common_functions; do
        android_count=$(find */src/androidMain -name "*.kt" 2>/dev/null | xargs grep -l "^fun $func" | wc -l)
        jvm_count=$(find */src/jvmMain -name "*.kt" 2>/dev/null | xargs grep -l "^fun $func" | wc -l)

        if [ "$android_count" -gt 0 ] && [ "$jvm_count" -gt 0 ]; then
            echo -e "${YELLOW}⚠${NC} Function '$func' found in both androidMain and jvmMain"
            echo "  Consider: Move to commonMain or jvmAndroid if truly shared"
        fi
    done
fi

# Summary
echo
echo "=== Summary ==="
if [ "$ISSUES_FOUND" -eq 0 ]; then
    echo -e "${GREEN}✓ All checks passed!${NC}"
    exit 0
else
    echo -e "${RED}✗ Found $ISSUES_FOUND issue(s)${NC}"
    echo
    echo "Common fixes:"
    echo "  1. Platform code in commonMain → Move to androidMain or create expect/actual"
    echo "  2. JVM libraries in commonMain → Move to jvmAndroid or migrate to kotlinx.*"
    echo "  3. Missing actual implementations → Implement in all target platforms"
    echo "  4. Duplicated logic → Move to commonMain or jvmAndroid"
    exit 1
fi
