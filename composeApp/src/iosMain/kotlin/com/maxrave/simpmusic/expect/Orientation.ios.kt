package com.maxrave.simpmusic.expect
import platform.UIKit.*
actual fun currentOrientation(): Orientation = when (activeWindow()?.windowScene?.interfaceOrientation) {
    UIInterfaceOrientationLandscapeLeft, UIInterfaceOrientationLandscapeRight -> Orientation.LANDSCAPE
    UIInterfaceOrientationPortrait, UIInterfaceOrientationPortraitUpsideDown -> Orientation.PORTRAIT
    else -> Orientation.UNSPECIFIED
}
