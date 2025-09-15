package com.maxrave.data.extension

import android.content.Context
import android.graphics.Point

fun getScreenSize(context: Context): Point {
    val x: Int = context.resources.displayMetrics.widthPixels
    val y: Int = context.resources.displayMetrics.heightPixels
    return Point(x, y)
}