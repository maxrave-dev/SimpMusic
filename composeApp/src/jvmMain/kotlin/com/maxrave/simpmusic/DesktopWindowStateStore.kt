package com.maxrave.simpmusic

import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import java.awt.GraphicsEnvironment
import java.io.File
import java.util.Properties

data class PersistedWindowState(
    val width: Float = 1200f,
    val height: Float = 750f,
    val x: Float? = null,
    val y: Float? = null,
    val isMaximized: Boolean = false,
)

object DesktopWindowStateStore {
    private val configFile: File by lazy {
        File(System.getProperty("user.home"), ".simpmusic/window_state.properties")
    }

    private var cachedFloatingWidth: Float = 1200f
    private var cachedFloatingHeight: Float = 750f
    private var cachedFloatingX: Float? = null
    private var cachedFloatingY: Float? = null

    fun load(): PersistedWindowState {
        val loaded =
            runCatching {
                if (!configFile.exists()) return@runCatching PersistedWindowState()
                val props = Properties()
                configFile.inputStream().use { props.load(it) }
                val w = props.getProperty("width")?.toFloatOrNull() ?: 1200f
                val h = props.getProperty("height")?.toFloatOrNull() ?: 750f
                val x = props.getProperty("x")?.toFloatOrNull()
                val y = props.getProperty("y")?.toFloatOrNull()
                val max = props.getProperty("isMaximized")?.toBooleanStrictOrNull() ?: false
                PersistedWindowState(width = w, height = h, x = x, y = y, isMaximized = max)
            }.getOrDefault(PersistedWindowState())

        val screenBounds =
            runCatching {
                GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds
            }.getOrNull()

        val safeWidth =
            if (screenBounds != null) {
                loaded.width.coerceIn(800f, screenBounds.width.toFloat().coerceAtLeast(800f))
            } else {
                loaded.width.coerceIn(800f, 1600f)
            }

        val safeHeight =
            if (screenBounds != null) {
                loaded.height.coerceIn(500f, screenBounds.height.toFloat().coerceAtLeast(500f))
            } else {
                loaded.height.coerceIn(500f, 1000f)
            }

        val safeX =
            if (screenBounds != null && loaded.x != null) {
                if (loaded.x < 0 || loaded.x > screenBounds.width - 100) null else loaded.x
            } else {
                loaded.x
            }

        val safeY =
            if (screenBounds != null && loaded.y != null) {
                if (loaded.y < 0 || loaded.y > screenBounds.height - 100) null else loaded.y
            } else {
                loaded.y
            }

        cachedFloatingWidth = safeWidth
        cachedFloatingHeight = safeHeight
        cachedFloatingX = safeX
        cachedFloatingY = safeY

        return PersistedWindowState(
            width = safeWidth,
            height = safeHeight,
            x = safeX,
            y = safeY,
            isMaximized = loaded.isMaximized,
        )
    }

    fun updateFloatingBounds(
        width: Float,
        height: Float,
        x: Float?,
        y: Float?,
    ) {
        val screenBounds =
            runCatching {
                GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds
            }.getOrNull()

        // Do not record dimensions if the window is maximized or fills the screen
        if (screenBounds != null) {
            if (width >= screenBounds.width && height >= screenBounds.height) {
                return
            }
            if (x != null && (x < 0 || x > screenBounds.width - 100)) return
            if (y != null && (y < 0 || y > screenBounds.height - 100)) return
        }

        if (width >= 400f) cachedFloatingWidth = width
        if (height >= 300f) cachedFloatingHeight = height
        if (x != null) cachedFloatingX = x
        if (y != null) cachedFloatingY = y
    }

    fun getFloatingSize(): DpSize = DpSize(cachedFloatingWidth.dp, cachedFloatingHeight.dp)

    fun getFloatingPosition(): WindowPosition {
        val x = cachedFloatingX
        val y = cachedFloatingY
        return if (x != null && y != null) {
            WindowPosition(x.dp, y.dp)
        } else {
            WindowPosition.Aligned(Alignment.Center)
        }
    }

    fun save(isMaximized: Boolean) {
        runCatching {
            configFile.parentFile?.mkdirs()
            val props = Properties()
            props.setProperty("width", cachedFloatingWidth.toString())
            props.setProperty("height", cachedFloatingHeight.toString())
            cachedFloatingX?.let { props.setProperty("x", it.toString()) }
            cachedFloatingY?.let { props.setProperty("y", it.toString()) }
            props.setProperty("isMaximized", isMaximized.toString())
            configFile.outputStream().use { props.store(it, "SimpMusic Window State") }
        }
    }
}
