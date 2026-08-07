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
4. If the glyph must flip in RTL, add `autoMirror = true,` inside `ImageVector.Builder(...)`
   (currently: `ArrowForwardIos`, `QueueMusic`, `Sort`, `TrendingUp`, `VolumeOff`, `VolumeUp`)

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

**The response is gzipped** even when the request asks for `identity` — `curl --compressed`, or
decompress by magic bytes (`\x1f\x8b`) when fetching from a script.
