package com.mocharealm.accompanist.lyrics.ui.utils

expect fun Char.isCjk(): Boolean

fun Char.isJapanese(): Boolean {
    return this.code in 0x3040..0x309F || this.code in 0x30A0..0x30FF || this.code in 0xFF66..0xFF9F
}

fun Char.isKorean(): Boolean {
    return this.code in 0xAC00..0xD7AF || this.code in 0x1100..0x11FF
}
expect fun Char.isArabic(): Boolean
expect fun Char.isDevanagari(): Boolean

fun String.isPureCjk(): Boolean {
    val cleanedStr = this.filter { it != ' ' && it != ',' && it != '\n' && it != '\r' }
    if (cleanedStr.isEmpty()) {
        return false
    }
    return cleanedStr.all { it.isCjk() }
}

fun String.containsJapanese(): Boolean = any { it.isJapanese() }

fun String.containsKorean(): Boolean = any { it.isKorean() }

fun String.isRtl(): Boolean {
    return any { it.isArabic() }
}

fun String.isPunctuation(): Boolean {
    return isNotEmpty() && all { char ->
        char.isWhitespace() ||
                char in ".,!?;:\"'()[]{}…—–-、。，！？；：\"\"''（）【】《》～·" ||
                Character.getType(char) in setOf(
            Character.CONNECTOR_PUNCTUATION.toInt(),
            Character.DASH_PUNCTUATION.toInt(),
            Character.END_PUNCTUATION.toInt(),
            Character.FINAL_QUOTE_PUNCTUATION.toInt(),
            Character.INITIAL_QUOTE_PUNCTUATION.toInt(),
            Character.OTHER_PUNCTUATION.toInt(),
            Character.START_PUNCTUATION.toInt()
        )
    }
}