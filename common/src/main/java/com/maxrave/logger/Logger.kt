package com.maxrave.logger

import io.github.oshai.kotlinlogging.KotlinLogging

object Logger {
    object Static {
        init {
            System.setProperty("kotlin-logging-to-android-native", "true")
        }
    }

    private val static = Static

    private val logger = KotlinLogging.logger("com.maxrave.simpmusic.dev")

    fun d(
        tag: String,
        message: String,
    ) {
        logger.debug {
            "[$tag]: $message"
        }
    }

    fun i(
        tag: String,
        message: String,
    ) {
        logger.info {
            "[$tag]: $message"
        }
    }

    fun w(
        tag: String,
        message: String,
    ) {
        logger.warn {
            "[$tag]: $message"
        }
    }

    fun e(
        tag: String,
        message: String,
        e: Throwable? = null,
    ) {
        logger.error {
            "[$tag]: $message"
        }
    }
}