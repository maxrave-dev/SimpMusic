@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.maxrave.simpmusic.expect

import platform.CoreGraphics.CGRectMake
import platform.UIKit.*

internal fun activeWindow(): UIWindow? = UIApplication.sharedApplication.connectedScenes
    .filterIsInstance<UIWindowScene>()
    .filter { it.activationState == UISceneActivationStateForegroundActive }
    .flatMap { it.windows.filterIsInstance<UIWindow>() }
    .firstOrNull { it.isKeyWindow() }

internal fun topViewController(): UIViewController? {
    var controller = activeWindow()?.rootViewController ?: return null
    while (true) {
        controller = when {
            controller.presentedViewController != null -> controller.presentedViewController!!
            controller is UINavigationController -> controller.visibleViewController ?: return controller
            controller is UITabBarController -> controller.selectedViewController ?: return controller
            else -> return controller
        }
    }
}

internal fun presentShare(items: List<Any>, title: String): Boolean {
    val presenter = topViewController() ?: return false
    val sheet = UIActivityViewController(items, null)
    sheet.title = title
    sheet.popoverPresentationController?.let {
        it.sourceView = presenter.view
        it.sourceRect = CGRectMake(0.0, 0.0, 1.0, 1.0)
        it.permittedArrowDirections = 0uL
    }
    presenter.presentViewController(sheet, true, null)
    return true
}
