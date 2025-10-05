package com.maxrave.logger

import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier

object Logger {
    init {
        Napier.base(DebugAntilog())
    }

    private val logger = Napier

    fun d(
        tag: String,
        message: String,
    ) {
        logger.d(tag = tag, message = message)
    }

    fun i(
        tag: String,
        message: String,
    ) {
        logger.i(tag = tag, message = message)
    }

    fun w(
        tag: String,
        message: String,
    ) {
        logger.w(tag = tag, message = message)
    }

    fun e(
        tag: String,
        message: String,
        e: Throwable? = null,
    ) {
        logger.e(tag = tag, message = message, throwable = e)
    }
}