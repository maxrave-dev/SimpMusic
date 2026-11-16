@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.maxrave.simpmusic.expect

import platform.Network.*
import platform.darwin.dispatch_get_main_queue

private object Connectivity {
    var unmetered = false
    private val monitor = nw_path_monitor_create()
    init {
        nw_path_monitor_set_update_handler(monitor) { path ->
            unmetered = path != null && nw_path_get_status(path) == nw_path_status_satisfied &&
                !nw_path_is_expensive(path) && !nw_path_is_constrained(path)
        }
        nw_path_monitor_set_queue(monitor, dispatch_get_main_queue())
        nw_path_monitor_start(monitor)
    }
}

actual fun isOnUnmeteredNetwork(): Boolean = Connectivity.unmetered
