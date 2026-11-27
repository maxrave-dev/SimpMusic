package com.maxrave.simpmusic.expect

/**
 * True while the device has a network that claims internet access.
 *
 * This is a snapshot, not a subscription: a request can still fail if connectivity drops right
 * after the check. Callers use it to skip work that cannot succeed offline, not to guarantee it.
 */
expect fun isNetworkAvailable(): Boolean