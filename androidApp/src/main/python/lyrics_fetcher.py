import syncedlyrics

def get_enhanced_lyrics(query):
    # The 'enhanced=True' flag is the secret sauce for word-by-word
    lrc = syncedlyrics.search(query, enhanced=True)
    return lrc if lrc else ""
