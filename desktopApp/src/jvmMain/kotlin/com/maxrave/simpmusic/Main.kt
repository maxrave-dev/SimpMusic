package com.maxrave.simpmusic

import java.awt.Color
import java.awt.Toolkit
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URLDecoder
import java.util.zip.ZipInputStream
import javax.swing.BorderFactory
import javax.swing.JLabel
import javax.swing.JWindow
import javax.swing.SwingConstants

private fun forceLinuxWmClass(appName: String = "SimpMusic") {
    if (!System.getProperty("os.name").orEmpty().contains("linux", ignoreCase = true)) return
    runCatching {
        val toolkit = Toolkit.getDefaultToolkit()
        if (toolkit.javaClass.name != "sun.awt.X11.XToolkit") return
        toolkit.javaClass.getDeclaredField("awtAppClassName").apply {
            isAccessible = true
            set(null, appName)
        }
    }.onFailure {}
}

class VlcPathLocator 

private fun configureVlcPath() {
    try {
        val location = VlcPathLocator::class.java.protectionDomain?.codeSource?.location
        val jarLocation = location?.path ?: System.getProperty("user.dir")
        val decodedPath = URLDecoder.decode(jarLocation, "UTF-8")
        val baseAppFolder = File(decodedPath).parentFile 
        
        val osName = System.getProperty("os.name").lowercase()
        
        val userWritableDir = when {
            osName.contains("win") -> File(System.getenv("LOCALAPPDATA") ?: (System.getProperty("user.home") + "/AppData/Local"), "SimpMusic")
            osName.contains("mac") -> File(System.getProperty("user.home"), "Library/Application Support/SimpMusic")
            else -> File(System.getProperty("user.home"), ".local/share/SimpMusic")
        }
        
        val vlcDir = File(userWritableDir, "vlc")
        val zipFile = File(baseAppFolder, "vlc_bundle.zip")

        if (zipFile.exists()) {
            if (!vlcDir.exists()) {
                vlcDir.mkdirs()
                ZipInputStream(FileInputStream(zipFile)).use { zis ->
                    var zipEntry = zis.nextEntry
                    while (zipEntry != null) {
                        val newFile = File(vlcDir, zipEntry.name)
                        if (zipEntry.isDirectory) {
                            newFile.mkdirs()
                        } else {
                            newFile.parentFile?.mkdirs()
                            FileOutputStream(newFile).use { fos ->
                                zis.copyTo(fos)
                            }
                        }
                        zipEntry = zis.nextEntry
                    }
                }
            }
        }

        val vlcLibName = when {
            osName.contains("win") -> "libvlc.dll"
            osName.contains("mac") -> "libvlc.dylib"
            else -> "libvlc.so"
        }

        val searchFolder = if (vlcDir.exists()) vlcDir else (baseAppFolder.parentFile ?: baseAppFolder)
        val libvlcFile = searchFolder.walkTopDown()
            .filter { it.isFile && it.name.equals(vlcLibName, ignoreCase = true) }
            .firstOrNull()

        if (libvlcFile != null) {
            val exactPath = libvlcFile.parentFile.absolutePath
            
            try {
                val jnaClass = Class.forName("com.sun.jna.NativeLibrary")
                val method = jnaClass.getMethod("addSearchPath", String::class.java, String::class.java)
                method.invoke(null, "libvlc", exactPath)
                method.invoke(null, "libvlccore", exactPath)
            } catch (e: Exception) {}
            
            System.setProperty("jna.library.path", exactPath)
            System.setProperty("vlc.bundled.path", exactPath)
            System.setProperty("VLC_PLUGIN_PATH", File(exactPath, "plugins").absolutePath)
        }
    } catch (e: Exception) {}
}

fun main(args: Array<String>) {
    Thread.setDefaultUncaughtExceptionHandler { _, _ -> }
    
    val splash = JWindow()
    val label = JLabel("Iniciando YouTube Music, cargando componentes...", SwingConstants.CENTER)
    label.isOpaque = true
    label.background = Color(25, 25, 25)
    label.foreground = Color.WHITE
    label.border = BorderFactory.createLineBorder(Color(60, 60, 60), 2)
    splash.add(label)
    splash.setSize(400, 120)
    
    val screenSize = Toolkit.getDefaultToolkit().screenSize
    splash.setLocation((screenSize.width - 400) / 2, (screenSize.height - 120) / 2)
    splash.isVisible = true

    Thread {
        try {
            var appWindowAppeared = false
            while (!appWindowAppeared) {
                Thread.sleep(100)
                for (window in java.awt.Window.getWindows()) {
                    if (window.isVisible && window !== splash) {
                        appWindowAppeared = true
                        break
                    }
                }
            }
            splash.isVisible = false
            splash.dispose()
        } catch (e: Exception) {}
    }.start()
    
    configureVlcPath()
    forceLinuxWmClass()
    
    runDesktopApp(args)
}