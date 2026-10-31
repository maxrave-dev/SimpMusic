package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxrave.domain.manager.DataStoreManager.Values.TRUE
import com.maxrave.simpmusic.expect.openUrl
import com.maxrave.simpmusic.expect.ui.PlatformBackdrop
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.ui.icon.ArrowOutward
import com.maxrave.simpmusic.ui.icon.Close
import com.maxrave.simpmusic.ui.icon.MicOff
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.utils.LyricsPromo
import com.maxrave.simpmusic.utils.VersionManager
import com.maxrave.simpmusic.utils.lyricsPromo
import com.maxrave.simpmusic.utils.osRequirementOf
import com.maxrave.simpmusic.viewModel.SharedViewModel
import kotlinx.coroutines.flow.first
import org.koin.compose.koinInject

/**
 * The line under the lyrics that a `promos` entry placed "lyrics" asks for ([lyricsPromo]): its
 * words, a tap opens its link, the X puts it away, and either counts as seen for this app version,
 * like a launch banner. Nothing at all while no entry fits this device. The words and the link live
 * in the remote config only. It wears the host page's own pill: glass over [backdrop] on the
 * fullscreen lyrics page, flat white like the floating buttons of the Apple Music player's lyrics tab
 * when [backdrop] is null.
 */
@Composable
fun LyricsPromoLine(
    backdrop: PlatformBackdrop?,
    modifier: Modifier = Modifier,
    height: Dp = 48.dp,
    sharedViewModel: SharedViewModel = koinInject(),
) {
    val promos by remember(sharedViewModel) { sharedViewModel.cachedLyricsPromos() }
        .collectAsStateWithLifecycle(emptyList())
    // Followed, not read once: the line can be on two pages at once (the Apple Music lyrics tab stays
    // composed under the fullscreen page), and closing it on one must take it off the other.
    val shown by remember(sharedViewModel) { sharedViewModel.shownPromosFlow() }
        .collectAsStateWithLifecycle(null)
    val language = Locale.current.toLanguageTag().substringBefore('-')
    var picked by remember { mutableStateOf<Pair<LyricsPromo, String>?>(null) }
    LaunchedEffect(promos, shown, language) {
        // Nothing until the seen ids are read, or a line already closed would flash up.
        val seen = shown ?: return@LaunchedEffect
        picked =
            lyricsPromo(
                promos = promos,
                shown = seen,
                appVersion = VersionManager.getVersionName(),
                language = language,
                localTrackingEnabled = sharedViewModel.getLocalTrackingEnabled().first() == TRUE,
                os = osRequirementOf(getPlatform().osName()),
            )
    }
    val (promo, text) = picked ?: return

    fun putAway() {
        promo.id?.let(sharedViewModel::markPromoShown)
        picked = null
    }

    val shape = RoundedCornerShape(height / 2)
    Row(
        modifier =
            modifier
                .height(height)
                .then(
                    if (backdrop != null) {
                        Modifier.liquidGlass(backdrop, shape)
                    } else {
                        Modifier.clip(shape).background(Color.White.copy(alpha = 0.24f))
                    },
                ).clickable {
                    putAway()
                    // Always https (lyricsPromo). openUrl rather than LocalUriHandler, which throws
                    // where no browser answers.
                    runCatching { openUrl(promo.link.orEmpty()) }
                }.padding(start = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = SimpIcons.MicOff, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = typo().bodyMedium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Icon(imageVector = SimpIcons.ArrowOutward, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        Box(
            modifier = Modifier.size(height).clickable { putAway() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = SimpIcons.Close, contentDescription = "", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
        }
    }
}
