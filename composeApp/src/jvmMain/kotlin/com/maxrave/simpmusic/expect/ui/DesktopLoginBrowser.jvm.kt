package com.maxrave.simpmusic.expect.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.multiplatform.webview.cookie.WebViewCookieManager as DesktopCookieManager
import com.multiplatform.webview.util.addTempDirectoryRemovalHook
import com.maxrave.simpmusic.expect.openUrl
import com.maxrave.simpmusic.ui.theme.typo
import dev.datlag.kcef.KCEF
import dev.datlag.kcef.KCEFBrowser
import dev.datlag.kcef.KCEFClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.cef.callback.CefCommandLine
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.browser.CefRendering
import org.cef.handler.CefLoadHandlerAdapter
import org.cef.handler.CefRenderHandlerAdapter
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.desktop_webview_description
import simpmusic.composeapp.generated.resources.error_occurred
import simpmusic.composeapp.generated.resources.open_blog_post
import java.io.File
import java.awt.Cursor
import java.awt.Graphics
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.HierarchyBoundsAdapter
import java.awt.event.HierarchyEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.Timer
import kotlin.math.roundToInt

private val desktopWebViewRoot =
    File(System.getProperty("user.home"), ".cache/simpmusic/desktop-webview")
private val desktopWebViewRemovalMarker =
    File(System.getProperty("user.home"), ".cache/simpmusic/remove-desktop-webview")

private enum class DesktopWebViewPhase(
    val message: String,
) {
    LOCATING("Checking desktop browser files…"),
    DOWNLOADING("Downloading desktop browser…"),
    EXTRACTING("Extracting desktop browser…"),
    INSTALLING("Finishing desktop browser setup…"),
    INITIALIZING("Starting desktop browser…"),
    READY("Desktop browser ready"),
}

private class LinuxCompatibleCefAppHandler : KCEF.AppHandler() {
    override fun onBeforeCommandLineProcessing(
        processType: String,
        commandLine: CefCommandLine,
    ) {
        super.onBeforeCommandLineProcessing(processType, commandLine)
        if (processType.isEmpty()) {
            commandLine.appendSwitchWithValue("ozone-platform", "x11")
            commandLine.appendSwitch("disable-gpu")
            commandLine.appendSwitch("disable-gpu-compositing")
            commandLine.appendSwitchWithValue(
                "disable-features",
                "Vulkan,WebAuthentication,WebAuthn,WebAuthenticationConditionalUI",
            )
            commandLine.appendSwitchWithValue("disable-blink-features", "WebAuth")
        }
    }
}

private class SoftwareCefPanel : JPanel() {
    private val frameLock = Any()

    @Volatile
    private var displayedFrame: BufferedImage? = null
    private var writableFrame: BufferedImage? = null
    var browser: CefBrowser? = null
    private val resizeTimer =
        Timer(100) {
            browser?.apply {
                notifyScreenInfoChanged()
                wasResized(width.coerceAtLeast(1), height.coerceAtLeast(1))
            }
        }.apply {
            isRepeats = false
        }

    val deviceScaleFactor: Double
        get() =
            graphicsConfiguration
                ?.defaultTransform
                ?.scaleX
                ?.coerceIn(1.0, 4.0)
                ?: 1.0

    init {
        isFocusable = true
        isOpaque = true

        addComponentListener(
            object : ComponentAdapter() {
                override fun componentResized(event: ComponentEvent) {
                    resizeTimer.restart()
                }
            },
        )
        addPropertyChangeListener("graphicsConfiguration") {
            resizeTimer.restart()
        }
        addHierarchyBoundsListener(
            object : HierarchyBoundsAdapter() {
                override fun ancestorMoved(event: HierarchyEvent) {
                    resizeTimer.restart()
                }
            },
        )
        addFocusListener(
            object : FocusAdapter() {
                override fun focusGained(event: FocusEvent) {
                    browser?.setFocus(true)
                }

                override fun focusLost(event: FocusEvent) {
                    browser?.setFocus(false)
                }
            },
        )
        addKeyListener(
            object : KeyAdapter() {
                override fun keyPressed(event: KeyEvent) = browser?.sendKeyEvent(event) ?: Unit
                override fun keyReleased(event: KeyEvent) = browser?.sendKeyEvent(event) ?: Unit
                override fun keyTyped(event: KeyEvent) = browser?.sendKeyEvent(event) ?: Unit
            },
        )
        addMouseListener(
            object : MouseAdapter() {
                override fun mousePressed(event: MouseEvent) {
                    requestFocusInWindow()
                    browser?.sendMouseEvent(event)
                }

                override fun mouseReleased(event: MouseEvent) = browser?.sendMouseEvent(event) ?: Unit
                override fun mouseClicked(event: MouseEvent) = browser?.sendMouseEvent(event) ?: Unit
                override fun mouseEntered(event: MouseEvent) = browser?.sendMouseEvent(event) ?: Unit
                override fun mouseExited(event: MouseEvent) = browser?.sendMouseEvent(event) ?: Unit
            },
        )
        addMouseMotionListener(
            object : MouseMotionAdapter() {
                override fun mouseMoved(event: MouseEvent) = browser?.sendMouseEvent(event) ?: Unit
                override fun mouseDragged(event: MouseEvent) = browser?.sendMouseEvent(event) ?: Unit
            },
        )
        addMouseWheelListener { event -> browser?.sendMouseWheelEvent(event) }
    }

    fun updateFrame(
        buffer: ByteBuffer,
        width: Int,
        height: Int,
    ) {
        if (width <= 0 || height <= 0) return
        val pixelCount = width.toLong() * height.toLong()
        if (pixelCount > Int.MAX_VALUE || buffer.remaining().toLong() < pixelCount * Int.SIZE_BYTES) {
            return
        }
        synchronized(frameLock) {
            val image =
                writableFrame?.takeIf { it.width == width && it.height == height }
                    ?: BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
            val pixels = (image.raster.dataBuffer as DataBufferInt).data
            buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().get(pixels, 0, pixels.size)
            writableFrame = displayedFrame?.takeIf { it.width == width && it.height == height }
            displayedFrame = image
        }
        SwingUtilities.invokeLater(::repaint)
    }

    fun dispose() {
        resizeTimer.stop()
        browser = null
        synchronized(frameLock) {
            displayedFrame = null
            writableFrame = null
        }
    }

    override fun paintComponent(graphics: Graphics) {
        super.paintComponent(graphics)
        displayedFrame?.let { graphics.drawImage(it, 0, 0, width, height, null) }
    }
}

private class SoftwareCefRenderHandler(
    private val panel: SoftwareCefPanel,
) : CefRenderHandlerAdapter() {
    override fun getDeviceScaleFactor(browser: CefBrowser): Double = panel.deviceScaleFactor

    override fun getViewRect(browser: CefBrowser): Rectangle =
        Rectangle(0, 0, panel.width.coerceAtLeast(1), panel.height.coerceAtLeast(1))

    override fun getScreenPoint(
        browser: CefBrowser,
        viewPoint: Point,
    ): Point =
        runCatching {
            Point(viewPoint).also { javax.swing.SwingUtilities.convertPointToScreen(it, panel) }
        }.getOrDefault(viewPoint)

    override fun onPaint(
        browser: CefBrowser,
        popup: Boolean,
        dirtyRects: Array<out Rectangle>,
        buffer: ByteBuffer,
        width: Int,
        height: Int,
    ) {
        if (!popup) panel.updateFrame(buffer, width, height)
    }

    override fun onCursorChange(
        browser: CefBrowser,
        cursorType: Int,
    ): Boolean =
        runCatching {
            panel.cursor = Cursor.getPredefinedCursor(cursorType)
            true
        }.getOrDefault(false)
}

private class DesktopBrowserSession(
    initUrl: String,
    private val state: MutableState<WebViewState>,
    private val onPageFinished: (String) -> Unit,
) {
    private val client: KCEFClient = KCEF.newClientBlocking()
    private val panel = SoftwareCefPanel()
    val browser: KCEFBrowser

    init {
        client.addLoadHandler(
            object : CefLoadHandlerAdapter() {
                override fun onLoadingStateChange(
                    browser: CefBrowser,
                    isLoading: Boolean,
                    canGoBack: Boolean,
                    canGoForward: Boolean,
                ) {
                    if (isLoading) state.value = WebViewState.Loading(0)
                }

                override fun onLoadEnd(
                    browser: CefBrowser,
                    frame: CefFrame,
                    httpStatusCode: Int,
                ) {
                    if (frame.isMain) {
                        state.value = WebViewState.Finished
                        frame.url?.takeIf { it.isNotBlank() }?.let(onPageFinished)
                    }
                }
            },
        )
        val rendering = CefRendering.CefRenderingWithHandler(SoftwareCefRenderHandler(panel), panel)
        browser = client.createBrowser(initUrl, rendering, false)
        panel.browser = browser
        browser.setWindowlessFrameRate(24)
        browser.createImmediately()
    }

    fun dispose() {
        panel.dispose()
        browser.dispose()
        client.dispose()
    }
}

@Composable
private fun DesktopCefWebView(
    initUrl: String,
    state: MutableState<WebViewState>,
    onPageFinished: (String) -> Unit,
) {
    val session = remember(initUrl) { DesktopBrowserSession(initUrl, state, onPageFinished) }
    DisposableEffect(session) {
        onDispose { session.dispose() }
    }
    SwingPanel(
        factory = { session.browser.uiComponent },
        modifier = Modifier.fillMaxSize(),
    )
}

private object DesktopWebViewRuntime {
    val downloadProgress = mutableFloatStateOf(0f)
    val phase = mutableStateOf(DesktopWebViewPhase.LOCATING)
    val failure = mutableStateOf<String?>(null)
    val restartRequired = mutableStateOf(false)

    private val initializationMutex = Mutex()
    private val shutdownHookInstalled = AtomicBoolean(false)

    @Volatile
    var isReady: Boolean = false
        private set

    suspend fun prepareLoginSession() {
        initialize()
    }

    suspend fun clearStorage(): DesktopLoginBrowserClearResult =
        withContext(Dispatchers.IO) {
            if (isReady) {
                val markerParent = desktopWebViewRemovalMarker.parentFile
                if ((markerParent == null || markerParent.exists() || markerParent.mkdirs()) &&
                    runCatching { desktopWebViewRemovalMarker.createNewFile() }.isSuccess
                ) {
                    DesktopLoginBrowserClearResult.SCHEDULED
                } else {
                    DesktopLoginBrowserClearResult.FAILED
                }
            } else if (deleteDesktopWebViewStorage()) {
                DesktopLoginBrowserClearResult.CLEARED
            } else {
                DesktopLoginBrowserClearResult.FAILED
            }
        }

    private suspend fun initialize() {
        if (isReady) return
        initializationMutex.withLock {
            if (isReady) return
            failure.value = null
            restartRequired.value = false

            withContext(Dispatchers.IO) {
                removeDesktopWebViewStorageIfPending()
                val isLinux = System.getProperty("os.name").orEmpty().contains("linux", ignoreCase = true)
                addTempDirectoryRemovalHook()
                KCEF.init(
                    builder = {
                        installDir(File(desktopWebViewRoot, "runtime"))
                        extractBuffer(1024 * 1024)
                        if (isLinux) {
                            appHandler(LinuxCompatibleCefAppHandler())
                        }
                        progress {
                            onLocating { phase.value = DesktopWebViewPhase.LOCATING }
                            onDownloading {
                                phase.value = DesktopWebViewPhase.DOWNLOADING
                                downloadProgress.floatValue = it.coerceAtLeast(0f)
                            }
                            onExtracting { phase.value = DesktopWebViewPhase.EXTRACTING }
                            onInstall { phase.value = DesktopWebViewPhase.INSTALLING }
                            onInitializing { phase.value = DesktopWebViewPhase.INITIALIZING }
                            onInitialized {
                                isReady = true
                                phase.value = DesktopWebViewPhase.READY
                            }
                        }
                        settings {
                            cachePath = File(desktopWebViewRoot, "browser-cache").absolutePath
                        }
                    },
                    onError = { throwable ->
                        failure.value = throwable?.message ?: "Could not initialize the desktop browser"
                    },
                    onRestartRequired = {
                        restartRequired.value = true
                    },
                )

                if (isReady && shutdownHookInstalled.compareAndSet(false, true)) {
                    Runtime.getRuntime().addShutdownHook(
                        Thread(
                            {
                                runCatching { KCEF.disposeBlocking() }
                                removeDesktopWebViewStorageIfPending()
                            },
                            "simpmusic-webview-shutdown",
                        ),
                    )
                }
            }
        }
    }
}

private fun removeDesktopWebViewStorageIfPending() {
    if (desktopWebViewRemovalMarker.exists() && deleteDesktopWebViewStorage()) {
        desktopWebViewRemovalMarker.delete()
    }
}

private fun deleteDesktopWebViewStorage(): Boolean =
    runCatching {
        !desktopWebViewRoot.exists() || desktopWebViewRoot.deleteRecursively()
    }.getOrDefault(false)

actual object DesktopLoginBrowserManager {
    actual suspend fun warmUp() {
        DesktopWebViewRuntime.prepareLoginSession()
    }

    actual suspend fun getStorage(): DesktopLoginBrowserStorage =
        withContext(Dispatchers.IO) {
            runCatching {
                DesktopLoginBrowserStorage(
                    bytes =
                        desktopWebViewRoot
                            .walkBottomUp()
                            .filter(File::isFile)
                            .sumOf(File::length),
                    removalPending = desktopWebViewRemovalMarker.exists(),
                )
            }.getOrElse {
                DesktopLoginBrowserStorage(removalPending = desktopWebViewRemovalMarker.exists())
            }
        }

    actual suspend fun clear(): DesktopLoginBrowserClearResult =
        DesktopWebViewRuntime.clearStorage()
}

private val desktopCookieScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

actual fun createWebViewCookieManager(): WebViewCookieManager =
    object : WebViewCookieManager {
        override fun getCookie(url: String): String {
            if (!DesktopWebViewRuntime.isReady) return ""
            return runCatching {
                runBlocking(Dispatchers.IO) {
                    DesktopCookieManager()
                        .getCookies(url)
                        .joinToString("; ") { cookie -> "${cookie.name}=${cookie.value}" }
                }
            }.getOrDefault("")
        }

        override fun removeAllCookies() {
            if (!DesktopWebViewRuntime.isReady) return
            desktopCookieScope.launch {
                runCatching {
                    DesktopCookieManager().removeAllCookies()
                }
            }
        }
    }

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
actual fun PlatformWebView(
    state: MutableState<WebViewState>,
    initUrl: String,
    aboveContent: @Composable (BoxScope.() -> Unit),
    onPageFinished: (String) -> Unit,
) {
    val currentOnPageFinished by rememberUpdatedState(onPageFinished)
    val downloadProgress by DesktopWebViewRuntime.downloadProgress
    val phase by DesktopWebViewRuntime.phase
    val failure by DesktopWebViewRuntime.failure
    val restartRequired by DesktopWebViewRuntime.restartRequired

    LaunchedEffect(initUrl) {
        DesktopWebViewRuntime.prepareLoginSession()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        when {
            phase == DesktopWebViewPhase.READY -> {
                DesktopCefWebView(
                    initUrl = initUrl,
                    state = state,
                    onPageFinished = { currentOnPageFinished(it) },
                )
            }

            restartRequired -> {
                Text(
                    "Restart SimpMusic to finish setting up the desktop login browser.",
                    style = typo().labelMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp),
                )
            }

            failure != null -> {
                Text(
                    "${stringResource(Res.string.error_occurred)}: $failure",
                    style = typo().labelMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp),
                )
            }

            else -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text(
                        if (phase == DesktopWebViewPhase.DOWNLOADING) {
                            "${phase.message} ${downloadProgress.roundToInt()}%"
                        } else {
                            phase.message
                        },
                        style = typo().labelMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        }
        aboveContent()
    }
}

@Composable
actual fun DiscordWebView(
    state: MutableState<WebViewState>,
    aboveContent: @Composable (BoxScope.() -> Unit),
    onLoginDone: (token: String) -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(Res.string.desktop_webview_description),
                style = typo().labelMedium,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = {
                    openUrl("https://www.simpmusic.org/blogs/en/how-to-log-in-to-Discord-on-desktop-app")
                },
            ) {
                Text(
                    stringResource(Res.string.open_blog_post),
                    style = typo().labelMedium,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center
                )
            }
        }
        aboveContent()
    }
}
