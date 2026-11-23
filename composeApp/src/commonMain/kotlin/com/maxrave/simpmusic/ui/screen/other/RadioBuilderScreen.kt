package com.maxrave.simpmusic.ui.screen.other

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.maxrave.domain.data.model.radiobuilder.RadioBuilder
import com.maxrave.domain.data.model.radiobuilder.RadioChipIcon
import com.maxrave.domain.data.model.radiobuilder.RadioSeed
import com.maxrave.domain.data.model.radiobuilder.RadioSeedKind
import com.maxrave.domain.data.model.radiobuilder.RadioTuningSection
import com.maxrave.domain.data.model.radiobuilder.RadioTuningStyle
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.expect.ui.DesktopMiniPlayerClearance
import com.maxrave.simpmusic.expect.ui.PlatformBackHandler
import com.maxrave.simpmusic.expect.ui.VerticalScrollBar
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.ui.component.CenterLoadingBox
import com.maxrave.simpmusic.ui.icon.ArrowBackIosNew
import com.maxrave.simpmusic.ui.icon.Check
import com.maxrave.simpmusic.ui.icon.Close
import com.maxrave.simpmusic.ui.icon.GraphicEq
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.Tune
import com.maxrave.simpmusic.ui.navigation.destination.home.RadioBuilderDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.PlaylistDestination
import com.maxrave.simpmusic.ui.theme.ArtworkShape
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.RadioBuilderUIState
import com.maxrave.simpmusic.viewModel.RadioBuilderViewModel
import com.maxrave.simpmusic.viewModel.groups
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.cancel
import simpmusic.composeapp.generated.resources.retry

/**
 * YouTube Music's radio builder, laid out like the official app: "Pick music" (a grid of seeds),
 * then "Adjust" (variety, discovery, filters). Every label on both pages is YouTube's own; Done plays
 * the radio and closes the page.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadioBuilderScreen(
    navController: NavController,
    innerPadding: PaddingValues,
    viewModel: RadioBuilderViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedSeeds by viewModel.selectedSeeds.collectAsStateWithLifecycle()
    val selectedChips by viewModel.selectedChips.collectAsStateWithLifecycle()
    var tuning by rememberSaveable { mutableStateOf(false) }
    val page = (uiState as? RadioBuilderUIState.Success)?.page
    // Desktop's capsule player floats over the page; on Android the bottom bar is in innerPadding.
    val bottomClearance =
        if (getPlatform() == Platform.Desktop) DesktopMiniPlayerClearance else innerPadding.calculateBottomPadding()

    PlatformBackHandler(enabled = tuning) { tuning = false }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        TopAppBar(
            title = {
                Text(
                    text = (if (tuning) page?.tuningTitle else page?.title).orEmpty(),
                    style = typo().titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            navigationIcon = {
                if (tuning) {
                    IconButton(onClick = { tuning = false }) {
                        Icon(SimpIcons.ArrowBackIosNew, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            },
            actions = {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(
                        SimpIcons.Close,
                        contentDescription = stringResource(Res.string.cancel),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        )
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (val state = uiState) {
                RadioBuilderUIState.Loading -> CenterLoadingBox(Modifier.fillMaxSize())
                is RadioBuilderUIState.Error -> {
                    Column(
                        Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(state.message, style = typo().bodyMedium, textAlign = TextAlign.Center)
                        TextButton(onClick = viewModel::load) {
                            Text(stringResource(Res.string.retry), color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                is RadioBuilderUIState.Success -> {
                    AnimatedContent(targetState = tuning) { showTuning ->
                        if (showTuning) {
                            TuningPage(
                                page = state.page,
                                selectedChips = selectedChips,
                                onPick = viewModel::pickChip,
                            )
                        } else {
                            SeedGrid(
                                seeds = state.page.seeds,
                                selected = selectedSeeds,
                                onToggle = viewModel::toggleSeed,
                            )
                        }
                    }
                }
            }
        }
        if (page != null) {
            BottomBar(
                tuneLabel = page.tuneLabel,
                doneLabel = page.doneLabel.orEmpty(),
                doneEnabled = selectedSeeds.isNotEmpty(),
                tuning = tuning,
                onTune = { tuning = !tuning },
                // Done opens the radio in the Playlist page's radio mode, replacing this page.
                onDone = {
                    viewModel.radioId()?.let { id ->
                        navController.navigate(PlaylistDestination(playlistId = id)) {
                            popUpTo<RadioBuilderDestination> { inclusive = true }
                        }
                    }
                },
                modifier = Modifier.padding(bottom = bottomClearance),
            )
        }
    }
}

@Composable
private fun SeedGrid(
    seeds: List<RadioSeed>,
    selected: List<String>,
    onToggle: (String) -> Unit,
) {
    val gridState = rememberLazyGridState()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Three across on a phone, as in the official app; a wide window gets as many 150dp tiles as
        // fit, counted here so the last row can be filled. The gaps are the items' own padding, so a
        // tile's ripple has room around its picture.
        val columns =
            if (maxWidth < 600.dp) {
                3
            } else {
                ((maxWidth - GRID_EDGE * 2) / (150.dp + ITEM_PADDING_X * 2)).toInt().coerceAtLeast(1)
            }
        val shown = remember(seeds, columns, selected) { seeds.endingOnFullRow(columns, selected) }
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = GRID_EDGE),
        ) {
            items(shown, key = { it.token }) { seed ->
                // Related artists fade in and the tiles after them slide along, instead of jumping.
                SeedItem(
                    seed = seed,
                    selected = seed.token in selected,
                    onClick = { onToggle(seed.token) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
        VerticalScrollBar(scrollState = gridState)
    }
}

// Half the old 14dp/20dp gaps on each side of a tile, and the 16dp page edge less a tile's own padding,
// so the pictures sit where they did.
private val ITEM_PADDING_X = 7.dp
private val ITEM_PADDING_Y = 10.dp
private val GRID_EDGE = 16.dp - ITEM_PADDING_X

/**
 * The grid ending on a full row: a short last row reads as cut off, so the extra tiles are taken from
 * the artists, which there are hundreds of, starting from the end and never one that is [picked].
 * Left as it is when there are not enough such artists.
 */
private fun List<RadioSeed>.endingOnFullRow(
    columns: Int,
    picked: List<String>,
): List<RadioSeed> {
    val extra = size % columns
    if (extra == 0) return this
    val dropped =
        asReversed()
            .asSequence()
            .filter { it.kind == RadioSeedKind.ARTIST && it.token !in picked }
            .take(extra)
            .map { it.token }
            .toSet()
    return if (dropped.size < extra) this else filterNot { it.token in dropped }
}

@Composable
private fun SeedItem(
    seed: RadioSeed,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isArtist = seed.kind == RadioSeedKind.ARTIST
    val shape: Shape = if (isArtist) CircleShape else ArtworkShape
    Column(
        modifier
            .clip(ArtworkShape)
            .clickable(onClick = onClick)
            .padding(horizontal = ITEM_PADDING_X, vertical = ITEM_PADDING_Y),
        horizontalAlignment = if (isArtist) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            if (seed.kind == RadioSeedKind.GENRE) {
                // A genre has no picture; the iOS app draws a tile with a waveform on it.
                Box(
                    Modifier.fillMaxSize().background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        SimpIcons.GraphicEq,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.fillMaxSize(0.4f),
                    )
                }
            } else {
                AsyncImage(
                    model =
                        ImageRequest
                            .Builder(LocalPlatformContext.current)
                            .data(seed.thumbnail)
                            .crossfade(true)
                            .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (selected) {
                Box(
                    Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(SimpIcons.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = seed.title,
            style = typo().bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = if (seed.subtitle != null) 1 else 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (isArtist) TextAlign.Center else TextAlign.Start,
        )
        seed.subtitle?.let {
            Text(
                text = it,
                style = typo().bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TuningPage(
    page: RadioBuilder,
    selectedChips: Map<Int, String>,
    onPick: (groupIndex: Int, chipId: String, required: Boolean) -> Unit,
) {
    val scroll = rememberScrollState()
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // A phone-width column, as the official page is: stretched across a desktop window the
            // circles grew to a third of it each and pushed the filters under the bottom bar.
            Column(
                Modifier.widthIn(max = TUNING_MAX_WIDTH).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                // Groups are keyed by their index across all sections, the order the radio id needs.
                var firstGroup = 0
                page.sections.forEach { section ->
                    val offset = firstGroup
                    firstGroup += section.groups.size
                    TuningSection(section = section, groupOffset = offset, selectedChips = selectedChips, onPick = onPick)
                }
            }
        }
        VerticalScrollBar(scrollState = scroll)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TuningSection(
    section: RadioTuningSection,
    groupOffset: Int,
    selectedChips: Map<Int, String>,
    onPick: (groupIndex: Int, chipId: String, required: Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(section.title, style = typo().bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        when (section.style) {
            RadioTuningStyle.SEGMENTED -> {
                section.groups.forEachIndexed { i, group ->
                    val index = groupOffset + i
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(4.dp),
                    ) {
                        group.chips.forEach { chip ->
                            val picked = selectedChips[index] == chip.id
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (picked) MaterialTheme.colorScheme.onSurface else Color.Transparent)
                                    .clickable { onPick(index, chip.id, group.required) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = chip.label,
                                    style = typo().bodyMedium,
                                    color = if (picked) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }

            RadioTuningStyle.CIRCLES -> {
                section.groups.forEachIndexed { i, group ->
                    val index = groupOffset + i
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        group.chips.forEach { chip ->
                            val picked = selectedChips[index] == chip.id
                            Column(
                                Modifier
                                    .weight(1f)
                                    // The filled circle is the feedback; a hover box around it read as a stray grey card.
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                    ) { onPick(index, chip.id, group.required) }
                                    .padding(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxWidth(0.9f)
                                        .widthIn(max = CIRCLE_MAX_SIZE)
                                        .aspectRatio(1f)
                                        .clip(CircleShape)
                                        .then(
                                            if (picked) {
                                                Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                                            } else {
                                                Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                            },
                                        ),
                                ) {
                                    chip.icon?.let { icon ->
                                        DiscoveryGlyph(
                                            icon = icon,
                                            color =
                                                if (picked) {
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                },
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = chip.label,
                                    style = typo().bodyMedium,
                                    fontWeight = if (picked) FontWeight.Bold else FontWeight.Normal,
                                    color = if (picked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }

            RadioTuningStyle.CHIPS -> {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    section.groups.forEachIndexed { i, group ->
                        val index = groupOffset + i
                        group.chips.forEach { chip ->
                            val picked = selectedChips[index] == chip.id
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (picked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceContainerHigh,
                                    ).clickable { onPick(index, chip.id, group.required) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                            ) {
                                Text(
                                    text = chip.label,
                                    style = typo().bodyMedium,
                                    color = if (picked) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomBar(
    tuneLabel: String?,
    doneLabel: String,
    doneEnabled: Boolean,
    tuning: Boolean,
    onTune: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Lit while the tuning page is open, in the colours of a picked choice on it, so it reads as the
    // way back to the grid.
    val tuneContainer by animateColorAsState(
        if (tuning) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
    )
    val tuneContent by animateColorAsState(
        if (tuning) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
    )
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(
            Modifier
                .widthIn(max = TUNING_MAX_WIDTH)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(tuneContainer)
                    .clickable(onClick = onTune),
                contentAlignment = Alignment.Center,
            ) {
                Icon(SimpIcons.Tune, contentDescription = tuneLabel, tint = tuneContent)
            }
            Box(
                Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (doneEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceContainerHigh,
                    ).clickable(enabled = doneEnabled, onClick = onDone),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = doneLabel,
                    style = typo().titleSmall,
                    color = if (doneEnabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * The pictures YouTube Music draws in its music-discovery circles, redrawn here since YouTube sends
 * only their names: Familiar is arcs held to the sides of a centre, close to what is known; Discover
 * is rings each turned further, spiralling outwards; Blend sits between, with arcs on both sides of
 * every ring.
 */
@Composable
private fun DiscoveryGlyph(
    icon: RadioChipIcon,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val unit = size.minDimension / 2f
        val stroke = Stroke(width = (unit * 0.035f).coerceAtLeast(1.dp.toPx()), cap = StrokeCap.Round)
        fun arc(
            radius: Float,
            start: Float,
            sweep: Float,
        ) = drawArc(
            color = color,
            startAngle = start,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = stroke,
        )
        when (icon) {
            RadioChipIcon.FAMILIAR -> {
                drawCircle(color, radius = unit * 0.08f, style = stroke)
                for (k in 2..6) {
                    val radius = unit * 0.12f * k
                    arc(radius, 145f, 70f)
                    arc(radius, -35f, 70f)
                }
            }

            RadioChipIcon.BLEND -> {
                drawCircle(color, radius = unit * 0.08f, style = stroke)
                for (k in 2..6) {
                    val radius = unit * 0.12f * k
                    val turn = k * 25f
                    arc(radius, 120f + turn, 110f)
                    arc(radius, -60f + turn, 110f)
                }
            }

            RadioChipIcon.DISCOVER -> {
                for (k in 1..6) arc(unit * 0.12f * k, k * 55f, 300f)
            }
        }
    }
}

private val TUNING_MAX_WIDTH = 560.dp
private val CIRCLE_MAX_SIZE = 96.dp
