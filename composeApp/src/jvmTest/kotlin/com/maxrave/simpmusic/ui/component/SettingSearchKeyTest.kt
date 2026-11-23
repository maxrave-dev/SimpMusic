package com.maxrave.simpmusic.ui.component

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The Settings search must find a row whether or not the user typed its accents. */
class SettingSearchKeyTest {
    @Test
    fun dropsCaseAndAccents() {
        assertEquals("loi bai hat", searchKey("Lời bài hát"))
        assertEquals("dong bo thu vien", searchKey("Đồng bộ thư viện"))
        assertEquals("lyrics timing offset", searchKey("Lyrics timing offset"))
    }

    @Test
    fun unaccentedQueryFindsAccentedRow() {
        assertTrue(searchKey("Âm thanh vòm 8D").contains(searchKey("am thanh")))
        assertTrue(searchKey("Âm thanh vòm 8D").contains(searchKey("ÂM THANH")))
    }
}
