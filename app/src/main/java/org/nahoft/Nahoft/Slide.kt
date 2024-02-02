package org.nahoft.nahoft

import android.content.Context

data class Slide constructor(
    val image: Int,
    val title: String,
    val description: String,
    val skipButtonText: String,
    val fullDescription: String? = null,
    val showButtonAsLink: Boolean = false
)

const val slideNameIntro = "slideIntro"
const val slideNameSetting = "slideSetting"
const val slideNameAboutAndFriends = "slideAboutAndFriends"
const val slideNameContactList = "slideContactList"
const val slideNameChat = "slideChat"
const val slideNameAbout = "slideAbout"

fun getIntroSlides(context: Context): ArrayList<Slide> {
    val arrayList: ArrayList<Slide> = ArrayList()
    arrayList.add(
        Slide(
            image = R.drawable.about_shadow,
            title = context.getString(R.string.nahofts_team),
            description = context.getString(R.string.nahofts_team_description),
            skipButtonText = context.getString(R.string.skip),
            showButtonAsLink = true
        )
    )
    arrayList.add(
        Slide(
            image = R.drawable.safe_shadow,
            title = context.getString(R.string.nahofts_usage),
            description = context.getString(R.string.nahofts_usage_description),
            skipButtonText = context.getString(R.string.skip),
            showButtonAsLink = true
        )
    )
    arrayList.add(
        Slide(
            image = R.drawable.encryption_shadow,
            title = context.getString(R.string.nahofts_features),
            description = context.getString(R.string.nahofts_features_description),
            skipButtonText = context.getString(R.string.get_started)
        )
    )

    return arrayList
}

fun getSettingSlides(context: Context): ArrayList<Slide> {
    val arrayList: ArrayList<Slide> = ArrayList()
    arrayList.add(
        Slide(
            image = R.drawable.ic_nahoft_intro_passcode,
            title = context.getString(R.string.passcode),
            description = context.getString(R.string.passcode_description),
            skipButtonText = context.getString(R.string.close)
        )
    )
    arrayList.add(
        Slide(
            image = R.drawable.ic_nahoft_intro_destruction,
            title = context.getString(R.string.destructionCode),
            description = context.getString(R.string.destruction_code_description),
            skipButtonText = context.getString(R.string.close)
        )
    )
    arrayList.add(
        Slide(
            image = R.drawable.ic_nahoft_intro_logout,
            title = context.getString(R.string.button_label_logout),
            description = context.getString(R.string.logout_description),
            skipButtonText = context.getString(R.string.close)
        )
    )
//    arrayList.add(
//        Slide(
//            image = R.drawable.ic_nahoft_intro_message_bubble,
//            title = context.getString(R.string.use_sms_as_default),
//            description = context.getString(R.string.use_sms_as_default_description),
//            skipButtonText = context.getString(R.string.close),
//            fullDescription = context.getString(R.string.use_sms_as_default_full_description)
//        )
//    )
    arrayList.add(
        Slide(
            image = R.drawable.ic_nahoft_intro_key,
            title = context.getString(R.string.your_public_key),
            description = context.getString(R.string.your_public_key_description),
            skipButtonText = context.getString(R.string.close),
            fullDescription = context.getString(R.string.your_public_key_full_description)
        )
    )
    arrayList.add(
        Slide(
            image = R.drawable.ic_nahoft_intro_about,
            title = context.getString(R.string.about_nahoft),
            description = context.getString(R.string.about_nahoft_description),
            skipButtonText = context.getString(R.string.close),
            fullDescription = context.getString(R.string.about_nahoft_full_description)
        )
    )
    arrayList.add(
        Slide(
            image = R.drawable.ic_nahoft_intro_about,
            title = context.getString(R.string.nahoft_creators),
            description = context.getString(R.string.nahoft_creators_description),
            skipButtonText = context.getString(R.string.close)
        )
    )

    return arrayList
}

fun getAboutAndFriendsSlides(context: Context): ArrayList<Slide> {
    val arrayList: ArrayList<Slide> = ArrayList()
//    arrayList.add(
//        Slide(
//            image = R.drawable.ic_nahoft_intro_about,
//            title = context.getString(R.string.button_label_about),
//            description = context.getString(R.string.what_is_nahoft_description),
//            skipButtonText = context.getString(R.string.close),
//            fullDescription = context.getString(R.string.about_user_guide_english)
//        )
//    )
//    arrayList.add(
//        Slide(
//            image = R.drawable.ic_nahoft_intro_friends,
//            title = context.getString(R.string.friends_list),
//            description = context.getString(R.string.friends_list_description),
//            skipButtonText = context.getString(R.string.close)
//        )
//    )
    arrayList.add(
        Slide(
            image = R.drawable.ic_nahoft_intro_add_user,
            title = context.getString(R.string.add_new_contact),
            description = context.getString(R.string.add_new_friend_description),
            skipButtonText = context.getString(R.string.close)
        )
    )
    arrayList.add(
        Slide(
            image = R.drawable.ic_nahoft_intro_remove_user,
            title = context.getString(R.string.delete_a_contact),
            description = context.getString(R.string.delete_friend_description),
            skipButtonText = context.getString(R.string.close)
        )
    )
    arrayList.add(
        Slide(
            image = R.drawable.ic_nahoft_intro_edit_user,
            title = context.getString(R.string.edit_contact_name),
            description = context.getString(R.string.edit_contact_name_description),
            skipButtonText = context.getString(R.string.close)
        )
    )
    arrayList.add(
        Slide(
            image = R.drawable.status_icon_default,
            title = context.getString(R.string.default_status),
            description = context.getString(R.string.default_status_description),
            skipButtonText = context.getString(R.string.close)
        )
    )
    arrayList.add(
        Slide(
            image = R.drawable.status_icon_requested,
            title = context.getString(R.string.requested_status),
            description = context.getString(R.string.requested_status_description),
            skipButtonText = context.getString(R.string.close)
        )
    )
    arrayList.add(
        Slide(
            image = R.drawable.status_icon_invited,
            title = context.getString(R.string.invited_status),
            description = context.getString(R.string.invited_status_description),
            skipButtonText = context.getString(R.string.close)
        )
    )
    arrayList.add(
        Slide(
            image = R.drawable.status_icon_approved,
            title = context.getString(R.string.approve_status),
            description = context.getString(R.string.approve_status_description),
            skipButtonText = context.getString(R.string.close)
        )
    )
    arrayList.add(
        Slide(
            image = R.drawable.status_icon_verified,
            title = context.getString(R.string.verify_status),
            description = context.getString(R.string.verify_status_description),
            skipButtonText = context.getString(R.string.close),
            fullDescription = context.getString(R.string.verify_status_full_description)
        )
    )

    return arrayList
}

//fun getContactSlides(context: Context): ArrayList<Slide> {
//    val arrayList: ArrayList<Slide> = ArrayList()
//    arrayList.add(
//        Slide(
//            image = R.drawable.ic_nahoft_icons_contact_help,
//            title = context.getString(R.string.contact_list),
//            description = context.getString(R.string.contact_list_description),
//            skipButtonText = context.getString(R.string.close)
//        )
//    )
//
//    return arrayList
//}

fun getChatSlides(context: Context): ArrayList<Slide> {
    val arrayList: ArrayList<Slide> = ArrayList()
    arrayList.add(
        Slide(
            image = R.drawable.ic_nahoft_icons_send_message_help,
            title = context.getString(R.string.manually_send_text),
            description = context.getString(R.string.manually_send_text_description),
            skipButtonText = context.getString(R.string.close),
            fullDescription = context.getString(R.string.manually_send_text_full_description)
        )
    )
    arrayList.add(
        Slide(
            image = R.drawable.ic_nahoft_icons_import_text_help,
            title = context.getString(R.string.manually_import_text),
            description = context.getString(R.string.manually_import_text_description),
            skipButtonText = context.getString(R.string.close),
            fullDescription = context.getString(R.string.manually_import_text_full_description)
        )
    )
//    arrayList.add(
//        Slide(
//            image = R.drawable.ic_nahoft_icons_auto_import_send,
//            title = context.getString(R.string.automatically_import_send_text),
//            description = context.getString(R.string.automatically_import_send_text_description),
//            skipButtonText = context.getString(R.string.close),
//            fullDescription = context.getString(R.string.automatically_import_send_text_full_description)
//        )
//    )
    arrayList.add(
        Slide(
            image = R.drawable.ic_nahoft_icons_share_image_help,
            title = context.getString(R.string.share_image_title),
            description = context.getString(R.string.share_image_description),
            skipButtonText = context.getString(R.string.close),
            fullDescription = context.getString(R.string.share_image_full_description)
        )
    )
    arrayList.add(
        Slide(
            image = R.drawable.ic_nahoft_icons_import_image_help,
            title = context.getString(R.string.import_image_title),
            description = context.getString(R.string.import_image_description),
            skipButtonText = context.getString(R.string.close),
            fullDescription = context.getString(R.string.import_image_full_description)
        )
    )
    arrayList.add(
        Slide(
            image = R.drawable.ic_nahoft_icons_share_image_help,
            title = context.getString(R.string.import_share_title),
            description = context.getString(R.string.import_share_description),
            skipButtonText = context.getString(R.string.close)
        )
    )
    arrayList.add(
        Slide(
            image = R.drawable.ic_nahoft_intro_key,
            title = context.getString(R.string.friends_public_key),
            description = context.getString(R.string.friends_public_key_description),
            skipButtonText = context.getString(R.string.close)
        )
    )

    return arrayList
}

fun getAboutSlides(context: Context): ArrayList<Slide> {
    val arrayList: ArrayList<Slide> = ArrayList()
    arrayList.add(
        Slide(
            image = R.drawable.ic_nahoft_intro_about,
            title = context.getString(R.string.about_nahoft),
            description = context.getString(R.string.about_nahoft_description),
            skipButtonText = context.getString(R.string.close),
            fullDescription = context.getString(R.string.about_nahoft_full_description)
        )
    )
    arrayList.add(
        Slide(
            image = R.drawable.ic_nahoft_intro_about,
            title = context.getString(R.string.nahoft_creators),
            description = context.getString(R.string.nahoft_creators_description),
            skipButtonText = context.getString(R.string.close)
        )
    )

    return  arrayList
}