package com.maxrave.simpmusic.utils

import com.maxrave.common.SponsorBlockType
import com.maxrave.simpmusic.extension.displayRes
import org.jetbrains.compose.resources.getString
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.explicit_content_blocked
import simpmusic.composeapp.generated.resources.new_albums
import simpmusic.composeapp.generated.resources.new_singles
import simpmusic.composeapp.generated.resources.sponsorblock_skip_segment
import simpmusic.composeapp.generated.resources.this_app_needs_to_access_your_notification
import simpmusic.composeapp.generated.resources.time_out_check_internet_connection_or_change_piped_instance_in_settings

object ComposeResUtils {
    suspend fun getResString(
        type: StringType,
        vararg format: String,
    ): String =
        when (type) {
            StringType.EXPLICIT_CONTENT_BLOCKED -> {
                getString(Res.string.explicit_content_blocked)
            }

            StringType.NOTIFICATION_REQUEST -> {
                getString(Res.string.this_app_needs_to_access_your_notification)
            }

            StringType.TIME_OUT_ERROR -> {
                getString(Res.string.time_out_check_internet_connection_or_change_piped_instance_in_settings, *format)
            }

            StringType.NEW_SINGLES -> {
                getString(Res.string.new_singles)
            }

            StringType.NEW_ALBUMS -> {
                getString(Res.string.new_albums)
            }

            StringType.SPONSOR_BLOCK_SKIP -> {
                // format[0] is the segment's category id ("sponsor", "selfpromo"…). An id this build
                // does not know is shown as-is rather than dropping the toast.
                val category = format.firstOrNull().orEmpty()
                val name = SponsorBlockType.fromValue(category)?.let { getString(it.displayRes()) } ?: category
                getString(Res.string.sponsorblock_skip_segment, name.lowercase())
            }
        }

    enum class StringType {
        EXPLICIT_CONTENT_BLOCKED,
        NOTIFICATION_REQUEST,
        TIME_OUT_ERROR,
        NEW_SINGLES,
        NEW_ALBUMS,
        SPONSOR_BLOCK_SKIP,
    }
}