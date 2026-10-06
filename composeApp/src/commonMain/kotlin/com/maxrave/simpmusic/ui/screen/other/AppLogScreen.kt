package com.maxrave.simpmusic.ui.screen.other

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.expect.copyToClipboard
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.ui.component.Chip
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.icon.ArrowBackIosNew
import com.maxrave.simpmusic.ui.icon.Close
import com.maxrave.simpmusic.ui.icon.ContentCopy
import com.maxrave.simpmusic.ui.icon.Delete
import com.maxrave.simpmusic.ui.icon.Download
import com.maxrave.simpmusic.ui.icon.Error
import com.maxrave.simpmusic.ui.icon.KeyboardDoubleArrowDown
import com.maxrave.simpmusic.ui.icon.Pause
import com.maxrave.simpmusic.ui.icon.PlayArrow
import com.maxrave.simpmusic.ui.icon.RestartAlt
import com.maxrave.simpmusic.ui.icon.Search
import com.maxrave.simpmusic.ui.icon.Share
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.UnfoldLess
import com.maxrave.simpmusic.ui.icon.UnfoldMore
import com.maxrave.simpmusic.ui.theme.LocalIsDarkTheme
import com.maxrave.simpmusic.ui.theme.typo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds
import multiplatform.network.cmptoast.ToastGravity
import multiplatform.network.cmptoast.showToast
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.all
import simpmusic.composeapp.generated.resources.app_log
import simpmusic.composeapp.generated.resources.copied_to_clipboard
import simpmusic.composeapp.generated.resources.error
import simpmusic.composeapp.generated.resources.log_empty
import simpmusic.composeapp.generated.resources.log_errors
import simpmusic.composeapp.generated.resources.log_export_failed
import simpmusic.composeapp.generated.resources.log_hide_stack_trace
import simpmusic.composeapp.generated.resources.log_level_info
import simpmusic.composeapp.generated.resources.log_level_warn
import simpmusic.composeapp.generated.resources.log_live
import simpmusic.composeapp.generated.resources.log_no_stack_trace
import simpmusic.composeapp.generated.resources.log_paused
import simpmusic.composeapp.generated.resources.log_search_hint
import simpmusic.composeapp.generated.resources.log_stack_trace_lines
import simpmusic.composeapp.generated.resources.share
import simpmusic.composeapp.generated.resources.space_mono_regular

private val POLL_INTERVAL = 1.seconds

// The live view never holds more than this; the export always takes the whole files.
private const val MAX_SHOWN_LINES = 5_000

// Warn has no Material role. Amber by convention, the way the traffic lights in Color.kt are.
private val WarnDark = Color(0xFFFFCF6B)
private val WarnLight = Color(0xFF8C5E00)

/**
 * The on-disk app log that [Logger.enableFileLogging] writes, read back for a person.
 *
 * Nothing here holds the log while the screen is closed: the files are the log, and this page only
 * follows them (`tail -f` style) while it is open.
 */
@Composable
fun AppLogScreen(
    paddingValues: PaddingValues,
    navController: NavController,
) {
    val scope = rememberCoroutineScope()
    val mono = FontFamily(Font(Res.font.space_mono_regular))
    val onSurface = MaterialTheme.colorScheme.onSurface
    val isDesktop = getPlatform() == Platform.Desktop

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var minLevel by rememberSaveable { mutableStateOf('I') }
    var query by rememberSaveable { mutableStateOf("") }
    var paused by rememberSaveable { mutableStateOf(false) }
    // Clearing hides what came before instead of deleting it: the writer keeps the file open.
    var clearedAt by rememberSaveable { mutableLongStateOf(0L) }
    // Android Studio's "Scroll to the End": a mode that keeps the newest line in view as lines
    // arrive, not a one-off jump. A person scrolling up turns it off, scrolling back to the bottom
    // turns it on again, and the toolbar button does either at will.
    var stickToBottom by rememberSaveable { mutableStateOf(true) }
    // Bumped by Restart, which re-reads the files from the top.
    var restarts by remember { mutableIntStateOf(0) }

    val appTail = remember { LogTail(Logger.APP_LOG, keep = MAX_SHOWN_LINES) }
    val errorTail = remember { LogTail(Logger.ERROR_LOG, keep = Int.MAX_VALUE) }
    // The tails are not thread-safe, and a restart cancels a loop that may still be reading on IO.
    val tailLock = remember { Mutex() }
    var appLines by remember { mutableStateOf(emptyList<LogLine>()) }
    var errorLines by remember { mutableStateOf(emptyList<LogLine>()) }
    val isPaused by rememberUpdatedState(paused)
    LaunchedEffect(restarts) {
        if (restarts > 0) {
            tailLock.withLock {
                appTail.reset()
                errorTail.reset()
            }
        }
        while (true) {
            if (!isPaused) {
                val (app, errors) =
                    tailLock.withLock {
                        withContext(Dispatchers.IO) {
                            (if (appTail.poll()) appTail.lines else null) to
                                (if (errorTail.poll()) errorTail.lines else null)
                        }
                    }
                app?.let { appLines = it }
                errors?.let { errorLines = it }
            }
            delay(POLL_INTERVAL)
        }
    }

    val shownLines =
        remember(appLines, minLevel, query, clearedAt) {
            val floor = levelRank(minLevel)
            appLines.filter { it.epochMillis > clearedAt && levelRank(it.level) >= floor && it.matches(query) }
        }
    // Newest first: an error is read from the top, unlike the running log.
    val shownErrors =
        remember(errorLines, query, clearedAt) {
            errorLines.filter { it.epochMillis > clearedAt && it.matches(query) }.asReversed()
        }
    val errorCount = remember(errorLines, clearedAt) { errorLines.count { it.epochMillis > clearedAt } }

    val logListState = rememberLazyListState()
    // Only a person scrolling reaches onPostScroll; scrollToItem below is not a nested scroll, so
    // the jumps that keep the list at the bottom never switch the mode off by themselves. Asking
    // "am I at the end" once new lines arrive does not work instead: by then they are laid out below.
    val stickScroll =
        remember(logListState) {
            object : NestedScrollConnection {
                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    stickToBottom = !logListState.canScrollForward
                    return Offset.Zero
                }
            }
        }
    // Keyed on the newest line, not the count: once MAX_SHOWN_LINES is reached every new line
    // pushes an old one out and the count stops changing. The index past the last line is
    // EndOfPage, which lands on the very bottom.
    LaunchedEffect(shownLines.lastOrNull()?.id, stickToBottom, tab) {
        if (tab == 0 && stickToBottom && shownLines.isNotEmpty()) logListState.scrollToItem(shownLines.size)
    }

    val copied = stringResource(Res.string.copied_to_clipboard)
    val exportFailed = stringResource(Res.string.log_export_failed)
    val shareTitle = stringResource(Res.string.share)
    val copy = { line: LogLine ->
        copyToClipboard("App log", line.render())
        showToast(copied, ToastGravity.Bottom)
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 840.dp
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.app_log),
                        style = typo().titleMedium,
                    )
                },
                navigationIcon = {
                    Box(Modifier.padding(horizontal = 5.dp)) {
                        RippleIconButton(
                            SimpIcons.ArrowBackIosNew,
                            Modifier.size(32.dp),
                            true,
                            tint = onSurface,
                        ) {
                            navController.navigateUp()
                        }
                    }
                },
                actions = {
                    // Android Studio's Logcat toolbar, in its order: clear, pause, restart, stick.
                    RippleIconButton(SimpIcons.Delete, tint = onSurface) {
                        clearedAt = nowEpochMillis()
                    }
                    RippleIconButton(if (paused) SimpIcons.PlayArrow else SimpIcons.Pause, tint = onSurface) {
                        paused = !paused
                    }
                    RippleIconButton(SimpIcons.RestartAlt, tint = onSurface) {
                        clearedAt = 0L
                        paused = false
                        stickToBottom = true
                        restarts++
                    }
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (stickToBottom) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Color.Transparent,
                            ),
                    ) {
                        RippleIconButton(
                            SimpIcons.KeyboardDoubleArrowDown,
                            tint = if (stickToBottom) MaterialTheme.colorScheme.primary else onSurface,
                        ) {
                            stickToBottom = !stickToBottom
                        }
                    }
                    RippleIconButton(if (isDesktop) SimpIcons.Download else SimpIcons.Share, tint = onSurface) {
                        scope.launch {
                            if (!exportLog(shareTitle)) showToast(exportFailed, ToastGravity.Bottom)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
            LogTabs(
                selected = tab,
                errorCount = errorCount,
                onSelect = { tab = it },
                modifier = if (wide) Modifier.padding(start = 20.dp).width(300.dp) else Modifier.fillMaxWidth(),
            )
            val meta =
                stringResource(
                    if (paused) Res.string.log_paused else Res.string.log_live,
                    shownLines.size.toString(),
                )
            if (wide) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    LogSearchField(query, { query = it }, Modifier.width(360.dp))
                    if (tab == 0) {
                        LevelChips(minLevel) { minLevel = it }
                        Spacer(Modifier.weight(1f))
                        MetaText(meta)
                    }
                }
            } else {
                LogSearchField(query, { query = it }, Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp))
                if (tab == 0) {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        LevelChips(minLevel) { minLevel = it }
                    }
                    MetaText(meta, Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp))
                }
            }
            val bottomInset = paddingValues.calculateBottomPadding() + 16.dp
            Box(Modifier.fillMaxWidth().weight(1f)) {
                when {
                    tab == 0 && shownLines.isEmpty() || tab == 1 && shownErrors.isEmpty() -> {
                        Text(
                            text = stringResource(Res.string.log_empty),
                            style = typo().bodyMedium,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }

                    tab == 0 -> {
                        LazyColumn(
                            state = logListState,
                            contentPadding = PaddingValues(bottom = bottomInset),
                            modifier = Modifier.nestedScroll(stickScroll),
                        ) {
                            itemsIndexed(shownLines, key = { _, line -> line.id }) { index, line ->
                                if (index > 0) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = if (wide) 20.dp else 16.dp),
                                        thickness = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                    )
                                }
                                LogRow(line, mono, wide) { copy(line) }
                            }
                            item(key = "end") {
                                EndOfPage()
                            }
                        }
                    }

                    else -> {
                        var expanded by remember { mutableStateOf(emptySet<Long>()) }
                        LazyColumn(
                            contentPadding = PaddingValues(top = 12.dp, bottom = bottomInset),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(horizontal = if (wide) 20.dp else 16.dp),
                        ) {
                            itemsIndexed(shownErrors, key = { _, line -> line.id }) { _, line ->
                                ErrorCard(
                                    line = line,
                                    mono = mono,
                                    expanded = line.id in expanded,
                                    onToggle = { expanded = if (line.id in expanded) expanded - line.id else expanded + line.id },
                                    onCopy = { copy(line) },
                                )
                            }
                            item(key = "end") {
                                EndOfPage()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogTabs(
    selected: Int,
    errorCount: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    SecondaryTabRow(
        selectedTabIndex = selected,
        modifier = modifier,
        containerColor = Color.Transparent,
        divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) },
    ) {
        // typo() styles carry a colour, so each Text passes LocalContentColor or the tab's
        // selected/unselected colours never reach it.
        Tab(
            selected = selected == 0,
            onClick = { onSelect(0) },
            selectedContentColor = MaterialTheme.colorScheme.onSurface,
            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            text = { Text(stringResource(Res.string.all), style = typo().titleSmall, color = LocalContentColor.current) },
        )
        Tab(
            selected = selected == 1,
            onClick = { onSelect(1) },
            selectedContentColor = MaterialTheme.colorScheme.onSurface,
            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(Res.string.log_errors), style = typo().titleSmall, color = LocalContentColor.current)
                    if (errorCount > 0) {
                        Spacer(Modifier.width(6.dp))
                        Badge(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ) {
                            Text(errorCount.toString(), fontSize = 11.sp, color = LocalContentColor.current)
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun LogSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        singleLine = true,
        shape = RoundedCornerShape(8.dp),
        textStyle = typo().labelMedium.copy(color = MaterialTheme.colorScheme.onSurface),
        placeholder = { Text(stringResource(Res.string.log_search_hint), style = typo().labelMedium) },
        leadingIcon = { Icon(SimpIcons.Search, contentDescription = null) },
        trailingIcon =
            if (query.isNotEmpty()) {
                {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(SimpIcons.Close, contentDescription = null)
                    }
                }
            } else {
                null
            },
        colors =
            TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
    )
}

/** The lowest level shown: Info is everything the file holds, Error the errors alone. */
@Composable
private fun LevelChips(
    selected: Char,
    onSelect: (Char) -> Unit,
) {
    listOf(
        'I' to stringResource(Res.string.log_level_info),
        'W' to stringResource(Res.string.log_level_warn),
        'E' to stringResource(Res.string.error),
    ).forEach { (level, label) ->
        Chip(isSelected = selected == level, text = label) { onSelect(level) }
    }
}

@Composable
private fun MetaText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.primary),
        )
        Spacer(Modifier.width(6.dp))
        Text(text, style = typo().bodySmall)
    }
}

@Composable
private fun LogRow(
    line: LogLine,
    mono: FontFamily,
    wide: Boolean,
    onClick: () -> Unit,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val rowModifier =
        Modifier
            .fillMaxWidth()
            .then(
                if (line.isError) {
                    Modifier.background(MaterialTheme.colorScheme.error.copy(alpha = 0.09f))
                } else {
                    Modifier
                },
            ).clickable(onClick = onClick)
    if (wide) {
        Row(rowModifier.padding(horizontal = 20.dp, vertical = 7.dp)) {
            Text(
                clockTime(line.epochMillis),
                fontFamily = mono,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = onSurfaceVariant,
                modifier = Modifier.width(108.dp),
            )
            Spacer(Modifier.width(12.dp))
            LevelBadge(line.level, mono, Modifier.padding(top = 1.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                line.tag,
                fontFamily = mono,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(230.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                line.message,
                fontFamily = mono,
                fontSize = 12.5.sp,
                lineHeight = 18.sp,
                color = onSurface,
                maxLines = 8,
                overflow = TextOverflow.Ellipsis,
            )
        }
    } else {
        Column(rowModifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(clockTime(line.epochMillis), fontFamily = mono, fontSize = 11.sp, color = onSurfaceVariant)
                Spacer(Modifier.width(8.dp))
                LevelBadge(line.level, mono)
                Spacer(Modifier.width(8.dp))
                Text(line.tag, fontFamily = mono, fontSize = 11.sp, color = onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                line.message,
                fontFamily = mono,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = onSurface,
                maxLines = 8,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}

@Composable
private fun LevelBadge(
    level: Char,
    mono: FontFamily,
    modifier: Modifier = Modifier,
) {
    val color =
        when (level) {
            'E', 'A' -> MaterialTheme.colorScheme.error
            'W' -> if (LocalIsDarkTheme.current) WarnDark else WarnLight
            'I' -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    Box(
        modifier
            .size(16.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(level.toString(), fontFamily = mono, fontSize = 10.sp, color = color)
    }
}

@Composable
private fun ErrorCard(
    line: LogLine,
    mono: FontFamily,
    expanded: Boolean,
    onToggle: () -> Unit,
    onCopy: () -> Unit,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    // The Settings card colour: one step off the page, which on a light Desktop panel is white.
    val cardColor =
        if (getPlatform() == Platform.Desktop && !LocalIsDarkTheme.current) {
            MaterialTheme.colorScheme.surfaceContainerLowest
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        }
    val hasTrace = line.detail.isNotEmpty()
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(cardColor.copy(alpha = 0.5f))
            .clickable(enabled = hasTrace, onClick = onToggle)
            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(SimpIcons.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                line.tag,
                style = typo().titleSmall,
                color = onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(clockTime(line.epochMillis), fontFamily = mono, fontSize = 11.sp, color = onSurfaceVariant)
            RippleIconButton(SimpIcons.ContentCopy, tint = onSurfaceVariant, onClick = onCopy)
        }
        Text(
            line.message,
            fontFamily = mono,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = onSurface,
            modifier = Modifier.padding(end = 12.dp),
        )
        if (!hasTrace) {
            Text(
                stringResource(Res.string.log_no_stack_trace),
                style = typo().bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )
        } else {
            if (expanded) {
                // Frames stay one per line and scroll sideways; wrapping them breaks package names
                // in the middle, which is what makes a stack trace unreadable on a phone.
                Box(
                    Modifier
                        .padding(top = 10.dp, end = 12.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    Text(
                        line.detail,
                        fontFamily = mono,
                        fontSize = 10.5.sp,
                        lineHeight = 15.5.sp,
                        color = onSurfaceVariant,
                        softWrap = false,
                    )
                }
            }
            Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (expanded) SimpIcons.UnfoldLess else SimpIcons.UnfoldMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    if (expanded) {
                        stringResource(Res.string.log_hide_stack_trace)
                    } else {
                        stringResource(Res.string.log_stack_trace_lines, line.detail.lines().size.toString())
                    },
                    style = typo().titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
