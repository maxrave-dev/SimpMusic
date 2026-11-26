# SimpMusic 2.7.0

## New
- **Radio builder**: Create → Radio lets you pick artists, songs and genres, then tune artist variety, music discovery and filters, and Done starts one radio made from all of them. Signed in, the artists come from your own YouTube Music taste, and a radio you like can be saved with the heart. ([#2664](https://github.com/maxrave-dev/SimpMusic/issues/2664))
- **Replay Mix**: a card in Your library plays the songs you played most this week, then this month. It updates once a day and keeps going as a radio when it runs out. ([#2664](https://github.com/maxrave-dev/SimpMusic/issues/2664))
- **Delete lyrics**: while lyrics are on screen, the song menu offers Delete lyrics. They leave this device and the same copy is not shown again for that song. SimpMusic lyrics are also taken off the server if you contributed them, and voted down if you did not. ([#2959](https://github.com/maxrave-dev/SimpMusic/issues/2959))
- **Animated lyrics background**: two switches under Settings → Interface → Lyrics style give lyrics pages a slowly moving background made from the artwork, which can also pulse with the song's bass. On Android 12 and later, and on Desktop. ([#2962](https://github.com/maxrave-dev/SimpMusic/issues/2962))
- **Search in Settings**: a search field at the top of Settings shows only the settings that match what you type. ([#2960](https://github.com/maxrave-dev/SimpMusic/issues/2960))
- **More AI providers**: Settings → AI now lists OpenRouter, Groq, DeepSeek, Mistral AI, xAI, Together AI, Fireworks AI, Cerebras and Anthropic, and you pick the model from a searchable list of the provider's own models. ([#2942](https://github.com/maxrave-dev/SimpMusic/issues/2942))
- **Choose the app icon** (Android): Settings → Interface → App icon offers Liquid Glass, Old-school, Minimalism Light, Minimalism Dark and Pixel. The app closes when you switch, and opens again from the new icon. ([#2799](https://github.com/maxrave-dev/SimpMusic/issues/2799), [#2723](https://github.com/maxrave-dev/SimpMusic/issues/2723))
- **AMOLED theme**: Settings → Interface → Theme offers AMOLED, a pure black theme, next to System, Dark and Light. ([#2774](https://github.com/maxrave-dev/SimpMusic/issues/2774))
- **Full screen on Desktop**: press F11 to fill the screen and Esc to leave. On macOS the green window button does the same. ([#2864](https://github.com/maxrave-dev/SimpMusic/issues/2864), [#2794](https://github.com/maxrave-dev/SimpMusic/issues/2794), [#2865](https://github.com/maxrave-dev/SimpMusic/issues/2865))
- **Stop scrolling text**: turn off Settings → Interface → Scroll long text and long titles stay still, cut at their edge. ([#196](https://github.com/maxrave-dev/SimpMusic/issues/196), [#2789](https://github.com/maxrave-dev/SimpMusic/issues/2789))

## Improved
- **AutoMix**: the fade now plays over the outgoing song's outro and never starts before the second half of its last drop, so the first half of the drop is always heard to its end. When Tidal has no tempo for a song, the fade length comes from the tempo SimpMusic measures. ([#2966](https://github.com/maxrave-dev/SimpMusic/issues/2966))
- **Android Auto playlists**: playlists, Favorites and Downloaded have a Play all item, a song you tap plays with the rest of its playlist as the queue, and local playlists play in the order you sorted them in the app. ([#2930](https://github.com/maxrave-dev/SimpMusic/issues/2930))
- **Action sheets on liquid glass**: with liquid glass on, song and playlist menus and the other floating sheets sit on glass and turn solid once they fill the screen. They also follow your theme over dark screens such as Now Playing. ([#2961](https://github.com/maxrave-dev/SimpMusic/issues/2961))
- **macOS title bar**: macOS uses its own title bar again, and it turns dark or light with the app theme. ([#2943](https://github.com/maxrave-dev/SimpMusic/issues/2943))
- **Narrow Desktop windows**: Analytics drops to one column and the Search moods grid shows fewer tiles when the panel is narrow, for example with the sidebar and Now Playing open. ([#2964](https://github.com/maxrave-dev/SimpMusic/issues/2964))
- **Equalizer**: the preamp goes down to −30 dB, so music can sit quietly under a game or a stream. ([#2577](https://github.com/maxrave-dev/SimpMusic/issues/2577))

## Fixed
- Android Auto: a crash when playing a song from Home, or from voice and search results. ([#2811](https://github.com/maxrave-dev/SimpMusic/issues/2811), [#2857](https://github.com/maxrave-dev/SimpMusic/issues/2857))
- Android Auto: tapping a song in search results played a different song, or nothing. ([#2445](https://github.com/maxrave-dev/SimpMusic/issues/2445))
- Android Auto: typing a search saved every character as its own entry in your YouTube search history. ([#2295](https://github.com/maxrave-dev/SimpMusic/issues/2295))
- Android Auto: no sound after getting back into the car until you paused and played again. ([#2207](https://github.com/maxrave-dev/SimpMusic/issues/2207))
- Android Auto: Next and Previous acting twice, so Previous in the middle of a song went back one song too many. ([#2968](https://github.com/maxrave-dev/SimpMusic/issues/2968))
- Desktop: Discord login never finishing on Windows and Linux after you authorized it in the browser. ([#2894](https://github.com/maxrave-dev/SimpMusic/issues/2894), [#2919](https://github.com/maxrave-dev/SimpMusic/pull/2919))
- Mix for you loading forever when YouTube refused the request. It now shows the error. ([#2944](https://github.com/maxrave-dev/SimpMusic/issues/2944), [#1688](https://github.com/maxrave-dev/SimpMusic/issues/1688))
- Removing one copy of a song that is in a playlist twice hid both copies, and the song could not be added again. ([#2933](https://github.com/maxrave-dev/SimpMusic/issues/2933))
- Shuffle not working on Liked Music, on playlists opened offline and on playlists whose Shuffle button could not be read. ([#2054](https://github.com/maxrave-dev/SimpMusic/issues/2054), [#2083](https://github.com/maxrave-dev/SimpMusic/issues/2083), [#2814](https://github.com/maxrave-dev/SimpMusic/issues/2814))
- Shuffling a local playlist played one random song, then the rest of the playlist in order. ([#2520](https://github.com/maxrave-dev/SimpMusic/issues/2520))
- Library sync copying playlists that both devices already had to the other side on every pairing. ([#2967](https://github.com/maxrave-dev/SimpMusic/issues/2967))
- AutoMix falling back to a fixed fade because its tempo and key lookups were refused. ([#2965](https://github.com/maxrave-dev/SimpMusic/issues/2965))
- Remixes and live versions getting the original song's lyrics from Spotify and LRCLIB. ([#2963](https://github.com/maxrave-dev/SimpMusic/issues/2963))
- AI lyrics translation failing with OpenRouter, and with Gemini unless you changed the model by hand. ([#2297](https://github.com/maxrave-dev/SimpMusic/issues/2297), [#2684](https://github.com/maxrave-dev/SimpMusic/issues/2684))
