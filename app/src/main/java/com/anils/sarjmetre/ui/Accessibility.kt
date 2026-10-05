package com.anils.sarjmetre.ui

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Samsung's "Reduce transparency and blur" accessibility switch. Apple's rule for Liquid Glass is to
 * turn frostier and drop the lensing when people ask for less transparency.
 */
@Composable
fun rememberReduceTransparency(): Boolean {
    val context = LocalContext.current
    return remember {
        runCatching {
            Settings.System.getInt(context.contentResolver, "accessibility_reduce_transparency", 0) == 1
        }.getOrDefault(false)
    }
}
