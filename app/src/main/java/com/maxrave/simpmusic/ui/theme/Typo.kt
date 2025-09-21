package com.maxrave.simpmusic.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontLoadingStrategy
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.maxrave.common.R

val fontFamily =
    FontFamily(
        Font(R.font.poppins_medium, FontWeight.Normal, FontStyle.Normal, FontLoadingStrategy.Async),
    )

val typo =
    Typography(
        /***
         * This typo is use for the title of the Playlist, Artist, Song, Album, etc. in Home, Mood, Genre, Playlist, etc.
         */
        titleSmall =
            TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = fontFamily,
                color = Color(0xFFA8A8A8),
            ),
        titleMedium =
            TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = fontFamily,
                color = Color(0xFFA8A8A8),
            ),
        titleLarge =
            TextStyle(
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = fontFamily,
                color = Color(0xFFA8A8A8),
            ),
        bodySmall =
            TextStyle(
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = fontFamily,
                color = Color(0xFFA8A8A8),
            ),
        bodyMedium =
            TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = fontFamily,
                color = Color(0xFFA8A8A8),
            ),
        bodyLarge =
            TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = fontFamily,
                color = Color(0xFFA8A8A8),
            ),
        displayLarge =
            TextStyle(
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = fontFamily,
                color = Color(0xFFA8A8A8),
            ),
        headlineMedium =
            TextStyle(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = fontFamily,
                color = Color(0xFFA8A8A8),
            ),
        headlineLarge =
            TextStyle(
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = fontFamily,
                color = Color(0xFFA8A8A8),
            ),
        labelMedium =
            TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = fontFamily,
                color = Color(0xFFA8A8A8),
            ),
        labelSmall =
            TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = fontFamily,
                color = Color(0xFFA8A8A8),
            ),
        // ...
    )