package org.nahoft.util

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.nahoft.nahoft.R

/**
 * Each entry represents one launchable app identity, corresponding to an
 * <activity-alias> in AndroidManifest.xml.
 *
 * [componentName] must match the alias's android:name exactly.
 * [labelRes]      is the localized display name shown under the icon.
 * [iconRes]       the alias icon
 *
 * To add a new identity:
 *   1. Add an entry here
 *   2. Add a matching <activity-alias> in AndroidManifest.xml (enabled="false")
 *   3. Add @mipmap/ic_launcher_X drawable resources
 *   4. Add label string to res/values/strings.xml and res/values-fa/strings.xml
 */
enum class AppIdentity(
    val componentName: String,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
    @DrawableRes val dialogIconRes: Int
) {
    NAHOFT(
        componentName = "org.nahoft.nahoft.activities.NahoftAlias",
        labelRes = R.string.app_identity_label_nahoft,
        iconRes = R.mipmap.ic_launcher,
        dialogIconRes = R.drawable.ic_identity_nahoft
    ),
    BLOOM(
        componentName = "org.nahoft.nahoft.activities.BloomAlias",
        labelRes = R.string.app_identity_label_bloom,
        iconRes = R.mipmap.ic_launcher_bloom,
        dialogIconRes = R.drawable.ic_identity_bloom
    ),
    SLEEPLY(
        componentName = "org.nahoft.nahoft.activities.SleeplyAlias",
        labelRes = R.string.app_identity_label_sleeply,
        iconRes = R.mipmap.ic_launcher_sleeply,
        dialogIconRes = R.drawable.ic_identity_sleeply
    ),
    ROOTEN(
        componentName = "org.nahoft.nahoft.activities.RootenAlias",
        labelRes = R.string.app_identity_label_rooten,
        iconRes = R.mipmap.ic_launcher_rooten,
        dialogIconRes = R.drawable.ic_identity_rooten
    ),
    XO(
        componentName = "org.nahoft.nahoft.activities.XOAlias",
        labelRes = R.string.app_identity_label_xo,
        iconRes = R.mipmap.ic_launcher_xo,
        dialogIconRes = R.drawable.ic_identity_xo
    ),
    DECO(
        componentName = "org.nahoft.nahoft.activities.DecoAlias",
        labelRes = R.string.app_identity_label_deco,
        iconRes = R.mipmap.ic_launcher_deco,
        dialogIconRes = R.drawable.ic_identity_deco
    )
}