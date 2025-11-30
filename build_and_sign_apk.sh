#!/bin/bash

# Exit on error
set -e

# Default variables
BUILD_TYPE="release"
BUILD_VARIANT="full"
KEYSTORE_PATH="./simpmusic.jks"
# Read passwords from environment variables or use default (for backward compatibility)
KEYSTORE_PASSWORD="${KEYSTORE_PASSWORD}"
KEY_ALIAS="${KEY_ALIAS}"
KEY_PASSWORD="${KEY_PASSWORD}"

# Check if KEY_PASSWORD is set
if [ -z "$KEY_PASSWORD" ]; then
  echo "Error: KEY_PASSWORD environment variable must be set"
  exit 1
fi

if [ -z "$KEYSTORE_PASSWORD" ]; then
  echo "Error: KEYSTORE_PASSWORD environment variable must be set"
  exit 1
fi

if [ -z "$KEY_ALIAS" ]; then
  echo "Error: KEY_ALIAS environment variable must be set"
  exit 1
fi



# Parse command line arguments
print_usage() {
  echo "Usage: $0 [options]"
  echo "Options:"
  echo "  --release          Build in release mode (default)"
  echo "  --debug            Build in debug mode"
  echo "  --full             Build full with Sentry"
  echo "  --foss             Build foss, compatibility with F-Droid, no Sentry"
  echo "  -h, --help         Show this help message"
  echo ""
  echo "Environment variables:"
  echo "  KEYSTORE_PASSWORD  Password for the keystore"
  echo "  KEY_PASSWORD       Password for the key (required, no default)"
  echo "  KEY_ALIAS          Alias for the key (required, no default)"
  exit 0
}

# Process command line arguments
while [[ "$#" -gt 0 ]]; do
  case $1 in
    --full) BUILD_VARIANT="full" ;;
    --foss) BUILD_VARIANT="foss" ;;
    --release) BUILD_TYPE="release" ;;
    --debug) BUILD_TYPE="debug" ;;
    -h|--help) print_usage ;;
    *) echo "Unknown parameter: $1"; print_usage ;;
  esac
  shift
done

# Set derived variables based on selected options
APK_OUTPUT_DIR="./composeApp/build/outputs/apk/$BUILD_TYPE"
SIGNED_APK_OUTPUT_DIR="./composeApp/build/outputs/apk/$BUILD_TYPE"

# Android build-tools path
BUILD_TOOLS_PATH="$ANDROID_HOME/build-tools/$(ls $ANDROID_HOME/build-tools | sort | tail -n 1)"
APKSIGNER="$BUILD_TOOLS_PATH/apksigner"
ZIPALIGN="$BUILD_TOOLS_PATH/zipalign"

# Create output directory for signed APKs
mkdir -p "$SIGNED_APK_OUTPUT_DIR"

# Log the start of the process
echo "===================="
echo "Building APK Process"
echo "===================="
echo "Build Type: $BUILD_TYPE"
echo "===================="

# Step 1: Clean the project
echo "[Step 1] Cleaning the project..."
./gradlew clean
echo "Project cleaned successfully."

# Step 2: Build the APK
echo "[Step 2] Building APK..."
./gradlew composeApp:assemble"$BUILD_TYPE"
echo "APK built successfully."

# Step 3: Locate the built APKs
APK_PATHS=$(find "$APK_OUTPUT_DIR" -name "*.apk")
if [ -z "$APK_PATHS" ]; then
  echo "Error: APKs not found in $APK_OUTPUT_DIR"
  exit 1
fi
echo "Built APKs located: $APK_PATHS"

# Step 4: Align and sign each APK
for APK_PATH in $APK_PATHS; do
  ALIGNED_APK_PATH="$SIGNED_APK_OUTPUT_DIR/aligned-$(basename "${APK_PATH/-unsigned/}")"
  RELEASE_NAME=$(basename "${APK_PATH/-unsigned/}")
  RELEASE_NAME="${RELEASE_NAME/app-/}"
  RELEASE_NAME="${RELEASE_NAME/composeApp-/}"
  SIGNED_APK_PATH="$SIGNED_APK_OUTPUT_DIR/SimpMusic-$BUILD_VARIANT-$(basename "$RELEASE_NAME")"

  echo "[Step 4] Aligning the APK: $APK_PATH..."
  if [ ! -f "$ZIPALIGN" ]; then
    echo "Error: zipalign tool not found in Android SDK."
    exit 1
  fi
  "$ZIPALIGN" -v 4 "$APK_PATH" "$ALIGNED_APK_PATH"
  echo "APK aligned and saved to: $ALIGNED_APK_PATH"

  echo "[Step 5] Signing the APK: $ALIGNED_APK_PATH..."
  "$APKSIGNER" sign \
    --alignment-preserved \
    --ks "$KEYSTORE_PATH" \
    --ks-key-alias "$KEY_ALIAS" \
    --ks-pass pass:"$KEYSTORE_PASSWORD" \
    --key-pass pass:"$KEY_PASSWORD" \
    --out "$SIGNED_APK_PATH" \
    "$ALIGNED_APK_PATH"
  echo "APK signed successfully: $SIGNED_APK_PATH"

  echo "[Step 6] Verifying the signed APK: $SIGNED_APK_PATH..."
  "$APKSIGNER" verify --verbose "$SIGNED_APK_PATH"
  echo "Signed APK verified successfully: $SIGNED_APK_PATH"
done

echo "[Step 7] Cleaning up temporary files..."
cd "$SIGNED_APK_OUTPUT_DIR"
rm -f *.idsig
rm -f *aligned*
rm -f *unsigned*

# Completion message
echo "===================="
echo "Process Completed Successfully!"
echo "Signed APKs Path: $SIGNED_APK_OUTPUT_DIR"
echo "===================="