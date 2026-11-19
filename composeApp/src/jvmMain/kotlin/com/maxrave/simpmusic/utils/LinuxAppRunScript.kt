package com.maxrave.simpmusic.utils

/**
 * Generates the POSIX `sh` `AppRun` entrypoint packaged inside the Linux AppImage
 * (`desktopApp/build.gradle.kts`).
 *
 * Behaviour (#2488):
 * - Honours `DESKTOPINTEGRATION=0` / `false` and `SIMPMUSIC_NO_DESKTOP_FILE=1` so AppImage
 *   managers and portable setups can opt out of self-integration.
 * - Detects when another `.desktop` entry in `${XDG_DATA_HOME:-$HOME/.local/share}/applications`
 *   (such as GearLever or AppImageLauncher) already targets `$APPIMAGE_PATH`, skipping self-managed
 *   icon/desktop writes and removing any stale duplicate `com-maxrave-simpmusic-MainKt.desktop`.
 * - Preserves automatic `com-maxrave-simpmusic-MainKt.desktop` registration (including
 *   `x-scheme-handler/simpmusic` and `x-scheme-handler/wordbyword` OAuth handlers) when the
 *   AppImage is launched standalone without an external manager.
 */
internal object LinuxAppRunScript {
    const val SELF_DESKTOP_FILENAME = "com-maxrave-simpmusic-MainKt.desktop"

    fun escapeDesktopExecPath(appImagePath: String): String {
        val escaped =
            appImagePath
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
        return "\"$escaped\""
    }

    fun shouldSkipDesktopIntegration(
        desktopIntegrationEnv: String?,
        noDesktopFileEnv: String?,
        existingDesktopFiles: Map<String, String>,
        appImagePath: String,
    ): Boolean {
        val normalizedIntegration = desktopIntegrationEnv?.trim()?.lowercase()
        if (normalizedIntegration == "0" || normalizedIntegration == "false" || normalizedIntegration == "no") {
            return true
        }
        val normalizedNoDesktop = noDesktopFileEnv?.trim()?.lowercase()
        if (normalizedNoDesktop == "1" || normalizedNoDesktop == "true" || normalizedNoDesktop == "yes") {
            return true
        }
        return existingDesktopFiles.any { (fileName, content) ->
            fileName != SELF_DESKTOP_FILENAME &&
                fileName.endsWith(".desktop") &&
                appImagePath.isNotEmpty() &&
                content.contains(appImagePath)
        }
    }

    fun generate(): String =
        """#!/bin/sh

SELF=${'$'}(readlink -f "${'$'}0")
HERE=${'$'}{SELF%/*}

DATA_HOME="${'$'}{XDG_DATA_HOME:-${'$'}HOME/.local/share}"
DESKTOP_DIR="${'$'}DATA_HOME/applications"
SELF_DESKTOP="${'$'}DESKTOP_DIR/$SELF_DESKTOP_FILENAME"
APPIMAGE_PATH="${'$'}{APPIMAGE:-${'$'}SELF}"

SKIP_DESKTOP=0
case "${'$'}{DESKTOPINTEGRATION:-}" in
    0|false|FALSE|no|NO) SKIP_DESKTOP=1 ;;
esac
case "${'$'}{SIMPMUSIC_NO_DESKTOP_FILE:-}" in
    1|true|TRUE|yes|YES) SKIP_DESKTOP=1 ;;
esac

EXTERNAL_MANAGED=0
if [ -d "${'$'}DESKTOP_DIR" ] && [ -n "${'$'}APPIMAGE_PATH" ]; then
    for entry in "${'$'}DESKTOP_DIR"/*.desktop; do
        [ -f "${'$'}entry" ] || continue
        [ "${'$'}entry" = "${'$'}SELF_DESKTOP" ] && continue
        if grep -Fq -- "${'$'}APPIMAGE_PATH" "${'$'}entry" 2>/dev/null; then
            EXTERNAL_MANAGED=1
            break
        fi
    done
fi

if [ "${'$'}EXTERNAL_MANAGED" -eq 1 ]; then
    if [ -f "${'$'}SELF_DESKTOP" ]; then
        rm -f "${'$'}SELF_DESKTOP"
        update-desktop-database "${'$'}DESKTOP_DIR" 2>/dev/null || true
    fi
elif [ "${'$'}SKIP_DESKTOP" -eq 0 ]; then
    ICON_DIR="${'$'}DATA_HOME/icons/hicolor/256x256/apps"
    if [ ! -f "${'$'}ICON_DIR/simpmusic.png" ] || [ "${'$'}HERE/simpmusic.png" -nt "${'$'}ICON_DIR/simpmusic.png" ]; then
        mkdir -p "${'$'}ICON_DIR"
        cp "${'$'}HERE/simpmusic.png" "${'$'}ICON_DIR/simpmusic.png"
        gtk-update-icon-cache -f -t "${'$'}DATA_HOME/icons/hicolor" 2>/dev/null || true
    fi

    mkdir -p "${'$'}DESKTOP_DIR"
    ESCAPED_EXEC=${'$'}(printf '%s' "${'$'}APPIMAGE_PATH" | sed 's/\\/\\\\/g; s/"/\\"/g')
    TMP_DESKTOP="${'$'}SELF_DESKTOP.tmp.${'$'}${'$'}"
    if (
        while IFS= read -r line || [ -n "${'$'}line" ]; do
            case "${'$'}line" in
                Exec=bin/simpmusic*) printf 'Exec="%s"%s\n' "${'$'}ESCAPED_EXEC" "${'$'}{line#Exec=bin/simpmusic}" ;;
                *) printf '%s\n' "${'$'}line" ;;
            esac
        done < "${'$'}HERE/simpmusic.desktop" > "${'$'}TMP_DESKTOP"
    ) 2>/dev/null; then
        if [ ! -f "${'$'}SELF_DESKTOP" ] || ! cmp -s "${'$'}TMP_DESKTOP" "${'$'}SELF_DESKTOP"; then
            mv -f "${'$'}TMP_DESKTOP" "${'$'}SELF_DESKTOP"
            update-desktop-database "${'$'}DESKTOP_DIR" 2>/dev/null || true
        else
            rm -f "${'$'}TMP_DESKTOP"
        fi
    else
        rm -f "${'$'}TMP_DESKTOP"
    fi
fi

export MALLOC_ARENA_MAX=2

cd "${'$'}HERE"
exec bin/simpmusic "${'$'}@"
"""
}
