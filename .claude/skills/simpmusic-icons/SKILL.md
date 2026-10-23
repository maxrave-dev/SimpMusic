---
name: simpmusic-icons
description: Add or change an icon in SimpMusic. Every icon is a generated Material Symbols ImageVector under ui/icon, addressed as SimpIcons.<Name> — there is no material-icons dependency and no XML icon drawable. Use when adding a new icon, replacing one, or hitting ImageVector/Painter type errors.
---

# SimpMusic Icons

Icons live in `composeApp/src/commonMain/kotlin/com/maxrave/simpmusic/ui/icon/`, one file per
icon, each declaring `val SimpIcons.<Name>: ImageVector`.

Two things are deliberately absent and must not come back:
- the `material-icons-core` / `material-icons-extended` dependency (`Icons.Rounded.*`)
- XML icon drawables in `composeResources/drawable` used through `painterResource(...)`

## Adding an icon

Google's font service generates the Compose file directly — no conversion tool needed:

```bash
curl -sfL --compressed \
  "https://fonts.gstatic.com/render/v1/Material+Symbols+Rounded/24dp/<symbol_name>.kt?var=opsz,wght,FILL,GRAD,ROND@24,400,1,0,50" \
  -o <PascalName>.kt
```

Keep these axes identical for every icon, or the set stops looking like one set:
**Rounded, opsz 24, wght 400, GRAD 0, ROND 50**, `FILL=1`.

Use `FILL=0` **only** for the "off" half of a state pair — `FavoriteBorder`,
`AddCircleOutline`, `DownloadForOfflineOutlined`. At `FILL=1` the off state renders identically to
the on state and the distinction is lost.

Then edit the downloaded file:

1. `package com.example.test` → `package com.maxrave.simpmusic.ui.icon`
2. `public val <symbol_name>: ImageVector` → `val SimpIcons.<PascalName>: ImageVector`
3. Rename the backing field `_<symbol_name>` → `_<PascalName>`, and `name = "<symbol_name>"` → `"<PascalName>"`
4. Never add `autoMirror = true` to `ImageVector.Builder(...)`: in right-to-left languages only text
   flips, icons keep their shape (owner's rule, 2026-10-14). `ArrowForwardIos`, `QueueMusic`, `Sort`,
   `TrendingUp`, `VolumeOff` and `VolumeUp` had it and drew mirrored until it was removed.
5. Replace the closing `.build()` with `.buildSimpIcon()` (in `SimpIcons.kt`): it is `build()` plus
   the fit into Material's 20×20 live area described under Traps. `SimpIconsFitTest` fails on a
   file that still ends with `.build()`.

## Using an icon

```kotlin
import com.maxrave.simpmusic.ui.icon.PlayArrow   // per-icon import, required
import com.maxrave.simpmusic.ui.icon.SimpIcons

Icon(SimpIcons.PlayArrow, contentDescription = null)
```

The project's own icon buttons take an `ImageVector`:
`RippleIconButton(imageVector = …)`, `LiquidGlassIconButton(imageVector = …)`,
`ActionButton(icon = …)`.

## Traps

**Every icon needs its own import.** `val SimpIcons.X` is an *extension property*; importing the
`SimpIcons` object alone leaves it unresolved. This is the same mechanism that lets R8 strip unused
icons, so never "simplify" the set into a `map` or a `when` — that would ship all of them.

**`ImageVector` is not a `Painter`.** `Icon` and `Image` have overloads for both, so those look
fine. These do not, and need `rememberVectorPainter(SimpIcons.X)`:
- `AsyncImage(placeholder = …, error = …)`
- anything drawn inside a `DrawScope` (`with(painter) { draw(...) }`)
- custom composables whose parameter is typed `Painter`

**Do not text-replace blindly.** A regex swap of `Res.drawable.X` → `SimpIcons.X` produced 40
compile errors in one pass: it also hit `Painter` parameters, and it landed *inside* multi-line
`painterResource(\n  Res.drawable.X,\n)` calls, leaving the syntactically valid but wrong
`painterResource(SimpIcons.X)`.

**Do not replace an icon whose colour carries meaning.** These stay as resources:

| File | Why |
|---|---|
| `baseline_downloaded.xml` | `#FF00A0CB` — the blue *is* the "downloaded" state |
| `baseline_favorite_24.xml` | `#D10000` — the red *is* the "liked" state |
| `mono.xml`, `monochrome.xml` | SimpMusic logos (`#0AEAF8` / black) |
| `app_icon.png`, `circle_app_icon.png`, `holder*.png` | bitmaps, not icons |

If a state needs that colour with a shared symbol, pass it explicitly — e.g. `ActionButton`'s
`iconColor = Color(0xFF00A0CB)` for the downloaded state.

**Verify the symbol name exists** before assuming a mapping, against
`google/material-design-icons` → `variablefont/MaterialSymbolsRounded[FILL,GRAD,opsz,wght].codepoints`.
Legacy names such as `favorite_border`, `add_circle_outline` and `thumb_up_alt` *do* still exist;
`person_add_alt_1` does not (use `person_add`).

**Glyphs that spill past Material's 20×20 live area are fitted into it (2026-10-18).** Material's
icon grid keeps a glyph inside the centred 20×20 square of the 24 box, with 2dp of padding, and
balances the glyphs inside it by keyline shapes of about equal area. 24 of our 111 Symbols spill
into that padding anyway: `Lyrics`' note reaches the box edge, `ThumbsUpDown` spans the
full width. At one size those read bigger than everything beside them. `buildSimpIcon()` measures
how far each glyph reaches from the centre along either axis (walking its path nodes, 16 samples
per curve) and shrinks one that passes 10 of 24 about the centre until it fits: `Lyrics` and
`ThumbsUpDown` ×0.83, the other 21 ×0.86–0.996. A glyph inside the square is left exactly as Google
drew it, nothing is enlarged, and anything it cannot measure (an arc, a group, a fit tighter than
×0.6) keeps the glyph as generated and logs under `SimpIcons`. `SimpIconsFitTest` (jvmTest) fails if
a file skips `buildSimpIcon()`, an icon outside `KEEP_AS_DRAWN` ends up outside the square, or one in
it gets rebuilt.
- `Groups` is named in `KEEP_AS_DRAWN` (in `SimpIcons.kt`) and keeps Google's drawing (2026-10-20).
  It spills only sideways, so fitting its 24 width also cut its height, which was never too tall,
  from 12 to 10 of 24, and it read shorter than every icon beside it. Five more spill only sideways
  and lose a little height to the fit (`PeopleAlt`, `PersonAdd`, `Insights` 9%, `AutoGraph` 6%,
  `AccessAlarm` 2%); they still end 14.5 of 24 tall or more, like Symbols Google draws at that
  height, so they stay fitted (owner's call).
- Do not fit glyphs into a circle instead. That was tried first: a circle judges a glyph by its one
  farthest point, so `Lyrics` shrank to ×0.72 to pull its note's tip inside and then read far
  smaller than `QueueMusic` beside it.

**A small round button cuts a glyph that fills the live area.** `IconButton` clips its content to
its `shape` (a circle by default), and a button sized 24dp makes that a 24dp circle, 12dp from the
centre. A glyph that fills the 20×20 square reaches 14.1 at its corners — `comment` reaches 13.3 —
so its corners are cut while round or open glyphs beside it are not. The icon file is not the
problem; the button is. Check the farthest glyph point before putting an icon in one: Classic's
Comments button clips to 28dp with `requiredSize(28.dp)` inside the same 24dp slot.

**Icons in one row take one size.** Tuning each glyph by eye is what made the Apple Music row read
big-small (23 and 24dp beside 22dp dock buttons, the title-row star 32dp beside 24dp glyphs). Only a
deliberate hierarchy, such as a Play button over Previous/Next, gets a different size.

**The response is gzipped** even when the request asks for `identity` — `curl --compressed`, or
decompress by magic bytes (`\x1f\x8b`) when fetching from a script.
