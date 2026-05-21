package org.nahoft.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import org.nahoft.nahoft.Persist

/**
 * Handles switching the app's launcher icon and display name.
 *
 * Android does not support in-place icon replacement. Switching works by enabling
 * the new <activity-alias> and disabling the old one via PackageManager. The launcher
 * will briefly remove the old icon and show the new one — timing varies by device.
 *
 * The active identity is persisted in EncryptedSharedPreferences so it survives
 * restarts. It is cleared by clearAllData().
 */
object AppIconManager
{

    /**
     * Returns the currently active identity.
     * Falls back to NAHOFT if the stored value is missing or invalid
     */
    fun getActiveIdentity(): AppIdentity
    {
        val ordinal = Persist.loadIntKey(
            Persist.sharedPrefActiveIdentityKey,
            AppIdentity.NAHOFT.ordinal
        )

        return AppIdentity.entries.toTypedArray().getOrElse(ordinal) { AppIdentity.NAHOFT }
    }

    /**
     * Switches the launcher identity to [newIdentity].
     *
     * Enables the new alias before disabling the old one to prevent a window
     * where no launcher entry exists. DONT_KILL_APP ensures the running app
     * is not affected.
     *
     * No-ops if [newIdentity] is already active.
     */
    fun setActiveIdentity(context: Context, newIdentity: AppIdentity)
    {
        val current = getActiveIdentity()
        if (current == newIdentity) return

        val pm = context.packageManager

        // Enable new alias first
        pm.setComponentEnabledSetting(
            ComponentName(context, newIdentity.componentName),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )

        // Disable the previously active alias
        pm.setComponentEnabledSetting(
            ComponentName(context, current.componentName),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )

        Persist.saveIntKey(Persist.sharedPrefActiveIdentityKey, newIdentity.ordinal)
    }
}