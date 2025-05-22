package com.maxrave.lyricsproviders.utils

class CaptchaException: Exception() {
    override val message: String
        get() = "Captcha required. Please solve the captcha to continue."
}