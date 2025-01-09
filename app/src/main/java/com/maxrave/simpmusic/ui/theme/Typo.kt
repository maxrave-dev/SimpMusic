package com.maxrave.simpmusic.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontLoadingStrategy
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.maxrave.simpmusic.R

val poppinsFamily =
    FontFamily(
        Font(R.font.poppins_lib, FontWeight.Normal, FontStyle.Normal, FontLoadingStrategy.Async),
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
                fontFamily = poppinsFamily,
            ),
        titleMedium =
            TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = poppinsFamily,
            ),
        titleLarge =
            TextStyle(
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = poppinsFamily,
            ),
        bodySmall =
            TextStyle(
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = poppinsFamily,
            ),
        bodyMedium =
            TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = poppinsFamily,
            ),
        bodyLarge =
            TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = poppinsFamily,
            ),
        displayLarge =
            TextStyle(
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = poppinsFamily,
            ),
        headlineMedium =
            TextStyle(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = poppinsFamily,
            ),
        headlineLarge =
            TextStyle(
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = poppinsFamily,
            ),
        labelMedium =
            TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = poppinsFamily,
            ),
        labelSmall =
            TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = poppinsFamily,
            ),
        // ...
    )