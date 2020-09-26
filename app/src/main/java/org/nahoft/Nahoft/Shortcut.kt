package org.nahoft.Nahoft

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import androidx.core.content.ContextCompat.getSystemService

const val shortcut_default_id = "id_default_shortcut"
const val shortcut_email_id = "id_email_shortcut"

object Shortcut {

    fun setUp(context: Context) {
        val shortcutManager = getSystemService<ShortcutManager>(context, ShortcutManager::class.java)

        val intents: Array<Intent> = arrayOf(Intent(Intent.ACTION_VIEW, null, context, HomeActivity::class.java ))

        val shortcutDefault = ShortcutInfo.Builder(context, shortcut_default_id)
            .setShortLabel("Default")
            .setLongLabel("Default Shortcut")
            .setIcon(Icon.createWithResource(context, R.drawable.app_shortcut_default))
            .setIntents(intents)
            .build()

        shortcutManager!!.dynamicShortcuts = listOf(shortcutDefault)
    }
}