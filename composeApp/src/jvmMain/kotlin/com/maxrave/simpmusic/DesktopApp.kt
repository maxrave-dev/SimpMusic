package com.maxrave.simpmusic

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.kdroid.composetray.tray.api.Tray
import com.kdroid.composetray.utils.SingleInstanceManager
import com.maxrave.data.di.loader.loadAllModules
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.domain.mediaservice.handler.ToastType
import com.maxrave.simpmusic.desktop.auth.AuthManager
import com.maxrave.simpmusic.desktop.auth.SecurityGuard
import com.maxrave.simpmusic.desktop.auth.UpdateInfo
import com.maxrave.simpmusic.desktop.ui.LoginScreen
import com.maxrave.simpmusic.di.viewModelModule
import com.maxrave.simpmusic.ui.component.CustomTitleBar
import com.maxrave.simpmusic.ui.mini_player.MiniPlayerManager
import com.maxrave.simpmusic.ui.mini_player.MiniPlayerWindow
import com.maxrave.simpmusic.utils.VersionManager
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.changeLanguageNative
import io.sentry.Sentry
import io.sentry.SentryLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import multiplatform.network.cmptoast.ToastHost
import multiplatform.network.cmptoast.showToast
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.java.KoinJavaComponent.inject
import org.koin.mp.KoinPlatform.getKoin
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.app_name
import simpmusic.composeapp.generated.resources.circle_app_icon
import simpmusic.composeapp.generated.resources.close_miniplayer
import simpmusic.composeapp.generated.resources.explicit_content_blocked
import simpmusic.composeapp.generated.resources.open_app
import simpmusic.composeapp.generated.resources.open_miniplayer
import simpmusic.composeapp.generated.resources.quit_app
import simpmusic.composeapp.generated.resources.time_out_check_internet_connection_or_change_piped_instance_in_settings
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

@OptIn(ExperimentalMaterial3Api::class)
fun runDesktopApp(args: Array<String> = emptyArray()) {
    CrashDialog.install()

    System.setProperty("java.net.preferIPv4Stack", "true")
    System.setProperty("compose.swing.render.on.graphics", "true")
    System.setProperty("compose.interop.blending", "true")
    System.setProperty("compose.layers.type", "COMPONENT")

    val isMacOS = System.getProperty("os.name", "").contains("Mac", ignoreCase = true)
    if (isMacOS && java.awt.Desktop.isDesktopSupported()) {
        try {
            java.awt.Desktop.getDesktop().setOpenURIHandler { event ->
                DesktopDeepLinkHandler.onNewUri(event.uri.toString())
            }
        } catch (_: UnsupportedOperationException) {}
    }

    val deepLinkArg = args.firstOrNull()?.takeIf { arg ->
        arg.startsWith("simpmusic://") || arg.startsWith("http://") || arg.startsWith("https://")
    }

    val isSingleInstance = SingleInstanceManager.isSingleInstance(
        onRestoreRequest = { DesktopRestoreSignal.request() },
    )
    if (!isSingleInstance) {
        deepLinkArg?.let { DesktopDeepLinkHandler.writePendingUri(it) }
        return
    }

    if (!isMacOS) {
        deepLinkArg?.let { DesktopDeepLinkHandler.onNewUri(it) }
    }

    startKoin {
        loadAllModules()
        loadKoinModules(viewModelModule)
    }

    val language = runBlocking {
        getKoin().get<DataStoreManager>().language.first().substring(0..1)
    }
    changeLanguageNative(language)

    VersionManager.initialize()
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
                ToastType.ExplicitContent -> "Contenido explícito bloqueado"
                is ToastType.PlayerError -> "Tiempo de espera agotado o error de red: ${type.error}"
            },
        )
    }
    
    mediaPlayerHandler.pushPlayerError = { error ->
        Sentry.withScope { scope ->
            Sentry.captureMessage("Player Error: ${error.message}, code: ${error.errorCode}, code name: ${error.errorCodeName}")
        }
    }

    WindowsProtocolRegistrar.register()

    val sharedViewModel = getKoin().get<SharedViewModel>()

    DesktopDeepLinkHandler.listener = { intent ->
        sharedViewModel.setIntent(intent)
    }

    application {
        setSingletonImageLoaderFactory { context ->
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .addInterceptor(Interceptor { chain ->
                    val request = chain.request().newBuilder()
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                        .header("Accept", "*/*")
                        .build()
                    chain.proceed(request)
                })
                .build()

            ImageLoader.Builder(context).components {
                add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient }))
            }.build()
        }

        var isUserLoggedIn by remember { mutableStateOf(AuthManager.isLoggedIn) } 
        var isSecurityChecked by remember { mutableStateOf(false) }
        var isBlocked by remember { mutableStateOf(false) }
        var blockTitle by remember { mutableStateOf("") }
        var blockMessage by remember { mutableStateOf("") }

        LaunchedEffect(isUserLoggedIn) {
            if (isUserLoggedIn) {
                if (System.currentTimeMillis() - AuthManager.getValidationTime() < 5000) {
                    isSecurityChecked = true
                    isBlocked = false
                    return@LaunchedEffect
                }

                val user = AuthManager.username ?: ""
                val pass = AuthManager.getSavedPassword() ?: ""

                withContext(Dispatchers.IO) {
                    val loginResult = AuthManager.login(user, pass)

                    withContext(Dispatchers.Main) {
                        if (loginResult.isSuccess) {
                            AuthManager.updateValidationTime()
                            isSecurityChecked = true
                            isBlocked = false
                        } else {
                            val errorMsg = loginResult.exceptionOrNull()?.message ?: ""
                            
                            if (errorMsg.contains("Error de red") || errorMsg.contains("Error en el servidor")) {
                                val lastTime = AuthManager.getValidationTime()
                                val currentTime = System.currentTimeMillis()
                                val sevenDaysMs = 604800000L

                                if (currentTime - lastTime > sevenDaysMs) {
                                    AuthManager.clearSession()
                                    isUserLoggedIn = false
                                    isBlocked = true
                                    blockTitle = "Sesión Expirada"
                                    blockMessage = "Han pasado más de 7 días sin conexión a internet. Por favor, vuelve a iniciar sesión."
                                } else {
                                    isSecurityChecked = true
                                    isBlocked = false
                                }
                            } else {
                                AuthManager.clearSession()
                                isUserLoggedIn = false
                                isBlocked = true
                                blockTitle = "Acceso Denegado"
                                blockMessage = errorMsg
                            }
                        }
                    }
                }
            } else {
                isSecurityChecked = false
            }
        }

        if (isBlocked) {
            Window(
                onCloseRequest = ::exitApplication,
                title = blockTitle,
                state = rememberWindowState(size = DpSize(400.dp, 250.dp)),
                resizable = false
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(blockTitle, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(blockMessage, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }
        } else if (!isUserLoggedIn) {
            Window(
                onCloseRequest = ::exitApplication,
                title = "Login - YouTube Music",
                state = rememberWindowState(size = DpSize(450.dp, 600.dp)),
                resizable = false
            ) {
                LoginScreen(onLoginSuccess = { isUserLoggedIn = true })
            }
        } else if (!isSecurityChecked) {
            Window(
                onCloseRequest = ::exitApplication,
                title = "Verificando...",
                state = rememberWindowState(size = DpSize(400.dp, 200.dp)),
                resizable = false
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Verificando seguridad de la cuenta...", color = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                }
            }
        } else {
            val windowState = rememberWindowState(size = DpSize(1500.dp, 860.dp))
            var isVisible by remember { mutableStateOf(true) }
            val coroutineScope = rememberCoroutineScope()

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

            Tray(
                icon = painterResource(Res.drawable.circle_app_icon),
                tooltip = stringResource(Res.string.app_name),
                primaryAction = {
                    isVisible = true
                    windowState.isMinimized = false
                },
            ) {
                if (!isVisible) {
                    Item(openAppString) {
                        isVisible = true
                        windowState.isMinimized = false
                    }
                }
                if (MiniPlayerManager.isOpen) {
                    Item(closeMiniPlayer) { MiniPlayerManager.isOpen = false }
                } else {
                    Item(openMiniPlayer) { MiniPlayerManager.isOpen = true }
                }
                Divider()
                Item(quitAppString) {
                    mediaPlayerHandler.release()
                    exitApplication()
                }
            }

            val isVM = remember {
                val osName = System.getProperty("os.name", "")
                if (!osName.contains("Windows", ignoreCase = true)) return@remember false
                val probes = listOf(
                    listOf("powershell", "-NoProfile", "-Command", "(Get-CimInstance Win32_ComputerSystem | Select-Object Manufacturer,Model | Format-List | Out-String).Trim()"),
                    listOf("wmic", "computersystem", "get", "manufacturer,model")
                )
                val sysInfo = probes.asSequence().mapNotNull { cmd ->
                    runCatching {
                        val p = ProcessBuilder(cmd).redirectErrorStream(true).start()
                        val out = p.inputStream.bufferedReader().readText()
                        if (p.waitFor() == 0 && out.isNotBlank()) out else null
                    }.getOrNull()
                }.firstOrNull().orEmpty()
                val vmTokens = listOf("Parallels", "VirtualBox", "VMware", "QEMU", "KVM", "Xen", "Hyper-V")
                vmTokens.any { sysInfo.contains(it, ignoreCase = true) } || System.getProperty("compose.window.no-transparent", "false").toBooleanStrictOrNull() == true
            }

            var showDaysAlert by remember { mutableStateOf(false) }
            var daysAlertTitle by remember { mutableStateOf("") }
            var daysAlertMessage by remember { mutableStateOf("") }
            var isAccountBlocked by remember { mutableStateOf(false) }

            var showUpdateAlert by remember { mutableStateOf(false) }
            var updateInfoState by remember { mutableStateOf<UpdateInfo?>(null) }
            var updateProgress by remember { mutableFloatStateOf(-1f) }
            var downloadedBytes by remember { mutableStateOf(0L) }
            var totalBytes by remember { mutableStateOf(0L) }

            LaunchedEffect(Unit) {
                val user = AuthManager.username
                if (!user.isNullOrBlank()) {
                    coroutineScope.launch(Dispatchers.IO) {
                        val securityStatus = SecurityGuard.checkRemainingDays(user)
                        val updateInfo = SecurityGuard.checkAppUpdates()
                        
                        withContext(Dispatchers.Main) {
                            if (!securityStatus.isValid && securityStatus.alertTitle != null) {
                                isAccountBlocked = true
                                daysAlertTitle = securityStatus.alertTitle ?: ""
                                daysAlertMessage = securityStatus.alertMessage ?: ""
                                showDaysAlert = true
                            } else if (securityStatus.alertTitle != null && securityStatus.alertMessage != null) {
                                daysAlertTitle = securityStatus.alertTitle ?: ""
                                daysAlertMessage = securityStatus.alertMessage ?: ""
                                showDaysAlert = true
                            }
                            
                            if (updateInfo?.hasUpdate == true) {
                                updateInfoState = updateInfo
                                if (!showDaysAlert) {
                                    showUpdateAlert = true
                                }
                            }
                        }
                    }
                }
            }

            Window(
                onCloseRequest = { isVisible = false },
                title = stringResource(Res.string.app_name),
                icon = painterResource(Res.drawable.circle_app_icon),
                undecorated = !isVM,
                transparent = !isVM,
                state = windowState,
                visible = isVisible,
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.fillMaxSize().then(if (!isVM) Modifier.clip(RoundedCornerShape(12.dp)) else Modifier)
                    ) {
                        if (!isVM) {
                            CustomTitleBar(
                                title = stringResource(Res.string.app_name),
                                windowState = windowState,
                                window = window,
                                onCloseRequest = { isVisible = false },
                            )
                        }

                        App()
                        ToastHost()
                    }

                    if (showDaysAlert) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f))
                        ) {
                            AlertDialog(
                                onDismissRequest = {
                                    if (!isAccountBlocked) {
                                        showDaysAlert = false
                                        if (updateInfoState?.hasUpdate == true) showUpdateAlert = true
                                    }
                                },
                                containerColor = Color(0xFF1E1E1E),
                                titleContentColor = Color.White,
                                textContentColor = Color.LightGray,
                                shape = RoundedCornerShape(12.dp),
                                title = {
                                    Text(
                                        text = daysAlertTitle,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        color = Color(0xFFFF3E3E)
                                    )
                                },
                                text = { Text(text = daysAlertMessage, fontSize = 16.sp) },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            if (isAccountBlocked) {
                                                AuthManager.clearSession()
                                                exitProcess(0)
                                            } else {
                                                showDaysAlert = false
                                                if (updateInfoState?.hasUpdate == true) showUpdateAlert = true
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3E3E), contentColor = Color.White)
                                    ) {
                                        Text(if (isAccountBlocked) "Salir" else "Entendido", fontWeight = FontWeight.Bold)
                                    }
                                }
                            )
                        }
                    }

                    if (showUpdateAlert && updateInfoState != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f))
                        ) {
                            AlertDialog(
                                onDismissRequest = {
                                    if (updateProgress < 0f && !updateInfoState!!.isMandatory) {
                                        showUpdateAlert = false
                                    }
                                },
                                containerColor = Color(0xFF1E1E1E),
                                titleContentColor = Color.White,
                                textContentColor = Color.LightGray,
                                shape = RoundedCornerShape(12.dp),
                                title = {
                                    Text(
                                        text = if (updateInfoState!!.isMandatory) "Actualización Requerida" else "¡Actualización Disponible!",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        color = if (updateInfoState!!.isMandatory) Color(0xFFFF3E3E) else Color(0xFF0090E7)
                                    )
                                },
                                text = {
                                    Column {
                                        Text("Nueva versión disponible. Notas:", fontWeight = FontWeight.Bold, color = Color.White)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(updateInfoState!!.changelog ?: "Mejoras generales y corrección de errores.", fontSize = 14.sp)
                                        
                                        if (updateProgress >= 0f) {
                                            Spacer(modifier = Modifier.height(16.dp))
                                            val downloadedMB = String.format("%.2f", downloadedBytes / (1024f * 1024f))
                                            val totalMB = if (totalBytes > 0) String.format("%.2f", totalBytes / (1024f * 1024f)) else "..."
                                            Text("Descargando: $downloadedMB MB / $totalMB MB", fontSize = 14.sp, color = Color.White)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            LinearProgressIndicator(
                                                progress = { updateProgress },
                                                modifier = Modifier.fillMaxWidth().height(8.dp),
                                                color = Color(0xFF00D25B),
                                                trackColor = Color(0xFF333333)
                                            )
                                        }
                                    }
                                },
                                confirmButton = {
                                    if (updateProgress < 0f) {
                                        Button(
                                            onClick = {
                                                updateProgress = 0.01f
                                                coroutineScope.launch(Dispatchers.IO) {
                                                    val success = SecurityGuard.downloadAndInstallUpdate(updateInfoState!!.downloadUrl!!) { progress, downloaded, total ->
                                                        kotlinx.coroutines.runBlocking(Dispatchers.Main) {
                                                            updateProgress = progress
                                                            downloadedBytes = downloaded
                                                            totalBytes = total
                                                        }
                                                    }
                                                    withContext(Dispatchers.Main) {
                                                        if (success) {
                                                            updateProgress = 1f
                                                            kotlinx.coroutines.delay(1000)
                                                            exitProcess(0)
                                                        }
                                                        else {
                                                            updateProgress = -1f
                                                        }
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (updateInfoState!!.isMandatory) Color(0xFFFF3E3E) else Color(0xFF0090E7), 
                                                contentColor = Color.White
                                            )
                                        ) { Text("Actualizar Ahora", fontWeight = FontWeight.Bold) }
                                    }
                                },
                                dismissButton = {
                                    if (updateProgress < 0f && !updateInfoState!!.isMandatory) {
                                        Button(
                                            onClick = { showUpdateAlert = false },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.Gray)
                                        ) { Text("Más tarde") }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            if (MiniPlayerManager.isOpen) {
                MaterialTheme(
                    colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
                ) {
                    MiniPlayerWindow(
                        sharedViewModel = sharedViewModel,
                        onCloseRequest = { MiniPlayerManager.isOpen = false },
                    )
                }
            }
        }
    }
}

private object DesktopRestoreSignal {
    private val _requests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val requests: SharedFlow<Unit> = _requests.asSharedFlow()
    fun request() { _requests.tryEmit(Unit) }
}