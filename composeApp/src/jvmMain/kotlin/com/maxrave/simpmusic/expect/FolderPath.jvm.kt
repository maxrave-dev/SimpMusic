package com.maxrave.simpmusic.expect

actual fun getDownloadFolderPath(): String = System.getProperty("user.home") + "/Downloads"