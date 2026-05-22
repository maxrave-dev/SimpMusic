#!/usr/bin/env bash
# wrap-mac-dmg.sh — wrap a Conveyor-built SimpMusic.app into a polished .dmg.
#
# Why this script exists:
#  • Conveyor 22.0 ships Mac apps as a plain `.zip`. On macOS 15 Sequoia,
#    quarantined .zip → extracted .app triggers Gatekeeper "developer cannot
#    be verified" hard-block (no right-click Open shortcut anymore).
#  • The v1.2.1 jpackage release shipped as `.dmg`; macOS treats `.app`s
#    propagated through a mounted DMG with a softer quarantine flag (`0381`
#    instead of `0181`), so the app opens with the normal first-launch
#    dialog instead of being blocked outright.
#  • This script reproduces that user experience by wrapping Conveyor's
#    extracted `.app` in a DMG with the SimpMusic background, drag-to-
#    Applications symlink, custom volume icon, and 192 px icons positioned
#    at the arrow endpoints in the background.
#
# Pipeline:
#  1. create-dmg (UDZO + window/icon layout + Applications drop-link)
#  2. hdiutil convert UDZO → UDRW (writable for the volume-icon step)
#  3. Mount UDRW, copy `.VolumeIcon.icns` to root, set CustomIcon + push the
#     icon outside the visible window via AppleScript so it doesn't appear
#     as a stray file in Finder when the user has "Show hidden files" on.
#  4. hdiutil convert UDRW → UDZO (final compressed image)
#
# Usage:
#   scripts/wrap-mac-dmg.sh \
#       <input-app>         e.g. /tmp/SimpMusic.app
#       <background-png>    e.g. composeApp/icon/dmg-bg-1400x800.png
#       <volume-icns>       e.g. composeApp/icon/circle_app_icon.icns
#       <output-dmg>        e.g. dist/SimpMusic-1.2.1-mac-aarch64.dmg
#
# Requires (macOS host):  create-dmg, hdiutil, SetFile (xcode-select),
#                         osascript, chflags.

set -euo pipefail

if [[ $# -ne 4 ]]; then
  echo "Usage: $0 <input-app> <background-png> <volume-icns> <output-dmg>" >&2
  exit 64
fi

INPUT_APP="$1"
BG_PNG="$2"
VOL_ICNS="$3"
OUT_DMG="$4"

[[ -d "$INPUT_APP" ]]  || { echo "Input .app not found: $INPUT_APP" >&2;  exit 1; }
[[ -f "$BG_PNG" ]]     || { echo "Background not found: $BG_PNG" >&2;     exit 1; }
[[ -f "$VOL_ICNS" ]]   || { echo "Volume icon not found: $VOL_ICNS" >&2;  exit 1; }

command -v create-dmg >/dev/null \
  || { echo "create-dmg not installed. brew install create-dmg" >&2; exit 1; }

SETFILE="$(xcrun -find SetFile 2>/dev/null || echo /usr/bin/SetFile)"
[[ -x "$SETFILE" ]] \
  || { echo "SetFile not available; install Xcode command-line tools" >&2; exit 1; }

# Build staging dir so the source .app is not modified in place and
# Finder's icon-extension-hidden flag survives create-dmg's mount cycle.
STAGING="$(mktemp -d -t simpmusic-dmg-staging)"
trap 'rm -rf "$STAGING"' EXIT
cp -R "$INPUT_APP" "$STAGING/"

mkdir -p "$(dirname "$OUT_DMG")"
TMP_DMG="$(mktemp -u "$(dirname "$OUT_DMG")/.simpmusic-final-XXXX").dmg"
RW_DMG="$(mktemp -u "$(dirname "$OUT_DMG")/.simpmusic-rw-XXXX").dmg"

echo "[wrap-mac-dmg] create-dmg → $TMP_DMG"
create-dmg \
  --volname "SimpMusic" \
  --window-pos 200 120 \
  --window-size 1400 800 \
  --icon-size 192 \
  --background "$BG_PNG" \
  --icon "$(basename "$INPUT_APP")" 350 380 \
  --hide-extension "$(basename "$INPUT_APP")" \
  --app-drop-link 1080 360 \
  "$TMP_DMG" "$STAGING"

echo "[wrap-mac-dmg] convert UDZO → UDRW"
hdiutil convert "$TMP_DMG" -format UDRW -o "$RW_DMG" >/dev/null
rm -f "$TMP_DMG"

echo "[wrap-mac-dmg] mount + apply volume icon"
hdiutil attach -nobrowse -quiet "$RW_DMG"
cp "$VOL_ICNS" "/Volumes/SimpMusic/.VolumeIcon.icns"
# Apple's Finder shows files with `chflags hidden` to users who have
# the default "hide dotfiles" setting. For users who enabled "Show all
# files" (Cmd+Shift+.), nudge the icon position outside the 1400x800
# viewport so it still doesn't visually clutter the install window.
"$SETFILE" -a V "/Volumes/SimpMusic/.VolumeIcon.icns"
osascript -e \
  'tell application "Finder" to set position of file ".VolumeIcon.icns" of disk "SimpMusic" to {2000, 2000}' \
  >/dev/null 2>&1 || true
chflags hidden "/Volumes/SimpMusic/.VolumeIcon.icns"
"$SETFILE" -a C "/Volumes/SimpMusic"
sync
sleep 2
hdiutil detach -quiet "/Volumes/SimpMusic"

echo "[wrap-mac-dmg] convert UDRW → UDZO → $OUT_DMG"
rm -f "$OUT_DMG"
hdiutil convert "$RW_DMG" -format UDZO -o "$OUT_DMG" >/dev/null
rm -f "$RW_DMG"

ls -lh "$OUT_DMG"
echo "[wrap-mac-dmg] done."
