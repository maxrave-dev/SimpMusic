package com.maxrave.simpmusic.ui.component.taste

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.maxrave.domain.data.model.taste.TasteException
import com.maxrave.domain.data.model.taste.TasteProfile
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.expect.copyToClipboard
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.icon.ContentCopy
import com.maxrave.simpmusic.ui.icon.Error
import com.maxrave.simpmusic.ui.icon.Share
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.TipsAndUpdates
import com.maxrave.simpmusic.ui.screen.home.analytics.formatChartDay
import com.maxrave.simpmusic.ui.theme.LocalIsDarkTheme
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.TasteUiState
import multiplatform.network.cmptoast.ToastGravity
import multiplatform.network.cmptoast.showToast
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.copied_to_clipboard
import simpmusic.composeapp.generated.resources.retry
import simpmusic.composeapp.generated.resources.settings
import simpmusic.composeapp.generated.resources.share
import simpmusic.composeapp.generated.resources.taste_created_on
import simpmusic.composeapp.generated.resources.taste_error_generic
import simpmusic.composeapp.generated.resources.taste_error_title
import simpmusic.composeapp.generated.resources.taste_error_with_code
import simpmusic.composeapp.generated.resources.taste_eyebrow
import simpmusic.composeapp.generated.resources.taste_invite
import simpmusic.composeapp.generated.resources.taste_loading
import simpmusic.composeapp.generated.resources.taste_loading_title
import simpmusic.composeapp.generated.resources.taste_not_configured
import simpmusic.composeapp.generated.resources.taste_not_configured_title
import simpmusic.composeapp.generated.resources.taste_not_enough_data
import simpmusic.composeapp.generated.resources.taste_regenerate
import simpmusic.composeapp.generated.resources.taste_title

/**
 * The "Your music taste" card in Your Library, in every state the reading goes through.
 *
 * Built from the same vocabulary as [com.maxrave.simpmusic.ui.component.WrappedEntryCard], which it
 * sits a few rows away from: an [ElevatedCard] at row height with a lead image, two lines and a
 * primary disc. Only the finished reading grows past row height, because it is text to read.
 */
@Composable
fun TasteCard(
    state: TasteUiState,
    onGenerate: () -> Unit,
    onOpenSettings: () -> Unit,
    onShare: (TasteProfile) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.animateContentSize()) {
        when (state) {
            TasteUiState.Hidden -> Unit

            is TasteUiState.Invite ->
                TasteRowCard(
                    images = state.artistImages,
                    title = stringResource(Res.string.taste_title),
                    subtitle = stringResource(Res.string.taste_invite),
                    onClick = onGenerate,
                ) { TasteDisc() }

            is TasteUiState.NotEnoughData ->
                TasteRowCard(
                    images = state.artistImages,
                    title = stringResource(Res.string.taste_title),
                    subtitle =
                        stringResource(
                            Res.string.taste_not_enough_data,
                            state.requiredDays.toString(),
                            state.activeDays.toString(),
                        ),
                ) { TasteDisc(enabled = false) }

            is TasteUiState.Loading ->
                TasteRowCard(
                    images = state.artistImages,
                    title = stringResource(Res.string.taste_loading_title),
                    subtitle = stringResource(Res.string.taste_loading),
                ) { TasteDisc(loading = true) }

            is TasteUiState.Failed -> {
                val notConfigured = state.kind == TasteException.Kind.NOT_CONFIGURED
                // A refused key or model is fixed in Settings; anything else is worth another try.
                val fixInSettings = state.kind != TasteException.Kind.TEMPORARY
                val detail = state.detail?.takeIf { it.isNotBlank() } ?: stringResource(Res.string.taste_error_generic)
                TasteRowCard(
                    images = state.artistImages,
                    title = stringResource(if (notConfigured) Res.string.taste_not_configured_title else Res.string.taste_error_title),
                    subtitle =
                        when {
                            notConfigured -> stringResource(Res.string.taste_not_configured)
                            state.statusCode != null -> stringResource(Res.string.taste_error_with_code, detail, state.statusCode.toString())
                            else -> detail
                        },
                    isError = true,
                ) {
                    FilledTonalButton(
                        onClick = if (fixInSettings) onOpenSettings else onGenerate,
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        modifier = Modifier.height(36.dp),
                    ) {
                        Text(
                            text = stringResource(if (fixInSettings) Res.string.settings else Res.string.retry),
                            color = LocalContentColor.current,
                        )
                    }
                }
            }

            is TasteUiState.Ready ->
                TasteReadingCard(
                    profile = state.profile,
                    regenerating = state.regenerating,
                    onRegenerate = onGenerate,
                    onShare = { onShare(state.profile) },
                )
        }
    }
}

@Composable
private fun TasteRowCard(
    images: List<String>,
    title: String,
    subtitle: String,
    isError: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit,
) {
    val container = libraryCardColor()
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(CARD_RADIUS),
        // Flat, like the collection cards above it on the Your library tab.
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.elevatedCardColors().copy(containerColor = container),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(GUTTER),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (images.isNotEmpty()) {
                ArtistStack(images = images, size = 38.dp, overlap = 12.dp, ring = container)
                Spacer(Modifier.width(14.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isError) {
                        Icon(
                            imageVector = SimpIcons.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = title,
                        style = typo().titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    style = typo().bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

/** The primary disc of [com.maxrave.simpmusic.ui.component.WrappedEntryCard], with the AI glyph in it. */
@Composable
private fun TasteDisc(
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    Surface(
        shape = CircleShape,
        color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
        modifier = Modifier.size(DISC_SIZE),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(DISC_GLYPH_SIZE),
                    color = onPrimary,
                    trackColor = onPrimary.copy(alpha = 0.25f),
                    strokeWidth = 3.dp,
                )
            } else {
                Icon(
                    imageVector = SimpIcons.TipsAndUpdates,
                    contentDescription = null,
                    tint = if (enabled) onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    modifier = Modifier.size(DISC_GLYPH_SIZE),
                )
            }
        }
    }
}

/** The finished reading: kept on the card until the user asks for a new one. */
@Composable
private fun TasteReadingCard(
    profile: TasteProfile,
    regenerating: Boolean,
    onRegenerate: () -> Unit,
    onShare: () -> Unit,
) {
    val container = libraryCardColor()
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CARD_RADIUS),
        // Flat, like the collection cards above it on the Your library tab.
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.elevatedCardColors().copy(containerColor = container),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(GUTTER)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (profile.artistImages.isNotEmpty()) {
                    ArtistStack(images = profile.artistImages, size = 24.dp, overlap = 7.dp, ring = container)
                    Spacer(Modifier.width(10.dp))
                }
                Text(
                    text = stringResource(Res.string.taste_eyebrow).toUpperCase(Locale.current),
                    style = typo().bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                // Up here rather than beside Regenerate and Share: a third control in that row
                // leaves the date only a few characters on a 360dp phone.
                val copiedMessage = stringResource(Res.string.copied_to_clipboard)
                RippleIconButton(
                    imageVector = SimpIcons.ContentCopy,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                ) {
                    copyToClipboard(label = profile.headline, text = "${profile.headline}\n\n${profile.summary}")
                    showToast(copiedMessage, ToastGravity.Bottom)
                }
            }
            Spacer(Modifier.height(12.dp))
            // Runs the card's full width: on Desktop the Your Library tab is already held to the
            // capsule player's width, so a cap of its own here only stopped the lines short.
            Text(
                text = profile.headline,
                style = typo().titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = profile.summary,
                style = typo().bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(Res.string.taste_created_on, formatChartDay(profile.generatedAt.date)),
                    style = typo().bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (regenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(horizontal = 12.dp).size(20.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    TextButton(onClick = onRegenerate) {
                        Text(
                            text = stringResource(Res.string.taste_regenerate),
                            color = LocalContentColor.current,
                        )
                    }
                }
                Spacer(Modifier.width(4.dp))
                Button(
                    onClick = onShare,
                    enabled = !regenerating,
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                ) {
                    Icon(
                        imageVector = SimpIcons.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(Res.string.share),
                        color = LocalContentColor.current,
                    )
                }
            }
        }
    }
}

/**
 * The listener's top artists as overlapping discs, the first one on top.
 *
 * Each disc carries a [ring] in the colour of whatever it sits on, which is what keeps three
 * overlapping pictures reading as three faces rather than one blot.
 */
@Composable
internal fun ArtistStack(
    images: List<String>,
    size: Dp,
    overlap: Dp,
    ring: Color,
    modifier: Modifier = Modifier,
) {
    if (images.isEmpty()) return
    val step = size - overlap
    Box(modifier.width(size + step * (images.size - 1)).height(size)) {
        images.forEachIndexed { index, url ->
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .offset(x = step * index)
                        .zIndex((images.size - index).toFloat())
                        .size(size)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .border(2.dp, ring, CircleShape),
            )
        }
    }
}

/**
 * The container every card on the Your library tab shares — this one and the collection cards above
 * it. [com.maxrave.simpmusic.ui.component.WrappedEntryCard]'s container, except on a light Desktop:
 * there the panel underneath already is a light grey, and the card would only show as a shadow, so
 * it lifts to white — the same rule the grouped Settings cards follow.
 */
@Composable
internal fun libraryCardColor(): Color =
    if (getPlatform() == Platform.Desktop && !LocalIsDarkTheme.current) {
        MaterialTheme.colorScheme.surfaceContainerLowest
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }

private val GUTTER = 14.dp

private val CARD_RADIUS = 20.dp

private val DISC_SIZE = 44.dp

private val DISC_GLYPH_SIZE = 22.dp
