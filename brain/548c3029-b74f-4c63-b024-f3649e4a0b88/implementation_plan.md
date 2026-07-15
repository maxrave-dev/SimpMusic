# Implementation Plan: Offline Cache Fix & Cached Songs Playlist

This plan addresses three interrelated issues:
1. **API Rate Limiting on Skip:** Skipping songs rapidly triggers API rate limits because the app now fetches a new stream URL even if the song is already cached.
2. **Offline Playback Broken:** When offline, fetching the stream URL fails, so cached songs refuse to play because the code throws an exception instead of falling back to the cache.
3. **New Feature Request:** A "Cached/Offline Songs" playlist that behaves like Spotify's Offline Songs.

## User Review Required
> [!IMPORTANT]
> The offline cache bug was caused by a recent upstream change designed to fix playback for very long tracks (like 1-hour podcasts) that exceed the cache chunk size. By reverting the logic to prioritize the cache *before* making an API call, we prevent rate limits and fix offline play, but long tracks *might* stall after 10MB if you lose connection. We will mitigate this by falling back to the cache if the network fails. Please confirm this trade-off is acceptable!

## Open Questions
- Where exactly should the "Cached Songs" playlist appear? I plan to add it to the Library tab alongside "Downloads", "Favorites", and "Playlists". Does this work for you?

## Proposed Changes

### Media3 Service Layer

#### [MODIFY] Media3ServiceModule.kt
- Modify `provideResolvingDataSourceFactory` in `Media3ServiceModule.kt`. 
- Adjust the logic: If the song is cached (`playerCached` or `downloadCache.isCached`), we will attempt to resolve the URL *but wrap it in a try/catch and timeout*. 
- If resolving fails (due to being offline or rate-limited), and the song is cached, we will simply return the `dataSpec` (which just has the video ID) so the player can still read from the local cache. If resolving succeeds, it will use the real URL for upstream. 
- To fix the skipping issue (making too many API calls), we can skip the URL resolution entirely if the device is explicitly offline, or we can check if the cache covers the *entire* remaining length of the file. Since ExoPlayer's `isCached` only checks `chunkLength`, we will return the cached `dataSpec` immediately if the cache fully covers the request, avoiding the API call.

### Domain & Repository Layer

#### [NEW] CachedSongsRepository.kt
- Create a new repository to query `SimpleCache` (the `PLAYER_CACHE`).
- It will read `playerCache.keys`, filter out video items, and match the keys against the local `SongEntity` database via `SongRepository`.
- This provides a `Flow<List<Track>>` representing all currently cached songs.

### UI & Navigation Layer

#### [MODIFY] LibraryScreen.kt (or similar Library UI)
- Add a new row/button for "Offline / Cached Songs" in the user's library.
- It will display the count of cached songs.

#### [NEW] CachedSongsScreen.kt
- Create a new screen that lists all offline cached songs.
- Implement "Play", "Shuffle", and standard track context menus.
- Clicking a song will play it from the cached playlist context.

#### [MODIFY] AppNavigationGraph.kt & LibraryScreenGraph.kt
- Add routes for navigating to `CachedSongsScreen`.

## Verification Plan

### Manual Verification
- Turn off Wi-Fi/Data and attempt to play a previously cached song. Verify it plays successfully without throwing a stream resolution error.
- Skip rapidly through 10 cached songs. Verify no API errors or rate limits are triggered.
- Open the Library tab, click "Cached Songs", and verify the playlist populates correctly and can be shuffled.
