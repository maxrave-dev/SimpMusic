import syncedlyrics

def search_synced_lyrics(query):
    try:
        # This uses the library you added in your build.gradle.kts
        # It returns an LRC formatted string (with timestamps)
        lrc_data = syncedlyrics.search(query)
        return lrc_data if lrc_data else "No lyrics found."
    except Exception as e:
        return f"Error: {str(e)}"
