// e.g., in util/WindowUtils.kt
package org.nahoft.util

import android.view.Window
import android.view.WindowManager
import org.nahoft.nahoft.BuildConfig

/**
 * Applies FLAG_SECURE to prevent screenshots/screen recording.
 * Disabled in debug builds to allow capturing screenshots for documentation.
 */
fun Window.applySecureFlag()
{
    if (!BuildConfig.DEBUG)
    {
        setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
    }
}