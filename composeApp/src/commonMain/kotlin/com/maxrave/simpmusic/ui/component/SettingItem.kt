package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.extension.greyScale
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.ui.theme.LocalIsDarkTheme
import com.maxrave.simpmusic.ui.theme.typo

@Composable
fun SettingItem(
    title: String = "Title",
    subtitle: String = "Subtitle",
    smallSubtitle: Boolean = false,
    isEnable: Boolean = true,
    onClick: (() -> Unit)? = null,
    switch: Pair<Boolean, ((Boolean) -> Unit)>? = null,
    otherView: @Composable (() -> Unit)? = null,
) {
    // isEnable only greys the row out; it deliberately writes nothing.
    //
    // There used to be an onDisable callback fired from a LaunchedEffect(isEnable) here, to clear a
    // login-gated child flag when its gate went false (#2157, #2064). It was wrong twice over. The
    // gate is a StateFlow filled asynchronously from DataStore, so the FIRST composition always sees
    // its "not loaded yet" value and read it as "signed out" — sync_follow_to_youtube was erased on
    // every visit to Settings. And it only ever ran if the user opened Settings and scrolled to the
    // row at all, so it could not do the job it existed for. Clearing a child flag now belongs to the
    // logout that invalidates it (SettingsViewModel: setSpotifyLogIn, logOutDiscord, logOutLastfm,
    // setUsedAccount, logOutAllYouTube, setAIApiKey), which is a real event and cannot misfire.
    val dividerColor = MaterialTheme.colorScheme.outlineVariant
    Box(
        Modifier
            // Rows separate themselves: a hairline along the top, from where the text starts to the
            // card's far edge (mirrored right-to-left). SettingGroup leaves the first row's undrawn,
            // so a row hidden by a condition never leaves a stray line at the top of the card.
            .drawBehind {
                val y = SettingDividerThickness.toPx() / 2
                val inset = SettingRowPaddingHorizontal.toPx()
                val rtl = layoutDirection == LayoutDirection.Rtl
                drawLine(
                    color = dividerColor,
                    start = Offset(if (rtl) 0f else inset, y),
                    end = Offset(if (rtl) size.width - inset else size.width, y),
                    strokeWidth = SettingDividerThickness.toPx(),
                )
            }.then(
                if (onClick != null && isEnable) {
                    Modifier.clickable { onClick.invoke() }
                } else {
                    Modifier
                },
            ).then(
                if (!isEnable) {
                    Modifier.greyScale()
                } else {
                    Modifier
                },
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = 12.dp,
                        horizontal = SettingRowPaddingHorizontal,
                    ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = title,
                    style =
                        typo().labelMedium.let {
                            if (!isEnable) it.greyScale() else it
                        },
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style =
                        if (smallSubtitle) {
                            typo().bodySmall.let {
                                if (!isEnable) it.greyScale() else it
                            }
                        } else {
                            typo().bodyMedium.let {
                                if (!isEnable) it.greyScale() else it
                            }
                        },
                    // No maxLines: with a cap and no overflow set, Compose defaults to Clip and cut
                    // the second line through the middle of the glyphs, with no ellipsis to show
                    // anything was missing. A settings list scrolls vertically anyway, and these
                    // descriptions are translated — German and Vietnamese run longer than the
                    // English they were sized against, so any fixed cap just moves the problem to
                    // another language.
                )

                otherView?.let {
                    Spacer(Modifier.height(16.dp))
                    it.invoke()
                }
            }
            if (switch != null) {
                Spacer(Modifier.width(10.dp))
                Switch(
                    modifier = Modifier.wrapContentWidth(),
                    checked = switch.first,
                    onCheckedChange = {
                        switch.second.invoke(it)
                    },
                    enabled = isEnable,
                )
            }
        }
    }
}

/**
 * One group of the Settings screen, laid out the way Apple Music groups its settings: a small
 * header above, the rows on one rounded card, and an optional note under the card ([footer]).
 */
@Composable
fun SettingGroup(
    title: String,
    modifier: Modifier = Modifier,
    footer: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val cardColor = settingGroupColor()
    Column(modifier.fillMaxWidth().padding(top = 24.dp)) {
        Text(
            text = title.toUpperCase(Locale.current),
            // The headers' size from before the groups; only the weight drops, so they step back.
            style = typo().labelMedium.copy(fontWeight = FontWeight.Normal),
            modifier = Modifier.padding(start = SettingRowPaddingHorizontal, end = SettingRowPaddingHorizontal, bottom = 8.dp),
        )
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(SettingGroupShape)
                    .background(cardColor.copy(alpha = 0.5f))
                    .drawWithContent {
                        // The first row's hairline would sit on the card's own edge, so that strip is
                        // left undrawn. Clipped rather than painted over: the card is translucent.
                        clipRect(top = SettingDividerThickness.toPx()) { this@drawWithContent.drawContent() }
                    },
            content = content,
        )
        if (footer != null) {
            Box(Modifier.padding(start = SettingRowPaddingHorizontal, end = SettingRowPaddingHorizontal, top = 8.dp)) {
                footer()
            }
        }
    }
}

// One step off the page the card sits on. Android's page is the theme background (black, or
// #FAFAFA); Desktop's is a panel, and its light panel is already surfaceContainer — the card's
// own colour — so there the card lifts to white instead.
@Composable
private fun settingGroupColor(): Color =
    if (getPlatform() == Platform.Desktop && !LocalIsDarkTheme.current) {
        MaterialTheme.colorScheme.surfaceContainerLowest
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }

private val SettingGroupShape = RoundedCornerShape(20.dp)
private val SettingRowPaddingHorizontal = 16.dp
private val SettingDividerThickness = 0.5.dp
