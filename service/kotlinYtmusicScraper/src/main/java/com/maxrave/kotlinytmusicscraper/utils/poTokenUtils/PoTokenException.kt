package com.maxrave.kotlinytmusicscraper.utils.poTokenUtils

import kotlin.text.contains

class PoTokenException(
    message: String,
) : Exception(message)

// to be thrown if the WebView provided by the system is broken
class BadWebViewException(
    message: String,
) : Exception(message)

fun buildExceptionForJsError(error: String): Exception =
    if (error.contains("SyntaxError")) {
        BadWebViewException(error)
    } else {
        PoTokenException(error)
    }