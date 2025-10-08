package com.maxrave.kotlinytmusicscraper

import platform.Foundation.NSLocale
import platform.Foundation.countryCode
import platform.Foundation.currentLocale
import platform.Foundation.languageCode

actual fun getCountry(): String = NSLocale.currentLocale.languageCode
actual fun getLanguage(): String = NSLocale.currentLocale.countryCode ?: "US"