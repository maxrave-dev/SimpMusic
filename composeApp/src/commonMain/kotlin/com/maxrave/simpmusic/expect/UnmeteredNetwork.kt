package com.maxrave.simpmusic.expect

/**
 * Whether the device is on a network the system does not meter, such as Wi-Fi. Smart downloads only
 * start a pass there. Always false on Desktop, which has no Smart downloads.
 */
expect fun isOnUnmeteredNetwork(): Boolean
