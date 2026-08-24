package com.maxrave.simpmusic

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import coil3.ImageLoader
import coil3.compose.LocalPlatformContext
import coil3.compose.setSingletonImageLoaderFactory
import coil3.disk.DiskCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.CachePolicy
import coil3.request.crossfade
import com.kdroid.composetray.tray.api.Tray
import com.kdroid.composetray.utils.SingleInstanceManager
import com.maxrave.data.di.loader.loadAllModules
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.domain.mediaservice.handler.ToastType
import com.maxrave.simpmusic.di.viewModelModule
import com.maxrave.simpmusic.extension.DesktopWindowChrome
import com.maxrave.simpmusic.ui.component.CustomTitleBar
import com.maxrave.simpmusic.ui.mini_player.MiniPlayerManager
import com.maxrave.simpmusic.ui.mini_player.MiniPlayerWindow
import com.maxrave.simpmusic.ui.theme.isDarkTheme
import com.maxrave.simpmusic.utils.VersionManager
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.changeLanguageNative
import io.sentry.Sentry
import io.sentry.SentryLevel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import multiplatform.network.cmptoast.ToastHost
import multiplatform.network.cmptoast.showToast
import okhttp3.OkHttpClient
import okio.FileSystem
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.java.KoinJavaComponent.inject
import org.koin.mp.KoinPlatform.getKoin
import org.simpmusic.lastfm.configLastfm
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.app_name
import simpmusic.composeapp.generated.resources.circle_app_icon
import simpmusic.composeapp.generated.resources.close_miniplayer
import simpmusic.composeapp.generated.resources.explicit_content_blocked
import simpmusic.composeapp.generated.resources.open_app
import simpmusic.composeapp.generated.resources.open_miniplayer
import simpmusic.composeapp.generated.resources.quit_app
import simpmusic.composeapp.generated.resources.time_out_check_internet_connection_or_change_piped_instance_in_settings

/**
 * Any `scheme://…` command-line argument. RFC 3986 §3.1 allows ALPHA followed by
 * ALPHA / DIGIT / "+" / "-" / ".".
 */
private val DEEP_LINK_ARG = Regex("^[A-Za-z][A-Za-z0-9+.\\-]*://.+")

@OptIn(ExperimentalMaterial3Api::class)
fun runDesktopApp(args: Array<String> = emptyArray()) {
    // Install crash dialog handler first — catches all uncaught exceptions
    CrashDialog.install()

    // Force AWT to probe the Desktop API now, BEFORE anything can load libmpv.
    //
    // On Linux the bundled libmpv drags in its own libglib-2.0.so.0 (2.72, built on
    // Ubuntu 22.04) through the $ORIGIN/lib rpath. Once that soname is taken, AWT's
    // XDesktopPeer.init() can no longer dlopen the SYSTEM libgio-2.0.so.0 — on a host
    // with newer glib (2.80 on Ubuntu 24.04) it dies with
    // "libgobject-2.0.so.0: undefined symbol: g_dir_unref" — and the JDK then reports
    // the whole Desktop API as unsupported for the rest of the process. Every external
    // link breaks: openUrl() silently does nothing, and Compose's LocalUriHandler
    // throws UnsupportedOperationException out of the click handler and crashes the app.
    //
    // XDesktopPeer caches the result of that probe on the first call, so calling it here
    // — while only the system glib is mapped — pins it to "supported" and lets the system
    // gio/gobject win the soname race. AWT is already initialized at this point anyway
    // (forceLinuxWmClass() in Main.kt touches the toolkit before we get here), so this
    // costs nothing.
    //
    // This is a workaround, not the cure: the real fix is to stop bundling glib in
    // scripts/mpv-linux/stage.sh, which needs the Linux mpv tarball rebuilt and
    // republished. Keep this call until that lands.
    java.awt.Desktop.isDesktopSupported()

    System.setProperty("compose.swing.render.on.graphics", "true")
    System.setProperty("compose.interop.blending", "true")
    System.setProperty("compose.layers.type", "COMPONENT")

    // Skiko's vsync wait can park the EDT forever after a display change:
    // macOS waits on a CVDisplayLink-signalled NSConditionLock with no timeout
    // (DisplayLinkThrottler.mm), Linux blocks inside the vsync'd glXSwapBuffers
    // when X11 loses its sync source (CMP-9725 / CMP-10019). Both surface as
    // "UI frozen, audio keeps playing" when the window moves to another
    // monitor. With vsync off, skiko paces frames with its refresh-rate frame
    // limiter (skiko.vsync.framelimit.fallback, on by default) instead.
    // Windows renders via DirectX and shows no such freeze — leave it alone.
    if (!System.getProperty("os.name", "").contains("Windows", ignoreCase = true)) {
        System.setProperty("skiko.vsync.enabled", "false")
    }

    // Handle deep link URIs
    // macOS: receives URI via Desktop open URI handler (app already running or launched via scheme)
    // Windows/Linux: receives URI as command-line argument
    val isMacOS = System.getProperty("os.name", "").contains("Mac", ignoreCase = true)
    if (isMacOS && java.awt.Desktop.isDesktopSupported()) {
        val desktop = java.awt.Desktop.getDesktop()
        try {
            desktop.setOpenURIHandler { event ->
                DesktopDeepLinkHandler.onNewUri(event.uri.toString())
            }
        } catch (_: UnsupportedOperationException) {
            // Shouldn't happen on macOS, but handle gracefully
        }
        // Clicking the Dock icon of a running app arrives as a "reopen" Apple
        // Event — macOS never spawns a second instance, so the
        // SingleInstanceManager restore path can't fire for it. Route it
        // through the same restore signal the tray and second instances use.
        if (desktop.isSupported(java.awt.Desktop.Action.APP_EVENT_REOPENED)) {
            desktop.addAppEventListener(
                java.awt.desktop.AppReopenedListener { DesktopRestoreSignal.request() },
            )
        }
    }
    // Handle URI passed as command-line argument (Windows/Linux, or explicit invocation)
    // Note: macOS does NOT pass URI as args — it uses Apple Events via setOpenURIHandler
    //
    // Matched by shape rather than against a fixed list of schemes. The list used to be
    // simpmusic:// + http:// + https://, which silently dropped the Last.fm callback
    // (wordbyword://lastfm-auth?token=…): the OS launched us with the token, this filter
    // discarded it, and the app merely came to the foreground while the login sat there
    // waiting forever. Any scheme we register with the OS must survive this line.
    val deepLinkArg = args.firstOrNull()?.takeIf { DEEP_LINK_ARG.matches(it) }
    // Single-instance guard — MUST run before startKoin. The DataStore Koin
    // singleton is `createdAtStart`, so a second Windows instance would touch
    // ~/.simpmusic/settings.preferences_pb and crash with an "Unable to rename
    // ...tmp" IOException (#2044) before it ever reached the old in-Compose check.
    // Bail out here, before Koin/DataStore initialize.
    val isSingleInstance =
        SingleInstanceManager.isSingleInstance(
            onRestoreRequest = { DesktopRestoreSignal.request() },
        )
    if (!isSingleInstance) {
        // Second instance: forward the deep link (if any) to the running instance,
        // then exit. Nothing has touched the DataStore file yet.
        deepLinkArg?.let { DesktopDeepLinkHandler.writePendingUri(it) }
        return
    }

    // First instance only: deliver our own deep link (non-macOS passes URI via args).
    if (!isMacOS) {
        deepLinkArg?.let { DesktopDeepLinkHandler.onNewUri(it) }
    }

    // Initialize Koin ONCE before application starts
    startKoin {
        loadAllModules()
        loadKoinModules(viewModelModule)
    }

    val language =
        runBlocking {
            getKoin()
                .get<DataStoreManager>()
                .language
                .first()
                .substring(0..1)
        }
    changeLanguageNative(language)

    VersionManager.initialize()
    configLastfm(BuildKonfig.lastfmApiKey, BuildKonfig.lastfmSecret)
    if (BuildKonfig.sentryDsn.isNotEmpty()) {
        Sentry.init { options ->
            options.dsn = BuildKonfig.sentryDsn
            options.release = "simpmusic-desktop@${VersionManager.getVersionName()}"
            options.setDiagnosticLevel(SentryLevel.ERROR)
        }
    }

    val mediaPlayerHandler by inject<MediaPlayerHandler>(MediaPlayerHandler::class.java)
    mediaPlayerHandler.showToast = { type ->
        showToast(
            when (type) {
                ToastType.ExplicitContent -> {
                    runBlocking { getString(Res.string.explicit_content_blocked) }
                }

                is ToastType.PlayerError -> {
                    runBlocking { getString(Res.string.time_out_check_internet_connection_or_change_piped_instance_in_settings, type.error) }
                }
            },
        )
    }
    mediaPlayerHandler.pushPlayerError = { error ->
        Sentry.withScope { scope ->
            Sentry.captureMessage("Player Error: ${error.message}, code: ${error.errorCode}, code name: ${error.errorCodeName}")
        }
    }

    // Register simpmusic:// protocol handler on Windows (HKCU, no admin needed)
    WindowsProtocolRegistrar.register()

    val sharedViewModel = getKoin().get<SharedViewModel>()
    if (sharedViewModel.shouldCheckForUpdate()) {
        sharedViewModel.checkForUpdate()
    }

    // Connect deep link handler to SharedViewModel
    DesktopDeepLinkHandler.listener = { intent ->
        sharedViewModel.setIntent(intent)
    }

    application {
        // Main Window
        val windowState =
            rememberWindowState(
                size = DpSize(1500.dp, 860.dp),
            )
        var isVisible by remember { mutableStateOf(true) }
        // The single-instance guard now runs before startKoin (top of
        // runDesktopApp). Here we only react to a restore request raised when a
        // second instance launches: bring the window back to the foreground and
        // consume any deep link the second instance forwarded.
        LaunchedEffect(Unit) {
            DesktopRestoreSignal.requests.collect {
                isVisible = true
                windowState.isMinimized = false
                DesktopDeepLinkHandler.consumePendingUri()
            }
        }
        val openAppString = stringResource(Res.string.open_app)
        val quitAppString = stringResource(Res.string.quit_app)
        val openMiniPlayer = stringResource(Res.string.open_miniplayer)
        val closeMiniPlayer = stringResource(Res.string.close_miniplayer)
        val appName = stringResource(Res.string.app_name)
        val nowPlayingData by sharedViewModel.nowPlayingScreenData.collectAsState()
        val nowPlayingTitle = nowPlayingData.nowPlayingTitle
        val nowPlayingArtist = nowPlayingData.artistName
        val hasTrack = nowPlayingTitle.isNotBlank()
        // Tray menus are narrow, so long titles would stretch the popup.
        val trayLabel: (String) -> String = { if (it.length > 40) it.take(39).trimEnd() + "…" else it }
        Tray(
            icon = painterResource(Res.drawable.circle_app_icon),
            tooltip =
                when {
                    hasTrack && nowPlayingArtist.isNotBlank() -> "$nowPlayingTitle — $nowPlayingArtist"
                    hasTrack -> nowPlayingTitle
                    else -> appName
                },
            primaryAction = {
                DesktopRestoreSignal.request()
            },
        ) {
            // Disabled entries: while the window is hidden the tray is the only place showing what
            // is playing.
            if (hasTrack) {
                Item(trayLabel(nowPlayingTitle), isEnabled = false)
                if (nowPlayingArtist.isNotBlank()) {
                    Item(trayLabel(nowPlayingArtist), isEnabled = false)
                }
                Divider()
            }
            if (!isVisible) {
                Item(openAppString) {
                    DesktopRestoreSignal.request()
                }
            }
            if (MiniPlayerManager.isOpen) {
                Item(closeMiniPlayer) {
                    MiniPlayerManager.isOpen = false
                }
            } else {
                Item(openMiniPlayer) {
                    MiniPlayerManager.isOpen = true
                }
            }
            Divider()
            Item(quitAppString) {
                mediaPlayerHandler.release()
                exitApplication()
            }
        }
        // Detect virtual machines (Parallels, VirtualBox, VMware, etc.).
        // Transparent + undecorated Compose windows don't render on VM
        // GPU drivers — the window stays invisible while the JVM keeps
        // running, so we must detect the VM and fall back to a normal
        // decorated window.
        //
        // We probe Manufacturer + Model because brand strings live in
        // different fields per hypervisor (Parallels-on-ARM puts
        // "Parallels Software International Inc." in Manufacturer and
        // "Parallels ARM Virtual Machine" in Model; VirtualBox uses
        // "innotek GmbH" + "VirtualBox"; etc).
        //
        // Microsoft removed `wmic` from Windows 11 (deprecated since
        // 10 21H1), so on modern Windows it returns "command not
        // recognized" and our previous detection always saw an empty
        // vendor — Parallels Win 11 ARM users hit this and got an
        // invisible window. PowerShell `Get-CimInstance` is the modern
        // replacement; we try it first and fall back to wmic for older
        // hosts.
        val isVM =
            remember {
                val osName = System.getProperty("os.name", "")
                if (!osName.contains("Windows", ignoreCase = true)) {
                    return@remember false
                }
                val probes =
                    listOf(
                        listOf(
                            "powershell",
                            "-NoProfile",
                            "-Command",
                            "(Get-CimInstance Win32_ComputerSystem | " +
                                "Select-Object Manufacturer,Model | " +
                                "Format-List | Out-String).Trim()",
                        ),
                        listOf("wmic", "computersystem", "get", "manufacturer,model"),
                    )
                val sysInfo =
                    probes
                        .asSequence()
                        .mapNotNull { cmd ->
                            runCatching {
                                val p =
                                    ProcessBuilder(cmd)
                                        .redirectErrorStream(true)
                                        .start()
                                val out = p.inputStream.bufferedReader().readText()
                                if (p.waitFor() == 0 && out.isNotBlank()) out else null
                            }.getOrNull()
                        }
                        .firstOrNull()
                        .orEmpty()
                val vmTokens = listOf("Parallels", "VirtualBox", "VMware", "QEMU", "KVM", "Xen", "Hyper-V")
                vmTokens.any { sysInfo.contains(it, ignoreCase = true) } ||
                    System.getProperty("compose.window.no-transparent", "false").toBooleanStrictOrNull() == true
            }
        // Publish whether the custom title bar will be mounted so getScreenSizeInfo() can
        // subtract the 40dp strip it occupies above the content (see DesktopWindowChrome).
        LaunchedEffect(isVM) {
            DesktopWindowChrome.customTitleBarVisible = !isVM
        }
        Window(
            onCloseRequest = {
                isVisible = false
            },
            title = stringResource(Res.string.app_name),
            icon = painterResource(Res.drawable.circle_app_icon),
            undecorated = !isVM,
            transparent = !isVM,
            state = windowState,
            visible = isVisible,
        ) {
            // Restore requests (Dock reopen, tray, second instance) also need a
            // z-order raise; visibility/minimized are reset by the
            // application-level collector, but toFront needs the AWT window.
            LaunchedEffect(Unit) {
                DesktopRestoreSignal.requests.collect {
                    window.toFront()
                }
            }
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .then(
                            if (!isVM) {
                                Modifier.clip(RoundedCornerShape(12.dp))
                            } else {
                                Modifier
                            },
                        ),
            ) {
                if (!isVM) {
                    // The bar sits outside AppTheme, so the colours are resolved here from the
                    // same stored setting AppTheme uses and handed down. Pure black / pure white
                    // to match the window colour the shell paints behind the panels.
                    val themeMode by sharedViewModel
                        .getThemeMode()
                        .collectAsState(DataStoreManager.THEME_MODE_DARK)
                    val isDark = isDarkTheme(themeMode)
                    CustomTitleBar(
                        title = stringResource(Res.string.app_name),
                        windowState = windowState,
                        window = window,
                        onCloseRequest = {
                            isVisible = false
                        },
                        containerColor = if (isDark) Color.Black else Color.White,
                        titleColor = if (isDark) Color.White else Color.Black,
                    )
                }

                val context = LocalPlatformContext.current
                setSingletonImageLoaderFactory {
                    ImageLoader
                        .Builder(context)
                        .components {
                            add(
                                OkHttpNetworkFetcherFactory(
                                    callFactory = {
                                        OkHttpClient()
                                    },
                                ),
                            )
                        }.diskCachePolicy(CachePolicy.ENABLED)
                        .networkCachePolicy(CachePolicy.ENABLED)
                        .diskCache(
                            DiskCache
                                .Builder()
                                .directory(FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "image_cache")
                                .maxSizeBytes(512L * 1024 * 1024)
                                .build(),
                        ).crossfade(true)
                        .build()
                }
                App()
                ToastHost()
            }
        }

        // Mini Player Window (separate window)
        if (MiniPlayerManager.isOpen) {
            MiniPlayerWindow(
                sharedViewModel = sharedViewModel,
                onCloseRequest = {
                    MiniPlayerManager.isOpen = false
                },
            )
        }
    }
}

/**
 * Bridges a restore request from the single-instance guard (which runs outside
 * Compose, at the top of [runDesktopApp]) into the running window's composition.
 * The guard calls [request] when a second instance launches; the window collects
 * [requests] to bring itself back to the foreground and pick up any deep link.
 */
private object DesktopRestoreSignal {
    private val _requests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val requests: SharedFlow<Unit> = _requests.asSharedFlow()

    fun request() {
        _requests.tryEmit(Unit)
    }
}