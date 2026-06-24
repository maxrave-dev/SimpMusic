package com.maxrave.simpmusic

import com.maxrave.simpmusic.utils.VersionManager
import io.sentry.Sentry
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.PrintWriter
import java.io.StringWriter
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.SwingUtilities
import javax.swing.UIManager
import kotlin.system.exitProcess

object CrashDialog {

    fun install() {
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                // Report to Sentry if available
                if (BuildKonfig.sentryDsn.isNotEmpty()) {
                    Sentry.captureException(throwable)
                }
            } catch (_: Exception) {
                // Sentry might not be initialized
            }

            // Show crash dialog on EDT
            try {
                if (SwingUtilities.isEventDispatchThread()) {
                    // Already on EDT — call directly
                    showCrashDialog(thread, throwable)
                } else {
                    SwingUtilities.invokeAndWait {
                        showCrashDialog(thread, throwable)
                    }
                }
            } catch (_: Exception) {
                // If EDT is broken too, print to stderr and exit
                System.err.println("Fatal crash in thread ${thread.name}:")
                throwable.printStackTrace()
            }

            exitProcess(1)
        }
    }

    private fun showCrashDialog(thread: Thread, throwable: Throwable) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        } catch (_: Exception) {
            // Use default L&F
        }

        val stackTrace = StringWriter().also { throwable.printStackTrace(PrintWriter(it)) }.toString()
        val versionInfo = try {
            "SimpMusic Desktop v${VersionManager.getVersionName()}"
        } catch (_: Exception) {
            "SimpMusic Desktop"
        }

        val dialog = JDialog().apply {
            title = "SimpMusic - Unexpected Error"
            isModal = true
            defaultCloseOperation = JDialog.DISPOSE_ON_CLOSE
            preferredSize = Dimension(700, 500)
            minimumSize = Dimension(500, 350)
        }

        val contentPanel = JPanel(BorderLayout(0, 12)).apply {
            border = BorderFactory.createEmptyBorder(16, 16, 16, 16)
        }

        // Header
        val headerPanel = JPanel(BorderLayout(8, 4)).apply {
            val titleLabel = JLabel("SimpMusic has crashed").apply {
                font = font.deriveFont(Font.BOLD, 16f)
            }
            val subtitleLabel = JLabel(
                "<html>An unexpected error occurred. The stack trace below may help diagnose the issue.<br>" +
                    "<font color='gray'>$versionInfo · Thread: ${thread.name}</font></html>"
            ).apply {
                font = font.deriveFont(12f)
            }
            add(titleLabel, BorderLayout.NORTH)
            add(subtitleLabel, BorderLayout.CENTER)
        }

        // Stack trace
        val textArea = JTextArea(stackTrace).apply {
            isEditable = false
            font = Font(Font.MONOSPACED, Font.PLAIN, 12)
            caretPosition = 0
            background = Color(30, 30, 30)
            foreground = Color(210, 210, 210)
            border = BorderFactory.createEmptyBorder(8, 8, 8, 8)
        }

        val scrollPane = JScrollPane(textArea).apply {
            border = BorderFactory.createLineBorder(Color(80, 80, 80))
        }

        // Buttons
        val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 8, 0))

        val copyButton = JButton("Copy Stack Trace").apply {
            addActionListener {
                val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                clipboard.setContents(StringSelection(stackTrace), null)
                text = "Copied!"
                isEnabled = false
            }
        }

        val closeButton = JButton("Close").apply {
            addActionListener {
                dialog.dispose()
            }
        }

        buttonPanel.add(copyButton)
        buttonPanel.add(closeButton)

        contentPanel.add(headerPanel, BorderLayout.NORTH)
        contentPanel.add(scrollPane, BorderLayout.CENTER)
        contentPanel.add(buttonPanel, BorderLayout.SOUTH)

        dialog.contentPane = contentPanel
        dialog.pack()
        dialog.setLocationRelativeTo(null) // Center on screen
        dialog.isVisible = true
    }
}
