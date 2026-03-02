#!/bin/bash
# Suggests KMP library alternatives for JVM-specific dependencies

set -e

PROJECT_ROOT="${1:-.}"
cd "$PROJECT_ROOT"

echo "=== KMP Dependency Suggestions ==="
echo

# Colors
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

SUGGESTIONS_FOUND=0

# Check for Jackson (suggest kotlinx.serialization)
echo "📦 Checking for Jackson JSON..."
if grep -r "jackson" */build.gradle.kts 2>/dev/null | grep -q "implementation\|api"; then
    echo -e "${YELLOW}⚠ Found Jackson dependency${NC}"
    echo "  Current: Jackson (JVM-only)"
    echo -e "  ${GREEN}Suggest: kotlinx.serialization${NC} (works on all platforms)"
    echo
    echo "  Migration:"
    echo "    // Remove:"
    echo "    api(libs.jackson.module.kotlin)"
    echo
    echo "    // Add to commonMain:"
    echo "    implementation(libs.kotlinx.serialization.json)"
    echo
    echo "    // Code change:"
    echo "    // Before (Jackson):"
    echo "    val mapper = ObjectMapper()"
    echo "    val event = mapper.readValue(json, Event::class.java)"
    echo
    echo "    // After (kotlinx.serialization):"
    echo "    @Serializable"
    echo "    data class Event(...)"
    echo "    val event = Json.decodeFromString<Event>(json)"
    echo
    SUGGESTIONS_FOUND=$((SUGGESTIONS_FOUND + 1))
else
    echo -e "${GREEN}✓ Not using Jackson (or already using kotlinx.serialization)${NC}"
fi

# Check for OkHttp (suggest ktor)
echo
echo "📦 Checking for OkHttp..."
if grep -r "okhttp" */build.gradle.kts 2>/dev/null | grep -q "implementation\|api"; then
    echo -e "${YELLOW}⚠ Found OkHttp dependency${NC}"
    echo "  Current: OkHttp (JVM-only)"
    echo -e "  ${GREEN}Suggest: ktor-client${NC} (works on all platforms)"
    echo
    echo "  Migration:"
    echo "    // Remove:"
    echo "    implementation(libs.okhttp)"
    echo
    echo "    // Add to commonMain:"
    echo "    implementation(libs.ktor.client.core)"
    echo "    // Platform-specific engines:"
    echo "    // androidMain: implementation(libs.ktor.client.android)"
    echo "    // jvmMain: implementation(libs.ktor.client.cio)"
    echo "    // iosMain: implementation(libs.ktor.client.darwin)"
    echo
    echo "    // Code change:"
    echo "    // Before (OkHttp):"
    echo "    val client = OkHttpClient()"
    echo "    val request = Request.Builder().url(url).build()"
    echo "    val response = client.newCall(request).execute()"
    echo
    echo "    // After (ktor):"
    echo "    val client = HttpClient()"
    echo "    val response: String = client.get(url)"
    echo
    SUGGESTIONS_FOUND=$((SUGGESTIONS_FOUND + 1))
else
    echo -e "${GREEN}✓ Not using OkHttp (or already using ktor)${NC}"
fi

# Check for java.time (suggest kotlinx.datetime)
echo
echo "📦 Checking for java.time usage..."
if find */src -name "*.kt" 2>/dev/null | xargs grep -l "import java.time\." >/dev/null 2>&1; then
    echo -e "${YELLOW}⚠ Found java.time imports${NC}"
    echo "  Current: java.time (JVM-only)"
    echo -e "  ${GREEN}Suggest: kotlinx.datetime${NC} (works on all platforms)"
    echo
    echo "  Migration:"
    echo "    // Add to commonMain:"
    echo "    implementation(libs.kotlinx.datetime)"
    echo
    echo "    // Code change:"
    echo "    // Before (java.time):"
    echo "    import java.time.Instant"
    echo "    val now = Instant.now()"
    echo
    echo "    // After (kotlinx.datetime):"
    echo "    import kotlinx.datetime.Clock"
    echo "    val now = Clock.System.now()"
    echo
    SUGGESTIONS_FOUND=$((SUGGESTIONS_FOUND + 1))
else
    echo -e "${GREEN}✓ Not using java.time (or already using kotlinx.datetime)${NC}"
fi

# Check for java.math.BigDecimal
echo
echo "📦 Checking for java.math.BigDecimal usage..."
if find */src -name "*.kt" 2>/dev/null | xargs grep -l "import java.math.BigDecimal" >/dev/null 2>&1; then
    echo -e "${YELLOW}⚠ Found java.math.BigDecimal imports${NC}"
    echo "  Current: java.math.BigDecimal (JVM-only)"
    echo -e "  ${BLUE}Note:${NC} KMP BigDecimal not yet in stable kotlinx"
    echo
    echo "  Options:"
    echo "    1. Use expect/actual (current approach in quartz)"
    echo "    2. Wait for kotlinx.decimal (proposal stage)"
    echo "    3. Use third-party KMP library (e.g., bignum)"
    echo
    SUGGESTIONS_FOUND=$((SUGGESTIONS_FOUND + 1))
else
    echo -e "${GREEN}✓ Not using java.math.BigDecimal directly${NC}"
fi

# Check for platform.posix usage
echo
echo "📦 Checking for platform.posix usage..."
if find */src/commonMain -name "*.kt" 2>/dev/null | xargs grep -l "import platform.posix\." >/dev/null 2>&1; then
    echo -e "${YELLOW}⚠ Found platform.posix in commonMain${NC}"
    echo "  Current: platform.posix (native platforms only, not web)"
    echo -e "  ${GREEN}Suggest:${NC} Abstract file I/O with expect/actual"
    echo
    echo "  For web compatibility:"
    echo "    - iOS/Native: platform.posix"
    echo "    - Web: Use kotlinx-io or ktor file APIs"
    echo "    - Create expect/actual for file operations"
    echo
    SUGGESTIONS_FOUND=$((SUGGESTIONS_FOUND + 1))
else
    echo -e "${GREEN}✓ Not using platform.posix in commonMain${NC}"
fi

# Summary
echo
echo "=== Summary ==="
if [ "$SUGGESTIONS_FOUND" -eq 0 ]; then
    echo -e "${GREEN}✓ No JVM-specific dependencies found!${NC}"
    echo "  Your code is ready for web/wasm targets."
else
    echo -e "${YELLOW}Found $SUGGESTIONS_FOUND suggestion(s) for KMP alternatives${NC}"
    echo
    echo "Priority recommendations:"
    echo "  1. ${GREEN}High:${NC} Jackson → kotlinx.serialization (enables web support)"
    echo "  2. ${GREEN}High:${NC} OkHttp → ktor-client (enables web support)"
    echo "  3. ${GREEN}Medium:${NC} java.time → kotlinx.datetime"
    echo "  4. ${GREEN}Low:${NC} Consider web compatibility for platform.posix usage"
    echo
    echo "Resources:"
    echo "  - kotlinx.serialization: https://github.com/Kotlin/kotlinx.serialization"
    echo "  - ktor: https://ktor.io/docs/client.html"
    echo "  - kotlinx.datetime: https://github.com/Kotlin/kotlinx-datetime"
fi

exit 0
