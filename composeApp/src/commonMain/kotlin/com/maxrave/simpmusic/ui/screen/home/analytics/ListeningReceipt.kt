package com.maxrave.simpmusic.ui.screen.home.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.extension.now
import com.maxrave.simpmusic.ui.component.EntryCard
import com.maxrave.simpmusic.ui.component.ShareImageSheet
import com.maxrave.simpmusic.ui.component.lyrics.ShareLyricsCardMaxWidth
import com.maxrave.simpmusic.ui.icon.Share
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.theme.fontFamily
import com.maxrave.simpmusic.viewModel.AnalyticsUiState
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.receipt_amount
import simpmusic.composeapp.generated.resources.receipt_auth_code
import simpmusic.composeapp.generated.resources.receipt_card
import simpmusic.composeapp.generated.resources.receipt_cardholder
import simpmusic.composeapp.generated.resources.receipt_date
import simpmusic.composeapp.generated.resources.receipt_entry_subtitle
import simpmusic.composeapp.generated.resources.receipt_entry_title
import simpmusic.composeapp.generated.resources.receipt_item
import simpmusic.composeapp.generated.resources.receipt_item_count
import simpmusic.composeapp.generated.resources.receipt_order
import simpmusic.composeapp.generated.resources.receipt_order_anonymous
import simpmusic.composeapp.generated.resources.receipt_paper
import simpmusic.composeapp.generated.resources.receipt_qty
import simpmusic.composeapp.generated.resources.receipt_share_title
import simpmusic.composeapp.generated.resources.receipt_thanks
import simpmusic.composeapp.generated.resources.receipt_total
import simpmusic.composeapp.generated.resources.space_mono_regular
import kotlin.math.roundToInt

/** One printed row: how many times it was played, what it was, and how long it runs. */
internal data class ReceiptLine(
    val plays: Int,
    val item: String,
    val durationSeconds: Int,
)

/** Everything the listening receipt prints, worked out from what the Analytics screen shows. */
internal data class ListeningReceipt(
    val period: String,
    val orderNumber: Long,
    val name: String?,
    val printedOn: LocalDate,
    val lines: List<ReceiptLine>,
    /** Seeds the barcode and the auth code, so the same ten songs always print the same marks. */
    val seed: Int,
) {
    val itemCount: Int get() = lines.sumOf { it.plays }

    /** Receiptify's arithmetic: each line costs QTY × AMT, and the total is their sum. */
    val totalSeconds: Long get() = lines.sumOf { it.plays.toLong() * it.durationSeconds.coerceAtLeast(0) }

    val authCode: String get() = (seed.toUInt() % 1_000_000u).toString().padStart(6, '0')
}

/**
 * The receipt as an image to save or send. It prints the period the screen is showing, so a
 * listener who stepped back three weeks gets the receipt for those weeks.
 */
@Composable
internal fun ShareReceiptSheet(
    uiState: AnalyticsUiState,
    accountName: String?,
    seedColor: Color,
    onDismiss: () -> Unit,
) {
    val receipt = listeningReceipt(uiState, accountName)
    ShareImageSheet(
        title = stringResource(Res.string.receipt_share_title),
        seedColor = seedColor,
        fileNamePrefix = "SimpMusic_receipt",
        onDismiss = onDismiss,
    ) { captureModifier ->
        // The shadow sits outside the captured node: it belongs to the sheet, not to the image.
        ListeningReceiptPaper(receipt = receipt, modifier = Modifier.shadow(16.dp).then(captureModifier))
    }
}

/**
 * The receipt's own card on the Analytics page, stacked above Wrapped's in the same shell. Its
 * picture is the real receipt for this period, shrunk and coming out of a dark slot like paper out
 * of a till, so the card shows what a tap prints before anyone taps it.
 */
@Composable
internal fun ReceiptEntryCard(
    uiState: AnalyticsUiState,
    accountName: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val receipt = listeningReceipt(uiState, accountName)
    EntryCard(
        title = stringResource(Res.string.receipt_entry_title),
        subtitle = stringResource(Res.string.receipt_entry_subtitle, receipt.period),
        discIcon = SimpIcons.Share,
        onClick = onClick,
        modifier = modifier,
    ) {
        Box(
            // The miniature is decoration: read out, it would be the whole receipt line by line.
            modifier = Modifier.fillMaxSize().background(SlotColor).clearAndSetSemantics {},
            contentAlignment = Alignment.TopCenter,
        ) {
            ListeningReceiptPaper(
                receipt = receipt,
                // Laid out at full size, then scaled from its top edge: the type, rules and barcode
                // stay the receipt's own instead of a second, simplified drawing of it.
                modifier =
                    Modifier
                        .wrapContentSize(align = Alignment.TopCenter, unbounded = true)
                        .requiredWidth(ShareLyricsCardMaxWidth)
                        .graphicsLayer {
                            scaleX = THUMBNAIL_SCALE
                            scaleY = THUMBNAIL_SCALE
                            transformOrigin = TransformOrigin(0.5f, 0f)
                            translationY = THUMBNAIL_TOP.toPx()
                        },
            )
        }
    }
}

@Composable
private fun listeningReceipt(
    uiState: AnalyticsUiState,
    accountName: String?,
): ListeningReceipt {
    val top = uiState.topTracks.data.orEmpty().take(RECEIPT_LINES)
    val start = uiState.periodStart
    val end = uiState.periodEnd
    // At the present the range's own name says it. Once the navigator has stepped back, "Last 30
    // days" would name the wrong thirty, so the receipt prints the span the navigator shows.
    val period =
        if (uiState.periodOffset == 0 || start == null || end == null) {
            stringResource(uiState.dayRange.labelRes())
        } else {
            formatPeriodSpan(start, end)
        }
    val printedOn = remember { now().date }
    return ListeningReceipt(
        period = period,
        orderNumber = uiState.stats.data?.plays ?: top.sumOf { it.first.playCount.toLong() },
        name = accountName,
        printedOn = printedOn,
        lines = top.map { (played, song) -> song.toReceiptLine(played.playCount) },
        seed = fnv1a(top.joinToString("|") { it.second.title }),
    )
}

private fun SongEntity.toReceiptLine(plays: Int): ReceiptLine =
    ReceiptLine(
        plays = plays,
        item =
            listOf(title.withoutFeaturing(), artistName.orEmpty().joinToString(", "))
                .filter { it.isNotBlank() }
                .joinToString(" - "),
        durationSeconds = durationSeconds,
    )

private val FEATURING = Regex("""\s*[(\[](feat\.?|ft\.?|featuring)\s[^)\]]*[)\]]""", RegexOption.IGNORE_CASE)

/** `Song (feat. A, B & C)` → `Song`: the guest list is what pushes a title past its two rows. */
private fun String.withoutFeaturing(): String = replace(FEATURING, "").trim().ifBlank { this }

/** `3:20`, the price column. A song whose length was never stored prints as dashes. */
private fun receiptClock(seconds: Int): String =
    if (seconds <= 0) "--:--" else "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"

/** `3:17:23` — hours always shown, as the total line of a Receiptify receipt prints them. */
private fun receiptTotal(seconds: Long): String =
    "${seconds / 3600}:${(seconds / 60 % 60).toString().padStart(2, '0')}:${(seconds % 60).toString().padStart(2, '0')}"

/** 32-bit FNV-1a, so one list of songs hashes the same on every device and every run. */
private fun fnv1a(text: String): Int {
    var hash = 0x811C9DC5.toInt()
    for (char in text) {
        hash = (hash xor char.code) * 0x01000193
    }
    return hash
}

/**
 * The barcode's bars as (x, width) in dp: xorshift32 off [seed], bars one to four units wide with
 * a one-unit gap, roughly three in five of them drawn. It is not a real symbology — it only has to
 * look like one, and look the same every time the same songs are printed.
 */
private fun barcodeBars(
    seed: Int,
    width: Int,
): List<Pair<Int, Int>> {
    var state = seed
    fun next(): Float {
        state = state xor (state shl 13)
        state = state xor (state ushr 17)
        state = state xor (state shl 5)
        return (state.toUInt() % 1000u).toInt() / 1000f
    }
    val bars = mutableListOf<Pair<Int, Int>>()
    var x = 0
    while (x < width) {
        val barWidth = 1 + (next() * 3.2f).toInt()
        if (next() > 0.42f) bars += x to barWidth
        x += barWidth + 1
    }
    return bars
}

/** The printed receipt itself — every size here is the approved mock's, in dp and sp. */
@Composable
private fun ListeningReceiptPaper(
    receipt: ListeningReceipt,
    modifier: Modifier = Modifier,
) {
    val scan = imageResource(Res.drawable.receipt_paper)
    val mono =
        TextStyle(
            fontFamily = FontFamily(Font(Res.font.space_mono_regular)),
            fontSize = 16.sp,
            lineHeight = 20.sp,
            color = Ink,
        )
    val order = receipt.orderNumber.toString().padStart(4, '0')
    val date = receipt.printedOn
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .widthIn(max = ShareLyricsCardMaxWidth)
                .paper(scan)
                .padding(start = 20.dp, top = 40.dp, end = 20.dp, bottom = 16.dp),
    ) {
        Text(
            text = "SIMPMUSIC",
            modifier = Modifier.fillMaxWidth(),
            style =
                TextStyle(
                    fontFamily = fontFamily(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 38.sp,
                    lineHeight = 40.sp,
                    color = Ink,
                    textAlign = TextAlign.Center,
                ),
        )
        Text(
            text = receipt.period.uppercase(),
            modifier = Modifier.fillMaxWidth().padding(top = 11.dp),
            style =
                TextStyle(
                    fontFamily = fontFamily(),
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    color = Ink,
                    textAlign = TextAlign.Center,
                ),
        )
        Spacer(modifier = Modifier.height(36.dp))
        val orderLine =
            if (receipt.name != null) {
                stringResource(Res.string.receipt_order, order, receipt.name)
            } else {
                stringResource(Res.string.receipt_order_anonymous, order)
            }
        Text(text = orderLine.uppercase(), style = mono)
        Text(
            text =
                stringResource(
                    Res.string.receipt_date,
                    weekdayFullName(date.dayOfWeek),
                    monthFullName(date.month),
                    date.day,
                    date.year,
                ).uppercase(),
            style = mono,
        )
        DashedRule()
        ReceiptRow(
            qty = stringResource(Res.string.receipt_qty),
            item = stringResource(Res.string.receipt_item),
            amount = stringResource(Res.string.receipt_amount),
            style = mono,
        )
        DashedRule()
        receipt.lines.forEach { line ->
            ReceiptRow(
                qty = line.plays.toString(),
                item = line.item,
                amount = receiptClock(line.durationSeconds),
                style = mono,
            )
        }
        DashedRule()
        SumRow(stringResource(Res.string.receipt_item_count), receipt.itemCount.toString(), mono)
        SumRow(stringResource(Res.string.receipt_total), receiptTotal(receipt.totalSeconds), mono)
        DashedRule()
        Text(
            text = stringResource(Res.string.receipt_card, date.year).uppercase(),
            modifier = Modifier.padding(top = 14.dp),
            style = mono,
        )
        Text(text = stringResource(Res.string.receipt_auth_code, receipt.authCode).uppercase(), style = mono)
        if (receipt.name != null) {
            Text(text = stringResource(Res.string.receipt_cardholder, receipt.name).uppercase(), style = mono)
        }
        Text(
            text = stringResource(Res.string.receipt_thanks).uppercase(),
            modifier = Modifier.fillMaxWidth().padding(top = 26.dp),
            style = mono.copy(textAlign = TextAlign.Center),
        )
        Barcode(
            seed = receipt.seed,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp, bottom = 4.dp),
        )
        Text(
            text = "simpmusic.org",
            modifier = Modifier.fillMaxWidth(),
            style = mono.copy(fontSize = 14.sp, lineHeight = 18.sp, textAlign = TextAlign.Center),
        )
    }
}

/** QTY, ITEM and AMT in fixed columns; the item wraps to two rows and is cut after that. */
@Composable
private fun ReceiptRow(
    qty: String,
    item: String,
    amount: String,
    style: TextStyle,
) {
    Row(modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 4.dp)) {
        Text(text = qty.uppercase(), modifier = Modifier.width(40.dp), style = style, maxLines = 1)
        Text(
            text = item.uppercase(),
            modifier = Modifier.weight(1f),
            style = style,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = amount,
            modifier = Modifier.width(54.dp),
            style = style.copy(textAlign = TextAlign.End),
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun SumRow(
    label: String,
    value: String,
    style: TextStyle,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label.uppercase(), style = style)
        Text(text = value, style = style)
    }
}

@Composable
private fun DashedRule() {
    Canvas(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).height(1.5.dp)) {
        drawLine(
            color = Ink,
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = size.height,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.5.dp.toPx(), 3.dp.toPx())),
        )
    }
}

@Composable
private fun Barcode(
    seed: Int,
    modifier: Modifier = Modifier,
) {
    val bars = remember(seed) { barcodeBars(seed, BARCODE_WIDTH) }
    Canvas(modifier = modifier.size(BARCODE_WIDTH.dp, BARCODE_HEIGHT.dp)) {
        val unit = 1.dp.toPx()
        bars.forEach { (x, barWidth) ->
            drawRect(color = Ink, topLeft = Offset(x * unit, 0f), size = Size(barWidth * unit, size.height))
        }
    }
}

/**
 * The scanned sheet, laid edge to edge across the receipt and repeated down its length, so a long
 * receipt is more paper rather than stretched paper. The flat grey under it shows only until the
 * scan is decoded; the clip keeps the last tile from running past the receipt's bottom edge.
 */
private fun Modifier.paper(scan: ImageBitmap): Modifier =
    drawBehind {
        drawRect(PaperBase)
        val width = size.width.roundToInt()
        val tileHeight = (size.width * scan.height / scan.width).roundToInt().coerceAtLeast(1)
        clipRect {
            var y = 0
            while (y < size.height) {
                drawImage(image = scan, dstOffset = IntOffset(0, y), dstSize = IntSize(width, tileHeight))
                y += tileHeight
            }
        }
    }

private const val RECEIPT_LINES = 10
private const val BARCODE_WIDTH = 210
private const val BARCODE_HEIGHT = 42
private val Ink = Color(0xFF1C1C1C)
private val PaperBase = Color(0xFFE4E4E4)

/** 340dp × 0.17 ≈ 58dp: the slip sits inside the card's 68dp picture with a margin either side. */
private const val THUMBNAIL_SCALE = 0.17f
private val THUMBNAIL_TOP = 8.dp
private val SlotColor = Color(0xFF141414)
