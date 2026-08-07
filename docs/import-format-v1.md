# SimpMusic import format v1

This is the contract between the **SimpMusic web converter** and the **SimpMusic app**.

The web converter takes an export from another service — an Exportify CSV from Spotify, a backup
from another YouTube Music client, and so on — matches every track to a YouTube Music `videoId`,
and emits a single JSON file in the shape below. The app does **no matching of its own**: it parses
the file and writes rows. If a track has no `videoId` by the time the file is written, it must not
appear in the file at all.

The file is produced by the converter at <https://simpmusic.org> and consumed by
`ImportRepository` in the app (see `core/domain/.../repository/ImportRepository.kt`).

## Envelope

```json
{
  "songs": [ ... ],
  "playlists": [ ... ]
}
```

There is deliberately **no** `version` field and **no** `source` field. Do not add envelope fields;
the app parses with `ignoreUnknownKeys = true`, so extra keys are silently dropped rather than
rejected, and a converter that relies on them will fail silently.

Both keys are optional in the parser and default to an empty list, but a file where **both** lists
are empty is rejected by the app as "not a valid import file" — that is what makes picking the
wrong file produce an error instead of a silent "imported 0 songs".

## `songs[]`

`songs` is the deduplicated catalogue: **each `videoId` appears exactly once**. Playlists never
inline a song; they reference it by `videoId`.

```json
{
  "videoId": "dQw4w9WgXcQ",
  "title": "Never Gonna Give You Up",
  "artistName": ["Rick Astley"],
  "artistId": ["UCuAXFkgsw1L7xaCfnd5JJOw"],
  "albumName": "Whenever You Need Somebody",
  "albumId": "MPREb_xxx",
  "duration": "3:33",
  "durationSeconds": 213,
  "isExplicit": false,
  "thumbnails": "https://lh3.googleusercontent.com/...",
  "videoType": "MUSIC_VIDEO_TYPE_ATV"
}
```

| Field | Type | Required | Meaning |
|---|---|---|---|
| `videoId` | string | **yes** | YouTube video id. Primary key of the `song` table. Must be unique across `songs`. |
| `title` | string | **yes** | Track title as it should be shown. |
| `artistName` | string[] | no | Display names, in order. `null`/absent when unknown. |
| `artistId` | string[] | no | YouTube channel ids, **positionally aligned with `artistName`**. See the length rule below. |
| `albumName` | string | no | Album title. Do not emit the literal string `"Album"` — older app builds used it as a placeholder and the app treats it as "no album". |
| `albumId` | string | no | YouTube browse id of the album (`MPREb_…`). |
| `duration` | string | no, defaults `""` | Human-readable duration, `m:ss` or `h:mm:ss`. Shown as-is in the UI. |
| `durationSeconds` | int | no, defaults `0` | Duration in seconds. Used for sorting and for scrobble thresholds. |
| `isExplicit` | bool | no, defaults `false` | Explicit marker. |
| `thumbnails` | string | no | A **single** URL, not a list. Prefer the largest square art available (`w544-h544` for songs). |
| `videoType` | string | no, defaults `""` | YouTube Music video type, e.g. `MUSIC_VIDEO_TYPE_ATV` (audio track) or `MUSIC_VIDEO_TYPE_OMV` (official video). Drives whether the app can play it as video. |

### The `artistId` length rule (important)

`artistId` must either be **absent/`null`**, or have **exactly the same length as `artistName`**.

The app pairs the two lists by index when it converts a stored song back into a track. A non-null
`artistId` that is shorter than `artistName` would read past its end. The app defends itself — it
drops `artistId` entirely whenever the sizes disagree — but that silently loses every channel id
for that track, so the converter should never emit a partial list. If only some artists resolved to
a channel id, emit `artistId: null` and keep the names.

## `playlists[]`

```json
{
  "title": "Chill mix",
  "thumbnail": "https://...",
  "videoIds": ["dQw4w9WgXcQ", "abc123xyz00"]
}
```

| Field | Type | Required | Meaning |
|---|---|---|---|
| `title` | string | **yes** | Playlist name. Created as a **local playlist**, not synced to YouTube. |
| `thumbnail` | string | no | Cover art URL. |
| `videoIds` | string[] | no, defaults `[]` | Track order. Each entry should appear in `songs`. |

Order matters: the app stores the position of each track from the order of this array.

A `videoId` in `videoIds` that has no matching entry in `songs` is **skipped**, and the app reports
how many were skipped at the end of the import. The remaining tracks are then re-numbered from 0 so
positions stay contiguous. Emitting dangling ids is therefore not fatal, but it is a converter bug.

## Caps

The web converter enforces these limits, and the app relies on them by parsing the whole file into
memory in one pass (no streaming decoder):

- **10,000** entries in `songs`
- **500** entries in `playlists`

A file beyond these caps is the converter's problem to reject, not the app's.

## What the app fills in itself

These are **not** in the file, and a converter must not try to supply them:

- `likeStatus` — set to `""`; imported songs are not liked.
- `liked` — `false`.
- `isAvailable` — `true`.
- `category`, `resultType` — `null`; these only mean something for search results.
- `totalPlayTime` — `0`.
- `downloadState` — not downloaded.
- `inLibrary`, `favoriteAt`, `downloadedAt` — set to the moment of import.
- `canvasUrl`, `canvasThumbUrl` — left empty; fetched later from Spotify at playback time.
- Playlist `id` — assigned by the database.
- Playlist `youtubePlaylistId` / sync state — imported playlists are local-only and unsynced.
- Track positions inside a playlist — derived from the order of `videoIds`.

## Write behaviour

- A song whose `videoId` is already in the database is **not overwritten**; the existing row keeps
  its play count, liked state and download state. The import only fills in an album name or artist
  list that the stored row is missing or that an older parse got wrong.
- Playlists are always created fresh. Importing the same file twice creates a second copy of every
  playlist; it does not merge into the first.
