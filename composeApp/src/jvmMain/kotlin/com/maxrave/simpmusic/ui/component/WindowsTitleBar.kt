package com.maxrave.simpmusic.ui.component

import androidx.compose.ui.graphics.Color
import com.maxrave.logger.Logger
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.NativeLibrary
import com.sun.jna.Pointer
import java.awt.Window
import javax.swing.SwingUtilities

/**
 * Manages native Windows title bar appearance using the Desktop Window Manager (DWM) API via JNA.
 *
 * Keeps the window native (decorated) with Aero Snap, Snap Layouts, resize handles, and shadow,
 * while matching the app's dark or light theme.
 *
 * Supported DWM attributes:
 * - DWMWA_USE_IMMERSIVE_DARK_MODE (20, Windows 10 20H1+ and Windows 11; fallback 19 on Windows 10 1809–1909)
 * - DWMWA_CAPTION_COLOR (35, Windows 11 build >= 22000)
 * - DWMWA_TEXT_COLOR (36, Windows 11 build >= 22000)
 *
 * Safe no-op on non-Windows platforms or when native calls fail.
 */
object WindowsTitleBar {
    private const val TAG = "WindowsTitleBar"

    private const val DWMWA_USE_IMMERSIVE_DARK_MODE_BEFORE_20H1 = 19
    private const val DWMWA_USE_IMMERSIVE_DARK_MODE = 20
    private const val DWMWA_CAPTION_COLOR = 35
    private const val DWMWA_TEXT_COLOR = 36

    /** Special sentinel in Windows DWM to reset caption/text color to system default. */
    private const val DWMWA_COLOR_DEFAULT = 0xFFFFFFFF.toInt()

    private const val SWP_NOSIZE = 0x0001
    private const val SWP_NOMOVE = 0x0002
    private const val SWP_NOZORDER = 0x0004
    private const val SWP_NOACTIVATE = 0x0010
    private const val SWP_FRAMECHANGED = 0x0020

    private val isWindows: Boolean =
        System.getProperty("os.name", "").contains("Windows", ignoreCase = true)

    private class Natives {
        private val dwmapi = NativeLibrary.getInstance("dwmapi")
        private val user32 = NativeLibrary.getInstance("user32")

        val dwmSetWindowAttribute = dwmapi.getFunction("DwmSetWindowAttribute")
        val setWindowPos = user32.getFunction("SetWindowPos")
    }

    private val natives: Natives? by lazy {
        if (!isWindows) return@lazy null
        runCatching { Natives() }
            .onFailure { Logger.w(TAG, "Native DWM/User32 library unavailable: $it") }
            .getOrNull()
    }

    private val windowsBuildNumber: Int by lazy {
        detectWindowsBuildNumber()
    }

    private fun detectWindowsBuildNumber(): Int {
        if (!isWindows) return 0
        return runCatching {
            val ntdll = NativeLibrary.getInstance("ntdll")
            val rtlGetVersion = ntdll.getFunction("RtlGetVersion")
            // RTL_OSVERSIONINFOW: 4 + 4 + 4 + 4 + 4 + 256 = 276 bytes
            val mem = Memory(276)
            mem.setInt(0, 276)
            val status = rtlGetVersion.invokeInt(arrayOf(mem))
            if (status == 0) {
                // dwBuildNumber is at offset 12
                val build = mem.getInt(12)
                Logger.d(TAG, "Detected Windows build: $build")
                build
            } else {
                fallbackBuildNumber()
            }
        }.getOrElse {
            fallbackBuildNumber()
        }
    }

    private fun fallbackBuildNumber(): Int {
        val osName = System.getProperty("os.name", "")
        return if (osName.contains("Windows 11", ignoreCase = true)) {
            22000
        } else {
            0
        }
    }

    /**
     * Converts a Compose [Color] to Win32 COLORREF format (0x00BBGGRR).
     */
    private fun Color.toColorRef(): Int {
        val r = (red * 255f).toInt().coerceIn(0, 255)
        val g = (green * 255f).toInt().coerceIn(0, 255)
        val b = (blue * 255f).toInt().coerceIn(0, 255)
        return r or (g shl 8) or (b shl 16)
    }

    /**
     * Applies dark or light title bar theme to the specified native [window].
     *
     * @param window The AWT window holding the native frame.
     * @param isDark True for dark title bar, false for light.
     * @param captionColor Optional title bar background color on Windows 11 (build >= 22000).
     * @param textColor Optional title bar text color on Windows 11 (build >= 22000).
     */
    fun applyTheme(
        window: Window,
        isDark: Boolean,
        captionColor: Color? = null,
        textColor: Color? = null,
    ) {
        if (!isWindows) return
        val n = natives ?: return

        if (!window.isDisplayable) {
            SwingUtilities.invokeLater {
                applyTheme(window, isDark, captionColor, textColor)
            }
            return
        }

        val hwnd = Native.getComponentPointer(window) ?: Native.getWindowPointer(window)
        if (hwnd == null || Pointer.nativeValue(hwnd) == 0L) {
            Logger.w(TAG, "Cannot get HWND for window: $window")
            return
        }

        runCatching {
            // 1. Set DWMWA_USE_IMMERSIVE_DARK_MODE (20, fallback 19)
            val darkModeVal = Memory(4).apply { setInt(0, if (isDark) 1 else 0) }
            var hr = n.dwmSetWindowAttribute.invokeInt(
                arrayOf(hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE, darkModeVal, 4)
            )
            if (hr != 0) {
                // Fallback for older Windows 10 builds (1809 - 1909)
                hr = n.dwmSetWindowAttribute.invokeInt(
                    arrayOf(hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE_BEFORE_20H1, darkModeVal, 4)
                )
                if (hr != 0) {
                    Logger.w(TAG, "DwmSetWindowAttribute IMMERSIVE_DARK_MODE returned hr=$hr")
                }
            }

            // 2. Windows 11 (build >= 22000): DWMWA_CAPTION_COLOR and DWMWA_TEXT_COLOR
            if (windowsBuildNumber >= 22000) {
                val captionColorRef = if (isDark) {
                    captionColor?.toColorRef() ?: Color.Black.toColorRef()
                } else {
                    captionColor?.toColorRef() ?: DWMWA_COLOR_DEFAULT
                }
                val captionMem = Memory(4).apply { setInt(0, captionColorRef) }
                val hrCaption = n.dwmSetWindowAttribute.invokeInt(
                    arrayOf(hwnd, DWMWA_CAPTION_COLOR, captionMem, 4)
                )
                if (hrCaption != 0) {
                    Logger.d(TAG, "DwmSetWindowAttribute DWMWA_CAPTION_COLOR returned hr=$hrCaption")
                }

                val textColorRef = if (isDark) {
                    textColor?.toColorRef() ?: Color.White.toColorRef()
                } else {
                    textColor?.toColorRef() ?: DWMWA_COLOR_DEFAULT
                }
                val textMem = Memory(4).apply { setInt(0, textColorRef) }
                val hrText = n.dwmSetWindowAttribute.invokeInt(
                    arrayOf(hwnd, DWMWA_TEXT_COLOR, textMem, 4)
                )
                if (hrText != 0) {
                    Logger.d(TAG, "DwmSetWindowAttribute DWMWA_TEXT_COLOR returned hr=$hrText")
                }
            }

            // 3. Force non-client repaint so the title bar updates immediately
            val flags = SWP_FRAMECHANGED or SWP_NOMOVE or SWP_NOSIZE or SWP_NOZORDER or SWP_NOACTIVATE
            n.setWindowPos.invokeInt(
                arrayOf(hwnd, Pointer.NULL, 0, 0, 0, 0, flags)
            )
            Logger.d(TAG, "Successfully applied title bar theme: isDark=$isDark")
        }.onFailure { e ->
            Logger.w(TAG, "Failed to apply Windows title bar theme: $e")
        }
    }
}
