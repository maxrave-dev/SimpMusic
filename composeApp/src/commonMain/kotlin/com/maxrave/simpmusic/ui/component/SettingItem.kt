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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.Layout
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
import java.text.Normalizer

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
    // While Settings is searched, a row that answers nothing typed is not drawn at all; a group left
    // with no rows then folds itself away (SettingGroup).
    if (!settingShown(title, subtitle)) return
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
    // The disabled colour the sheets use (SurfaceDarkColors.disabled). TextStyle.greyScale() alone
    // left a disabled title as bright as an enabled one, because the title passes its own colour,
    // and an enabled subtitle is already grey.
    val disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
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
                    style = typo().labelMedium,
                    color = if (isEnable) MaterialTheme.colorScheme.onSurface else disabledContentColor,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = if (smallSubtitle) typo().bodySmall else typo().bodyMedium,
                    color = if (isEnable) Color.Unspecified else disabledContentColor,
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
    val query = LocalSettingSearch.current
    // A group whose own name is searched shows whole, as if nothing were typed.
    val titleMatches = query.isNotEmpty() && searchKey(title).contains(query)
    val filtering = query.isNotEmpty() && !titleMatches
    CompositionLocalProvider(LocalSettingSearch provides if (titleMatches) "" else query) {
        Layout(
            contents =
                listOf(
                    {
                        Text(
                            text = title.toUpperCase(Locale.current),
                            // The headers' size from before the groups; only the weight drops, so they step back.
                            style = typo().labelMedium.copy(fontWeight = FontWeight.Normal),
                            modifier = Modifier.padding(start = SettingRowPaddingHorizontal, end = SettingRowPaddingHorizontal, bottom = 8.dp),
                        )
                    },
                    {
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
                    },
                    {
                        // A note about the whole group says nothing about the few rows a search leaves.
                        if (footer != null && !filtering) {
                            Box(Modifier.padding(start = SettingRowPaddingHorizontal, end = SettingRowPaddingHorizontal, top = 8.dp)) {
                                footer()
                            }
                        }
                    },
                ),
            modifier = modifier.fillMaxWidth(),
        ) { (headers, cards, footers), constraints ->
            val loose = constraints.copy(minWidth = 0, minHeight = 0)
            val card = cards.map { it.measure(loose) }
            // Decided while measuring, not from state: rows that do not match draw nothing, so a card
            // with none left is 0 tall, and the header and the 24dp above it go with it in the same
            // frame. A 0-tall item also lets the lazy list go on composing the groups below.
            if (filtering && card.all { it.height == 0 }) return@Layout layout(constraints.maxWidth, 0) {}
            val header = headers.map { it.measure(loose) }
            val note = footers.map { it.measure(loose) }
            val top = SettingGroupTopPadding.roundToPx()
            val headerHeight = header.sumOf { it.height }
            val cardHeight = card.sumOf { it.height }
            layout(constraints.maxWidth, top + headerHeight + cardHeight + note.sumOf { it.height }) {
                header.forEach { it.placeRelative(0, top) }
                card.forEach { it.placeRelative(0, top + headerHeight) }
                note.forEach { it.placeRelative(0, top + headerHeight + cardHeight) }
            }
        }
    }
}

/** What the Settings search field holds, already put through [searchKey]; empty while nothing is typed. */
val LocalSettingSearch = compositionLocalOf { "" }

/** Lower case, accents dropped and đ read as d, so typing "loi bai hat" finds "Lời bài hát". */
fun searchKey(text: String): String =
    Normalizer
        .normalize(text.lowercase(), Normalizer.Form.NFD)
        .replace(CombiningMarks, "")
        .replace('đ', 'd')

/** Whether a row with these texts stays on screen for what is typed in the Settings search. */
@Composable
fun settingShown(vararg texts: String): Boolean {
    val query = LocalSettingSearch.current
    return query.isEmpty() || texts.any { searchKey(it).contains(query) }
}

private val CombiningMarks = Regex("\\p{Mn}+")

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

internal val SettingGroupShape = RoundedCornerShape(20.dp)
private val SettingRowPaddingHorizontal = 16.dp
private val SettingDividerThickness = 0.5.dp
private val SettingGroupTopPadding = 24.dp
