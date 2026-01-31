import syncedlyrics

def search_synced_lyrics(query):
    try:
        lrc_data = syncedlyrics.search(query, enhanced = True)
        return lrc_data if lrc_data else "No lyrics found."
    except Exception as e:
        return f"Error: {str(e)}"
